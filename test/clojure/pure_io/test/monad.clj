(ns clojure.pure-io.test.monad
  (:require [clojure.test :refer :all]
            [clojure.pure-io.monad :refer (io-m defn-io)]
            [clojure.pure-io.core :as io]
            [clojure.pure-io.impl :refer (println')]
            [clojure.algo.monads :as m]))

(defn eval-io-expr [expr]
  (let [result (atom nil)
        output
        (with-out-str
          (reset! result (io/perform-io! expr)))]
    [output @result]))

(defn io= [& io-exprs]
  (apply = (map eval-io-expr io-exprs)))

(defn-io id-print [item]
  (m/domonad
   [_ (println' item)]
   item))

(deftest helper-functions
  (is (= ["hello\n" nil]
         (eval-io-expr (println' "hello")))))

(deftest monad-laws
  (m/with-monad io-m

    (testing "Left Identity"
      (is (io= (m-bind (m-result "hello") println')
               (println' "hello"))))

    (testing "Right Identity"
      (is (io= (m-bind (println' "hello") m-result)
               (println' "hello"))))

    (testing "Associativity"
      (is (io= (-> (m-result "hello") (m-bind id-print) (m-bind println'))
               (m-bind (m-result "hello") (fn [x] (m-bind (id-print x) println'))))))))