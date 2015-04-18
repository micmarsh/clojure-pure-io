(ns examples.clojure.pure-io.guessing
  (:require [clojure.algo.monads :as m]
            [clojure.pure-io.core :as io]
            [clojure.pure-io.impl :refer (read-line' println')]))

(def prompt' #(io/as-io (println %) (read-line)))

(defn rand-int'
  "Wrap our impure random number generation in IO,
   just to get in the spirit of things :-)"
  ([upper] (rand-int' 0 upper))
  ([lower upper]
     (io/as-io
      (-> (rand)
          (* (- upper lower))
          (+ lower)
          (int)))))

(defn cast-int [string]
  (try
    (Integer. string)
    (catch NumberFormatException e)))

(def ^:const success "You guessed correctly!\n")

(defn eval-guess [secret guess]
  (cond (nil? guess)
        "That's not an integer"
        (> guess secret)
        "Too high, guess again!"
        (< guess secret)
        "Too low, guess again!"
        (= guess secret)
        success))

(declare number-game)

(io/defn-io eval-feedback [message secret]
  (let [m-message (println' message)]
    (if (= success message)
      m-message
      (m-bind m-message
              (constantly (number-game secret))))))

(io/defn-io number-game [secret-number]
  (m/domonad
   [guess-str (prompt' "Guess a number between 1 and 100!")
    :let [guess-num (cast-int guess-str)
          feedback (eval-guess secret-number guess-num)]
    result (eval-feedback feedback secret-number)]
   result))

(io/defn-io -main
  "Follow the prompts to guess the proper number between on and one hundre"
  [& _]
  (io/perform-io!
   (let [m-secret-number (rand-int' 1 101)]
     (m-bind m-secret-number number-game))))
