package org.jsam;

import java.util.ArrayList;
import java.util.List;

public class JavaListBuilder implements IArrayBuilder {

    private final List<Object> list = new ArrayList<>();

    @Override
    public void conj(final Object el) {
        list.add(el);
    }

    @Override
    public Object build() {
        return list;
    }
}
