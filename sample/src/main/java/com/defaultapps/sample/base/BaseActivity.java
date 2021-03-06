package com.defaultapps.sample.base;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import easybind.EasyBind;
import easybind.EasyBinder;
import easybind.bindings.BindLayout;
import easybind.bindings.BindName;

public abstract class BaseActivity extends AppCompatActivity {

    @BindLayout
    public int layoutId;

    @BindName
    public String screenName;

    private EasyBinder easyBinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        easyBinder = EasyBind.bind(this);
        easyBinder.onAttach();
        setContentView(layoutId);
        Log.d("Activity", "ScreenName: " + screenName);
    }

    @Override
    protected void onStart() {
        super.onStart();
        easyBinder.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        easyBinder.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        easyBinder.onDetach();
    }
}
