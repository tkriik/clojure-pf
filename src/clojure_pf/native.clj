(ns clojure-pf.native
  "Wrapper functions to native packet filter routines."
  (:require [net.n01se.clojure-jna :as jna]))

(defn open [interface read-length data-link-type]
  "Returns a file descriptor pointing to a socket or BPF device
  that is configured with the given interface, data link type
  and read buffer length."
  (let [fd (jna/invoke Integer
                       clojure_pf/pf_open
                       interface
                       read-length
                       data-link-type)]
    (when-not (= fd -1)
      fd)))

(defn set-filter [fd instructions]
  "Sets the filter program to be used by a socket/device."
  ; TODO
  nil)

(defn read [fd size]
  "Reads at most size bytes from a socket/device.
  Returns a data buffer on success."
  (let [data  (byte-array size)
        nr    (jna/invoke Integer
                          clojure_pf/pf_read
                          fd
                          data
                         (count data))]
    (when-not (= nr -1)
      (take nr data))))

(defn write [fd data]
  "Writes data to a socket/device.
  Returns the number of bytes written on success."
  (let [nw (jna/invoke Integer
                       clojure_pf/pf_write
                       data
                       (count data))]
    (when-not (= nw -1)
      nw)))

(defn close [fd]
  "Closes a socket/device associated with the given file descriptor."
  (jna/invoke Void clojure_pf/pf_close fd))
