package com.defaultapps.easybind.sample.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.deafaultapps.easybind.EasyBind;
import com.deafaultapps.easybind.EasyBinder;
import com.defaultapps.easybind.bindings.BindLayout;
import com.defaultapps.easybind.bindings.BindName;

public class BaseFragment extends Fragment {

    @BindLayout
    public int layoutId;

    @BindName
    public String screenName;

    private EasyBinder easyBinder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        easyBinder = EasyBind.bind(this);
        easyBinder.onAttach();
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onDestroyView() {
        easyBinder.onDetach();
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        easyBinder.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        easyBinder.onStop();
    }
}
