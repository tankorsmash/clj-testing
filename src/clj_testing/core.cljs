(ns clj-testing.core
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-testing.logging :refer [log info]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clj-testing.person :as person]
            [clj-testing.open-dota :as dota]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)

(defroute "/users/:id" {:as params}
  (js/console.log (str "User: " (:id params))))

(defroute home-path "/" []
  (js/console.log "You're home!"))

(secretary/set-config! :prefix "#")


(let [h (History.)]
  (goog.events/listen h EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h
    (.setEnabled true)))

;; (doto (History.)
;;   (events/listen EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
;;   (.setEnabled true))

;; (secretary/dispatch! "/")

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
(def prem (person/to-valid-person (.parse js/JSON prem_json)))
(def olivia (person/to-valid-person (person/->Person "Olivia"  1234567897 100)))

(def generated (s/exercise (s/cat :age :person/age :name :person/name) 2))

(defn add_x [x] (+ x 10))

(defn get-age-long [{:person/keys (age)}]
  (let [new-age age]
    (-> new-age
        add_x
        add_x)))

(defn handle-person [person]
  (str
   (person/tryget-person-name person)
   ": "
   (person/with-valid-person person get-age-long)))

(def raw_people [joshua sandy prem matthew olivia])

(defonce click-count (r/atom 0))
(defonce seconds-elapsed (r/atom 0))
(defonce atom-people (r/atom
                      (mapv #(person/with-valid-person % identity) raw_people)))

(defonce time-updater (js/setInterval
                       #(swap! seconds-elapsed inc) 1000))

(def to-output (clojure.string/join
                "\n"
                (map handle-person
                     @atom-people)))

(defn render_dom "takes nothing and returns a new string for the entire DOM" []
  (html [:h2 {} (str "Generated: " generated)]
        [:span.foobar "classed span"]
        [:div {} "this is a newline"]
        [:pre {:style "font-size: 24px"} to-output]))

(defn change-people [people]
  (let [new-people (concat people people)]
    (prn (count new-people))
    new-people))

(defn on-click []
  (swap! click-count inc))

(defn on-click-change-people []
  (swap! atom-people change-people))

(defn change-person-age [person]
  (update-in person [:person/age] #(+ % 1)))

(defn swap-person [people idx person]
  (assoc people idx person))

(defn on-click-change-person [idx person]
  (let [new-person (change-person-age person)]
    (swap! atom-people #(swap-person % idx new-person))))

(defn clickable-age [idx person]
  [:div {:key idx :on-click #(on-click-change-person idx person)}
   [:div "This is a " [:b "CLICKABLE"] "-age: "
    (person/tryget-person-age person) "-" (person/tryget-person-name person)]])

(defn root-component [innertext]
  (fn []
    [:div.container
     [:span.someclass
      "I have " [:strong "bold"] " and the click-count of: " (str @click-count)
      [:span {:style {:color "red"}} " and red "] "text. "]
     [:input {:type "button" :value "CLICK ME!"
              :on-click on-click}]
     [:span " Seconds elapsed: " @seconds-elapsed]
     [:div
      [:a {:href "#"} "Home"]]
     [:div
       [:a {:href "#users/123"} "Users 123"]]
     (take 2 (map-indexed clickable-age @atom-people))
     [dota/render-user-data dota/user-data]
     [dota/render-hero-stats dota/all-hero-stats]]))

(def app-elem (js/document.getElementById "app"))
(def react-app-elem (js/document.getElementById "react-app"))

(defn render-simple []
  (rdom/render
   [root-component "inner text"]
   react-app-elem))

(def start-up (do (render-simple) true))

(defn dota-download []
  (log "dota channel: " (clj-testing.open-dota/do-request-for-player-data!)))

(comment
  (js/console.clear)
  (dota-download)
  (dota/do-request-for-player-data!))
