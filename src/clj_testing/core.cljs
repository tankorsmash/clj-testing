(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [hiccups.runtime :as hiccupsrt]
           [clojure.spec.alpha :as s]))



;; (use 'hiccup.core)

(enable-console-print!)

(hiccups/defhtml my-template []
  [:div
   [:a {:href "https://github.com/weavejester/hiccup"}
    "Hiccup"]])

;; (defn make_span [text]
;;   (html [:span {:class "foo"} text]))

(def my-doc (js/document.getRootNode))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} "head 4"]
        [:div {} "ASDSD"]
        (:span {:class "foobar"} "ASSS2dsds")))

(def main-doc (js/document.getRootNode))
;; uncomment this to alter the provided "app" DOM element
(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))
;; (set! (.-innerHTML (js/document.getRootNode)) (render_dom))

;; (def to-output (person :name))

(def person {:name "josh" :age 21})

(def to-output (person :age))

(println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
