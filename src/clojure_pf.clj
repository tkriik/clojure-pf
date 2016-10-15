(ns clojure-pf
  "Packet socket/device context functions."
  (:require [clojure-pf.binary          :as binary]
            [clojure-pf.data-link-type  :as dlt]
            [clojure-pf.form            :as form]
            [clojure-pf.io              :as io]))

; Packet socket/device context
(deftype Context [handle    ; socket/device file descriptor
                  entries   ; parsed packet form entries
                  options]) ; context creation options

(def ^:private ^:const default-options
  "Default packet context options."
  {:read-buffer-size  65536
   :maximum-packets   4096
   :data-link-type    :null
   :header-complete   false
   :immediate         false})

(defn create
  "Creates a packet socket/device context with the given interface,
  form and options."
  ([interface form]
   (create interface form {}))
  ([interface form options]
   (let [entries          (form/to-entries form)
         options          (merge default-options options)
         read-buffer-size (:read-buffer-size options)
         data-link-type   (or (dlt/to-code (:data-link-type options)) 0)
         header-complete  (:header-complete options)
         immediate        (:immediate options)
         handle           (io/open interface
                                   read-buffer-size
                                   data-link-type
                                   header-complete
                                   immediate)]
     (if handle
       (->Context handle entries options)))))

(defn receive
  "Returns one or more destructured packets in a list on success."
  [context]
  (let [handle      (.handle context)
        entries     (.entries context)
        options     (.options context)
        raw-packet  (io/read-raw handle
                             (:read-buffer-size options)
                             (:maximum-packets options))]
    (if raw-packet
      (binary/deserialize raw-packet entries))))

(defn destroy-context
  "Destroys a packet socket/device context."
  [context]
  (io/close (.handle context)))

;
; DEBUG
;

(def my-form [:fc             :byte   2
              :dur            :byte   2
              :dst            :byte   6
              :src            :byte   6
              :bssid          :byte   6
              :seq      :be   :short
              :char4          :char   4
              :short4   :be   :short  4
              :int4     :be   :int    4
              :long4    :be   :long   4
              :float4   :be   :float  4
              :double4  :be   :double 4])

(def my-opts {:immediate true :data-link-type :ieee80211})

(def my-ctx (create "iwn0" my-form my-opts))

