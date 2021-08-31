(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [hiccups.runtime :as hiccupsrt]))


;; (use 'hiccup.core)

(enable-console-print!)

(hiccups/defhtml my-template []
  [:div
   [:a {:href "https://github.com/weavejester/hiccup"}
    "Hiccup"]])

(defn make_span [text]
  (html [:span {:class "foo"} text]))

(def my-doc (js/document.getRootNode))

(defn hello []
  (html [ :h2 {} "HEADER 2"]
        [:div {} "ASDSD"]
        (make_span "ASSS2")))

;; (def main-doc (js/document))
;; uncomment this to alter the provided "app" DOM element
(set! (.-innerHTML (js/document.getElementById "app")) (hello))
;; (set! (.-innerHTML (js/document.getRootNode)) (hello))

(println (str
            "the doc:\n" (hello)))
