(ns clojure-pf
  "Packet socket/device context functions."
  (:require [clojure-pf.data-link-type :as dlt]
            [clojure-pf.native :as native]
            [clojure-pf.packet :as packet]))

; Packet socket/device context
(deftype Context [handle
                  form
                  options])

(def ^:private ^:const default-options
  "Default packet context options."
  {:read-buffer-size  65536
   :data-link-type    :null
   :header-complete   false
   :immediate         false})

(defn create-context
  "Creates a packet socket/device context with the given interface,
  form and options."
  ([interface form]
   (create-context interface form {}))
  ([interface form options]
   (let [options          (merge default-options options)
         read-buffer-size (:read-buffer-size options)
         data-link-type   (or (dlt/to-code (:data-link-type options)) 0)
         header-complete  (:header-complete options)
         immediate        (:immediate options)
         fd               (native/open interface
                                       read-buffer-size
                                       data-link-type
                                       header-complete
                                       immediate)]
     (if fd
       (->Context fd form options)))))

(defn receive
  "Returns one or more destructured packets."
  [context]
  (let [fd    (.handle context)
        form  (.form context)
        size  (:read-buffer-size (.options context))
        data  (native/read fd size)]
    (packet/deserialize data form)))

(defn destroy-context
  "Destroys a packet socket/device context."
  [context]
  (native/close (:handle context)))

;
; DEBUG
;

(def my-form [:fc     :buf  2
              :dur    :buf  2
              :dst    :buf  6
              :src    :buf  6
              :bssid  :buf  6
              :seq    :buf  2
              :data   :buf  1500])

(def my-opts {:data-link-type :ieee80211})

(def my-ctx (create-context "iwn0" my-form my-opts))

