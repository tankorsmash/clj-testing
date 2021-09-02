(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clj-testing.person :as person]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(enable-console-print!)

;;   "these are the commands to start up the repl for vim. not sure how to automate this"
;; (defmacro set_up_vim_repl []
;;   (do (require 'figwheel.main.api
;;        (figwheel.main.api/start {:mode :serve} "dev")
;;        (figwheel.main.api/cljs-repl "dev"))))
;;   ;; :Piggieback! (do (require 'figwheel-sidecar.repl-api) (figwheel-sidecar.repl-api/cljs-repl))
;;   `(js/alert "Hello from the ClojureScript REPL"))
;;

(hiccups/defhtml my-template [link-text]
  [:div]
  [:a {:href "https://github.com/weavejester/hiccup"} link-text])

(def my-doc (js/document.getRootNode))

(def main-doc (js/document.getRootNode))
(def joshua {:person/name "Josh" :person/age 23})
(def sandy {:person/name "Sandy" :person/age 44})
(def matthew {:name "Matt" :age 12})

(def prem_json "{\"name\":\"Prem\",\"age\":40}")
(def prem (.parse js/JSON prem_json))
(def olivia (person/->Person "Olivia"  1234567897 100))

(def generated (s/exercise (s/cat :age :person/age :name :person/name) 2))

(defn get-age [{:person/keys (age)}] age)
(defn get-name [{:person/keys (name)}] name)

(defn add_x [x] (+ x 10))

(defn get-age-long [{:person/keys (age)}]
  (let [new-age age]
    (-> new-age
        add_x
        add_x)))

(defn handle-person [person]
  ;; (str (person/tryget-person-name person) ": " (person/with-valid-person person get-age))
  (str
   (person/tryget-person-name person)
   ": "
   (person/with-valid-person person get-age-long)))

(def people [joshua sandy prem matthew olivia {}])

(def to-output (clojure.string/join
                "\n"
                (map handle-person
                     people)))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Generated: " generated)]
        [:span.foobar "classed span"]
        [:div {} "this is a newline"]
        [:pre {:style "font-size: 24px"} to-output]))

(defonce click-count (r/atom 0))
(defonce seconds-elapsed (r/atom 0))

(defn on-click []
  (swap! click-count inc))

(defn child-comp [num]
  [:h4 "H4 Header in react " num])

(defn uses-settimeout []
  (fn []
    ;; this stacks setTimeouts if you hotreload too many times
    (js/setTimeout #(swap! seconds-elapsed inc) 1000)
    [:div
     "Seconds elapsed: " @seconds-elapsed]))

(defn clickable-age [idx person]
  [:div {:key idx}
   [:p "This is a UNclickable-age: " (get-age person) "-" (get-name person)]])

;; ^{:key (:person/age %)}

(defn root-component [innertext]
  (let [ages (map-indexed clickable-age people)]
    (fn []
      [:div
       [:p "I am a component! " innertext]
       [:p.someclass
        "I have " [:strong "bold"] " and the click-count of: " (str @click-count)
        [:span {:style {:color "red"}} " and red "] "text."]
       [:input {:type "button" :value "CLICK ME!"
                :on-click on-click}]
       ;; [(map child-comp (range 5))]
       [child-comp 1]
       [uses-settimeout]
       [child-comp 2]
       ;; (for [i (take 1 ages)]
       ;;   ^{:key 1} i)])))
       ages])))

(def app-elem (js/document.getElementById "app"))
(def react-app-elem (js/document.getElementById "react-app"))

(defn render-simple []
  (set! (.-innerHTML (js/document.getElementById "app")) (render_dom))
  (rdom/render
   [root-component "inner text"]
   react-app-elem))

(def start-up (do (render-simple) true))

;; (println (str "|- start output -|\n" to-output "\n|- ended output -|"))
;;             "the doc:\n" (render_dom)))
