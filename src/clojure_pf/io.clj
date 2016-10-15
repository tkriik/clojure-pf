(ns clojure-pf.io
  "Wrapper functions to native packet filter routines."
  (:require [net.n01se.clojure-jna  :as     jna]
            [clojure-pf.packet      :refer  :all]))

(defn open [interface
            read-buffer-size
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
                           data-link-type
                           header-complete
                           immediate)]
    (if-not (= handle -1)
      handle)))

(defn read-raw [handle read-buffer-size maximum-packets]
  "Reads at most 'read-buffer-size' bytes from a socket/device handle,
  containing at most 'maximum-packets' payloads.
  Returns a RawPacket record on success."
  (let [data            (byte-array read-buffer-size)
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
        (->RawPacket data
                     timestamps
                     payload-regions)))))

(defn write [handle data]
  "Writes data to a socket/device.
  Returns the number of bytes written on success."
  (let [nw (jna/invoke Integer
                       clojure_pf/pf_write
                       data
                       (count data))]
    (if-not (= nw -1)
      nw)))

(defn close [handle]
  "Closes a socket/device associated with the given file descriptor."
  (jna/invoke Void
              clojure_pf/pf_close
              handle))
