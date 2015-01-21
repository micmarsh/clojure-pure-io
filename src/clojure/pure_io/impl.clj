(ns clojure.pure-io.impl
  (:require [clojure.pure-io.core :refer (as-io)]
            [clojure.pprint :refer (pprint)]))

(defmacro ^:private defprinter [name-sym]
  (let [name-sym' (symbol (str (name name-sym) "'"))]
    `(defn ~name-sym' [& args#]
       (as-io (apply ~name-sym args#)))))

(defprinter println)
(defprinter print)
(defprinter prn)
(defprinter pr)
(defprinter pprint)

(def read-line' (as-io (read-line)))
(def read' (as-io (read)))
