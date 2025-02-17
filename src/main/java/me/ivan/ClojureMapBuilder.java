package me.ivan;

import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.PersistentArrayMap;

public class ClojureMapBuilder implements IObjectBuilder {

    private ITransientMap map = PersistentArrayMap.EMPTY.asTransient();

    @Override
    public void append(Object key, Object val) {
        map = map.assoc(key, val);
    }

    @Override
    public Object build() {
        return map.persistent();
    }
}
