(ns clj-testing.server
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [pprint]]
   [ring.util.response :refer [resource-response content-type not-found]]
   [reitit.ring :as ring]
   [clojure.spec.alpha :as s]
   [clojure.data.json :as json]
   [clj-http.client :as client]))

;; the routes that we want to be resolved to index.html
(def route-set #{"/" "/contact" "/menu" "/about /testme" "/ajax"})

(defn home-page [req]
  (some-> (resource-response "index.html" {:root "public"})
          (content-type "text/html; charset=utf-8")))

(defn handler404 [req]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "This is a homemade custom 404 path"})

(defonce frame-types-to-filename
  {:weapon "all_weapon_frames.json"
   :armor "all_armor_frames.json"
   :zone "all_zone_frames.json"
   :weapon_category "all_weapon_category_frames.json"
   :attribute "all_attribute_frames.json"
   :battle_text_struct "all_battle_text_struct_frames.json"})

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
                         :message "WTFFF"})})

(def router
  (ring/router
    ["/" ::home
     ["api/"
      ["frames/"
       ["" {:name ::frames-home :get test-handler}]
       [":frame-type/" ::frames-frame-type]]]]))



(defn add-missing-slash [uri]
  (let [endswith-slash? (string/ends-with? uri "/")]
    (if (not endswith-slash?)
      (str uri "/")
      uri)))

;; (defn dispatcher [req match]
;;   if )

(defn handler2 [req]
  ;; (let [rs (r/routes router)]
  ;;   (let [path (map #(first (vector %)) rs)]
  ;;     (pprint path)))

 (let [raw-uri (:uri req)
       uri (add-missing-slash raw-uri)
       match (r/match-by-path router uri)
       match-name (get-in match [:data :name])]

   (ring/ring-handler
    (ring/router
      ["/" ::home
       ["api/"
        ["frames/"
         ["" {:name ::frames-home :get test-handler}]
         [":frame-type/" ::frames-frame-type]]]]))))
     
     ;; (cond
     ;;   (not= raw-uri uri) (handler-redirect req uri)
     ;;   (= match-name ::ajax) (handler-ajax req)
     ;;   (not (nil? match-name)) (debug-handler req match)
     ;;   :else (handler404 req))))

(def handler
   (ring/ring-handler
    (ring/router
      ["/" ::home
       ["api/"
        ["frames/"
         ["" {:name ::frames-home :get test-handler}]
         [":frame-type/" {:name ::frames-frame-type :get debug-handler}]]]])

    ;; (default-handler)))
    (constantly {:status 404, :body "sdasd"})))

(comment
  (client/head "http://httpbin.org/get")
  ,)
