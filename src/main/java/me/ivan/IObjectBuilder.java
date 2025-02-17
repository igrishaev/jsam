package me.ivan;

public interface IObjectBuilder {
    void append(final Object key, final Object val);
    Object build();
}
