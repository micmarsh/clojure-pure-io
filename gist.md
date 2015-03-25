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
let result = printPlusEight 3 ; doesn't print anything!
:t result
-- result :: IO ()
result
-- 11
```
Woah, wild! Haskell, unlike most other languages, uses data structures to represent impure operations, such as printing to the screen.

In the Clojure example, `result` was of type `nil` because our function already printed and returned nothing, but in the Haskell example, our function just returned something of type `IO ()`, which means it's an IO action that hasn't actually happened yet, in that case printing "11" to the screen.

Haskell's type system makes this concept especially pervasive: every Haskell program has a `main` value, which must be something of type `IO ()`. If you need to do any impure operation, you'll need to compose your data structures so it all comes together at `main`. The static type checker will ensure that everything is in order before your program ever even compiles.

## Implementing an IO Monad in Clojure

With the above knowledge in mind, the natural next question to is: how can we do this in Clojure? If you're as geeked out by this stuff as I am, read on!

### The Data Structure

Since we'll be passing around some data, but eventually using it to peform an actual IO operation, it makes sense to define a protocol for our data types to implement:
```clojure
(defprotocol PerformIO (-perform-io [io]))
```
Now that we have that, we can define some types, but first we need to attempt the world's shortest (and probably most incomplete) explaination of Monads.

Monads are, for our purposes, a "container" type for another value. Slightly more specifically, a monad is a monad as opposed to some other type because it defines two operations: one for "wrapping" values in a new instance of that monad, and another for "transforming" (in a pure, functional way), the value inside the monad into a new value.

With that in mind, let's use `clojure.algo.monad`'s `defmonad` to define a monad of our very own:
```clojure
(defmonad io-m
  [m-result (fn [v] (IOResult. v))
   m-bind (fn [m f] (IOBind. m f))])
```
Althought I've deferred the actual work of the functions to two custom Clojure/Java types (which we'll get to in a second), you can see the two operations defined here:
*  `m-result` (called `return` in Haskell) will wrap the given value `v` in an `io-m` monad
*  `m-bind` (called `(>>=)` in Haskell), will use the function `f` to "transform" (again, purely functionally), the value inside the given monad `m`.

#### IOResult for m-result
 Let's a define a type to represent an instance of an IO monad returned by `m-result`
```clojure
(deftype IOResult [v]
  PerformIO
  (-perform-io [_] v))
```
As you can see, when `-perform-io` is called on something of this type, all it does is return the value passed into it. As a reminder, `-perform-io`, is NOT part of the definition of a monad, it's just something special we want to be able to do with our IO data. Either way, this type is pretty boring, so let's move on.

#### IOBind for m-bind
```clojure
(deftype IOBind [io f]
  PerformIO
  (-perform-io [_]
    (-perform-io (f (-perform-io io)))))
```
Much more exciting! To help figure out what's going on here, let's look at the Haskell type signature of `>>=`:
```haskell
(>>=) :: Monad m => m a -> (a -> m b) -> m b
```
`Monad m =>` is saying "m must be a monad", and `a` and `b` can be of any types whatsoever. They could even be the same, but they don't have to be.

* The first argument, `m a`, is a monad that can contain any type
* The argument, `(a -> m b)`, is a function that takes a value of type of `a`, and returns a function of type `b`.
* `m b` is the return value of `>>=`, a new monad containing something of type `b`

Now, think of each instance of `IOBind` as a `m b`. Since we eventually want to perform some io action using `-perform-io`

So how does all this relate to `m-bind`/`IOBind`? If you look at `IOBind`'s constructor, imagine `io` is `m a` and `f` is `(a -> m b)`. Since all of these IO monads represent an IO action to do *eventually*,`(f (-perform-io io))` could be thought of as