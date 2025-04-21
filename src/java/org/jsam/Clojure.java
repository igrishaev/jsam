package org.jsam;

import clojure.lang.IFn;
import static clojure.java.api.Clojure.var;

public class Clojure {
    public static IFn keyword = var("clojure.core", "keyword");
}
