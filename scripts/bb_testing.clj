#!C:\Users\Josh\Documents\cocos_projects\clojure\clojure\bb

(require '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str])


(defn node [& args]
  (apply sh "node" args))


(defn handle-mapper-json [mapper-json]
  (println mapper-json))

(defn main []
  ;; (sh "node" "scripts/mapper_parsing.js" "weaponMapper.js")
  (let [result (node "scripts/mapper_parsing.js" "weaponMapper.js")]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result))
      (println "\nERROR!!!\n\n" result))))

(main)
