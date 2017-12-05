package com.defaultapps.easybind.sample;

import android.os.Bundle;

import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.sample.base.BaseActivity;

import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements MainContract.MainView {

    @BindPresenter
    public MainContract.MainPresenter presenter = new MainPresenterImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }
}