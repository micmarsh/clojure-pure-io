(ns examples.clojure.pure-io.sorting
  (:require [clojure.algo.monads :as m]
            [clojure.pure-io.core :as io]
            [clojure.pure-io.impl :refer (read-all' println')]
            [clojure.string :refer (split join)]))

(def split-newline #(split % #"\n"))

(def sort-lines
  (comp (partial join \newline)
        sort
        split-newline))

(io/defn-io -main
  "Reads all lines from stdin, and prints them back in alphabetical order.
   Works best piping a file in, try sample.txt"
  [& _]
  (io/perform-io!
   (m-bind read-all' (comp println' sort-lines))))
