package com.defaultapps.easybind.sample.base;

import com.defaultapps.easybind.NavigatorClass;
import com.defaultapps.easybind.calls.OnAttach;
import com.defaultapps.easybind.calls.OnDetach;

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
