(ns examples.clojure.pure-io.sorting
  (:require [clojure.algo.monads :as m]
            [clojure.pure-io.core :as io]
            [clojure.pure-io.monad :refer (defn-io)]
            [clojure.pure-io.impl :refer (read-all' println')]
            [clojure.string :refer (split)]))

(def split-newline #(split % #"\n"))

(def println-all' (partial apply println'))

(defn-io -main [& args]
  (io/perform-io!
   (m-bind read-all'
           (comp println-all'
                 sort
                 split-newline))))
