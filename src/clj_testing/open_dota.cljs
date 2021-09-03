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

(def player-profile-keys
  [:dota.profile/is_contributor
   :dota.profile/loccountry_code
   :dota.profile/account_id
   :dota.profile/avatarfull
   :dota.profile/avatar
   :dota.profile/profileurl])

(s/def :dota/tracked_until string?) ;;TODO make it a string of numbers
(s/def :dota/rank_tier int?)
(s/def :dota.profile/profile (s/keys :req (vector player-profile-keys)))
(s/def :dota.profile/profile-unq (s/keys :req-unq (vector player-profile-keys)))

(def player-data-keys
  [:dota/tracked_until
   :dota/rank_tier
   :dota.profile/profile])

(s/def :dota/player-data (s/keys :req (vector player-data-keys)))
(s/def :dota/player-data-unq (s/keys :req-unq (vector player-data-keys)))

(defn do-request-for-hero-stats []
  {:doc "makes a request"}
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
    (let [ud @user-data
          p (:profile @user-data)
          request-btn-cfg {:type "button"
                           :value "CLICK ME"
                           :on-click do-request-for-hero-stats}]
      [:div
       [:h2 "OPEN DOTA USER DATA"]
       [:div
        (if-not (nil? ud)
          [:div
           [:h1 "LOADED!!!"]
           [:div
            [:div "Tracked until " (str (:tracked_until ud))]
            [:div "Rank Tier " (str (:rank_tier ud))]
            [:div "profile:"
             [:div "Persona Name " (str (:personaname p))]
             [:div "Account ID " (str (:account_id p))]
             [:div "Avatar Full "
              [:img {:src (str (:avatarfull p))}]]
             [:div "Profile URL "
              [:a {:href (str (:profileurl p))} "Link"]]]]

           ;;dump the rest of the data
           [:pre {:style {:white-space "break-spaces"}} (person/pp-str ud)]]
          (let [null ud]
            [:div "No user dota yet" null
             [:div
              [:input request-btn-cfg]]]))]])))

(comment
  (do-request-for-hero-stats))
