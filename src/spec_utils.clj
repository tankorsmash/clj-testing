(ns spec-utils
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

;; (defmacro define-spec [spec-key validator]
;;   (let [kk spec-key
;;         vv validator]
;;     ;; `(s/def ~kk ~vv)))
;;     `(s/def ~spec-key ~validator)))

(defmacro define-spec-inner [raw validator]
  (println raw)
  (let [kk raw]
    `(s/def ~kk validator)))

(defmacro define-spec [spec-key validator]
  `(s/def ~spec-key ~validator))


(def my-key :frame-data.weapon/pretty_name)
(def validator string?)
(define-spec my-key validator)
(s/def (eval my-key) validator)
(s/describe my-key)
(s/describe (resolve my-key))


