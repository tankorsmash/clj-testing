(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

;; (use 'hiccup.core)

(def root_json_server_url "http://localhost:5021/")
(def hero_stats_url (str root_json_server_url "open_dota_player_data"))

(def resp
  (go (let [response (<! (http/get hero_stats_url {}))]
        (prn (str "status: " (:status response)))
        (pprint (get-in response [:body])))))

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
(def sandy {:person/name "Sandy" :person/age 44})
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

(defn rec-to-person [person_rec]
  "converts an Person record to a valid person"
  {:person/age person_rec.age :person/name person_rec.name})
(defn unq-to-person [unq_person]
  "converts an unqualified person to a valid person"
  {:person/age (:age unq_person) :person/name (:name unq_person)})

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

(defn get-age [{:person/keys (age)}] age)

(defn with-valid-person [person fn]
  "calls `(fn person)` if its a valid person, Person, or JSON Person"
  (if (s/valid? :person/isValid person)
    (fn person)
    (if (instance? Person person)
      (fn (rec-to-person person))
      (if (map? person)
        (if (s/valid? :person/isValidUnq person)
          (fn (unq-to-person person))
          (str
           "Not a valid person, 'map' "
           (clojure.string/trim-newline (with-out-str (cljs.pprint/pprint person)))))
        (if (object? person)
          (str "Not a valid person, 'JS Object' " (js->clj person))
          (str (str "Not a valid person, unknown type" (type person)) person))))))

(defn try-person-name [person]
  (or (:person/name person) (:name person) person.name "unknown"))

(defn handle-person [person]
  (str (try-person-name person) ": " (with-valid-person person get-age)))

(def to-output (clojure.string/join
                "\n"
                (map handle-person
                     [joshua sandy prem matthew olivia {}])))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Generated: " generated)]
        [:span.foobar "classed span"]
        [:div {} "this is a newline"]
        [:pre {:style "font-size: 24px"} to-output]))

(set! (.-innerHTML (js/document.getElementById "app")) (render_dom))

(println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
