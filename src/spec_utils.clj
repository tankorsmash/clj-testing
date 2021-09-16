(ns spec-utils
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

;; (defmacro define-spec [spec-key validator]
;;   (let [kk spec-key
;;         vv validator]
;;     ;; `(s/def ~kk ~vv)))
;;     `(s/def ~spec-key ~validator)))

(defmacro define-spec-inner [raw validator3]
  ;; (println raw)
  (let [kk raw]
    `(s/def ~kk validator3)))

(defmacro define-spec [spec-key validator2]
  `(eval `(s/def ~~spec-key ~~validator2)))

(defmacro defspec [k spec-form]
  `(s/def-impl ~k (quote ~spec-form) ~spec-form))
(comment

  (defmacro defkeys [req-keys]
    `(s/keys :req ~@req-keys))

  (def my-key :frame-data.weapon/poop)
  (def validator string?)

  (def new-key :test/pee)
  (def form-validator (s/coll-of string?))
  (defspec new-key form-validator)
  (s/describe new-key)
  (s/valid? new-key "ASDASD")
  (s/valid? new-key 123)
  (s/valid? new-key ["asd"])
  (s/valid? new-key ["asd", 123])

  (def required-keys [:frame-data.weapon/pretty_name])
  ;; (s/def ::list-of-keys (s/keys :req-un [:frame-data.weapon/pretty_name]))
  (s/def ::list-of-keys (s/keys :req-un required-keys))
  (s/def ::dyn-list-of-keys (defkeys required-keys))
  (s/describe ::dyn-list-of-keys)
  (s/valid? ::list-of-keys {})
  (s/valid? ::list-of-keys {:pretty_name "asd"})
  (def map-key :test/map)
  (defspec map-key ::list-of-keys)
  (s/valid? map-key {})
  (s/valid? map-key {:pretty_name "ASD"})
  ;; (def map-validator (s/keys))

  (define-spec my-key validator)
  (s/def (eval `my-key) validator)
  (s/describe my-key)
  (s/describe :frame-data.weapon/poop)
  (s/describe (resolve my-key))
  ,)
