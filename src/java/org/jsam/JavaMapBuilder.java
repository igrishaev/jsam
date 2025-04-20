package org.jsam;

import java.util.HashMap;
import java.util.Map;

public class JavaMapBuilder implements IObjectBuilder {

    private final Map<String, Object> map = new HashMap<>();

    @Override
    public void assoc(String key, Object val) {
        map.put(key, val);
    }

    @Override
    public Object build() {
        return map;
    }
}
