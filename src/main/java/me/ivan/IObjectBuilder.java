package me.ivan;

public interface IObjectBuilder {
    void assoc(final String key, final Object val);
    Object build();
}
