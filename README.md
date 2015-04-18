# Pure IO

An experiment in implementing and enforcing an IO monad in Clojure. Why should Haskell have all the fun(ctional purity)?

For an in-depth (and slightly rambling) explaination of the motivations and process that went into this, check out [my write-up](/gist.md)

## Why Use This?

You probably don't want to use this for any money-making purpose, but it was a lot of fun to write, and hopefully contains some educational value for others.

## Usage

* Include `[pure-io "0.1.0"]` and `[org.clojure/algo.monads "0.1.5"]` in project.clj

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

## License

Copyright © 2015 Michael Marsh

Distributed under the Eclipse Public License version 1.0
