# Purely Functional IO in Clojure

Clojure is an excellent functional programming language (I repeat myself), but it's not *pure*. Purity means every function returns the same result given the same input, just like mathematical equations.

Here's an example of a pure function in Clojure:
```clojure
(def add-eight (partial + 8))

```
And, because we're going to be talking about it a lot anyway, in Haskell:
```haskell
addEight = (+ 8)
```
No matter what we pass to each function, we'll always get the same result (or an exception, in Clojure's case). Now, let's add some impurity to the mix:
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
Woah, wild! Haskell, unlike most other languages, uses data structures to represent impure operations, such as printing to the screen.

In the Clojure example, `result` was of type `nil` because our function already printed and returned nothing, but in the Haskell example, our function just returned something of type `IO ()`, which means it's an IO action that hasn't actually happened yet, in that case printing "11" to the screen.

Haskell's type system makes this concept especially pervasive: every Haskell program has a `main` value, which must be something of type `IO ()`. If you need to do any impure operation, you'll need to compose your data structures so it all comes together at `main`. The static type checker will ensure that everything is in order before your program ever even compiles.

While Clojure and Haskell pretty sharply diverge in the "static checking" department, we can still get pretty darn close to having enforcably pure IO in Clojure. If you're as geeked out by this stuff as I am, read on!

## The Data Structure

Since we'll be passing around some data, but eventually using it to peform an actual IO operation, it makes sense to define a protocol for our data types to implement:
```clojure
(defprotocol PerformIO (-perform-io [io]))
```
`-perform-io` will evaluate the given data structure, performing some side effects and returning a result. While it's going to play an important part in our monad implementation below, it's important to keep in mind that this doesn't necessarily have anything to do with monads, just our specific case.

Monads are, for our purposes, a "container" type for another value. Slightly more specifically, a monad is a monad because it defines two operations: one for "wrapping" values in a new instance of that monad, and another for "transforming" (in a pure, functional way), the value inside the monad into a new value.

With that in mind, let's use `clojure.algo.monad`'s `defmonad` to define a monad of our very own:
```clojure
(defmonad io-m
  [m-result (fn [v] (IOResult. v))
   m-bind (fn [m f] (IOBind. m f))])
```
Althought I've deferred the actual work of the functions to two custom Clojure/Java types (which we'll get to in a second), you can see the two operations defined here:
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
Much more exciting! To help figure out what's going on here, let's first let's look at the Haskell type signature of `>>=`:
```haskell
(>>=) :: Monad m => m a -> (a -> m b) -> m b
```
* `Monad m =>` is saying "m must be a monad"
* The first argument, `m a`, is a monad that can contain any type
 * this is the value `io` in `IOBind`'s constructor
* The argument, `(a -> m b)`, is a function that takes a value of type of `a`, and returns a function of type `b`
 * this is the function `f` in `IOBind`'s constructor
* `m b` is the return value of `>>=`, a new monad containing something of type `b`
 * this is an instance of `IOBind`

Now, remember how `-perform-io` executes the given IO action and returns a result? In the context of the above, we can think of `-perform-io` as a magical, non-monadic ([co-monadic, actually]()), function with type signature `m a -> a`, that takes a value *out* of its monad container.

If you "follow the types" (a very common admonishment in the Haskell community), and look at the implementation of `-perform-io` for `IOBind`, you should be able to see how everything lines up to do what's expected.

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

`read-line'` is a great illustration of the "data structures for side effects" conconcept: it's not a function, because it doesn't have to be.

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

## Bonus Points: a "Type System"

Functional purity is all well and good, but can we *enforce* it, similar to the way Haskell does? After all, there's nothing preventing this
```clojure
(defn bad-print [& args]
  (apply println args)
  (apply println' args))
```
which performs an impure IO operation right alongside returning a pure one. As it turns out we *kind of* can, thanks to Clojure's use of dynamically-bound variables for stdin and stdout, `*in*` and `*out*`.  To start, let's define a "higher level" `perform-io` function
```clojure
(defn perform-io! [io]
  (binding [*in* (rebind-input *in*)
            *out* (rebind-output *out*)]
    (-perform-io io)))
```
This function, as you can see, is basically a drop-in replacement for calling `-perform-io`, but with the important difference of re-binding `*in*` and `*out*`. What to we re-bind them to? Get ready for a big wall of code
```clojure
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
```
There's a lot of un-DRYness going on here, but hopefully the point is clear enough: for every possible IO operation, throw an exception unless `*performing-io*` is set to true. Now, let's revisit our old `as-io` macro, which we used to implement our printing and reading functions
```clojure
(defmacro as-io [& body]
  `(reify PerformIO
     (-perform-io [_]
       (binding [*performing-io* true]
         ~@body))))

(defn println' [& args]
  (as-io (apply println args)))

(def read-line' (as-io (read-line)))
```
Perfect! Now, as long as we follow a few conventions (always use `perform-io!` instead of `-perform-io`, don't manually bind `*performing-io*`, `*in*`, or `*out*`), we'll have a system that yells at us whenever we do something impure:
```clojure
(def bad-echo
  (with-monad io-m
    (m-bind read-line' bad-print)))

(perform-io! bad-echo)
;; read some input...
;; then throws Exception: "Impure IO!"
(perform-io! echo)
;; everything runs smoothly
```