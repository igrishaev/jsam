package org.jsam;

import java.util.function.Supplier;

public class Suppliers {
    public static Supplier<IArrayBuilder> ARR_JAVA_LIST = ArrayListBuilder::new;
    public static Supplier<IArrayBuilder> ARR_CLJ_VECTOR = PersistentVectorBuilder::new;
    public static Supplier<IObjectBuilder> OBJ_JAVA_MAP = HashMapBuilder::new;
    public static Supplier<IObjectBuilder> OBJ_CLJ_MAP = PersistentMapBuilder::new;
}
