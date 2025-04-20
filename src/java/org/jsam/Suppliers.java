package org.jsam;

import java.util.function.Supplier;

public class Suppliers {
    public static Supplier<IArrayBuilder> ARR_JAVA_LIST = JavaListBuilder::new;
    public static Supplier<IArrayBuilder> ARR_CLJ_VECTOR = ClojureVectorBuilder::new;
    public static Supplier<IObjectBuilder> OBJ_JAVA_MAP = JavaMapBuilder::new;
    public static Supplier<IObjectBuilder> OBJ_CLJ_MAP = ClojureMapBuilder::new;
}
