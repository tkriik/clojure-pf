(ns clojure-pf
  "Packet socket/device context functions."
  (:require [clojure-pf.core.binary :as binary]
            [clojure-pf.core.buffer :as buffer]
            [clojure-pf.core.form   :as form]
            [clojure-pf.core.native :as native]))

; Record type for a packet socket/device context.
(defrecord Context [handle        ; socket/device file descriptor
                    read-buffer   ; input packet buffer
                    write-buffer  ; output packet buffer
                    forms         ; parsed packet forms
                    options])     ; context creation options

(def ^:const default-options
  "Default context options."
  {:data-link-type    :null
   :read-buffer-size  65536
   :write-buffer-size 65536
   :maximum-packets   4096
   :header-complete?  false
   :immediate?        true})

(defn open
  "Opens a packet socket/device context with the given interface,
  form and options."
  ([interface raw-form]
   (open interface raw-form {}))
  ([interface raw-form options]
   (let [options            (merge default-options options)

         data-link-type     (:data-link-type options)
         read-buffer-size   (:read-buffer-size options)
         write-buffer-size  (:write-buffer-size options)
         header-complete?   (:header-complete? options)
         immediate?         (:immediate? options)

         handle             (native/open interface
                                     read-buffer-size
                                     write-buffer-size
                                     data-link-type
                                     header-complete?
                                     immediate?)
         read-buffer        (buffer/allocate read-buffer-size)
         write-buffer       (buffer/allocate write-buffer-size)
         forms              (form/to-forms raw-form)]
     (if handle
       (->Context handle
                  read-buffer
                  write-buffer
                  forms
                  options)))))

(defn receive-packets
  "Returns one or more destructured packets in a list on success."
  [context]
  (let [handle            (:handle context)
        forms             (:forms context)
        options           (:options context)
        raw-input-packet  (native/read-raw handle
                                           (:read-buffer context)
                                           (:maximum-packets options))]
    (if raw-input-packet
      (binary/deserialize raw-input-packet forms))))

(defn send-packet
  "Writes one destructured packet payload through a socket/device.
  Returns the number of bytes written on success."
  [context packet]
  (let [handle            (:handle context)
        entries           (:entries context)
        raw-output-packet (binary/serialize packet entries)
        write-buffer-size (get-in context [:options :write-buffer-size])]
    (native/write-raw handle raw-output-packet write-buffer-size)))

(defn close
  "Closes a packet socket/device context."
  [context]
  (native/close (:handle context)))

;
; DEBUG
;

(def my-form [:eth [:dst   :byte   6 ; array of 6 bytes (MAC address)
                    :src   :byte   6
                    :type  :short]   ; 16-bit integer
              :arp [:htype :short
                    :ptype :short
                    :hlen  :byte     ; single byte
                    :plen  :byte
                    :oper  :short
                    :sha   :byte   6
                    :spa   :byte   4 ; array of 4 bytes (IPv4 address)
                    :tha   :byte   6
                    :tpa   :byte   4]])

; Form-based deserialization utilities
(def my-opts {:immediate true :data-link-type :ieee80211})

(def my-ctx (open "iwn0" my-form my-opts))

