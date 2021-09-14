(ns clj-testing.server
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [pprint]]
   [ring.util.response :refer [resource-response content-type not-found]]
   [reitit.ring :as ring]
   [reitit.spec :as rs]
   [reitit.dev.pretty :as pretty]
   [clojure.spec.alpha :as s]
   [clojure.data.json :as json]
   [clj-http.client :as client]))

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

(comment
  (client/head "http://httpbin.org/get")
  ,)
