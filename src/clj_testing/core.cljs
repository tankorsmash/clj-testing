(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [hiccups.runtime :as hiccupsrt]
            [clojure.test.check.generators]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

;; (use 'hiccup.core)

(enable-console-print!)

(hiccups/defhtml my-template
  [link-text] [:div]
   [:a {:href "https://github.com/weavejester/hiccup"}
    link-text])

;; (defn make_span [text]
;;   (html [:span {:class "foo"} text]))

(def my-doc (js/document.getRootNode))

(def main-doc (js/document.getRootNode))
;; (set! (.-innerHTML (js/document.getRootNode)) (render_dom))

;; (def to-output (person :name))
(def generated (gen/generate (s/gen string?)))

(def joshua {:person/name "Josh" :person/age 23})
(def sandy {:person/name "Sandy" :person/age 23})
(def matthew {:name "Matt" :age 12})


(def test-spec (s/conform even? 1004))

(s/def :person/age int?)
(s/def :person/name string?)
(s/def :person/isValid (s/keys :req [:person/age :person/name]))
(s/def :person/isValidUnq (s/keys :req-un [:person/age :person/name]))
;; (s/def :person/age #{:age})
;; (s/def :number/small '(1 2 3 4 5))
;; (s/def :number/smaller '(1 2))
;;
;; (def my-spec (s/conform :number/small joshua))
(defrecord Person [name phone age])

(def olivia (->Person "Olivia"  1234567897 100))

(defn person-name
  [person]
  {:pre [(s/valid? :person/isValid person)]}
  (str (:person/name person) "---" (:person/age person)))


(defn takes-person [{:keys [:person/name :person/age]}]
  (str name " ++++ " age))

;; (def to-output (s/conform :person/isValid joshua))
;; (def to-output (s/conform :person/isValidUnq matthew))
;; (def to-output (s/explain-str :person/isValidUnq olivia))
;; (def to-output (person-name joshua))
(def to-output (takes-person joshua))
;; (def to-output (person-name olivia))
;; (def to-output (person-name matthew))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Generated: " generated)]
        [:div {} "ASDSD"]
        (:span {:class "foobar"} "ASSS2dsds")))

(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))

(println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
