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
(def result (print-plus-eight 3)) ; prints 11
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

In the Clojure example, `result` was of type `nil` because our function already printed and returned nothing, but in the Haskell example, our function just returned something of type `IO ()`, which means it's an IO action that hasn't actually happened yet, in this case printing `11` to the screen.

This approach makes a lot of sense in statically-typed Haskell land, but it's also fairly simple to implement in Clojure, for fun and learning purposes.

## The Data Structure

### The Protocol

Since we'll be manipulating data, eventually using it to peform an actual IO operation, it makes sense to define a protocol for our data types to implement:

```clojure
(defprotocol PerformIO (-perform-io [io]))
```
`-perform-io` will evaluate the given data structure, presumably performing some side effects and returning a result.

Now, we can define a way to create an arbitrary instance of `PerformIO`
```clojure
(defmacro as-io [& body]
  `(reify PerformIO
     (-perform-io [_]
       ~@body)))
```
and use it to create a pure version of `println`
```clojure
(defn println' [& args]
  (as-io (apply println args)))
```
Now, we can re-implement our example from above
```clojure
(def print-plus-eight (comp println' add-eight))
(def result (print-plus-eight 3)) ; doesn't print!
(type result) ; => definitely not nil!

(-perform-io result) ; => prints 11
```
### Composition (Monads)

So far, the above isn't much more than a glorifed `delay`. However, there's more we can do: use monads to structure impure operations in a pure way.

Monads are, for our purposes, a "container" type for another value. Slightly more specifically, a monad is a monad because it defines two operations:
* one for "wrapping" values in a new instance of that monad
* another for "transforming" (in a pure, functional way), the value inside the monad into a new value.

For a more detailed explaination of monads that won't make category theorists cringe, check out the resources referred to by [`clojure.algo.monad`](https://github.com/clojure/algo.monads), although the above should be good enough for the rest of this gist.

With that in mind, let's use `clojure.algo.monad`'s `defmonad` to define a monad of our very own:
```clojure
(defmonad io-m
  [m-result (fn [v] (IOResult. v))
   m-bind (fn [m f] (IOBind. m f))])
```
Although I've deferred the actual work of the functions to two custom Clojure/Java types (which we'll get to in a second), you can see the two operations defined here:
*  `m-result` will wrap the given value `v` in an `io-m` monad
*  `m-bind` will use the function `f` to "transform" (again, purely functionally), the value inside the given monad `m`.

Let's a define a type to represent an instance of an IO monad returned by `m-result`
```clojure
(deftype IOResult [v]
  PerformIO
  (-perform-io [_] v))
```
As you can see, when `-perform-io` is called on something of this type, all it does is return the value passed into it. This type is pretty boring, so let's move on.

```clojure
(deftype IOBind [io f]
  PerformIO
  (-perform-io [_]
    (-perform-io (f (-perform-io io)))))
```
There's a lot going on here, but the most important part is the placement of `f`: when `-perform-io` is called on an instance of `IOBind`, we call `f` on whatever the result of `io` is.

With our instance of `io-m` fully defined, let's compose some operations
```clojure
(with-monad io-m
  (def echo
    (m-bind read-line' println')))

(-perform-io echo) ; prompts for stdin, prints whatever you type!
(-perform-io echo) ; does the same thing!
```
`with-monad` boilerplate aside, hopefully it's pretty clear what's going on here: the result of `read-line'` will be passed to `println'`, and we have a new monad representing an `echo` operation.

Because neither `m-bind`, `read-line'`, or `println'` produce any side-effects, everything up until the point we call `-perform-io` is *purely* functional. Mission accomplished!

For all of the code used here, and more examples, check out the [parent repo](https://github.com/micmarsh/clojure-pure-io)
