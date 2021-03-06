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
            [clojure.spec.test.alpha :as stest]
            [clojure.data.json :as json]
            [cheshire.core :as ches]
            [clj-http.client :as client]
            [clojure.java.shell :refer [sh]]
            [clojure.core.match :refer [match]]
            [jumblerg.middleware.cors :refer [wrap-cors]]))

(declare try-update-existing-frames)
(declare valid-by-frame-type-un?)
(declare explain-str-by-frame-type-un)
(declare explain-data-by-frame-type-un)

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
    :body (str "This is a arg-driven custom 404 path: " (:uri req))})
  ([req message]
   {:status 404 :headers {"Content-Type" "text/html"} :body message}))

(def valid-frame-types
  #{:weapon
    :armor
    :zone
    :weapon_category
    :attribute
    :battle_text_struct})

(defn assert-all-valid-keys? [coll]
  (for [frame-type valid-frame-types]
    (let [valid-key? (contains? coll frame-type)]
      (assert valid-key? (str "Missing keyword: " frame-type))
      valid-key?)))


(def frame-types-to-filename
         {:weapon "all_weapon_frames.json"
          :armor "all_armor_frames.json"
          :zone "all_zone_frames.json"
          :weapon_category "all_weapon_category_frames.json"
          :attribute "all_attribute_frames.json"
          :battle_text_struct "all_battle_text_struct_frames.json"})

(assert-all-valid-keys? frame-types-to-filename)

(defonce frame-types-to-frame-spec
  {:weapon {:req :frame-data.weapon/frame
            :req-un :frame-data.weapon/frame-un}
   :armor {:req :frame-data.armor/frame
           :req-un :frame-data.armor/frame-un}
   :zone {:req :frame-data.zone/frame
          :req-un :frame-data.zone/frame-un}
   :weapon_category_category {:req :frame-data.weapon-category/frame
                              :req-un :frame-data.weapon-category/frame-un}
   :attribute {:req :frame-data.attribute/frame
               :req-un :frame-data.attribute/frame-un}
   :battle_text_struct {:req :frame-data.battle-text-struct/frame
                        :req-un :frame-data.battle-text-struct/frame-un}})

(assert-all-valid-keys? frame-types-to-frame-spec)

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

(defn invalid-json-response
  "Returns a failed JSON response.
  Expects a message string, and data of any type"
  ([data]
   {:status 400
    :headers {"Content-Type" "application/json"}
    :body (json/write-str
            {:success false :message "Request unsuccessful" :data data})})
  ([message data]
   {:status 400
    :headers {"Content-Type" "application/json"}
    :body (json/write-str {:success false :message message :data data})}))

(defn frame-type-to-abs-path [frame-type]
  (str
    root-static-asset-dir
    "\\"
    (frame-type frame-types-to-filename)))

(defn read-frames-from-file
  [relative-filename]
  (let [str-frame-data (slurp
                         (str root-static-asset-dir
                              "\\"
                              relative-filename))]
      (json/read-str str-frame-data :key-fn keyword)))

(defn frame-type-to-toplevel-key [frame-type-kw]
  (keyword (str "all_" (name frame-type-kw) "_frames")))

(defn read-frames-from-frame-type
  [frame-type-kw]
  (let [full-frame-data (read-frames-from-file (frame-type-kw frame-types-to-filename))
        frame-key (frame-type-to-toplevel-key frame-type-kw)
        all-frames (frame-key full-frame-data)]
    (println "found" (count all-frames) " total frames")
    all-frames))

(defn get-by-frame-type
  [req, frame-type-kw]
  (do (let [frame-data (read-frames-from-frame-type frame-type-kw)]
        (valid-json-response frame-data))))

(defn update-by-frame-type
  [req, frame-type-kw]
  (do (let [frame-data (read-frames-from-frame-type frame-type-kw)
            post-body (:body req)]
        (do (println "post-map:" post-body)
            (valid-json-response frame-data)))))

(defn get-single-frame
  [req, frame-type-kw]
  (let [match (:reitit.core/match req)
        frame-id (Integer/parseInt (get-in match [:path-params :frame-id]))]
      (do (let [all-frames (read-frames-from-frame-type frame-type-kw)
                frame-data (filter #(= (:frame_id %) frame-id) all-frames)]
            (if (zero? (count frame-data))
              (handler404 req (str "No matching frame id of type: " (name frame-type-kw) " for frame-id: " frame-id))
              (if (= (count frame-data) 1)
                (valid-json-response frame-data)
                (handler404 req (str "More than one matching frame of frame-type: " (name frame-type-kw) " for frame-id: " frame-id))))))))

(defonce my-pretty-printer
  (ches/create-pretty-printer
          (assoc ches/default-pretty-print-options
                 :indentation "    ")))

(defn write-single-frame-data-to-disk [frame-type frame-data]
  (if-not (zero? (count frame-data))
    (if (s/valid?
          (s/coll-of (:req-un (frame-types-to-frame-spec frame-type)))
          frame-data)
      (let [final-data {(frame-type-to-toplevel-key frame-type) frame-data}]
        (spit
          ;; (frame-type-to-abs-path frame-type)
          "tmp.json"
          (ches/generate-string final-data {:pretty my-pretty-printer}))))))

(defn update-single-frame
  [req, frame-type-kw]
  (let [match (:reitit.core/match req)
        frame-id (get-in match [:path-params :frame-id])]
      (do (let [frame-data (read-frames-from-frame-type frame-type-kw)
                post-body (:body req)]
            (do (println post-body)
                (let [new-data (try-update-existing-frames
                                 frame-type-kw
                                 frame-data post-body)]
                  (if (map? new-data) ;;explain-data returns a map
                    (invalid-json-response "New data didn't conform to spec" new-data)
                    (do (write-single-frame-data-to-disk frame-type-kw new-data)
                        (valid-json-response new-data)))))))))


(defn create-single-frame
  [req, frame-type-kw]
  (let [frame-data (:body req)
        valid-frame? (valid-by-frame-type-un? frame-type-kw frame-data)]
    (if-not valid-frame?
      (invalid-json-response
        "Not a valid frame body"
        (explain-str-by-frame-type-un frame-type-kw frame-data))
      (if (-> (read-frames-from-frame-type frame-type-kw)
              (contains? (:frame_id frame-data)))
        (invalid-json-response (str "frame with frame_id: " (:frame_id frame-data) " already exists") {})
        (do (write-single-frame-data-to-disk frame-type-kw frame-data)
            (valid-json-response
              "The valid frame was successfully created"
              frame-data))))))

(defn with-valid-frame-type
  [req, callback]
  (let [match (:reitit.core/match req)
        frame-type (get-in match [:path-params :frame-type])
        frame-type-kw (keyword frame-type)]
    (if-not (contains? frame-types-to-filename frame-type-kw)
      (handler404 req (str "Unknown frame-type: " frame-type))
      (callback req frame-type-kw))))

(comment
  (def example-post-req
    {:reitit.core/match {:path-params {:frame-type :weapon}}
     ;; :body (json/write-str {:name "josh" :age 21})

     :body {:name "josh" :age 21}
     :headers {"content-type" "application/json"}})

  (:body example-post-req)

  (def example-frame
    (assoc
      (first all-weapon-frames)
      :frame_id
      122))
  (update-existing-frames all-weapon-frames example-frame)

  (def qwe
    (update-single-frame example-post-req)
    (update-single-frame (assoc example-post-req :body (json/write-str
                                                         (first all-weapon-frames)))))
                                                         ;; (assoc
                                                         ;;   (first all-weapon-frames)
                                                         ;;   :frame_id
                                                         ;;   1001
                                                         ;;   :pretty_name
                                                         ;;   "QWEQWE")))))
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
      (conj all-frames new-frame) ;;append to list
      (map (fn [existing-frame] ;;update the matching ones
             (if (frame-ids-match? existing-frame new-frame)
               (merge existing-frame new-frame)
               existing-frame))
        all-frames))));;update the matching ones

(declare explain-by-frame-type-un)
(declare explain-data-by-frame-type-un)
(declare explain-str-by-frame-type-un)

(defn try-update-existing-frames [frame-type all-frames new-frame]
  "makes sure new-frame is valid, and then updates all-frames if it is
  otherwise, it explains the spec failure"
  (if-not (valid-by-frame-type-un? frame-type new-frame)
    (do (explain-by-frame-type-un frame-type new-frame)
        (explain-data-by-frame-type-un frame-type new-frame))
    (update-existing-frames all-frames new-frame)))

(defn valid-by-frame-type-un? [frame-type frame]
  (let [req-un-spec (:req-un (frame-types-to-frame-spec frame-type))]
    (s/valid? req-un-spec frame)))

(defn explain-by-frame-type-un [frame-type frame]
  (let [req-un-spec (:req-un (frame-types-to-frame-spec frame-type))]
    (s/explain req-un-spec frame)))

(defn explain-data-by-frame-type-un [frame-type frame]
  (let [req-un-spec (:req-un (frame-types-to-frame-spec frame-type))]
    (s/explain-data req-un-spec frame)))

(defn explain-str-by-frame-type-un [frame-type frame]
  (let [req-un-spec (:req-un (frame-types-to-frame-spec frame-type))]
    (s/explain-str req-un-spec frame)))

(defn add-missing-slash
  [uri]
  (let [endswith-slash? (string/ends-with? uri "/")]
    (if (not endswith-slash?) (str uri "/") uri)))


(defn hardware-root
  [req]
  (valid-json-response "Hardware root"))


(defn full-keys [my-map]
  (map str (keys my-map)))


(defn hardware-volume
  [{{template :template
     {vol-pct :vol-pct
      :as params} :path-params
     :as match} :reitit.core/match
    :as req}]
  (let [from-python (sh "python39" "./scripts/volume_setter.py" "-v" vol-pct)]
    (valid-json-response from-python)))
  ;; (valid-json-response template)) 

(defn hardware-volume-get
  [{{{vol-pct :vol-pct
      :as params} :path-params
     :as match} :reitit.core/match
    :as req}]
  (let [from-python (sh "python39" "./scripts/volume_setter.py" "-g")]
    (valid-json-response from-python)))
;; (valid-json-response template)) 

(def inner-handler
  (ring/ring-handler
    (ring/router
      ["" ::home
       ["/api"
        ["/frames"
         ["" {:name ::frames-home :get test-handler}]
         ["/:frame-type" {:name ::frames-frame-type
                          :get #(with-valid-frame-type % get-by-frame-type)
                          :post #(with-valid-frame-type % update-by-frame-type)}]
         ["/:frame-type/:frame-id" {:name ::frames-single-frame
                                    :get #(with-valid-frame-type % get-single-frame)
                                    :post #(with-valid-frame-type % update-single-frame)}]]
        ["/create_frame/:frame-type" {:name ::frames-create-frames
                                      :get #(with-valid-frame-type % create-single-frame)
                                      :post #(with-valid-frame-type % create-single-frame)}]
        ["/hardware"
         ["" {:name ::hardware-root
              :get hardware-root}]
         ["/volume/:vol-pct" {:name ::hardware-volume
                              :get hardware-volume}]
         ["/volume-get" {:name ::hardware-volume-get
                         :get hardware-volume-get}]]]])
    (ring/routes (ring/redirect-trailing-slash-handler)
                 (ring/create-default-handler {:not-found handler404}))))

(def handler
  (wrap-cors
    (ring.middleware.json/wrap-json-body
        inner-handler
      {:keywords? true})))



(defn node [& args] (apply sh "node" args))

(defn parse-field
  [spec-ns {:keys [attrName prettyName type] :as field}]
  (let [vvvalidator (match [type]
                      ["string"] #'string?
                      ["string[]"] '(s/coll-of string?)
                      ["number"] #'number?
                      ["number[]"] '(s/coll-of number?)
                      ["boolean"] #'boolean?
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

(do (register-specs "weaponMapper.js" "frame-data.weapon")
    (register-specs "armorMapper.js" "frame-data.armor")
    (register-specs "zoneMapper.js" "frame-data.zone")
    (register-specs "weaponCategoryMapper.js" "frame-data.weapon-category")
    (register-specs "attributeMapper.js" "frame-data.attribute")
    (register-specs "battleTextStructMapper.js"
                    "frame-data.battle-text-struct"))

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

  (def raw-frame-str-data
    "{ \"frame_id\": 1,
        \"pretty_name\": \"TEST SPEARss test TEST!\",
        \"description\": \"\",
        \"frame_image_path\": \"combined_spear.png\",
        \"damage_type\": 2,
        \"bonus_power\": 0,
        \"bonus_attack\": 1,
        \"bonus_encumbrance\": 4,
        \"carry_weight\": 4,
        \"affects_morale\": false,
        \"battle_row_type\": 1,
        \"rarity_type\": 0}")
  (println (json/read-str raw-frame-str-data))
  (println (ches/parse-string raw-frame-str-data))
  ,

  ;; (def all-weapon-frames
  ;;   [{:bonus_attack 1
  ;;     :description ""
  ;;     :frame_id 1
  ;;     :pretty_name "TEST SPEARss test TEST!"
  ;;     :rarity_type 0
  ;;     :damage_type 2
  ;;     :bonus_encumbrance 4
  ;;     :frame_image_path "combined_spear.png"
  ;;     :affects_morale false
  ;;     :carry_weight 4
  ;;     :bonus_power 0
  ;;     :battle_row_type 1}
  ;;    {:bonus_attack 1
  ;;     :description ""
  ;;     :frame_id 2
  ;;     :pretty_name "TEST WEAPON whose category is without specified attributes"
  ;;     :rarity_type 0
  ;;     :damage_type 2
  ;;     :bonus_encumbrance 4
  ;;     :frame_image_path "combined_spear.png"
  ;;     :affects_morale false
  ;;     :carry_weight 4
  ;;     :bonus_power 0
  ;;     :battle_row_type 1}
  ;;    {:bonus_attack 1
  ;;     :description ""
  ;;     :frame_id 1000
  ;;     :pretty_name "Spear"
  ;;     :rarity_type 0
  ;;     :damage_type 2
  ;;     :bonus_encumbrance 4
  ;;     :frame_image_path "combined_spear.png"
  ;;     :affects_morale false
  ;;     :carry_weight 4
  ;;     :bonus_power 0
  ;;     :battle_row_type 1}
  ;;    {:bonus_attack 1
  ;;     :description ""
  ;;     :frame_id 1001
  ;;     :pretty_name "Shortbow"
  ;;     :rarity_type 0
  ;;     :damage_type 1
  ;;     :bonus_encumbrance 4
  ;;     :frame_image_path "combined_shortbow.png"
  ;;     :affects_morale false
  ;;     :carry_weight 2
  ;;     :bonus_power 0
  ;;     :battle_row_type 1}
  ;;    {:bonus_attack 0
  ;;     :description ""
  ;;     :frame_id 1002
  ;;     :pretty_name "Claw"
  ;;     :rarity_type 0
  ;;     :damage_type 3
  ;;     :bonus_encumbrance 5
  ;;     :frame_image_path "combined_claw.png"
  ;;     :affects_morale false
  ;;     :carry_weight 0
  ;;     :bonus_power 1
  ;;     :battle_row_type 0}
  ;;    {:bonus_attack 1
  ;;     :description ""
  ;;     :frame_id 1003
  ;;     :pretty_name "Flail"
  ;;     :rarity_type 0
  ;;     :damage_type 3
  ;;     :bonus_encumbrance 8
  ;;     :frame_image_path "combined_flail.png"
  ;;     :affects_morale false
  ;;     :carry_weight 10
  ;;     :bonus_power 2
  ;;     :battle_row_type 0}
  ;;    {:bonus_attack -1
  ;;     :description ""
  ;;     :frame_id 1004
  ;;     :pretty_name "Two-Handed Club"
  ;;     :rarity_type 0
  ;;     :damage_type 2
  ;;     :bonus_encumbrance 5
  ;;     :frame_image_path "combined_oaken_club.png"
  ;;     :affects_morale false
  ;;     :carry_weight 4
  ;;     :bonus_power 2
  ;;     :battle_row_type 0}
  ;;    {:bonus_attack 1
  ;;     :description ""
  ;;     :frame_id 1005
  ;;     :pretty_name "Arming Sword (Ulfburt)"
  ;;     :rarity_type 1
  ;;     :damage_type 3
  ;;     :bonus_encumbrance 2
  ;;     :frame_image_path "combined_blunt_cutlass.png"
  ;;     :affects_morale false
  ;;     :carry_weight 3
  ;;     :bonus_power 0
  ;;     :battle_row_type 0}])

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
  (s/valid? :frame-data.weapon/frame-un new-weapon-frame-to-add)
  (s/explain :frame-data.weapon/frame-un new-weapon-frame-to-add)

  (valid-by-frame-type-un? :weapon new-weapon-frame-to-add)
  (explain-by-frame-type-un :weapon new-weapon-frame-to-add)
  (first (update-existing-frames all-weapon-frames new-weapon-frame-to-add))
  (try-update-existing-frames :weapon all-weapon-frames new-weapon-frame-to-add)
  (try-update-existing-frames :weapon all-weapon-frames (first all-weapon-frames))

  (s/describe my-key)
  (s/describe :frame-data.weapon/affects_morale)
  (s/valid? :frame-data.weapon/frame_id 123)
  (s/valid? :frame-data.weapon/frame_id "ASD")
  (s/def :frame-data.weapon/pretty_name123 string?)

  (defn my-inc [x] (inc x))
  (s/fdef my-inc
          :args (s/cat :x number?)
          :ret number?)
  (stest/check `my-inc)

  (defn generate-list [num]
    (doall (map my-inc (range num))))
  (s/fdef generate-list ;;idk why this doesnt work
          :args (s/cat :num (s/and
                              number?
                              pos-int?
                              #(<= (:num %) 10)))
          :ret (s/coll-of number?))
  (generate-list 10)
  (stest/check `generate-list)
  (my-inc nil)

  ,)
