package com.defaultapps.easybind.sample.base;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.deafaultapps.easybind.EasyBind;
import com.deafaultapps.easybind.EasyBinder;
import com.defaultapps.easybind.bindings.BindLayout;

public abstract class BaseActivity extends AppCompatActivity {

    @BindLayout
    public int layoutId;

    private EasyBinder easyBinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        easyBinder = EasyBind.bind(this);
        easyBinder.onAttach();
        setContentView(layoutId);
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
