(ns clojure-pf.packet
  "Packet type definitions.")

; Raw packet type returned from socket/device reads
; containing data for one or more packets.
(deftype RawPacket [data
                    header-boundaries
                    payload-boundaries])

; boundary type for denoting the start index and size of
; a packet header or payload.
(deftype RawPacketBoundary [index size])
