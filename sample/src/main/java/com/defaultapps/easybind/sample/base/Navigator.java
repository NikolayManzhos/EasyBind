package com.defaultapps.easybind.sample.base;

public interface Navigator<V extends MvpView> {

    void bind(V view);
    void unbind();
}
