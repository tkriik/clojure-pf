(ns clojure-pf
  "Packet socket/device context functions."
  (:require [clojure-pf.binary          :as binary]
            [clojure-pf.data-link-type  :as dlt]
            [clojure-pf.form            :as form]
            [clojure-pf.io              :as io]))

; Record type for a packet socket/device context.
(defrecord Context [handle    ; socket/device file descriptor
                    entries   ; parsed packet form entries
                    options]) ; context creation options

(def ^:const default-options
  "Default context options."
  {:data-link-type    :null
   :header-complete   false
   :immediate         true
   :maximum-packets   4096
   :read-buffer-size  65536
   :write-buffer-size 65536})

(defn open
  "Opens a packet socket/device context with the given interface,
  form and options."
  ([interface form]
   (open interface form {}))
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

(defn receive-packets
  "Returns one or more destructured packets in a list on success."
  [context]
  (let [handle            (:handle context)
        entries           (:entries context)
        options           (:options context)
        raw-input-packet  (io/read-raw handle
                                       (:read-buffer-size options)
                                       (:maximum-packets options))]
    (if raw-input-packet
      (binary/deserialize raw-input-packet entries))))

(defn send-packet
  "Writes one destructured packet payload through a socket/device.
  Returns the number of bytes written on success."
  [context packet]
  (let [handle            (:handle context)
        entries           (:entries context)
        raw-output-packet (binary/serialize packet entries)
        write-buffer-size (get-in context [:options :write-buffer-size])]
    (io/write-raw handle raw-output-packet write-buffer-size)))

(defn close
  "Closes a packet socket/device context."
  [context]
  (io/close (:handle context)))

;
; DEBUG
;

(def my-form [:fc             :byte   2
              :dur            :byte   2
              :dst            :byte   6
              :src            :byte   6
              :bssid          :byte   6
              :seq      :be   :short
              :data           :byte   128])

(def my-opts {:immediate true :data-link-type :ieee80211})

(def my-ctx (open "iwn0" my-form my-opts))

