(ns examples.clojure.pure-io.sorting
  (:require [clojure.algo.monads :as m]
            [clojure.pure-io.core :as io]
            [clojure.pure-io.monad :refer (io-m)]
            [clojure.pure-io.impl :refer (read-all' println')]
            [clojure.string :refer (split)]))

(defn -main [& args]
  (io/perform-io!
   (m/with-monad io-m
     (m-bind read-all'
             (fn [in]
               (as-> in *
                   (split *  #"\n")
                   (sort *)
                   (apply println' *)))))))
