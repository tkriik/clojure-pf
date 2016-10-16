(ns clojure-pf.core.data-link-type
  "Supported data link type level codes."
  (:require [clojure.set :refer [map-invert]]))

(def ^:private ^:const dlt-map
  {:null      0     ; no link-layer encapsulation
   :en10mb    1     ; ethernet (10Mb)
   :ieee80211 105}) ; IEEE 802.11 wireless

(def ^:private ^:const code-map
  (map-invert dlt-map))

(defn to-code [dlt]
  (get dlt-map dlt))

(defn from-code [code]
  (get code-map code))
