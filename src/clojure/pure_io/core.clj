(ns clojure.pure-io.core
  (:require [clojure.pure-io.monad :refer (PerformIO -perform-io)]))

(defmacro as-io [& body]
  `(reify PerformIO
     (-perform-io [_]
       ~@body)))

(defn perform-io! [io]
  (binding [*in* (rebind-input *in*)
            *out* (rebind-output *out*)]
    (-perform-io io)))

(defmacro defn-io
  "A convenience wrapper for the way `clojure.algo.monads` makes you declare your monad type.
   Example:
    ;; Instead of
    (defn do-stuff [things]
      (with-monad io-m
        ...do some stuff...))
    ;; can shorten to this
    (defn-io do-stuff [things]
       ...do the same stuff...)"
  [name doc args & body]
  (let [[doc args body]
        (if (vector? args)
          [doc args body]
          ["" doc (cons args body)])]
    `(defn ~name ~doc ~args
       (with-monad io-m
         ~@body))))
