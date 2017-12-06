package com.deafaultapps.easybind;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class EasyBind {

    public static EasyBinder bind(Object target) {
        Class<?> targetCls = target.getClass();

        String clsName = targetCls.getName();
        try {
            Class<?> bindingClass= targetCls.getClassLoader().loadClass(clsName + "Binder");
            //noinspection unchecked
            Constructor<? extends EasyBinder> constructor
                    = (Constructor<? extends EasyBinder>) bindingClass.getConstructor(targetCls);
            return constructor.newInstance(target);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
