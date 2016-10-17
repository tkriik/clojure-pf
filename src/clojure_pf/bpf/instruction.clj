(ns clojure-pf.bpf.instruction
  "BPF instruction types.")

; BPF instruction type
(defrecord Instruction [opcode
                        true-offset
                        false-offset
                        operand])

(defn jump [opcode operand true-offset false-offset]
  "Constructs a jump instruction."
  (->Instruction opcode
                 true-offset
                 false-offset
                 operand)

(defn stmt [opcode operand]
  "Constructs a statement instruction."
  (->Instruction opcode
                 true-offset
                 false-offset
                 operand))
