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
            [cljs.core.async :refer [<! >! chan]]
            [clj-testing.person :as person]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce user-data (r/atom nil))
(defonce all-hero-stats (r/atom nil))
(defonce selected-hero-id (r/atom 1))
(defonce all-selected-hero-ids (r/atom (set [])))

(defn event-handler [state [event-name arg1 arg2 arg3 arg4 & args]]
  (case event-name
    :print (println event-name args)
    :log (log event-name args)
    :add-selected-hero (let [hero-id arg1]
                          (conj state hero-id))
    :remove-selected-hero (let [hero-id arg1]
                             (disj state hero-id))
    :clear-selected-heroes #{}))

(defn emit [e]
  (r/rswap! all-selected-hero-ids event-handler e))

(defn add-selected-hero! [all-selected-hero-ids hero-id]
  {:pre [(= (type all-selected-hero-ids) reagent.ratom/RAtom)]}
  (swap! all-selected-hero-ids conj hero-id))

(defn remove-selected-hero! [all-selected-hero-ids hero-id]
  {:pre [(= (type all-selected-hero-ids) reagent.ratom/RAtom)]}
  (swap! all-selected-hero-ids disj hero-id))

(defn clear-selected-hero-ids! [all-selected-hero-ids]
  {:pre [(= (type all-selected-hero-ids) reagent.ratom/RAtom)]}
  (reset! all-selected-hero-ids #{}))


(defn sum [& args]
  (apply #(reduce + %) args))

(defn ahs-getter [ahs k]
  (map k ahs))

(defn all-winrates [ahs]
  (ahs-getter ahs #(/ (:7_win %) (:7_pick %))))

(defn sum-7_wins [ahs]
  (let [wins (map :7_win ahs)]
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

(defn divider-with-text [text & chilcren]
  (let [is-open (r/atom true)]
    (fn [text & children]
      "basically -------text-----"
      [:div
       [:div {:style
              {:width "100%"
               :text-align :center
               :border-bottom "1px solid grey"
               :line-height "0.1em"
               :margin "10px 0 20px"
               :user-select :none
               :cursor :pointer}
              :on-click #(reset! is-open (not @is-open))}
        [:small.text-muted {:style
                            {:background "white"
                             :padding "0 10px"
                             :color "red"}}
         text]]
       (when @is-open children)])))

(defn do-request-for-player-data! []
  "makes a request for player data"
  (go (let [response (<! (http/get player_data_url {}))]
        ;; (log (str "response status: " (:status response)))
        (let [body (get-in response [:body])]
             ;; (log body)
          (if (ct/is (s/valid? :dota/player-data-unq body))
               ;; (do (log "mfer is valid, assigning to variable")
            (reset! user-data body)
               ;; (do (log "player-data-unq failed to match")
            (s/explain :dota/player-data-unq body))))))
      ;; (log "in do-requiest" response))))

(defn do-request-for-hero-stats! []
  "makes a request for hero stats"
  (go (let [response (<! (http/get hero_stats_url {}))]
        ;; (log (str "response status: " (:status response)))
        (let [body (get-in response [:body])]
          ;; (log body)
          (if (ct/is (s/coll-of (s/valid? :dota/hero-stats-unq body)))
            ;; (do (log "heroic mfer is valid, assigning to variable")
            (reset! all-hero-stats body)
            ;;do (log "player-data-unq failed to match")
            (s/explain :dota/hero-stats-unq body))))))
        ;; (log "in do-request for all-hero-stats" response))))

(defn render-user-data-loaded [ud p]
  [:div
   [:h5 {:style {:color "green"}} "Data has loaded!"]
   [:div
    [divider-with-text "user-data"
      [:div.row
       [:div.col "Tracked until " (str (:tracked_until ud))]
       [:div.col "Rank Tier " (str (:rank_tier ud))]]]

    [divider-with-text "user-data.profile"
      [:div.row
       [:div.col "Persona Name " (str (:personaname p))]
       [:div.col "Account ID " (str (:account_id p))]
       [:div.col-2 "Avatar Full "
        [:img.img-thumbnail {:src (str (:avatarfull p))}]]
       [:div.col "Profile URL "
        [:a {:href (str (:profileurl p))} "Link"]]]]]

   ;;dump the rest of the data
   [divider-with-text "raw user-data"
     [:pre {:style {:white-space "break-spaces"}} (person/pp-str ud)]]])

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

(defn get-winrate
  [{wins :7_win
    picks :7_pick :as hero-stats}]
  (/ wins picks))

(defn change-selected-hero-id [new-hero-id]
  (log "new-hero-id: " new-hero-id)
  (reset! selected-hero-id new-hero-id))

(defn render-single-hero-stat [ahs hero-stat]
  (let [all-wins (sum-7_wins ahs)
        my-wins (:7_win hero-stat)
        my-picks (:7_pick hero-stat)
        my-losses (- (:7_pick hero-stat) my-wins)
        winrate (get-winrate hero-stat)
        img-icon (:icon hero-stat)]
    [:div
     [:div.row.row-cols-auto
      [:div.col
       [:img.img-fluid {:src (str "https://steamcdn-a.akamaihd.net/" img-icon)}]
       [:b (str " " (:localized_name hero-stat))]]
      [:div.col "ID: " (str (:hero_id hero-stat))]
      [:div.col "Wins: " (str my-wins)]
      [:div.col "Losses: " (str my-losses)]
      [:div.col "Matches: " (str my-picks)]]]))

(defn is-hero-in-selection
  [selection {hero-id :hero_id  :as hero-stat}]
  (contains? selection hero-id))

(defn get-displayed-heroes [all-hero-stats selection]
  (filter #(is-hero-in-selection selection %) all-hero-stats))


(defn render-single-hero-winrates
  [{wins :7_win
    picks :7_pick
    hero-id :hero_id
    hero-name :localized_name
    :as hero-stat}
   all-selected-hero-ids]
  (let [winrate (get-winrate hero-stat)
        ;; ashi all-selected-hero-ids
        btn-style {:style {:cursor :pointer :user-select :none}}]

    [:div.row.show-child-on-hover.w-100
     {:key hero-id :on-click #(change-selected-hero-id (:hero_id hero-stat))}
     [:div.col-3 [:progress {:value winrate :max 1} winrate]]
     " "
     [:div.col.align-self-begin
      [:strong hero-name]
      " "
      [:span
       [:span {:style {:color :green}} wins] "/" [:span picks]]
      " "
      [:span " (" (float-to-percentage-str (get-winrate hero-stat)) ")"]]
     [:div.col.align-self-end
      [:div.row.row-cols-auto.show-me-on-hover
         ;;deliberately not using .btn on these buttons because it grows their size too much.
         (let [add-style (merge btn-style {:on-click #(emit [:add-selected-hero hero-id])})
               rem-style (merge btn-style {:on-click #(emit [:remove-selected-hero hero-id])})
               hero-in-selection (is-hero-in-selection @all-selected-hero-ids hero-stat)]
              [:div.row.ctrl-rows
                (if-not hero-in-selection
                  [:div.col.btn-primary add-style "Add"]
                  [:div.col.btn-primary rem-style "Remove"])])]]]))


(defn get-selected-hero [sid ahs]
  (first (filter #(= (:hero_id %) sid) ahs)))


(defn lookup-by-hero-id [ahs hero-id]
  (first (filter #(= (:hero_id %) hero-id) ahs)))


(defn render-hero-stats [all-hero-stats]
  (let [should-filter-by-selection (r/atom false)]
    (fn [all-hero-stats]
      (let [ahs @all-hero-stats
            sid @selected-hero-id
            ashi @all-selected-hero-ids
            toggle-filter-by-hids #(swap! should-filter-by-selection not)]
        [:div
         [:h4 "OPEN DATA HERO STATS"]
         [:div.row.row-cols-auto
          [:div.col
           [:input.btn.btn-outline-danger {:type :button
                                           :value "Clear"
                                           :on-click #(emit [:clear-selected-heroes])}]
           [:input.btn.btn-outline-secondary {:type :button
                                              :value "Next Hero ID"
                                              :on-click #(swap! selected-hero-id inc)}]]
          [:div.col
           [:div "Viewing: " (:localized_name (lookup-by-hero-id ahs sid))]
           [:div
            {:style {:visibility (if-not (empty? ashi) :inherit :hidden)}}
            (str "Selected hero ids: ")
            [:span (for [selected-hero-id ashi]
                        (if-not (= (last ashi) selected-hero-id)
                                [:span {:key selected-hero-id :on-click #(emit [:remove-selected-hero selected-hero-id])} (str selected-hero-id ", ")]
                                [:span {:key selected-hero-id :on-click #(emit [:remove-selected-hero selected-hero-id])} (str selected-hero-id)]))]]]

          [:div.col
           [:input.btn.btn-primary {:value (if-not @should-filter-by-selection "Filter" "Unfilter")
                                    :type :button
                                    :on-click toggle-filter-by-hids}]]]
         [:div
          (if-not (nil? ahs)
            (let [selected-hero (get-selected-hero sid ahs)]
              [:div
               [render-single-hero-stat ahs selected-hero]
               [:div {:style {:max-height "100px"
                              :overflow-y :scroll}}
                (let [coll (if (= true @should-filter-by-selection)
                               (get-displayed-heroes ahs ashi)
                               ahs)
                      sorted-heroes (sort
                                      #(> (get-winrate %1) (get-winrate %2))
                                      coll)
                      render-a-hero #(render-single-hero-winrates %1 all-selected-hero-ids)
                      all-rendered-heroes (into [] (map render-a-hero) sorted-heroes)]
                  (for [rendered-hero all-rendered-heroes]
                      rendered-hero))]

               [divider-with-text "raw selected-hero data"
                [:pre {:key 1 :style {:white-space "break-spaces"}}
                 (person/pp-str selected-hero)]]])
            [:div "no hero stats downloaded"
             [:br]
             [:input request-btn-cfg-hero-stats]])]]))))


(s/def :dota/rank1 (s/keys :req-un [::1_win ::1_pick]))
(s/def :dota/rank2 (s/keys :req-un [::2_win ::2_pick]))
(s/def :dota/rank3 (s/keys :req-un [::3_win ::3_pick]))
(s/def :dota/rank4 (s/keys :req-un [::4_win ::4_pick]))
(s/def :dota/rank5 (s/keys :req-un [::5_win ::5_pick]))
(s/def :dota/rank6 (s/keys :req-un [::6_win ::6_pick]))
(s/def :dota/rank7 (s/keys :req-un [::7_win ::7_pick]))
(s/def :dota/ranks (s/merge :dota/rank1
                            :dota/rank2
                            :dota/rank3
                            :dota/rank4
                            :dota/rank5
                            :dota/rank6
                            :dota/rank7))

(comment
  (js/console.clear)

  (defn setup []
    (do (do-request-for-hero-stats!)
        (def selected-hero (nth @all-hero-stats @selected-hero-id))
        (def rank-keys (rest (s/describe :dota/ranks)))
        (def rank1keys (first (rest (rest (s/describe :dota/rank1)))))
        (log ((apply juxt (mapv (comp keyword name) rank1keys)) selected-hero))))
  (setup)

  (defn get-just-keywords-from-rank [sdef]
    (first (rest (rest (s/describe sdef)))))
  (def all-rank-keys
    (flatten (map get-just-keywords-from-rank rank-keys)))

  (defn get-rank-values [rank-keys]
    ((apply juxt (mapv (comp keyword name) rank-keys)) selected-hero))
  ,)
