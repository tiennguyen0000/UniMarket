package com.example.unimarket.data.service.base;

public interface Identifiable<T> {
    Long getId(T item);
    void setId(T item, Long id);
}
