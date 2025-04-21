package org.jsam;

public interface IObjectBuilder {
    void assoc(final Object key, final Object val);
    Object build();
}
