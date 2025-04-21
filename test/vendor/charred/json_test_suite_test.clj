(ns vendor.charred.json-test-suite-test
  (:require
   [jsam.core :as jsam]
   [clojure.test :refer :all]
   [clojure.string :as str]))

(defmacro str= [a b]
  `(= (str ~a) (str ~b)))

(deftest i-number-double-huge-neg-exp-test
  (is (= [0.0] (jsam/read-string "[123.456e-789]"))))

(deftest i-number-huge-exp-test
  (is (= [##Inf]
         (jsam/read-string "[0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006]"))))

(deftest i-number-neg-int-huge-exp-test
  (is (= [##-Inf] (jsam/read-string "[-1e+9999]"))))

(deftest i-number-pos-double-huge-exp-test
  (is (= [##Inf] (jsam/read-string "[1.5e+9999]"))))

(deftest i-number-real-neg-overflow-test
  (is (= [##-Inf] (jsam/read-string "[-123123e100000]"))))

(deftest i-number-real-pos-overflow-test
  (is (= [##Inf] (jsam/read-string "[123123e100000]"))))

(deftest i-number-real-underflow-test
  (is (= [0.0] (jsam/read-string "[123e-10000000]"))))

(deftest i-number-too-big-neg-int-test
  (is (= [-123123123123123123123123123123N]
         (jsam/read-string "[-123123123123123123123123123123]"))))

(deftest i-number-too-big-pos-int-test
  (is (= [100000000000000000000N] (jsam/read-string "[100000000000000000000]"))))

(deftest i-number-very-big-negative-int-test
  (is (= [-237462374673276894279832749832423479823246327846N]
         (jsam/read-string "[-237462374673276894279832749832423479823246327846]"))))

(deftest n-array-1-true-without-comma-test
  (is (thrown? Exception (jsam/read-string "[1 true]"))))

(deftest n-array-colon-instead-of-comma-test
  (is (thrown? Exception (jsam/read-string "[\"\": 1]"))))

(deftest n-array-comma-and-number-test
  (is (thrown? Exception (jsam/read-string "[,1]"))))

(deftest n-array-double-comma-test
  (is (thrown? Exception (jsam/read-string "[1,,2]"))))

(deftest n-array-double-extra-comma-test
  (is (thrown? Exception (jsam/read-string "[\"x\",,]"))))

(deftest n-array-extra-comma-test
  (is (thrown? Exception (jsam/read-string "[\"\",]"))))

(deftest n-array-incomplete-invalid-value-test
  (is (thrown? Exception (jsam/read-string "[x"))))

(deftest n-array-incomplete-test
  (is (thrown? Exception (jsam/read-string "[\"x\""))))

(deftest n-array-inner-array-no-comma-test
  (is (thrown? Exception (jsam/read-string "[3[4]]"))))

(deftest n-array-items-separated-by-semicolon-test
  (is (thrown? Exception (jsam/read-string "[1:2]"))))

(deftest n-array-just-comma-test
  (is (thrown? Exception (jsam/read-string "[,]"))))

(deftest n-array-just-minus-test
  (is (thrown? Exception (jsam/read-string "[-]"))))

(deftest n-array-missing-value-test
  (is (thrown? Exception (jsam/read-string "[   , \"\"]"))))

(deftest n-array-newlines-unclosed-test
  (is (thrown? Exception (jsam/read-string "[\"a\",\n4\n,1,"))))

(deftest n-array-number-and-comma-test
  (is (thrown? Exception (jsam/read-string "[1,]"))))

(deftest n-array-number-and-several-commas-test
  (is (thrown? Exception (jsam/read-string "[1,,]"))))

(deftest n-array-spaces-vertical-tab-formfeed-test
  (is (thrown? Exception (jsam/read-string "[\"a\"\\f]"))))

(deftest n-array-star-inside-test
  (is (thrown? Exception (jsam/read-string "[*]"))))

(deftest n-array-unclosed-test
  (is (thrown? Exception (jsam/read-string "[\"\""))))

(deftest n-array-unclosed-trailing-comma-test
  (is (thrown? Exception (jsam/read-string "[1,"))))

(deftest n-array-unclosed-with-new-lines-test
  (is (thrown? Exception (jsam/read-string "[1,\n1\n,1"))))

(deftest n-array-unclosed-with-object-inside-test
  (is (thrown? Exception (jsam/read-string "[{}"))))

(deftest n-number-++-test
  (is (thrown? Exception (jsam/read-string "[++1234]"))))

(deftest n-number-+1-test
  (is (thrown? Exception (jsam/read-string "[+1]"))))

(deftest n-number-+Inf-test
  (is (thrown? Exception (jsam/read-string "[+Inf]"))))

(deftest n-number--01-test
  (is (thrown? Exception (jsam/read-string "[-01]"))))

(deftest n-number--1.0.-test
  (is (thrown? Exception (jsam/read-string "[-1.0.]"))))

(deftest n-number--2.-test
  (is (thrown? Exception (jsam/read-string "[-2.]"))))

(deftest n-number--NaN-test
  (is (thrown? Exception (jsam/read-string "[-NaN]"))))

(deftest n-number-.-1-test
  (is (thrown? Exception (jsam/read-string "[.-1]"))))

(deftest n-number-.2e-3-test
  (is (thrown? Exception (jsam/read-string "[.2e-3]"))))

(deftest n-number-0-capital-E+-test
  (is (thrown? Exception (jsam/read-string "[0E+]"))))

(deftest n-number-0-capital-E-test
  (is (thrown? Exception (jsam/read-string "[0E]"))))

(deftest n-number-0.1.2-test
  (is (thrown? Exception (jsam/read-string "[0.1.2]"))))

(deftest n-number-0.3e+-test
  (is (thrown? Exception (jsam/read-string "[0.3e+]"))))

(deftest n-number-0.3e-test
  (is (thrown? Exception (jsam/read-string "[0.3e]"))))

(deftest n-number-0.e1-test
  (is (thrown? Exception (jsam/read-string "[0.e1]"))))

(deftest n-number-0e+-test
  (is (thrown? Exception (jsam/read-string "[0e+]"))))

(deftest n-number-0e-test
  (is (thrown? Exception (jsam/read-string "[0e]"))))

(deftest n-number-1-000-test
  (is (thrown? Exception (jsam/read-string "[1 000.0]"))))

(deftest n-number-1.0e+-test
  (is (thrown? Exception (jsam/read-string "[1.0e+]"))))

(deftest n-number-1.0e--test
  (is (thrown? Exception (jsam/read-string "[1.0e-]"))))

(deftest n-number-1.0e-test
  (is (thrown? Exception (jsam/read-string "[1.0e]"))))

(deftest n-number-1eE2-test
  (is (thrown? Exception (jsam/read-string "[1eE2]"))))

(deftest n-number-2.e+3-test
  (is (thrown? Exception (jsam/read-string "[2.e+3]"))))

(deftest n-number-2.e-3-test
  (is (thrown? Exception (jsam/read-string "[2.e-3]"))))

(deftest n-number-2.e3-test
  (is (thrown? Exception (jsam/read-string "[2.e3]"))))

(deftest n-number-9.e+-test
  (is (thrown? Exception (jsam/read-string "[9.e+]"))))

(deftest n-number-Inf-test
  (is (thrown? Exception (jsam/read-string "[Inf]"))))

(deftest n-number-NaN-test
  (is (thrown? Exception (jsam/read-string "[NaN]"))))

(deftest n-number-expression-test
  (is (thrown? Exception (jsam/read-string "[1+2]"))))

(deftest n-number-hex-1-digit-test
  (is (thrown? Exception (jsam/read-string "[0x1]"))))

(deftest n-number-hex-2-digits-test
  (is (thrown? Exception (jsam/read-string "[0x42]"))))

(deftest n-number-infinity-test
  (is (thrown? Exception (jsam/read-string "[Infinity]"))))

(deftest n-number-invalid+--test
  (is (thrown? Exception (jsam/read-string "[0e+-1]"))))

(deftest n-number-invalid-negative-real-test
  (is (thrown? Exception (jsam/read-string "[-123.123foo]"))))

(deftest n-number-minus-infinity-test
  (is (thrown? Exception (jsam/read-string "[-Infinity]"))))

(deftest n-number-minus-sign-with-trailing-garbage-test
  (is (thrown? Exception (jsam/read-string "[-foo]"))))

(deftest n-number-minus-space-1-test
  (is (thrown? Exception (jsam/read-string "[- 1]"))))

(deftest n-number-neg-int-starting-with-zero-test
  (is (thrown? Exception (jsam/read-string "[-012]"))))

(deftest n-number-neg-real-without-int-part-test
  (is (thrown? Exception (jsam/read-string "[-.123]"))))

(deftest n-number-neg-with-garbage-at-end-test
  (is (thrown? Exception (jsam/read-string "[-1x]"))))

(deftest n-number-real-garbage-after-e-test
  (is (thrown? Exception (jsam/read-string "[1ea]"))))

(deftest n-number-real-without-fractional-part-test
  (is (thrown? Exception (jsam/read-string "[1.]"))))

(deftest n-number-starting-with-dot-test
  (is (thrown? Exception (jsam/read-string "[.123]"))))

(deftest n-number-with-alpha-char-test
  (is (thrown? Exception (jsam/read-string "[1.8011670033376514H-308]"))))

(deftest n-number-with-alpha-test
  (is (thrown? Exception (jsam/read-string "[1.2a-3]"))))

(deftest n-number-with-leading-zero-test
  (is (thrown? Exception (jsam/read-string "[012]"))))

(deftest n-object-non-string-key-but-huge-number-instead-test
  (is (thrown? Exception (jsam/read-string "{9999E9999:1}"))))

(deftest n-structure-array-with-unclosed-string-test
  (is (thrown? Exception (jsam/read-string "[\"asd]"))))

(deftest n-structure-end-array-test
  (is (thrown? Exception (jsam/read-string "]"))))

(deftest n-structure-number-with-trailing-garbage-test
  (is (= 2 (jsam/read-string "2@"))))

(deftest n-structure-open-array-apostrophe-test
  (is (thrown? Exception (jsam/read-string "['"))))

(deftest n-structure-open-array-comma-test
  (is (thrown? Exception (jsam/read-string "[,"))))

(deftest n-structure-open-array-open-object-test
  (is (thrown? Exception (jsam/read-string "[{"))))

(deftest n-structure-open-array-open-string-test
  (is (thrown? Exception (jsam/read-string "[\"a"))))

(deftest n-structure-open-array-string-test
  (is (thrown? Exception (jsam/read-string "[\"a\""))))

(deftest n-structure-open-object-close-array-test
  (is (thrown? Exception (jsam/read-string "{]"))))

(deftest n-structure-open-object-open-array-test
  (is (thrown? Exception (jsam/read-string "{["))))

(deftest n-structure-unclosed-array-partial-null-test
  (is (thrown? Exception (jsam/read-string "[ false, nul"))))

(deftest n-structure-unclosed-array-test
  (is (thrown? Exception (jsam/read-string "[1"))))

(deftest n-structure-unclosed-array-unfinished-false-test
  (is (thrown? Exception (jsam/read-string "[ true, fals"))))

(deftest n-structure-unclosed-array-unfinished-true-test
  (is (thrown? Exception (jsam/read-string "[ false, tru"))))

(deftest y-array-arraysWithSpaces-test
  (is (= [[]] (jsam/read-string "[[]   ]"))))

(deftest y-array-empty-string-test
  (is (= [""] (jsam/read-string "[\"\"]"))))

(deftest y-array-empty-test
  (is (= [] (jsam/read-string "[]"))))

(deftest y-array-ending-with-newline-test
  (is (= ["a"] (jsam/read-string "[\"a\"]"))))

(deftest y-array-false-test
  (is (= [false] (jsam/read-string "[false]"))))

(deftest y-array-heterogeneous-test
  (is (= [nil 1 "1" {}] (jsam/read-string "[null, 1, \"1\", {}]"))))

(deftest y-array-null-test
  (is (= [nil] (jsam/read-string "[null]"))))

(deftest y-array-with-1-and-newline-test
  (is (= [1] (jsam/read-string "[1\n]"))))

(deftest y-array-with-leading-space-test
  (is (= [1] (jsam/read-string " [1]"))))

(deftest y-array-with-several-null-test
  (is (= [1 nil nil nil 2] (jsam/read-string "[1,null,null,null,2]"))))

(deftest y-array-with-trailing-space-test
  (is (= [2] (jsam/read-string "[2] "))))

(deftest y-number-0e+1-test
  (is (= [0.0] (jsam/read-string "[0e+1]"))))

(deftest y-number-0e1-test
  (is (= [0.0] (jsam/read-string "[0e1]"))))

(deftest y-number-after-space-test
  (is (= [4] (jsam/read-string "[ 4]"))))

(deftest y-number-double-close-to-zero-test
  (is (= [-1.0E-78]
         (jsam/read-string "[-0.000000000000000000000000000000000000000000000000000000000000000000000000000001]"))))

(deftest y-number-int-with-exp-test
  (is (= [200.0] (jsam/read-string "[20e1]"))))

(deftest y-number-minus-zero-test
  (is (= [0] (jsam/read-string "[-0]"))))

(deftest y-number-negative-int-test
  (is (= [-123] (jsam/read-string "[-123]"))))

(deftest y-number-negative-one-test
  (is (= [-1] (jsam/read-string "[-1]"))))

(deftest y-number-negative-zero-test
  (is (= [0] (jsam/read-string "[-0]"))))

(deftest y-number-real-capital-e-neg-exp-test
  (is (= [0.01] (jsam/read-string "[1E-2]"))))

(deftest y-number-real-capital-e-pos-exp-test
  (is (= [100.0] (jsam/read-string "[1E+2]"))))

(deftest y-number-real-capital-e-test
  (is (= [1.0E22] (jsam/read-string "[1E22]"))))

(deftest y-number-real-exponent-test
  (is (= [1.23E47] (jsam/read-string "[123e45]"))))

(deftest y-number-real-fraction-exponent-test
  (is (= [1.23456E80] (jsam/read-string "[123.456e78]"))))

(deftest y-number-real-neg-exp-test
  (is (= [0.01] (jsam/read-string "[1e-2]"))))

(deftest y-number-real-pos-exponent-test
  (is (= [100.0] (jsam/read-string "[1e+2]"))))

(deftest y-number-simple-int-test
  (is (= [123] (jsam/read-string "[123]"))))

(deftest y-number-simple-real-test
  (is (= [123.456789] (jsam/read-string "[123.456789]"))))

(deftest y-number-test
  (is (= [1.23E67] (jsam/read-string "[123e65]"))))

(deftest y-object-extreme-numbers-test
  (is (= {:min -1.0E28, :max 1.0E28}
         (jsam/read-string "{\"min\": -1.0e+28, \"max\": 1.0e+28}"))))

(deftest y-string-in-array-test
  (is (= ["asd"] (jsam/read-string "[\"asd\"]"))))

(deftest y-string-in-array-with-leading-space-test
  (is (= ["asd"] (jsam/read-string "[ \"asd\"]"))))

(deftest y-structure-true-in-array-test
  (is (= [true] (jsam/read-string "[true]"))))

(deftest y-structure-whitespace-array-test
  (is (= [] (jsam/read-string " [] "))))
