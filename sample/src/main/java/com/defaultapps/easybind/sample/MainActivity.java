package com.defaultapps.easybind.sample;

import android.os.Bundle;
import android.widget.Button;

import com.defaultapps.easybind.bindings.BindNavigator;
import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.sample.base.BaseActivity;
import com.defaultapps.easybind.sample.base.BasePresenter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements MainContract.MainView {

    @BindView(R.id.activity_a)
    Button activityButton;

    @BindPresenter
    public MainContract.MainPresenter presenter = new MainPresenterImpl();

    @BindNavigator
    public MainContract.MainNavigator navigator = new MainNavigatorImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }
}