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

(declare get-vol)

(defn set-vol [vol-pct]
  (go (let [response (<! (http/get (str "api/hardware/volume/" vol-pct) {}))]
        (let [body (get-in response [:body])]
          (get-vol)))))

(defonce current-vol (r/atom 0))

(defn get-vol []
  (go (let [response (<! (http/get "api/hardware/volume-get" {}))]
        (let [body (get-in response [:body])
              current-vol-from-server (get-in body [:data :out])]
          (reset! current-vol current-vol-from-server)))))

(defn home-page []
    (fn []
      [:div
       [:h1 "THIS IS HOME"]
       [:div "an unset page from router"
        [:div.row.row-cols-auto
         [:div.col
          [:div.btn.btn-outline-secondary
           {:on-click get-vol } "Check volume"]]
         [:div.col
          [:div (str "Current volume: " (* 100 @current-vol) "%")]]]
        [:div.row.row-cols-auto
          [:div.col
           "Set Volume:"]]
        [:div.row.row-cols-auto
         [:div.col
          [:div.btn.btn-outline-secondary {:on-click #(set-vol 0) } "Vol 0%"]]
         [:div.col
          [:div.btn.btn-outline-secondary {:on-click #(set-vol 0.25) } "Vol 25%"]]
         [:div.col
          [:div.btn.btn-outline-secondary {:on-click #(set-vol 0.50) } "Vol 50%"]]
         [:div.col
          [:div.btn.btn-outline-secondary {:on-click #(set-vol 0.75) } "Vol 75%"]]
         [:div.col
          [:div.btn.btn-outline-secondary {:on-click #(set-vol 1.00) } "Vol 100%"]]]]]))

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

