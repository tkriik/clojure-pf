(ns clojure-pf.native
  "Wrapper functions to native packet filter routines."
  (:require [net.n01se.clojure-jna :as jna]))

(defn open [interface
            read-buffer-size
            data-link-type
            header-complete
            immediate]
  "Returns a file descriptor pointing to a socket or BPF device
  that is configured with the given interface, read buffer size,
  data link type, header complete -flag and immediate read -flag."
  (let [fd (jna/invoke Integer
                       clojure_pf/pf_open
                       interface
                       read-buffer-size
                       data-link-type
                       header-complete
                       immediate)]
    (if-not (= fd -1)
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
    (if-not (= nr -1)
      data)))

(defn write [fd data]
  "Writes data to a socket/device.
  Returns the number of bytes written on success."
  (let [nw (jna/invoke Integer
                       clojure_pf/pf_write
                       data
                       (count data))]
    (if-not (= nw -1)
      nw)))

(defn close [fd]
  "Closes a socket/device associated with the given file descriptor."
  (jna/invoke Void clojure_pf/pf_close fd))
