(ns clojure-pf.native
  "Wrapper functions to native packet filter routines."
  (:require [net.n01se.clojure-jna :as jna]))

(defn open [interface data-link-type]
  "Returns a file descriptor pointing to a socket or BPF device
  that is associated with the given interface and data link type."
  (let [fd (jna/invoke Integer
                       clojure_pf_native/pf_open
                       interface
                       data-link-type)]
    (when-not (= fd -1)
      fd)))

(defn set-read-buffer-size [fd size]
  "Sets the read buffer size of a socket/device.
  Returns true on success, false otherwise."
  (let [rc (jna/invoke Integer
                       clojure_pf_native/pf_set_read_buffer_size
                       fd
                       size)]
    (not= rc -1)))

(defn set-filter [fd instructions]
  "Sets the filter program to be used by a socket/device."
  ; TODO
  nil)

(defn read [fd data]
  "Reads from a socket/device to a data buffer.
  Returns the number of bytes read on success."
  (let [nr (jna/invoke Integer
                       clojure_pf_native/pf_read
                       data
                       (count data))]
    (when-not (= nr -1)
      nr)))

(defn write [fd data]
  "Writes data to a socket/device.
  Returns the number of bytes written on success."
  (let [nw (jna/invoke Integer
                       clojure_pf_native/pf_write
                       data
                       (count data))]
    (when-not (= nw -1)
      nw)))

(defn close [fd]
  "Closes a socket/device associated with the given file descriptor."
  (jna/invoke Void clojure_pf_native/pf_close fd))
