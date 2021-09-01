(ns clj-testing.person
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(s/def :person/age int?)
(s/def :person/name string?)
(s/def :person/isValid (s/keys :req [:person/age :person/name]))
(s/def :person/isValidUnq (s/keys :req-un [:person/age :person/name]))

(defrecord Person [name phone age])

(defn takes-person [{:keys [:person/name :person/age] :as person}]
  {:pre [(s/valid? :person/isValid person)]}
  (str name " ++++ " age " (person: " person ")"))

(defn rec-to-person [person_rec]
  {:pre [instance? Person person_rec]}
  "converts an Person record to a valid person"
  {:person/age person_rec.age :person/name person_rec.name})

(defn unq-to-person [unq_person]
  "converts an unqualified person to a valid person"
  {:pre [s/valid? :person/isValidUnq unq_person]}
  {:person/age (:age unq_person) :person/name (:name unq_person)})

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

(defn tryget-person-name [person]
  "tries real hard to get a person's name for debugging purposes"
  (or (:person/name person) (:name person) person.name "unknown"))

