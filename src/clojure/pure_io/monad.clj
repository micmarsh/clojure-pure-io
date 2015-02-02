(ns clojure.pure-io.monad
  (:require [clojure.algo.monads :refer (defmonad with-monad)]))
   
(defprotocol PerformIO (-perform-io [io]))

(deftype IOResult [v]
  PerformIO
  (-perform-io [_] v))

(deftype IOBind [io f]
  PerformIO
  (-perform-io [_]
    (-perform-io (f (-perform-io io)))))

(defmonad io-m
  [m-zero (IOResult. nil)
   m-result (fn m-result-io [v]
              (IOResult. v))
   m-bind (fn m-bind-io [m f]
            (if (instance? IOResult m)
             (f (.v m))
             (IOBind. m f)))])

(defmacro defn-io [name args & body]
  `(defn ~name [~@args]
     (with-monad io-m
       ~@body)))
