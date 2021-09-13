(ns clj-testing.server
  (:require
   [clojure.pprint :refer [pprint]]
   [ring.util.response :refer [resource-response content-type not-found]]
   [reitit.core :as r]
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

(defn handler-ajax [req]
  ;; (let [txt (json/read-str (:body (client/get "http://httpbin.org/get")))])
  (let [txt "ASDASD"]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:success true :message txt})}))

(defn debug-handler [req match]
 {:status 200
  :headers {"Content-Type" "application/json"}
  :body (json/write-str {:success true :message
                         {
                          :match-name (get-in match [:data :name])
                          :match-path (get-in match [:path])}})})

(def router
  (r/router
    ["/" ::home
     ["api/"
      ["frames/"
       ["" ::frames-home]
       [":frame-type/" ::frames-frame-type]]]]))


(defn handler [req]
 (let [rs (r/routes router)]
  (let [path (map #(first (vector %)) rs)]
    (pprint path)))

 (let [match (r/match-by-path router (:uri req))
       match-name (get-in match [:data :name])]
  (println "Name:" match-name "-- URI:" (:uri req) " -- MATCH: " match)
  (or
     (if (= match-name ::ajax)
       (handler-ajax req))
     (if (not (nil? match-name))
       (debug-handler req match))
   (handler404 req))))

(comment
  (client/head "http://httpbin.org/get")
  ,)
