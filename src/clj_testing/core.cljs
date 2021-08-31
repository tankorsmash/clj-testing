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

(def main-doc (js/document.getRootNode))
;; (set! (.-innerHTML (js/document.getRootNode)) (render_dom))

;; (def to-output (person :name))

(def joshua {:name "josh" :age 23})

(def test-spec (s/conform even? 1000))

(s/def :person/age #{:age})
(s/def :number/small '(1 2 3 4 5))
(s/def :number/smaller '(1 2))

(def my-spec (s/valid? :number/small joshua))

(def to-output (joshua :age))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Header: " test-spec)]
        [:div {} "ASDSD"]
        (:span {:class "foobar"} "ASSS2dsds")))

(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))

(println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
