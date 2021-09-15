(ns clj-testing.server
  (:require
   [spec-utils :refer [define-spec]]
   [clojure.string :as string]
   [clojure.pprint :refer [pprint]]
   [ring.util.response :refer [resource-response content-type not-found]]
   [reitit.ring :as ring]
   [reitit.spec :as rs]
   [reitit.dev.pretty :as pretty]
   [clojure.spec.alpha :as s]
   [clojure.data.json :as json]
   [clj-http.client :as client]
   [clojure.java.shell :refer [sh]]
   [clojure.core.match :refer [match]]))

;; the routes that we want to be resolved to index.html
(def route-set #{"/" "/contact" "/menu" "/about /testme" "/ajax"})

(defn home-page [req]
  (some-> (resource-response "index.html" {:root "public"})
          (content-type "text/html; charset=utf-8")))

(defn handler404
  ([] {:status 404
       :headers {"Content-Type" "text/html"}
       :body "This is a homemade custom 404 path without args"})
  ([req] {:status 404
          :headers {"Content-Type" "text/html"}
          :body "This is a arg-driven custom 404 path"})
  ([req message] {:status 404
                  :headers {"Content-Type" "text/html"}
                  :body message}))

(defonce frame-types-to-filename
  {:weapon "all_weapon_frames.json"
   :armor "all_armor_frames.json"
   :zone "all_zone_frames.json"
   :weapon_category "all_weapon_category_frames.json"
   :attribute "all_attribute_frames.json"
   :battle_text_struct "all_battle_text_struct_frames.json"})


(def root-static-asset-dir
    "C:\\Users\\Josh\\Documents\\cocos_projects\\magnolia_cocos\\Resources\\static_asset_dir")

(defn handler-redirect [req to-uri]
 {:status 302
  :headers {"Location" to-uri}})

(defn handler-ajax [req]
  ;; (let [txt (json/read-str (:body (client/get "http://httpbin.org/get")))])
  (let [txt "ASDASD"]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success true :message txt})}))

(defn debug-handler [req]
  (let [match (:reitit.core/match req)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success true
                            :message {:match-name (get-in match [:data :name])
                                      :match-path (get-in match [:path])}})}))


(defn default-handler [] ;;reitit doesnt give you a request for these for some reason i cant possibly understand
 {:status 200
  :headers {"Content-Type" "application/json"}
  :body (json/write-str {:success true
                         :message "WTFFF"})})

(defn test-handler [req]
 {:status 200
  :headers {"Content-Type" "application/json"}
  :body (json/write-str {:success true
                         :message "TEST WTFFF"})})

(defn valid-json-response
  "Returns a valid JSON response.
  Expects a message string, and data of any type"
  ([data]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body (json/write-str {:success true
                           :message "Request successful"
                           :data data})})
  ([message data]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body (json/write-str {:success true
                           :message message
                           :data data})}))

(defn get-by-frame-type [req]
  (let [match (:reitit.core/match req)
        frame-type (get-in match [:path-params :frame-type])]
    (if-not (contains? frame-types-to-filename (keyword frame-type))
      (handler404 req (str "Unknown frame-type: " frame-type))
      (do
        (let [str-frame-data (slurp
                               (str root-static-asset-dir
                                    "\\"
                                    ((keyword frame-type) frame-types-to-filename)))
              frame-data (json/read-str str-frame-data)]
          (valid-json-response  frame-data))))))


(defn frame-ids-match? [frame other-frame]
  (= (:frame_id frame)
     (:frame_id other-frame)))

(defn matching-frame [all-frames target-frame]
  (filter #(= (:frame_id %)
              (:frame_id target-frame))
          all-frames))

(defn update-existing-frames [all-frames new-frame]
  "takes all-frames and a single new-frame, then either appends it
  or updates the existing frames"
  (let [matching-frames (matching-frame all-frames new-frame)]
    (if (zero? (count matching-frames))
      (apply conj all-frames '(new-frame)) ;;append to list
      (map (fn [existing-frame] ;;update the matching ones
             (if (frame-ids-match? existing-frame new-frame)
               (merge existing-frame new-frame)
               existing-frame))
           all-frames)))) ;;update the matching ones

(defn add-missing-slash [uri]
  (let [endswith-slash? (string/ends-with? uri "/")]
    (if (not endswith-slash?)
      (str uri "/")
      uri)))

(def handler
   (ring/ring-handler
    (ring/router
      ["/" ::home
       ["api/"
        ["frames/"
         ["" {:name ::frames-home :get test-handler}]
         [":frame-type/" {:name ::frames-frame-type :get get-by-frame-type}]]]])

    (ring/routes
      (ring/redirect-trailing-slash-handler)
      (ring/create-default-handler
        {:not-found handler404}))))


(defn node [& args]
  (apply sh "node" args))



;; (defmacro define-spec [spec-key validator]
;;   '(s/def @spec-key `validator))

(def my-key :frame-data.weapon/frame_id)
(def validator string?)
(define-spec my-key validator)
(eval `(s/def ~my-key validator))
(s/describe my-key)
(s/describe :frame-data.weapon/frame_id)
(s/valid? :frame-data.weapon/frame_id 123)
(s/valid? :frame-data.weapon/frame_id "ASD")

(s/def :frame-data.weapon/pretty_name123 string?)

(defn parse-field [spec-ns
                   {:keys [attrName prettyName type] :as field}]
  (let [validator (match [type]
                        ["string"] #'string?
                        ["number"] #'number?
                        ["enum"] #'number?
                        ["hidden"] #'number?)]
    (def validator validator)
    (let [spec-kw (keyword spec-ns attrName)
          new-def (define-spec spec-kw validator)]
      (println "registered:" new-def)
      (println (s/valid? new-def 10))
      (prn new-def))))
        ;; (println attrName " - " prettyName " <> " type)
        ;; (prn field))

(defn handle-mapper-json [mapper-json]
  (let [parsed-mapper (json/read-str mapper-json :key-fn keyword)
        filename (first (keys parsed-mapper))
        fields (filename parsed-mapper)]
    (println "The filename of the mapped file is:" (name filename))
    (map (partial parse-field "frame-data.weapon") (vec fields))))


(comment
  (let [result (node "scripts/mapper_parsing.js" "weaponMapper.js")]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result))
      (println "\nERROR!!!\n\n" result)))
  (client/head "http://httpbin.org/get")
  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*))) ;;clean namespace entirely

  (def raw-json
        "[{\"damage_type\":2,\"bonus_attack\":1,\"frame_id\":1,\"pretty_name\":\"TEST SPEARss test TEST!\",\"bonus_power\":0,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":1,\"carry_weight\":4,\"frame_image_path\":\"combined_spear.png\",\"description\":\"\",\"bonus_encumbrance\":4},{\"damage_type\":2,\"bonus_attack\":1,\"frame_id\":2,\"pretty_name\":\"TEST WEAPON whose category is without specified attributes\",\"bonus_power\":0,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":1,\"carry_weight\":4,\"frame_image_path\":\"combined_spear.png\",\"description\":\"\",\"bonus_encumbrance\":4},{\"damage_type\":2,\"bonus_attack\":1,\"frame_id\":1000,\"pretty_name\":\"Spear\",\"bonus_power\":0,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":1,\"carry_weight\":4,\"frame_image_path\":\"combined_spear.png\",\"description\":\"\",\"bonus_encumbrance\":4},{\"damage_type\":1,\"bonus_attack\":1,\"frame_id\":1001,\"pretty_name\":\"Shortbow\",\"bonus_power\":0,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":1,\"carry_weight\":2,\"frame_image_path\":\"combined_shortbow.png\",\"description\":\"\",\"bonus_encumbrance\":4},{\"damage_type\":3,\"bonus_attack\":0,\"frame_id\":1002,\"pretty_name\":\"Claw\",\"bonus_power\":1,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":0,\"carry_weight\":0,\"frame_image_path\":\"combined_claw.png\",\"description\":\"\",\"bonus_encumbrance\":5},{\"damage_type\":3,\"bonus_attack\":1,\"frame_id\":1003,\"pretty_name\":\"Flail\",\"bonus_power\":2,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":0,\"carry_weight\":10,\"frame_image_path\":\"combined_flail.png\",\"description\":\"\",\"bonus_encumbrance\":8},{\"damage_type\":2,\"bonus_attack\":-1,\"frame_id\":1004,\"pretty_name\":\"Two-Handed Club\",\"bonus_power\":2,\"rarity_type\":0,\"affects_morale\":false,\"battle_row_type\":0,\"carry_weight\":4,\"frame_image_path\":\"combined_oaken_club.png\",\"description\":\"\",\"bonus_encumbrance\":5},{\"damage_type\":3,\"bonus_attack\":1,\"frame_id\":1005,\"pretty_name\":\"Arming Sword (Ulfburt)\",\"bonus_power\":0,\"rarity_type\":1,\"affects_morale\":false,\"battle_row_type\":0,\"carry_weight\":3,\"frame_image_path\":\"combined_blunt_cutlass.png\",\"description\":\"\",\"bonus_encumbrance\":2}]")

  (def all-weapon-frames
    (json/read-str raw-json :key-fn keyword))

  (def new-weapon-frame-to-add
    {:frame_id 1
     :pretty_name "newly added frame"})

  (first (update-existing-frames all-weapon-frames new-weapon-frame-to-add))


  ,)
