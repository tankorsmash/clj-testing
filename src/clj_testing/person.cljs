(ns clj-testing.person
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test]
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

(defn pprint-person [person]
  (clojure.string/trim-newline
   (with-out-str (cljs.pprint/pprint person))))


(declare with-valid-person)


(defn is-map-true [person the_fn]
  (if (s/valid? :person/isValidUnq person)
      (do (println "is isValidUnq") (the_fn (unq-to-person person)))
      (do (println "is NOT isValidUnq")
          (str "Not a valid person because its a map but not a person map: " (pprint-person person))
          {:person/age 0 :person/name ""})))
(defn is-map-false [person the_fn]
  (if (object? person)
      (do (println "is object") (with-valid-person (js->clj person) the_fn))
      (do (println "is NOT object") (str (str "Not a valid person, unknown type" (type person)) person))))

(defn is-isvalid-false [person the_fn]
  (if (instance? Person person)
      (do (println "is Person") (the_fn (rec-to-person person)))
      (do (println "is NOT Person") (if (map? person)
                                        (do (println "so its a map???") ( is-map-true person the_fn))
                                        (is-map-false person the_fn)))))

(defn to-valid-person [person]
   {:post [(clojure.test/is (s/valid? :person/isValid %))]}) ;;NOTE this doesnt work because this function also returns the (the_fn person)

(defn with-valid-person
  [person the_fn]
  ;; (let [valid-person (to-valid-person person)]
  ;;   (the_fn valid-person))
  {:doc "calls `(the_fn person)` if its a valid person, Person, or JSON Person"}
   ;; :post [(clojure.test/is (s/valid? :person/isValid %))]} ;;NOTE this doesnt work because this function also returns the (the_fn person)
  (println "with-valid-person with :" person)
  (if (s/valid? :person/isValid person)
      (do (println "is isValid") (the_fn person))
      (do (println "is NOT isValid") (is-isvalid-false person the_fn))))

(defn tryget-person-name [person]
  {:doc "tries real hard to get a person's name for debugging purposes"}
  (or (:person/name person) (:name person) person.name "unknown"))

(defn tryget-person-age [person]
  "tries real hard to get a person's age for debugging purposes"
  (or (:person/age person) (:age person) person.age "unknown"))

