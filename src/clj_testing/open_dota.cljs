(ns clj-testing.open-dota
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-testing.logging :refer [log info]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test :as ct]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clj-testing.person :as person]
            [reagent.core :as r]
            [reagent.dom :as rdom]))


(defonce user-data (r/atom {:dota/player-data nil}))

(def root_json_server_url "http://localhost:5021/")
(def hero_stats_url (str root_json_server_url "open_dota_player_data"))

(s/def :dota/tracked_until string?) ;;TODO make it a string of numbers
(s/def :dota/rank_tier int?)

(s/def :dota/player-data (s/keys :req [:dota/tracked_until
                                       :dota/rank_tier]))
(s/def :dota/player-data-unq (s/keys :req-unq [:dota/tracked_until
                                               :dota/rank_tier]))


(defn do-request-for-hero-stats []
  "ASDSAD"
  (go (let [response (<! (http/get hero_stats_url {}))]
        (log (str "response status: " (:status response)))
        (let [body (get-in response [:body])]
          (log body)
          (if (ct/is (s/valid? :dota/player-data-unq body))
            (do (log "mfer is valid, assigning to variable")
                (reset! user-data body))
            (do (log "player-data-unq failed to match")
                (s/explain :dota/player-data-unq body))))
        (log "in do-requiest" response))))

(defn render-user-data [user-data]
  [:h2 "OPEN DOTA USER DATA"]
  [:div "ASD" @user-data])
  ;; (log (:dota/player-data user-data))
  ;; (if-not (:dota/player-data user-data)
  ;;   [:div "no user data"]
    ;; [:h1 "LOADED!!!"]])

(comment
  (do-request-for-hero-stats))
