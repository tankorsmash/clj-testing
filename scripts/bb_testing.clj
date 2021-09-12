#!C:\Users\Josh\Documents\cocos_projects\clojure\clojure\bb

(require '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str]
         ;; '[clojure.spec.alpha :as s] ;;replaced with spartan.spec
         '[clojure.core.match :refer [match]]
         '[cheshire.core :refer [parse-string]]
         '[babashka.deps :as deps])

;; add babashka-equivalent to spec
(deps/add-deps
 '{:deps {borkdude/spartan.spec {:git/url "https://github.com/borkdude/spartan.spec"
                                 :sha "12947185b4f8b8ff8ee3bc0f19c98dbde54d4c90"}}})

(require 'spartan.spec)
(alias 's 'clojure.spec.alpha)


(defn node [& args]
  (apply sh "node" args))



(defmacro define-spec [spec-key validator]
  '(s/def @spec-key `validator))

(defn parse-field [spec-ns
                   {:keys [attrName prettyName type] :as field}]
  (let [validator (match [type]
                        ["string"] #'string?
                        ["number"] #'number?
                        ["enum"] #'number?
                        ["hidden"] #'number?)]
    (println "The validator has been found: " validator)
    (let [spec-kw (keyword spec-ns attrName)
          new-def (define-spec spec-kw validator)]
      (println "registered" new-def)
      (println (s/valid? new-def 10))
      (prn new-def))))
        ;; (println attrName " - " prettyName " <> " type)
        ;; (prn field))

(defn handle-mapper-json [mapper-json]
  (let [parsed-mapper (parse-string mapper-json true)
        filename (first (keys parsed-mapper))
        fields (filename parsed-mapper)]
    (println "The filename of the mapped file is:" (name filename))
    ;; (println "the body of the mapper is:\n" (filename parsed-mapper))
    ;; (parse-field (first (vec fields)))))
    (map (partial parse-field "frame-data.weapon") (vec fields))))

(defn main []
  ;; (sh "node" "scripts/mapper_parsing.js" "weaponMapper.js")
  (let [result (node "scripts/mapper_parsing.js" "weaponMapper.js")]
    (if (zero? (:exit result))
      (handle-mapper-json (:out result))
      (println "\nERROR!!!\n\n" result))))
  ;; (println "testing spec after the fact:" (s/valid? :frame-data.weapon/carry_weight 123)))

;; (main)
