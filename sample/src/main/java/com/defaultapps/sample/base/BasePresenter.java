package com.defaultapps.sample.base;


import easybind.PresenterClass;
import easybind.calls.OnAttach;
import easybind.calls.OnDetach;
import easybind.calls.OnDispose;

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
