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


(comment
  (def my-key :frame-data.weapon/poop)
  (def validator string?)
  (define-spec my-key validator)
  (s/def (eval my-key) validator)
  (s/describe my-key)
  (s/describe :frame-data.weapon/poop)
  (s/describe (resolve my-key))
  ,)
