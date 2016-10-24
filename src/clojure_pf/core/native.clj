(ns clojure-pf.core.native
  "Wrapper functions to native packet filter routines."
  (:require [net.n01se.clojure-jna          :as     jna]
            [clojure-pf.core.data-link-type :as     dlt]
            [clojure-pf.core.packet         :refer  :all]))

(defn open [interface
            read-buffer-size
            write-buffer-size
            data-link-type
            header-complete
            immediate]
  "Returns a file descriptor pointing to a socket or BPF device
  that is configured with the given interface, read buffer size,
  data link type, header complete -flag and immediate read -flag."
  (let [handle (jna/invoke Integer
                           clojure_pf/pf_open
                           interface
                           read-buffer-size
                           (dlt/to-code data-link-type)
                           header-complete
                           immediate)]
    (if-not (= handle -1)
      handle)))

(defn read-raw [handle buffer maximum-packets]
  "Reads at most 'maximum-packets' payloads to a buffer.
  Returns a RawInputPacket on success."
  (let [data            (.array buffer)
        seconds         (long-array maximum-packets)
        microseconds    (long-array maximum-packets)
        payload-indices (int-array maximum-packets)
        payload-sizes   (int-array maximum-packets)
        payload-count   (int-array 1)
        read-count      (jna/invoke Integer
                                    clojure_pf/pf_read
                                    handle
                                    data            (count data)
                                    maximum-packets
                                    seconds         microseconds
                                    payload-indices payload-sizes
                                    payload-count)]
    (if-not (= read-count -1)
      (let [payload-count   (first payload-count)
            timestamps      (map ->Timestamp
                                 (take payload-count seconds)
                                 (take payload-count microseconds))
            payload-regions (map ->RawPacketRegion
                                 (take payload-count payload-indices)
                                 (take payload-count payload-sizes))]
        (->RawInputPacket timestamps buffer payload-regions)))))

(defn write-raw [handle raw-output-packet write-buffer-size]
  "Writes at most 'write-buffer-size' bytes from a RawOutputPacket
  through a file handle. Returns the number of bytes written on success."
  (let [data      (:data raw-output-packet)
        data-size (get-in raw-output-packet [:payload-region :size])
        size      (min data-size write-buffer-size)
        nwritten  (jna/invoke Integer clojure_pf/pf_write handle data size)]
    (if-not (= nwritten -1)
      nwritten)))

(defn close [handle]
  "Closes a socket/device associated with the given file descriptor."
  (jna/invoke Void clojure_pf/pf_close handle))
