package com.example.PropertyDemo.Builders;

import com.example.PropertyDemo.Property.SaleProperty;

public interface SalePropertyBuilder<T extends SalePropertyBuilder<T>> extends PropertyBuilder<T> {

    public T withPrice(double price);
    public SaleProperty build();

}
