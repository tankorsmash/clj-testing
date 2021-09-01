(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clj-testing.person :as person]))

(enable-console-print!)

(hiccups/defhtml my-template [link-text]
  [:div]
  [:a {:href "https://github.com/weavejester/hiccup"} link-text])

(def my-doc (js/document.getRootNode))

(def main-doc (js/document.getRootNode))
(def joshua {:person/name "Josh" :person/age 23})
(def sandy {:person/name "Sandy" :person/age 44})
(def matthew {:name "Matt" :age 12})

(def prem_json "{\"name\":\"Prem\",\"age\":40}")
(def prem (.parse js/JSON prem_json))

(def olivia (person/->Person "Olivia"  1234567897 100))

(def generated (s/exercise (s/cat :age :person/age :name :person/name) 2))

(defn get-age [{:person/keys (age)}] age)

(defn handle-person [person]
  (str (person/tryget-person-name person) ": " (person/with-valid-person person get-age)))

(def people [joshua sandy prem matthew olivia {}])

(def to-output (clojure.string/join
                "\n"
                (map handle-person
                     people)))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Generated: " generated)]
        [:span.foobar "classed span"]
        [:div {} "this is a newline"]
        [:pre {:style "font-size: 24px"} to-output]))

(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))

(println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
