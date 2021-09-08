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
            [goog.events :as events])
  (:import [goog History]
           [goog.history EventType]))

(defn unknown-page []
  [:h1 "WHERE THE F ARE YOU?"])

(defn home-page []
  [:h1 "THIS IS HOME"])

(defn user-page []
  [:h2 "THIS IS USER"])

(defonce current-page (r/atom #'unknown-page))

(defroute user-path "/users/:id" {:as params}
  (reset! current-page #'user-page)
  (js/console.log (str "anon user" "User: " (:id params))))

(defroute home-path "/" []
  (reset! current-page #'home-page)
  (js/console.log "You're home-path!"))

(defroute "*" []
  (reset! current-page #'unknown-page)
  (js/console.log "You're in unknown territory!"))

(secretary/set-config! :prefix "#")


(let [h (History.)]
  (goog.events/listen
    h
    EventType.NAVIGATE
    #(secretary/dispatch! (.-token %))) ;; goog.history only supports #token so I'd need a different solution if i wasn't going to have a url embedded
  (doto h
    (.setEnabled true)))

;; (doto (History.)
;;   (events/listen EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
;;   (.setEnabled true))

;; (secretary/dispatch! "/")

