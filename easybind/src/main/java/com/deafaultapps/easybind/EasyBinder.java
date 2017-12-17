package com.deafaultapps.easybind;

public interface EasyBinder {
    void onAttach();
    void onDetach();
    void onStop();
    void onStart();

    EasyBinder EMPTY = new EasyBinder() {
        @Override
        public void onAttach() {}
        @Override
        public void onDetach() {}
        @Override
        public void onStop() {}
        @Override
        public void onStart() {}
    };
}
