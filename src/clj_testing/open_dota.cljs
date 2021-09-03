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
(defonce all-hero-stats (r/atom nil))

(defn sum [& args]
 (apply #(reduce + %) args))

(defn sum-7_wins [ahs]
 (let [ahs @all-hero-stats
       wins (map :7_win ahs)]
    (sum wins)))

(def root_json_server_url "http://localhost:5021/")
(def player_data_url (str root_json_server_url "open_dota_player_data"))
(def hero_stats_url (str root_json_server_url "open_dota_hero_stats"))

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

(def hero-stats-keys
  [:dota.hero-stats/hero_id
   :dota.hero-stats/icon
   :dota.hero-stats/img
   :dota.hero-stats/localized_name])

(s/def :dota/hero-stats (s/keys :req (vector hero-stats-keys)))
(s/def :dota/hero-stats-unq (s/keys :req-unq (vector hero-stats-keys)))

;; h2 { width:100%; text-align:center; border-bottom: 1px solid #000; line-height:0.1em; margin:10px 0 20px; }
;;     h2 span { background:#fff}}; padding:0 10px; color:red}

(defn divider-with-text [text]
  "basically -------text-----"
  [:div {:style
         {:width "100%"
          :text-align :center
          :border-bottom "1px solid grey"
          :line-height "0.1em"
          :margin "10px 0 20px"}}
   [:small.text-muted {:style
                       {:background "white"
                        :padding "0 10px"
                        :color "red"}} text]])

(defn do-request-for-player-data! []
  "makes a request for player data"
  (go (let [response (<! (http/get player_data_url {}))]
        (log (str "response status: " (:status response)))
        (let [body (get-in response [:body])]
          (log body)
          (if (ct/is (s/valid? :dota/player-data-unq body))
            (do (log "mfer is valid, assigning to variable")
                (reset! user-data body))
            (do (log "player-data-unq failed to match")
                (s/explain :dota/player-data-unq body))))
        (log "in do-requiest" response))))

(defn do-request-for-hero-stats! []
  "makes a request for hero stats"
  (go (let [response (<! (http/get hero_stats_url {}))]
        (log (str "response status: " (:status response)))
        (let [body (get-in response [:body])]
          (log body)
          (if (ct/is (s/coll-of (s/valid? :dota/hero-stats-unq body)))
            (do (log "heroic mfer is valid, assigning to variable")
                (reset! all-hero-stats body))
            (do (log "player-data-unq failed to match")
                (s/explain :dota/hero-stats-unq body))))
        (log "in do-request for all-hero-stats" response))))

(defn render-user-data-loaded [ud p]
  [:div
   [:h5 {:style {:color "green"}} "Data has loaded!"]
   [:div
    [divider-with-text "user-data"]
    [:div.row
     [:div.col "Tracked until " (str (:tracked_until ud))]
     [:div.col "Rank Tier " (str (:rank_tier ud))]]

    [divider-with-text "user-data.profile"]
    [:div.row
     [:div.col "Persona Name " (str (:personaname p))]
     [:div.col "Account ID " (str (:account_id p))]
     [:div.col-2 "Avatar Full "
      [:img.img-thumbnail {:src (str (:avatarfull p))}]]
     [:div.col "Profile URL "
      [:a {:href (str (:profileurl p))} "Link"]]]]

   ;;dump the rest of the data
   [divider-with-text "raw user-data"]
   [:pre {:style {:white-space "break-spaces"}} (person/pp-str ud)]])

(defn request-btn-cfg [callback]
  {:type "button"
   :value "Download"
   :class ["btn" "btn-outline-secondary"]
   ;; :on-click do-request-for-player-data!
   :on-click callback})

(def request-btn-cfg-hero-stats
  (request-btn-cfg do-request-for-hero-stats!))

(def request-btn-cfg-player-stats
  (request-btn-cfg do-request-for-player-data!))

(defn render-user-data-notloaded [ud]
  [:div "No user dota yet" ud
   [:div
    [:input request-btn-cfg-player-stats]]])

(defn render-user-data [user-data]
  (fn [user-data]
    (let [ud @user-data
          p (:profile @user-data)]
      [:div
       [:h4 "OPEN DOTA USER DATA"]
       [:div
        (if-not (nil? ud)
          (render-user-data-loaded ud p)
          (render-user-data-notloaded ud))]])))

(defn float-to-percentage-str [flt]
 (str (double (/ (Math/floor (* flt 10000)) 100)) "%"))

(defn render-single-hero-stat [ahs hero-stat]
 (let [all-wins (sum-7_wins ahs)
       my-wins (:7_win hero-stat)
       my-picks (:7_pick hero-stat)
       my-losses (- (:7_pick hero-stat) my-wins)
       winrate (/ my-wins my-picks)]
  [:div
   [:div "this is about a hero named: " (str (:localized_name hero-stat))]
   [:div "and ive got this many rank7 wins: " (str my-wins)]
   [:div "and ive got this many rank7 losses: " (str my-losses)]
   [:div "and ive got this many rank7 winrate " (float-to-percentage-str winrate)]]))


(defn render-hero-stats [all-hero-stats]
  (let [selected-hero-id (r/atom 0)]
    (fn [all-hero-stats]
      (let [ahs @all-hero-stats]
        [:div
          [:h4 "OPEN DATA HERO STATS"]
          [:div "the selected hero id " @selected-hero-id]
          [:input.btn.btn-primary {:type :button :value "Next Hero ID" :on-click #(swap! selected-hero-id inc)}]
          [:div
           (if-not (nil? ahs)
             (let [selected-hero (nth ahs @selected-hero-id)]
               [:div
                [render-single-hero-stat ahs selected-hero]
                [divider-with-text "raw user-data"]
                [:pre {:style {:white-space "break-spaces"}} (person/pp-str selected-hero)]])
             [ :div "no hero stats downloaded"
              [:br]
              [:input request-btn-cfg-hero-stats]])]]))))

(def sample-hero-stat
 {:5_win 6040, :hero_id 59, :str_gain 3.4, :agi_gain 1.6, :base_mana 75,
  :attack_range 400, :2_pick 22572, :base_armor -1, :2_win 11630,
  :1_pick 17365, :7_win 1202, :move_speed 290, :3_pick 23656, :3_win 12177,
  :1_win 8965, :base_int 18, :name "npc_dota_hero_huskar",
  :roles ["Carry" "Durable" "Initiator"], :base_agi 13,
  :attack_type "Ranged", :8_win 278, :primary_attr "str", :pro_ban 23,
  :icon "/apps/dota2/images/heroes/huskar_icon.png",
  :base_str 21, :8_pick 511, :5_pick 11338, :pro_pick 12, :attack_rate 1.6,
  :pro_win 7, :cm_enabled true, :projectile_speed 1400, :6_win 2609, :4_win 9981,
  :int_gain 1.5, :legs 2, :id 59, :turbo_wins 28578, :base_mana_regen 0,
  :4_pick 19198, :base_attack_max 26, :7_pick 2266, :base_health 200,
  :null_pick 1001867, :base_health_regen nil, :6_pick 4908
  :turn_rate nil, :base_mr 25, :null_win 0,
  :img "/apps/dota2/images/heroes/huskar_full.png?", :base_attack_min 21,
  :localized_name "Huskar", :turbo_picks 54811})


(comment
  (sum [1 2 3])
  (do-request-for-player-data!)
  (do (do-request-for-hero-stats!)
      (log "ASD " (count @all-hero-stats))
      #_(let [ahs @all-hero-stats]
            wins (map :7_win ahs))
        log (sum wins)
      (log "7_wins " (sum-7_wins @all-hero-stats)))
  (def winrate
   (let [all-wins (sum-7_wins @all-hero-stats)
         my-wins (:7_win sample-hero-stat)
         my-picks (:7_pick sample-hero-stat)
         my-losses (- my-picks my-wins)]
    (/ my-wins my-picks))))
