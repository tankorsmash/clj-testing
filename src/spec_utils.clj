(ns
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))


(defmacro define-spec [spec-key validator]
  '(s/def @spec-key `validator))

