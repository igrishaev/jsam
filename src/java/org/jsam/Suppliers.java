package org.jsam;

import java.util.function.Supplier;

public class Suppliers {
    @SuppressWarnings("unused")
    public static Supplier<IArrayBuilder> ARR_JAVA_LIST = JavaListBuilder::new;
    public static Supplier<IArrayBuilder> ARR_CLJ_VEC = ClojureVectorBuilder::new;
    @SuppressWarnings("unused")
    public static Supplier<IObjectBuilder> OBJ_JAVA_MAP = JavaMapBuilder::new;
    public static Supplier<IObjectBuilder> OBJ_CLJ_MAP = ClojureMapBuilder::new;
}
