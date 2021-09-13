(ns clj-testing.server
  (:require
   [ring.util.response :refer [resource-response content-type not-found]]
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
   :body "This is a homemade custom path"})

(def text-content
  "LOADING 2sdasda s ")

(defn handler-ajax [req]
  (let [txt (slurp "C:\\Users\\Josh\\temp.txt")]
  ;; (let [txt text-content]
    {:status 200
     :headers {"Content-Type" "application/json"}
     ;; :body "{'success': true, 'message': 'THIS WAS A SUCCESS!'}"
     :body (json/write-str {:success true :message txt})}))


(defn handler [req]
  (or
   (when (route-set (:uri req))
     (if (= (:uri req) "/ajax")
       (handler-ajax req)
       (home-page req)))
   (handler404 req)))
   ;; (home-page req)))

(comment
  (client/head "http://httpbin.org/get")
  ,)
