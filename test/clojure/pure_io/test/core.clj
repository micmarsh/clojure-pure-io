(ns clojure.pure-io.test.core
  (:require [clojure.test :refer :all]
            [clojure.pure-io.impl :refer (println' read-line')]
            [clojure.pure-io.monad :refer (io-m)]
            [clojure.pure-io.core :refer (perform-io! as-io)]
            [clojure.algo.monads :as m]))

(def echo
  (m/with-monad io-m
    (m-bind read-line' println')))

(defn bad-print [& args]
  (println args)
  (println' args))

(defn- bad-read []
  (read-line)
  read-line')

(defmacro ^:private with-io
  [input & body]
  `(with-in-str ~input
     (with-out-str ~@body)))

(deftest basic-usage
  (testing "Simple usage works as expected"
    (let [result (with-io "hello" (perform-io! echo))]
      (is (= "hello\n" result)))))

(deftest throws-exceptions
  (m/with-monad io-m
    (with-in-str "first line\nsecond line\nthird line"

      (testing "Impure output throws exception"
        (is (thrown-with-msg?
             Exception #"Impure IO!"
             (perform-io! (m-bind echo bad-print)))))

      (testing "Impure (slightly contrived) input throws exception"
        (is (thrown-with-msg?
             Exception #"Impure IO!"
             (perform-io! (m-bind echo (fn [_] (bad-read))))))))))
