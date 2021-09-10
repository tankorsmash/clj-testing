#!C:\Users\Josh\Documents\cocos_projects\clojure\clojure\bb

(require '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str]
         '[cheshire.core :refer :all])


(defn node [& args]
  (apply sh "node" args))


(defn handle-mapper-json [mapper-json]
  (let [parsed-mapper (parse-string mapper-json true)
        filename (first (keys parsed-mapper))]
    (println "The filename of the mapped file is:" (name filename))
    (println "the body of the mapper is:\n" (filename parsed-mapper))))

(defn main []
  ;; (sh "node" "scripts/mapper_parsing.js" "weaponMapper.js")
  (let [result (node "scripts/mapper_parsing.js" "weaponMapper.js")]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result))
      (println "\nERROR!!!\n\n" result))))

(main)
