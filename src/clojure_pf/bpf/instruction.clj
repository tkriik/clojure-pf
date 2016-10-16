(ns clojure-pf.bpf.instruction
  "BPF instruction constants categorized by usage.")

(def class-table
  "Instruction classes."
  {:ld    0x00    ; load to acc
   :ldx   0x01    ; load to idx
   :st    0x02    ; store acc
   :stx   0x03    ; store idx
   :alu   0x04    ; op between accumulator and source
   :ret   0x06    ; return
   :misc  0x07})  ; miscellaneous

(def size-table
  "Data size variants."
  {:w     0x00    ; word
   :h     0x08    ; halfword
   :b     0x10})  ; byte

(def mode-table
  "Addressing modes."
  {:imm   0x00    ; constant
   :abs   0x20    ; data at fixed-offset
   :ind   0x40    ; data at variable-offset
   :mem   0x60    ; data in memory
   :len   0x80    ; packet length
   :msh   0xa0})  ; IP header length

(def op-table
  "Arithmetic and jump instructions."
  {:add   0x00    ; acc <- acc + k 
   :sub   0x10    ; acc <- acc - k 
   :mul   0x20    ; acc <- acc * k 
   :div   0x30    ; acc <- acc / k 
   :or    0x40    ; acc <- acc | k 
   :and   0x50    ; acc <- acc & k 
   :lsh   0x60    ; acc <- acc << k 
   :rsh   0x70    ; acc <- acc >> k 
   :neg   0x80    ; acc <- -acc  
   :ja    0x00    ; jump-if-above
   :jeq   0x10    ; jump-if-equal
   :jgt   0x20    ; jump-if-greater-than
   :jge   0x30    ; jump-if-greater-or-equal
   :jset  0x40})  ; jump-if-set

(def src-table
  "Value modes."
  {:k     0x00    ; constant
   :x     0x08})  ; index register

(def rval-table
  "Return value modes."
  {:k     0x00    ; constant
   :x     0x08    ; index register
   :a     0x10})  ; accumulator
