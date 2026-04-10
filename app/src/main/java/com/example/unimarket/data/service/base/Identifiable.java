package com.example.unimarket.data.service.base;

public interface Identifiable<T> {
    String getId(T item);
    void setId(T item, String id);
}
