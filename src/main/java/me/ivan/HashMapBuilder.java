package me.ivan;

import java.util.HashMap;
import java.util.Map;

public class HashMapBuilder implements IObjectBuilder {

    private final Map<Object, Object> map = new HashMap<>();

    @Override
    public void append(Object key, Object val) {
        map.put(key, val);
    }

    @Override
    public Object build() {
        return map;
    }
}
