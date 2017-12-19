package com.defaultapps.sample.base;


import easybind.NavigatorClass;
import easybind.calls.OnAttach;
import easybind.calls.OnDetach;

@NavigatorClass
public class BaseNavigator<V extends MvpView> implements Navigator<V> {

    private V view;

    @Override
    @OnAttach
    public void bind(V view) {
        this.view = view;
    }

    @Override
    @OnDetach
    public void unbind() {
        view = null;
    }
}
