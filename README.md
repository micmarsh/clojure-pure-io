# Pure IO

An experiment in implementing an IO monad in Clojure. Why should Haskell have all the fun(ctional purity)?

For an in-depth (and slightly rambling) explaination of the motivations and process that went into this, check out [my write-up](/gist.md)

## Why Use This?

You probably don't want to use this for any money-making purpose, but it was a lot of fun to write, and hopefully contains some educational value for others.

## Usage

Include `[pure-io "0.1.0"]` and `[org.clojure/algo.monads "0.1.5"]` in project.clj

### The Very General Idea

* Define an instance of the IO monad
```clojure
(require '[clojure.pure-io.impl :refer [println' read-line']]
         '[clojure.algo.monads :refer [with-monad]]
         '[clojure.pure-io.monad :refer [io-m]])

(def echo
  (with-monad io-m
    (m-bind read-line' println')))
```

* Peform your IO
```clojure
(require '[clojure.pure-io.core :refer [perform-io!]])

(perform-io! echo)
;; type "hello"
;; prints "hello"
```

* Bask in the glory of your functional purity

### Non-trivial Purposes

If you're really feeling bold, you can easily define your own pure IO operations using the `as-io` macro
```clojure
(require '[clojure.pure-io.core :refer [as-io]])

(defn pure-database-query [query]
  (as-io (some-database-operation query)))

(with-monad io-m
  (def db-read-print
    (m-bind (pure-database-query {:where ...})
            (partial println' "Database query results:"))))
```
`as-io` will return the code inside of it as an IO instance, for you to compose as you wish before calling `perform-io!` on it.
