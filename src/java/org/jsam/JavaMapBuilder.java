package org.jsam;

import java.util.HashMap;
import java.util.Map;

public class JavaMapBuilder implements IObjectBuilder {

    private final Map<Object, Object> map = new HashMap<>();

    @Override
    public void assoc(final Object key, final Object val) {
        map.put(key, val);
    }

    @Override
    public Object build() {
        return map;
    }
}
