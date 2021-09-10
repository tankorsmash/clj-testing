(ns clj-testing.router
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-testing.logging :refer [log info]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.core :as r]
            ;; [reagent.dom :as rdom]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]

            [clj-testing.open-dota :as dota]
            [clj-testing.frame-data :as frame-data])

  (:import [goog.history Html5History]
  ;; (:import [goog History]
           [goog.history EventType]))

(secretary/set-config! :prefix "#")

(defn unknown-page []
  [:h1 "WHERE THE F ARE YOU?"])

(defn home-page []
  [:div
   [:h1 "THIS IS HOME"]
   [:div "an unset page from router"]])

(defn dota-page []
  [dota/render-user-data dota/user-data])

(defn dota-user-page []
  [dota/render-user-data dota/user-data])

(defn dota-hero-stats-page []
  [dota/render-hero-stats dota/all-hero-stats])

(defn frame-data-root []
  [frame-data/render-root])

(defn user-page []
  [:h2 "THIS IS USER"])

(defonce current-page (r/atom #'unknown-page))

(defroute user-path "/users/:id" {:as params}
  (reset! current-page #'user-page))

(defroute home-path "/" []
  (reset! current-page #'home-page))

(defroute dota-path "/dota" []
  (reset! current-page #'dota-page))

(defroute dota-user-path "/dota/user" []
  (reset! current-page #'dota-user-page))

(defroute dota-hero-stats-path "/dota/hero-stats" []
  (reset! current-page #'dota-hero-stats-page))

(defroute frame-data-root-path "/frame_data" []
  (reset! current-page #'frame-data-root))

(defroute "*" []
  (reset! current-page #'unknown-page)
  (js/console.log "You're in unknown territory!"))

(def root-nav-items
  [{:path (home-path)
    :text "Home"}
   {:path (user-path {:id 123})
    :text "Users"}
   {:path (dota-user-path)
    :text "Dota User"}
   {:path (dota-hero-stats-path)
    :text "Dota Hero Stats"}
   {:path (frame-data-root-path)
    :text "Frame Data"}])

(let [h (Html5History.)]
;; (let [h (History.)]
  (goog.events/listen
   h
   EventType.NAVIGATE
   #(secretary/dispatch! (.-token %))) ;; goog.history only supports #token so I'd need a different solution if i wasn't going to have a url embedded
  (doto h
    (.setEnabled true)))

(comment
  (def hash js/location.hash)
  (log hash)
  (log (str (dota-user-path) " vs hash: " hash))
  (println (nil? (secretary/route-matches (dota-user-path) js/location.hash)))
  (println (nil? (secretary/route-matches (dota-user-path) "ASDASD"))))

;; (doto (History.)
;;   (events/listen EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
;;   (.setEnabled true))

;; (secretary/dispatch! "/")

