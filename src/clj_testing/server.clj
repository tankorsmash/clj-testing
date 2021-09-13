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

(defn handler-ajax [req]
  (let [txt (json/read-str (:body (client/get "http://httpbin.org/get")))]
    {:status 200
     :headers {"Content-Type" "application/json"}
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
