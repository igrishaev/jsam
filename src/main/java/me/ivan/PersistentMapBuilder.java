package me.ivan;

import clojure.lang.*;

public class PersistentMapBuilder implements IObjectBuilder {

    private ITransientMap map = PersistentArrayMap.EMPTY.asTransient();

    @Override
    public void assoc(String key, Object val) {
        map = map.assoc(Keyword.intern(key), val);
    }

    @Override
    public Object build() {
        return map.persistent();
    }
}
