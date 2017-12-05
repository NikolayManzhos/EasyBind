package com.defaultapps.easybind.sample;


import com.defaultapps.easybind.sample.base.MvpPresenter;
import com.defaultapps.easybind.sample.base.MvpView;

public interface MainContract {
    interface MainView extends MvpView {}
    interface MainPresenter extends MvpPresenter<MainView> {}
}
