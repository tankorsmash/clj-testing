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


(def root_json_server_url "http://localhost:5021/")
(def all_weapon_frames_url (str root_json_server_url "all_weapon_frames"))

(defonce all-weapon-frames (r/atom []))

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
  [
   :frame-data.weapon/frame_id
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

(s/def :frame-data.weapon/frame (s/keys :req (vector weapon-frame-keys)))
(s/def :frame-data.weapon-unq/frame (s/keys :req-unq (vector weapon-frame-keys)))

(defn do-request-for-weapon-frames! []
  "makes a request for weapon frames"
  (go (let [response (<! (http/get all_weapon_frames_url {}))]
        (let [body (get-in response [:body])]
          (if (ct/is (s/coll-of (s/valid? :frame-data.weapon-unq/frame body)))
            (reset! all-weapon-frames body)
            (s/explain :frame-data.weapon-unq/frame body))))))


(defn render-root []
  (fn []
    [:div "This is the frame data rootsdsds"
      [:div (str "Count of frames: " (count @all-weapon-frames))]
      [:div
       (for [{:keys [pretty_name frame_id] :as weapon-frame} @all-weapon-frames]
        ^{:key frame_id}
        [:div
         [:span pretty_name]
         [:span frame_id]])]]))

(comment
  (do-request-for-weapon-frames!)
  (log @all-weapon-frames)
  ,)
