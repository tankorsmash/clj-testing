(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint]
            [clojure.test.check]
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

(def generated (s/exercise (s/cat :age :person/age :name :person/name) 2))

(def prem_json "{\"name\":\"Prem\",\"age\":40}")
(def prem (.parse js/JSON prem_json))

(def olivia (->Person "Olivia"  1234567897 100))

;; (defn person-name
;;   [person]
;;   {:pre [(s/valid? :person/isValid person)]}
;;   (str (:person/name person) "---" (:person/age person)))

(defn takes-person [{:keys [:person/name :person/age] :as person}]
  {:pre [(s/valid? :person/isValid person)]}
  (str name " ++++ " age " (person: " person ")"))

(defn with-valid-person [person fn]
  (if (s/valid? :person/isValid person)
    (fn person)
    (if (map? person)
      (str
       "Not a valid person map: "
       (clojure.string/trim-newline (with-out-str (cljs.pprint/pprint person))))
      (if (object? person)
        (str "Not a valid person, see console " (js/console.table person))
        (str (str "Not a valid person, unknown type" (type person)) person)))))

(def to-output (clojure.string/join
                "\n"
                (map #(with-valid-person % takes-person)
                     [joshua prem matthew olivia])))
                      ;; [olivia])))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Generated: " generated)]
        [:span.foobar "classed span"]
        [:div {} "this is a newline"]
        [:pre {:style "font-size: 24px"} to-output]))

(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))

(println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
