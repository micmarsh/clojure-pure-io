(ns clojure.pure-io.test.core
  (:require [clojure.test :refer :all]
            [clojure.pure-io.impl :refer (println' read-line')]
            [clojure.pure-io.monad :refer (io-m)]
            [clojure.pure-io.core :refer (perform-io!)]
            [clojure.algo.monads :as m]))

(def echo
  (m/with-monad io-m
    (m-bind read-line' println')))

(defmacro ^:private with-io
  [input & body]
  `(with-in-str ~input
     (with-out-str ~@body)))

(deftest basic-usage
  (testing "Simple usage works as expected"
    (let [result (with-io "hello" (perform-io! echo))]
      (is (= "hello\n" result)))))
