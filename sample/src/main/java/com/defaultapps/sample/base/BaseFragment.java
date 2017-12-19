package com.defaultapps.sample.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import easybind.EasyBind;
import easybind.EasyBinder;
import easybind.bindings.BindLayout;

public class BaseFragment extends Fragment {

    @BindLayout
    public int layoutId;

    private EasyBinder easyBinder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        easyBinder = EasyBind.bind(this);
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        easyBinder.onAttach();
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
