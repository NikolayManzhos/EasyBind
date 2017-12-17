package com.defaultapps.easybind.sample.base;

import com.defaultapps.easybind.PresenterClass;
import com.defaultapps.easybind.calls.OnAttach;
import com.defaultapps.easybind.calls.OnDetach;
import com.defaultapps.easybind.calls.OnDispose;
import com.defaultapps.easybind.calls.OnStart;
import com.defaultapps.easybind.calls.OnStop;

@PresenterClass
public abstract class BasePresenter<V extends MvpView> implements MvpPresenter<V> {

    private V view;

    @Override
    @OnAttach
    public void onAttach(V view) {
        this.view = view;
    }

    @Override
    @OnDetach
    public void onDetach() {
        view = null;
    }

    public V getView() {
        return view;
    }

    @OnDispose
    @Override
    public void dispose() {

    }
}
