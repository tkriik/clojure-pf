(ns clojure-pf
  "Packet socket/device context functions."
  (:require [clojure-pf.data-link-type :as dlt]
            [clojure-pf.native :as native]))

(def ^:private ^:const default-options
  "Default packet context options."
  {:buffer-length   65536
   :data-link-type  :null
   :header-complete false
   :immediate       false})

(defn create-context
  "Creates a packet socket/device context with the given interface
  and option map."
  ([interface]
   (create-context interface {}))
  ([interface options]
   (let [options          (merge default-options options)
         buffer-length    (:buffer-length options)
         data-link-type   (or (dlt/to-code (:data-link-type options)) 0)
         header-complete  (:header-complete options)
         immediate        (:immediate options)
         fd               (native/open interface
                                       buffer-length
                                       data-link-type
                                       header-complete
                                       immediate)]
     (if fd
       {:handle         fd
        :buffer-length  buffer-length}))))

(defn receive
  "Receives possibly many destructured packets in a sequence."
  [context]
  (native/read (:handle context) (:buffer-length context)))

(defn destroy-context
  "Destroys a packet socket/device context."
  [context]
  (native/close (:handle context)))
