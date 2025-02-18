package me.ivan;

import clojure.lang.ITransientCollection;
import clojure.lang.PersistentVector;

public class VectorArrayBuilder implements IArrayBuilder {

    private ITransientCollection vector = PersistentVector.EMPTY.asTransient();

    @Override
    public void conj(final Object el) {
        vector = vector.conj(el);
    }

    @Override
    public Object build() {
        return vector.persistent();
    }
}
