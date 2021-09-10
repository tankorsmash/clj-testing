(ns clj-testing.frame_data
  (:require-macros [hiccups.core :as hiccups :refer [html]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-testing.logging :refer [log info]])
  (:require [hiccups.runtime :as hiccupsrt]
            [cljs.pprint :refer [pprint]]
            [clojure.test :as ct]
            [clojure.test.check]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clojure.core.match :refer-macros [match]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent-catch.core :as rc]

            [clj-testing.person :as person]
            [clj-testing.open-dota :as dota]

            [reagent.core :as r]
            [reagent.dom :as rdom]
            [secretary.core :as secretary :refer-macros [defroute]]))

(def root_json_server_url "http://localhost:5021/")
(def all_weapon_frames_url (str root_json_server_url "all_weapon_frames"))

(defonce all-weapon-frames (r/atom []))

(s/def :frame-data.enum/raw-value int?)
(s/def :frame-data.enum/pretty-name string?)
(s/def :frame-data.enum/data-name string?)

(s/def :frame-data.enum/enum (s/keys :req [:frame-data.enum/raw-value
                                           :frame-data.enum/pretty-name
                                           :frame-data.enum/data-name]))

(s/def :frame-data.weapon/frame_id int?)
(s/def :frame-data.weapon/pretty_name' string?)
(s/def :frame-data.weapon/description' string?)
(s/def :frame-data.weapon/affects_morale' boolean?)
(s/def :frame-data.weapon/frame_image_path' string?)
(s/def :frame-data.weapon/battle_row_type' int?) ;;TODO implement enum support
(s/def :frame-data.weapon/damage_type' int?)
(s/def :frame-data.weapon/bonus_attack' int?)
(s/def :frame-data.weapon/bonus_power' int?)
(s/def :frame-data.weapon/bonus_encumbrance' int?)
(s/def :frame-data.weapon/rarity_type' int?)
(s/def :frame-data.weapon/carry_weight' int?)

(def weapon-frame-keys
  [:frame-data.weapon/frame_id
   :frame-data.weapon/pretty_name
   :frame-data.weapon/description
   :frame-data.weapon/affects_morale
   :frame-data.weapon/frame_image_path
   :frame-data.weapon/battle_row_type
   :frame-data.weapon/damage_type
   :frame-data.weapon/bonus_attack
   :frame-data.weapon/bonus_power
   :frame-data.weapon/bonus_encumbrance
   :frame-data.weapon/rarity_type
   :frame-data.weapon/carry_weight])

(declare int-to-weapon-damage-type)
(declare to-battle-row-type)

(s/def ::wdt (s/and int? #(s/int-in-range? 0 4 %) (s/conformer int-to-weapon-damage-type)))

(s/def
  :frame-data/weapon-damage-type
  (s/and (s/int-in 0 4) (s/conformer int-to-weapon-damage-type)))
  ;; (s/and (s/int-in 10 14) int?))
  ;; (s/int-in 10 14))

(s/def
  :frame-data/battle-row-type
  (s/and (s/int-in 0 3) (s/conformer to-battle-row-type)))

(s/def :frame-data.weapon/frame (s/keys :req (vector weapon-frame-keys)))
(s/def :frame-data.weapon-unq/frame (s/and
                                     (s/keys :req-unq [:frame-data/battle-row-type
                                                       :frame-data/weapon-damage-type])
                                     (s/keys :req-unq (vector weapon-frame-keys))))

(defn do-request-for-weapon-frames! []
  "makes a request for weapon frames"
  (go (let [response (<! (http/get all_weapon_frames_url {}))]
        (let [body (get-in response [:body])]
          (if (ct/is (s/coll-of (s/valid? :frame-data.weapon-unq/frame body)))
            (reset! all-weapon-frames body)
            (s/explain :frame-data.weapon-unq/frame body))))))

(defn int-to-battle-row [raw-battle-row]
  (match [raw-battle-row]
    [0] {:frame-data.enum/pretty-name "Melee" :frame-data.enum/data-name "melee" :frame-data.enum/raw-value 0}
    [1] {:frame-data.enum/pretty-name "Ranged" :frame-data.enum/data-name "ranged" :frame-data.enum/raw-value 1}
    [2] {:frame-data.enum/pretty-name "Rear" :frame-data.enum/data-name "rear" :frame-data.enum/raw-value 1}))

(defn int-to-weapon-damage-type [raw-damage-type]
  (match [raw-damage-type]
    [0] {:frame-data.enum/pretty-name "Unset" :frame-data.enum/data-name "unset" :frame-data.enum/raw-value 0}
    [1] {:frame-data.enum/pretty-name "Piercing" :frame-data.enum/data-name "piercing" :frame-data.enum/raw-value 1}
    [2] {:frame-data.enum/pretty-name "Blunt" :frame-data.enum/data-name "blunt" :frame-data.enum/raw-value 2}
    [3] {:frame-data.enum/pretty-name "Slashing" :frame-data.enum/data-name "slashing" :frame-data.enum/raw-value 3}))

(defn colored-stat [stat-val text]
  (cond
    (pos? stat-val) [:div {:style {:color :green}} text ": "stat-val]
    (neg? stat-val) [:div {:style {:color :red}} text ": "stat-val]
    :else [:div text ": "stat-val]))

(defn render-weapon-frame-row
  [{:keys [pretty_name frame_id battle_row_type
           damage_type bonus_attack bonus_power
           bonus_encumbrance carry_weight] :as weapon-frame}]
  ^{:key frame_id}
  [:div.row.rows-col-auto.mb-1.pb-1.border-bottom
   [:div.col-1 "#" frame_id " "]
   [:div.col pretty_name]
   [:div.col-1
    [:div (:frame-data.enum/pretty-name (int-to-battle-row battle_row_type))]
    [:div (:frame-data.enum/pretty-name (int-to-weapon-damage-type damage_type))]]
   [:div.col
    [colored-stat bonus_attack "ATK"]
    [colored-stat bonus_power "PWR"]]
   [:div.col
    [:div "ENC: " bonus_encumbrance]
    [:div "WGT: " carry_weight]]])

(defn render-root []
  (fn []
    [rc/catch
     [:div "This is the frame data root"
      [:div (str "Count of frames: " (count @all-weapon-frames))]
      [:div
       (for [weapon-frame @all-weapon-frames]
         ^{:key (:frame_id weapon-frame)}
         (render-weapon-frame-row weapon-frame))]]]))

;; (log (s/conform :frame-data.weapon/frame (first @all-weapon-frames)))
(comment
  (log (s/explain :frame-data/weapon-damage-type (first @all-weapon-frames)))
  (log (s/conform (s/keys :req-unq [:frame-data/weapon-damage-type]) (first @all-weapon-frames)))
  (def val (:damage_type (first @all-weapon-frames)))
  (log (s/explain :frame-data/weapon-damage-type val))
  (log (s/conform :frame-data/weapon-damage-type (:damage_type (first @all-weapon-frames)))))


(comment
  (def weapon-frame (first @all-weapon-frames))
  (s/describe :frame-data.weapon-unq/frame weapon-frame)
  (s/def ::damage_type ::wdt)
  (s/conform ::damage_type weapon-frame)
  (s/conform ::wdt (:damage_type weapon-frame))
  (s/explain ::wdt weapon-frame)
  (s/conform ::wdt 1)
  (s/valid? ::wdt 1)
  (s/conform ::wdt 10)
  (s/valid? ::wdt 10)
  (s/conform ::wdt "ASDASD"))

(comment
  (do-request-for-weapon-frames!)
  (log @all-weapon-frames))
