(ns clojure.pure-io.core
  (:require [clojure.pure-io.monad :refer (PerformIO -perform-io)]))

(def ^{:dynamic true :private true} *performing-io* false)

(defmacro ^:private when-io [& body]
  `(if *performing-io*
     (do ~@body)
     (throw (Exception. "Impure IO!"))))

(defn- rebind-input
  [^java.io.Reader stdin]
  (clojure.lang.LineNumberingPushbackReader.
   (proxy [java.io.Reader] []
     (close []
       (when-io (.close stdin)))
     (read [cbuf off len]
       (when-io
        (.read stdin cbuf off len))))))

(defn- rebind-output
  [^java.io.Writer stdout]
  (java.io.BufferedWriter.
   (proxy [java.io.Writer] []
     (close [] (when-io (.close stdout)))
     (flush [] (when-io (.flush stdout)))
     (write [cbuf off len]
       (when-io
        (.write stdout cbuf off len))))))

(defmacro as-io [& body]
  `(reify PerformIO
     (-perform-io [_]
       (binding [*performing-io* true]
         ~@body))))

(defn perform-io! [io]
  (binding [*in* (rebind-input *in*)
            *out* (rebind-output *out*)]
    (-perform-io io)))
