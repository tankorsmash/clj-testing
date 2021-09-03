(ns clj-testing.person
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test :as ct]
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
  "converts an Person record to a valid person"
  {:pre [instance? Person person_rec]}
  {:person/age person_rec.age :person/name person_rec.name})

(defn unq-to-person [unq_person]
  "converts an unqualified person to a valid person"
  {:pre [s/valid? :person/isValidUnq unq_person]}
  {:person/age (:age unq_person) :person/name (:name unq_person)})

(declare pp-str)
(defn pprint-person [person]
  (pp-str person))

(defn pp-str [text]
  (clojure.string/trim-newline
   (with-out-str (cljs.pprint/pprint text))))

(declare with-valid-person)
(declare to-valid-person)

(def is-verbose-to-valid-person false)

(defn log [msg]
  (when is-verbose-to-valid-person (println msg)))

(def default-valid-person {:person/age 0 :person/name "Mr. Valid B. Empty"})

(defn is-map-true [person]
  (if (s/valid? :person/isValidUnq person)
    (do (log "is isValidUnq")
        (unq-to-person person))
    (do (log (str "is NOT isValidUnq" "Not a valid person because its a map but not a person map: " (pprint-person person)))
        default-valid-person)))
(defn is-map-false [person]
  (if (object? person)
    (do (log "is object")
        (to-valid-person (js->clj person)))
    (do (log "is NOT object...")
        (log (str "Not a valid person, unknown type" (type person) person))
        default-valid-person)))

(defn is-isvalid-false [person]
  (if (instance? Person person)
    (do (log "is Person")
        (rec-to-person person))
    (do (log "is NOT Person")
        (if (map? person)
          (do (log "so its a map???") (is-map-true person))
          (is-map-false person)))))

(defn to-valid-person [person]
  {:post [(clojure.test/is (s/valid? :person/isValid %))]} ;;NOTE this doesnt work because this function also returns the (the_fn person)
  (if (s/valid? :person/isValid person)
    (do (log "is isValid") person)
    (do (log "is NOT isValid") (is-isvalid-false person))))

(defn with-valid-person
  "calls `(the_fn person)` if its a valid person, Person, or JSON Person"
  [person the_fn]
  (log (str "with-valid-person with :" person))
  (let [valid-person (to-valid-person person)]
    (the_fn valid-person)))

(defn tryget-person-name [person]
  {:doc "tries real hard to get a person's name for debugging purposes"}
  (or (:person/name person) (:name person) person.name "unknown"))

(defn tryget-person-age [person]
  "tries real hard to get a person's age for debugging purposes"
  (or (:person/age person) (:age person) person.age "unknown"))

;; (ct/deftest person-test
;;   (let [age 20] (ct/is( = 1 age))))

(ct/deftest test-to-valid-person
  (let [valid-person {:person/age 21 :person/name "Mr Tested Name"}
        map-person {:age 35 :name 452}
        empty-map-person {}]
    (ct/is (= valid-person (to-valid-person valid-person)))
    (ct/is (s/valid? :person/isValid (to-valid-person valid-person)))

      ;;conversions
    (ct/is (s/valid? :person/isValid (to-valid-person map-person)))
    (ct/is (= default-valid-person (to-valid-person empty-map-person)))
    (ct/is (= default-valid-person (to-valid-person 123)))))

(def results (ct/run-tests))
