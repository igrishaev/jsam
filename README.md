
# JSAM

[wiki]: https://metalgear.fandom.com/wiki/Samuel_Rodrigues

A lightweight, zero-deps JSON parser and writer. Named after [Jetstream
Sam][wiki].

- Small: only 14 Java files with no extra libraries;
- Not the fastest one but is pretty good (see the chart below);
- Has got its own features, e.g. read and write multiple values;
- Flexible and extendable.

## Installation

Add a new dependency:

~~~clojure
;; lein
[com.github.igrishaev/jsam "0.1.0-SNAPSHOT"]

;; deps
com.github.igrishaev/jsam {:mvn/version "0.1.0-SNAPSHOT"}
~~~

Import the library:

~~~clojure
(ns org.some.project
  (:require
    [jsam.core :as jsam]))
~~~

## Reading

To read a string:

~~~clojure
(jsam/read-string "[42.3e-3, 123, \"hello\", true, false, null, {\"some\": \"map\"}]")

[0.0423 123 "hello" true false nil {:some "map"}]
~~~

To read any kind of a source: a file, a URL, a socket, an input stream, a
reader, etc:

~~~clojure
(jsam/read "data.json") ;; a file named data.json
(jsam/read (io/input-stream ...))
(jsam/read (io/reader ...))
~~~

Both functions accept an optional map of settings:

~~~clojure
(jsam/read-string "..." {...})
(jsam/read (io/file ...) {...})
~~~

Here is a table of options that affect reading:

| option                   | default       | comment                              |
|--------------------------|---------------|--------------------------------------|
| `:read-buf-size`         | 8k            | Size of a buffer to read             |
| `:temp-buf-scale-factor` | 2             | Scale factor for an innter buffer    |
| `:temp-buf-size`         | 255           | Inner temp buffer initial size       |
| `:parser-charset`        | UTF-8         | Must be an instance of `Charset`     |
| `:arr-supplier`          | `ARR_CLJ_VEC` | An object to collect array values    |
| `:obj-supplier`          | `OBJ_CLJ_MAP` | An object to collect key-value pairs |
| `:bigdec?`               | `false`       | Use BigDecimal when parsing numbers  |
| `:fn-key`                | `keyword`     | A function to process keys           |

If you want keys to stay strings, and parse large numbers using `BigDecimal` to
avoid infinite values, this is what you pass:

~~~clojure
(jsam/read-string "..." {:fn-key identity :bigdec? true})
~~~

We will discuss suppliers a bit later.

## Writing

To dump data into a string, use `write-string`:

~~~clojure
(jsam/write-string {:hello "test" :a [1 nil 3 42.123]})

"{\"hello\":\"test\",\"a\":[1,null,3,42.123]}"
~~~

To write into a destination, which might be a file, an output stream, a writer,
etc, use `write`:

~~~clojure
(jsam/write "data2.json" {:hello "test" :a [1 nil 3 42.123]})

;; or

(jsam/write (io/file ...))

;; or

(with-open [writer (io/writer ...)]
  (jsam/write writer {...}))
~~~

Both functions accept a map of options for writing:

| option             | default | comment                          |
|--------------------|---------|----------------------------------|
| `:writer-charset`  | UTF-8   | Must be an instance of `Charset` |
| `:pretty?`         | `false` | Use indents and line breaks      |
| `:pretty-indent`   | 2       | Indent growth for each level     |
| `:multi-separator` | `\n`    | How to split multiple values     |

This is how you pretty-print data:

~~~clojure
(jsam/write "data3.json"
            {:hello "test" :a [1 {:foo [1 [42] 3]} 3 42.123]}
            {:pretty? true
             :pretty-indent 4})
~~~

This is what you'll get (maybe needs some further adjustment):

~~~json
{
    "hello": "test",
    "a": [
        1,
        {
            "foo": [
                1,
                [
                    42
                ],
                3
            ]
        },
        3,
        42.123
    ]
}
~~~

## Handling Multiple Values

When you have 10.000.000 of rows of data to dump into JSON, a regular approach
is not developer friendly. It leads to a single array with 10M items that you
read into memory at once. Only few libraries provide facilities to read arrays
lazily.

It's much better to dump rows one by one into a stream and then read them one by
one without saturating memory. Here is how you do it:

~~~clojure
(jsam/write-multi "data4.json"
                  (for [x (range 0 3)]
                    {:x x}))
~~~

The second argument is a collection that might be lazy as well. The content of
the file is:

~~~json
{"x":0}
{"x":1}
{"x":2}
~~~

Now read it back:

~~~clojure
(doseq [item (jsam/read-multi "data4.json")]
  (println item))

;; {:x 0}
;; {:x 1}
;; {:x 2}
~~~

The `read-multi` function returns a **lazy** iterable object meaning it won't
read everything at once. Also, both `write-` and `read-multi` functions are
pretty-print friendly:

~~~clojure
;; write
(jsam/write-multi "data5.json"
                  (for [x (range 0 3)]
                    {:x [x x x]})
                  {:pretty? true})

;; read
(doseq [item (jsam/read-multi "data5.json")]
  (println item))

;; {:x [0 0 0]}
;; {:x [1 1 1]}
;; {:x [2 2 2]}
~~~

The content of the data5.json file:

~~~json
{
  "x": [
    0,
    0,
    0
  ]
}
{
  "x": [
    1,
    1,
    1
  ]
}
{
  "x": [
    2,
    2,
    2
  ]
}
~~~

## Type Mapping and Extending

This chapter covers how to control type mapping between Clojure and JSON realms.

Writing is served using a protocol named `jsam.core/IJSON` with a single encidng
method:

~~~clojure
(defprotocol IJSON
  (-encode [this writer]))
~~~

The default mapping is the following:

| Clojure | JSON   | Comment                   |
|---------|--------|---------------------------|
| nil     | null   |                           |
| String  | string |                           |
| Boolean | bool   |                           |
| Number  | number |                           |
| Ratio   | string | e.g. `(/ 3 2)` -> `"3/2"` |
| Atom    | any    | gets `deref`-ed           |
| Ref     | any    | gets `deref`-ed           |
| List    | array  | lazy seqs as well         |
| Map     | object | keys coerced to strings   |
| Keyword | string | leading `:` is trimmed    |

Anything else gets encoded like a string using the `.toString` invocation under
the hood:

~~~clojure
(extend-protocol IJSON
  ...
  Object
  (-encode [this ^JsonWriter writer]
    (.writeString writer (str this)))
  ...)
~~~

Here is how you override encoding. Imagine you have a special type `SneakyType`:

~~~clojure
(deftype SneakyType [a b c]

  ;; some protocols...

  jsam/IJSON
  (-encode [this writer]
    (jsam/-encode ["I used to be a SneakyType" a b c] writer)))
~~~

Test it:

~~~clojure
(let [data1 {:foo (new SneakyType :a "b" 42)}
      string (jsam/write-string data1)]
  (jsam/read-string string))

;; {:foo ["I used to be a SneakyType" "a" "b" 42]}
~~~

When reading the data, there is a way to specify how array and object values get
collected. Options `:arr-supplier` and `:obj-supplier` accept a `Supplier`
instance where the `get` method returns instances of `IArrayBuilder` or
`IObjectBuilder` interfaces. Each interface knows how to add a value into a
collection how to finalize it.

Default implementations build Clojure persistent collections like
`PersistentVector` or `PersistenHashMap`. There is a couple of Java-specific
suppliers that build `ArrayList` and `HashMap`, respectively. Here is how you
use them:

~~~clojure
(jsam/read-string "[1, 2, 3]"
                  {:arr-supplier jsam/sup-arr-java})

;; [1 2 3]
;; java.util.ArrayList

(jsam/read-string "{\"test\": 42}"
                  {:obj-supplier jsam/sup-obj-java})

;; {:test 42}
;; java.util.HashMap
~~~

Here are some crazy examples that allow to modify data while you build
collections. For an array:

~~~clojure
(let [arr-supplier
      (reify java.util.function.Supplier
        (get [this]
          (let [state (atom [])]
            (reify org.jsam.IArrayBuilder
              (conj [this el]
                (swap! state clojure.core/conj (* el 10)))
              (build [this]
                @state)))))]

  (jsam/read-string "[1, 2, 3]"
                    {:arr-supplier arr-supplier}))

;; [10 20 30]
~~~

And for an object:

~~~clojure
(let [obj-supplier
      (jsam/supplier
        (let [state (atom {})]
          (reify org.jsam.IObjectBuilder
            (assoc [this k v]
              (swap! state clojure.core/assoc k (* v 10)))
            (build [this]
              @state))))]

  (jsam/read-string "{\"test\": 1}"
                    {:obj-supplier obj-supplier}))

;; {:test 10}
~~~

## Benchmarks

Jsam doesn't try to gain as much performance as possible; tuning JSON reading
and writing is pretty challenging. But so far, the library is not as bad as you
might think! It's two times slower that Jsonista and slightly slower than
Cheshire. But it's times faster than data.json which is written in pure Clojure
and thus is so slow.

The chart below renders my measures of reading a 100MB Json file. Then the data
read from this file were dumped into a string. It's pretty clear that Json is
not the best nor the worst one in this competition. I'll keep the question of
performance for further work.

Measured on MacBook M3 Pro 36Gb.

![](/media/benches.svg)

## On Tests

One can be interested in how this library was tested. Although being considered
as a simple format, JSON has got plenty of surprises. Jsam has tree sets of
tests, namely:

[charred]: https://github.com/cnuernber/charred
[cnuernber]: https://github.com/cnuernber

- basic cases written by me;
- a large test suite borrowed from the [Charred library][charred]. Many thanks
  to [Chris Nuernberger][cnuernber] who allowed me to use his code.
- an extra set of generative tests borrowed from the official
  `clojure.data.json` library developed by Clojure team.

These three, I believe, cover most of the cases. Should you face any weird
behavior, please let me know.

~~~
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
Ivan Grishaev, 2025. © UNLICENSE ©
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
~~~
