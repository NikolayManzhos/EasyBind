package com.defaultapps.sample.main;


import com.defaultapps.sample.base.MvpPresenter;
import com.defaultapps.sample.base.MvpView;
import com.defaultapps.sample.base.Navigator;

public interface MainContract {
    interface MainView extends MvpView {}
    interface MainPresenter extends MvpPresenter<MainView> {}
    interface MainNavigator extends Navigator<MainView> {}
}
