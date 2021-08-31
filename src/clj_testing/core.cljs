(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [hiccups.runtime :as hiccupsrt]))



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
  (html [:h2 {} "HEADER 2"]
        [:div {} "ASDSD"]
        (:span {:class "foobar"} "ASSS2dsds")))

(def main-doc (js/document.getRootNode))
;; uncomment this to alter the provided "app" DOM element
(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))
;; (set! (.-innerHTML (js/document.getRootNode)) (render_dom))

(def person {:name "josh" :age 21})
(println (str "|- start output -|\n" (person :name) "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
