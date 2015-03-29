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

If you "follow the types" (a very common admonishment in the Haskell community), and look at the implementation of `-perform-io` for `IOBind`, you should be able to see how everything lines up.