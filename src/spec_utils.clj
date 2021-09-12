(ns utils
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))


(defmacro define-spec [spec-key validator]
  (let [kk spec-key
        vv validator]
    `(s/def ~kk ~vv)))

