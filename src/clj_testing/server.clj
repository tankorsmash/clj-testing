(ns clj-testing.server
  (:require [spec-utils :refer [define-spec def-keys-req def-keys-req-un]]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :refer
             [resource-response content-type not-found]]
            [ring.middleware.json :only [wrap-json-body]]
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

(defn home-page
  [req]
  (some-> (resource-response "index.html" {:root "public"})
          (content-type "text/html; charset=utf-8")))

(defn handler404
  ([]
   {:status 404
    :headers {"Content-Type" "text/html"}
    :body "This is a homemade custom 404 path without args"})
  ([req]
   {:status 404
    :headers {"Content-Type" "text/html"}
    :body "This is a arg-driven custom 404 path"})
  ([req message]
   {:status 404 :headers {"Content-Type" "text/html"} :body message}))

(defonce frame-types-to-filename
         {:weapon "all_weapon_frames.json"
          :armor "all_armor_frames.json"
          :zone "all_zone_frames.json"
          :weapon_category "all_weapon_category_frames.json"
          :attribute "all_attribute_frames.json"
          :battle_text_struct "all_battle_text_struct_frames.json"})

(def root-static-asset-dir
  "C:\\Users\\Josh\\Documents\\cocos_projects\\magnolia_cocos\\Resources\\static_asset_dir")

(defn handler-redirect [req to-uri] {:status 302 :headers {"Location" to-uri}})

(defn handler-ajax
  [req]
  ;; (let [txt (json/read-str (:body (client/get "http://httpbin.org/get")))])
  (let [txt "ASDASD"]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success true :message txt})}))

(defn debug-handler
  [req]
  (let [match (:reitit.core/match req)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success true
                            :message {:match-name (get-in match [:data :name])
                                      :match-path (get-in match [:path])}})}))

(defn default-handler
  [] ;;reitit doesnt give you a request for these for some reason i cant
     ;;possibly understand
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:success true :message "WTFFF"})})

(defn test-handler
  [req]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:success true :message "TEST WTFFF"})})

(defn valid-json-response
  "Returns a valid JSON response.
  Expects a message string, and data of any type"
  ([data]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body (json/write-str
            {:success true :message "Request successful" :data data})})
  ([message data]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body (json/write-str {:success true :message message :data data})}))

(defn read-frames-from-file
  [relative-filename]
  (let [str-frame-data (slurp
                         (str root-static-asset-dir
                              "\\"
                              relative-filename))]
      (json/read-str str-frame-data)))

(defn read-frames-from-frame-type
  [frame-type-kw]
  (read-frames-from-file
    (frame-type-kw frame-types-to-filename)))

(defn get-by-frame-type
  [req]
  (let [match (:reitit.core/match req)
        frame-type (get-in match [:path-params :frame-type])
        frame-type-kw (keyword frame-type)]
    (if-not (contains? frame-types-to-filename frame-type-kw)
      (handler404 req (str "Unknown frame-type: " frame-type))
      (do (let [frame-data (read-frames-from-frame-type frame-type-kw)]
            (valid-json-response frame-data))))))

(defn update-by-frame-type
  [req]
  (let [match (:reitit.core/match req)
        frame-type (get-in match [:path-params :frame-type])
        frame-type-kw (keyword frame-type)]
    (if-not (contains? frame-types-to-filename frame-type-kw)
      (handler404 req (str "Unknown frame-type: " frame-type))
      (do (let [frame-data (read-frames-from-frame-type frame-type-kw)
                post-body (json/read-str (:body req))]
            (do (println "post-map:" post-body)
                (valid-json-response frame-data)))))))

(defn get-single-frame
  [req]
  (let [match (:reitit.core/match req)
        frame-type (get-in match [:path-params :frame-type])
        frame-id (get-in match [:path-params :frame-id])
        frame-type-kw (keyword frame-type)]
    (if-not (contains? frame-types-to-filename frame-type-kw)
      (handler404 req (str "Unknown frame-type: " frame-type))
      (do (println "\n\nTHE TYPE:" (type (:body req)) "\n\n\n")
          (let [frame-data (read-frames-from-frame-type frame-type-kw)]
            (if (zero? (count frame-data))
              (handler404 req (str "No matching frame id of type: " frame-type " for frame-id: " frame-id))
              (if (= (count frame-data) 1)
                (valid-json-response frame-data)
                (handler404 req (str "More than one matching frame of frame-type: " frame-type " for frame-id: " frame-id)))))))))

(defn update-single-frame
  [req]
  (let [match (:reitit.core/match req)
        frame-type (get-in match [:path-params :frame-type])
        frame-type-kw (keyword frame-type)]
    (if-not (contains? frame-types-to-filename frame-type-kw)
      (handler404 req (str "Unknown frame-type: " frame-type))
      (do (let [frame-data (read-frames-from-frame-type frame-type-kw)
                post-body (json/read-str {:key-fn keyword} (:body req))]
            (do (println "post-map:" post-body)
                (valid-json-response frame-data)))))))

(comment
  (def example-post-req
    {:reitit.core/match {:path-params {:frame-type :weapon}}
     :body (json/write-str {:name "josh" :age 21})})

  (:body example-post-req)

  (def qwe
    (update-single-frame example-post-req))
  ,)

(defn frame-ids-match?
  [frame other-frame]
  (= (:frame_id frame) (:frame_id other-frame)))

(defn matching-frame
  [all-frames target-frame]
  (filter #(= (:frame_id %) (:frame_id target-frame)) all-frames))

(defn update-existing-frames
  [all-frames new-frame]
  "takes all-frames and a single new-frame, then either appends it
  or updates the existing frames"
  (let [matching-frames (matching-frame all-frames new-frame)]
    (if (zero? (count matching-frames))
      (apply conj all-frames '(new-frame)) ;;append to list
      (map (fn [existing-frame] ;;update the matching ones
             (if (frame-ids-match? existing-frame new-frame)
               (merge existing-frame new-frame)
               existing-frame))
        all-frames))));;update the matching ones

(defn add-missing-slash
  [uri]
  (let [endswith-slash? (string/ends-with? uri "/")]
    (if (not endswith-slash?) (str uri "/") uri)))

(def inner-handler
    (ring/ring-handler
      (ring/router
        ["/" ::home
         ["api/"
          ["frames/" ["" {:name ::frames-home :get test-handler}]
           [":frame-type/" {:name ::frames-frame-type
                            :get get-by-frame-type
                            :post update-by-frame-type}
              [":frame-id/" {:name ::frames-single-frame
                             :get get-single-frame}]]]]])
      (ring/routes (ring/redirect-trailing-slash-handler)
                   (ring/create-default-handler {:not-found handler404}))))

(def handler
  (ring.middleware.json/wrap-json-body
    inner-handler
    {:keywords? true}))

    

(defn node [& args] (apply sh "node" args))

(defn parse-field
  [spec-ns {:keys [attrName prettyName type] :as field}]
  (let [vvvalidator (match [type]
                      ["string"] #'string?
                      ["string[]"] '(s/coll-of string?)
                      ["number"] #'number?
                      ["number[]"] '(s/coll-of number?)
                      ["enum"] #'number?
                      ["hidden"] #'number?)]
    (let [spec-kw (keyword spec-ns attrName)
          new-def (define-spec spec-kw vvvalidator)]
      (prn "The new spec: " new-def)
      new-def)))

(defn handle-mapper-json
  [mapper-json namespace_]
  (let [parsed-mapper (json/read-str mapper-json :key-fn keyword)
        filename (first (keys parsed-mapper))
        fields (filename parsed-mapper)]
    (println "The filename of the mapped file is:" (name filename))
    (let [field-specs (map (partial parse-field namespace_) (vec fields))]
      (pprint fields)
      (println "Registered count specs:" (count field-specs))
      (println "...and wrapped keys in:"
               (define-spec (keyword namespace_ "frame")
                            (def-keys-req field-specs)))
      (println "...and wrapped keys in un:"
               (define-spec (keyword namespace_ "frame-un")
                            (def-keys-req-un field-specs))))))

(defn register-specs
  [filename namespace_]
  (let [result (node "scripts/mapper_parsing.js" filename)]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result) namespace_)
      (println "\nERROR!!!\n\n" result))))

(comment
  (let [result (node "scripts/mapper_parsing.js" "weaponMapper.js")]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result))
      (println "\nERROR!!!\n\n" result)))

  (do (register-specs "weaponMapper.js" "frame-data.weapon")
      (register-specs "armorMapper.js" "frame-data.armor")
      (register-specs "zoneMapper.js" "frame-data.zone")
      (register-specs "weaponCategoryMapper.js" "frame-data.weapon-category")
      (register-specs "attributeMapper.js" "frame-data.attribute")
      (register-specs "battleTextStructMapper.js"
                      "frame-data.battle-text-struct"))

  (s/describe :frame-data.zone/location_data_names_in_the_zone)
  (s/describe :frame-data.zone/frame)
  (s/describe :frame-data.armor/frame)

  (client/head "http://httpbin.org/get")

  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*))) ;;clean namespace entirely

  (def all-weapon-frames
    [{:bonus_attack 1
      :description ""
      :frame_id 1
      :pretty_name "TEST SPEARss test TEST!"
      :rarity_type 0
      :damage_type 2
      :bonus_encumbrance 4
      :frame_image_path "combined_spear.png"
      :affects_morale false
      :carry_weight 4
      :bonus_power 0
      :battle_row_type 1}
     {:bonus_attack 1
      :description ""
      :frame_id 2
      :pretty_name "TEST WEAPON whose category is without specified attributes"
      :rarity_type 0
      :damage_type 2
      :bonus_encumbrance 4
      :frame_image_path "combined_spear.png"
      :affects_morale false
      :carry_weight 4
      :bonus_power 0
      :battle_row_type 1}
     {:bonus_attack 1
      :description ""
      :frame_id 1000
      :pretty_name "Spear"
      :rarity_type 0
      :damage_type 2
      :bonus_encumbrance 4
      :frame_image_path "combined_spear.png"
      :affects_morale false
      :carry_weight 4
      :bonus_power 0
      :battle_row_type 1}
     {:bonus_attack 1
      :description ""
      :frame_id 1001
      :pretty_name "Shortbow"
      :rarity_type 0
      :damage_type 1
      :bonus_encumbrance 4
      :frame_image_path "combined_shortbow.png"
      :affects_morale false
      :carry_weight 2
      :bonus_power 0
      :battle_row_type 1}
     {:bonus_attack 0
      :description ""
      :frame_id 1002
      :pretty_name "Claw"
      :rarity_type 0
      :damage_type 3
      :bonus_encumbrance 5
      :frame_image_path "combined_claw.png"
      :affects_morale false
      :carry_weight 0
      :bonus_power 1
      :battle_row_type 0}
     {:bonus_attack 1
      :description ""
      :frame_id 1003
      :pretty_name "Flail"
      :rarity_type 0
      :damage_type 3
      :bonus_encumbrance 8
      :frame_image_path "combined_flail.png"
      :affects_morale false
      :carry_weight 10
      :bonus_power 2
      :battle_row_type 0}
     {:bonus_attack -1
      :description ""
      :frame_id 1004
      :pretty_name "Two-Handed Club"
      :rarity_type 0
      :damage_type 2
      :bonus_encumbrance 5
      :frame_image_path "combined_oaken_club.png"
      :affects_morale false
      :carry_weight 4
      :bonus_power 2
      :battle_row_type 0}
     {:bonus_attack 1
      :description ""
      :frame_id 1005
      :pretty_name "Arming Sword (Ulfburt)"
      :rarity_type 1
      :damage_type 3
      :bonus_encumbrance 2
      :frame_image_path "combined_blunt_cutlass.png"
      :affects_morale false
      :carry_weight 3
      :bonus_power 0
      :battle_row_type 0}])
  (def single-zone-frame
    {:name "The Greater Capital Area"
     :data_name "the_greater_capital_area"
     :required_zone_data_name_to_unlock ""
     :location_data_names_in_the_zone ["the_forest" "the_mountains"
                                       "the_plains"]})
  (def invalid-raw-single-zone-frame-json
    "{ \"required_zone_data_name_to_unlock\": \"\", \"location_data_names_in_the_zone\": [ \"the_forest\", \"the_mountains\", \"the_plains\" ] }")
  (def invalid-single-zone-frame
    (json/read-str invalid-raw-single-zone-frame-json :key-fn keyword))

  (s/describe :frame-data.zone/frame-un)
  (s/valid? :frame-data.zone/frame-un single-zone-frame)
  (s/valid? :frame-data.zone/frame-un invalid-single-zone-frame)
  (s/explain-data :frame-data.zone/frame-un invalid-single-zone-frame)
  (s/valid? :frame-data.zone/frame-un {})
  (s/valid? :frame-data.zone/frame-un 0)

  (def new-weapon-frame-to-add {:frame_id 1 :pretty_name "newly added frame"})
  (first (update-existing-frames all-weapon-frames new-weapon-frame-to-add))

  (s/describe my-key)
  (s/describe :frame-data.weapon/affects_morale)
  (s/valid? :frame-data.weapon/frame_id 123)
  (s/valid? :frame-data.weapon/frame_id "ASD")
  (s/def :frame-data.weapon/pretty_name123 string?))
