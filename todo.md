

keep java reader?
rollback writeArray(List)?

- fromStream
- fromURL
- fromFile
...
own buffer

?
fn-obj-key
fn-obj-val
fn-arr-val
fn-arr
fn-string
fn-num
fn-bool

tests
- charred
- clojure.data.json
- jsonista
- chesire

bench
- vs jsonista
- vs chesire

test encode:
- java.time
- regex
- all arrays
- date uri url file bb
- atom/refs
- macro to extend as string
- macro to extend custom

test writer dest:
- string
- char[]
- byte[]
- writer
- file
- output stream
- socket

encode functions
- fn-key
- fn-array
- fn-object
- fn-string
- fn-number

decode functions
- fn-key
- fn-array
- fn-object
- fn-string
- fn-number

tests:
- common tests
- generative tests (with jsonista)

benchmarks:
- vs data.json
- vs charred
- vs jsonista
