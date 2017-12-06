package com.defaultapps.easybind.sample;


import com.defaultapps.easybind.sample.base.MvpPresenter;
import com.defaultapps.easybind.sample.base.MvpView;
import com.defaultapps.easybind.sample.base.Navigator;

public interface MainContract {
    interface MainView extends MvpView {}
    interface MainPresenter extends MvpPresenter<MainView> {}
    interface MainNavigator extends Navigator<MainView> {}
}
