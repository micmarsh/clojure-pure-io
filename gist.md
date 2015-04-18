# Purely Functional IO in Clojure

Clojure is an excellent functional programming language, but it's not *pure*. Purity means every function returns the same result given the same input, just like mathematical equations.

Here's an example of a pure function in Clojure:
```clojure
(def add-eight (partial + 8))

```
And, because we're going to be talking about it a lot anyway, in Haskell:
```haskell
addEight = (+ 8)
```
No matter what we pass to each function, we'll always get the same result. Now, let's add some impurity to the mix:
```clojure
(def print-plus-eight (comp println add-eight))
(def result (print-plus-eight 3)) ; prints "11"
(type result) ; => nil
```
and in a Haskell GHCI repl:
```Haskell
let printPlusEight = print . (+ 8)
let result = printPlusEight 3 -- doesn't print anything!
:t result
-- result :: IO ()
result
-- 11
```
Cool! Haskell, unlike most other languages, uses data structures to represent impure operations, such as printing to the screen.

In the Clojure example, `result` was of type `nil` because our function already printed and returned nothing, but in the Haskell example, our function just returned something of type `IO ()`, which means it's an IO action that hasn't actually happened yet, in that case printing `11` to the screen.

While this approach makes a lot of sense in statically-typed Haskell land, it's also fairly simple to implement in Clojure, for fun and learning purposes.

## The Data Structure

Since we'll be manipulating data, eventually using it to peform an actual IO operation, it makes sense to define a protocol for our data types to implement:
```clojure
(defprotocol PerformIO (-perform-io [io]))
```
`-perform-io` will evaluate the given data structure, performing some side effects and returning a result. While it's going to play an important part in our monad implementation below, it's essential to keep in mind that this doesn't necessarily have anything to do with monads, just our specific case.

Monads are, for our purposes, a "container" type for another value. Slightly more specifically, a monad is a monad because it defines two operations: one for "wrapping" values in a new instance of that monad, and another for "transforming" (in a pure, functional way), the value inside the monad into a new value.

With that in mind, let's use `clojure.algo.monad`'s `defmonad` to define a monad of our very own:
```clojure
(defmonad io-m
  [m-result (fn [v] (IOResult. v))
   m-bind (fn [m f] (IOBind. m f))])
```
Although I've deferred the actual work of the functions to two custom Clojure/Java types (which we'll get to in a second), you can see the two operations defined here:
*  `m-result` (called `return` in Haskell) will wrap the given value `v` in an `io-m` monad
*  `m-bind` (called `(>>=)` in Haskell), will use the function `f` to "transform" (again, purely functionally), the value inside the given monad `m`.

### IOResult for m-result
 Let's a define a type to represent an instance of an IO monad returned by `m-result`
```clojure
(deftype IOResult [v]
  PerformIO
  (-perform-io [_] v))
```
As you can see, when `-perform-io` is called on something of this type, all it does is return the value passed into it. This type is pretty boring, so let's move on.

### IOBind for m-bind
```clojure
(deftype IOBind [io f]
  PerformIO
  (-perform-io [_]
    (-perform-io (f (-perform-io io)))))
```

## Implementation
With all of the above in place, we can start to implement some functions to actually use these types and things we've been going on and on about. First, let's start with a macro:

```clojure
(defmacro as-io [& body]
  `(reify PerformIO
     (-perform-io [_]
       ~@body)))
```
Simple enough, right? Whatever code (presumably impure) you wrap inside of `as-io`, will be stuck inside an instance of `PerformIO`, so it's compatible with everything we defined above. For example
```clojure
(defn println' [& args]
  (as-io (apply println args)))

(def read-line' (as-io (read-line)))
```
`println'` takes a variable number of arguments, just like `println`, but returns an instance of `PerformIO`, a data structure that *represents* printing the arguments, rather than printing them right away.

`read-line'` is a great illustration of the "data structures for side effects" concept: it's not a function, because it doesn't have to be.

Now, to put it all together:
```clojure
(def echo
  (with-monad io-m
    (m-bind read-line' println')))

(-perform-io echo) ; prompts for stdin, prints whatever you type!
(-perform-io echo) ; does the same thing!
```
`with-monad io-m`, is just part of `clojure.algo.monad`: you have to declare what kind of monad you're using, so we use `io-m` from our `defmonad` expression above.

Boilerplate aside, hopefully it's pretty clear what's going on here: we `m-bind` `read-line'` to `println'`, and when we evaluate the result, we get the actions we expect!

Because neither `m-bind`, `read-line'`, or `println'` produce any side-effects, everything up until the point we call `-perform-io` is *purely* functional. Mission accomplished!