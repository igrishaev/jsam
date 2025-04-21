package org.jsam;

import clojure.lang.*;

public class ClojureMapBuilder implements IObjectBuilder {

    private ITransientMap map = PersistentArrayMap.EMPTY.asTransient();

    @Override
    public void assoc(final Object key, final Object val) {
        map = map.assoc(key, val);
    }

    @Override
    public Object build() {
        return map.persistent();
    }
}
