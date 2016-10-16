(ns clojure-pf.core.packet
  "Packet type definitions.")

; Destructured packet used in the application layer.
(defrecord Packet [timestamp payload])

; Packet-specific timestamp.
(defrecord Timestamp [seconds microseconds])

; Record type returned from socket/device reads, containing data
; for one or more packets.
(defrecord RawInputPacket [data timestamps payload-regions])

; Record type passed to socket/device reads, containing
; data and region info for one packet.
(defrecord RawOutputPacket [data payload-region]) 

; Record type for denoting the start index and size of a packet payload.
(defrecord RawPacketRegion [index size])
