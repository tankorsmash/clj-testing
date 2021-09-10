#!C:\Users\Josh\Documents\cocos_projects\clojure\clojure\bb

(require '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str]
         '[cheshire.core :refer [parse-string]])


(defn node [& args]
  (apply sh "node" args))


(defn parse-field [{:keys [attrName prettyName type] :as field}]
  ;; (println attrName " - " prettyName " <> " type)
  (println field))

(defn handle-mapper-json [mapper-json]
  (let [parsed-mapper (parse-string mapper-json true)
        filename (first (keys parsed-mapper))
        fields (filename parsed-mapper)]
    (println "The filename of the mapped file is:" (name filename))
    ;; (println "the body of the mapper is:\n" (filename parsed-mapper))
    ;; (parse-field (first (vec fields)))))
    (map parse-field (vec fields))))

(defn main []
  ;; (sh "node" "scripts/mapper_parsing.js" "weaponMapper.js")
  (let [result (node "scripts/mapper_parsing.js" "weaponMapper.js")]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result))
      (println "\nERROR!!!\n\n" result))))

(main)
