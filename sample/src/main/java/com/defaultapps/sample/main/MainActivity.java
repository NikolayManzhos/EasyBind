package com.defaultapps.sample.main;

import android.os.Bundle;
import android.widget.Button;

import com.defaultapps.sample.R;
import com.defaultapps.sample.base.BasePresenter;
import com.defaultapps.sample.utis.ProxyActivity;

import butterknife.BindView;
import butterknife.OnClick;
import easybind.Layout;
import easybind.bindings.BindNavigator;
import easybind.bindings.BindPresenter;

@Layout(id = R.layout.activity_main, name = "MainActivity")
public class MainActivity extends ProxyActivity implements MainContract.MainView {

    @BindView(R.id.activity_a)
    Button activityButton;

    @BindPresenter
    MainContract.MainPresenter presenter = new MainPresenterImpl();

    @BindNavigator
    public MainContract.MainNavigator navigator = new MainNavigatorImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((BasePresenter) presenter).getView();
    }

    @OnClick(R.id.activity_a)
    void onActivityAClick() {

    }
}