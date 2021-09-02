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

(defonce user-data (r/atom nil))

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
  (fn [user-data]
    (let [ud @user-data]
      [:div
       [:h2 "OPEN DOTA USER DATA"]
       [:div
        (if-not (nil? ud)
          [:div
           [:h1 "LOADED!!!"]
           [:div
              [:div "Tracked until " (str (:tracked_until ud))]
              [:div "Rank Tier " (str (:rank_tier ud))]]
           [:pre (str ud)]]
          (let [null ud]
            [:div "No user dota yet" null]))]])))

(comment
  (do-request-for-hero-stats))
