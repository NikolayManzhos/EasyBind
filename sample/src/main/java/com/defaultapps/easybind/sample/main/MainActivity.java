package com.defaultapps.easybind.sample.main;

import android.os.Bundle;
import android.widget.Button;

import com.defaultapps.easybind.bindings.BindNavigator;
import com.defaultapps.easybind.bindings.BindPresenter;
import com.defaultapps.easybind.Layout;
import com.defaultapps.easybind.sample.R;
import com.defaultapps.easybind.sample.base.BaseActivity;
import com.defaultapps.easybind.sample.base.BasePresenter;
import com.defaultapps.easybind.sample.utis.ProxyActivity;

import butterknife.BindView;
import butterknife.OnClick;

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