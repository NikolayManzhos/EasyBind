package easybind;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EasyBind {

    private static Map<Class<?>, Constructor<? extends EasyBinder>> BINDINGS = new LinkedHashMap<>();

    private static final String TAG = EasyBind.class.getSimpleName();
    private static final boolean DEBUG = false;

    private EasyBind() {
        throw new AssertionError("No instances.");
    }

    public static EasyBinder bind(Object target) {
        Class<?> targetCls = target.getClass();
        Constructor<? extends EasyBinder> constructor = findClassConstructor(targetCls);
        //noinspection TryWithIdenticalCatches
        if (constructor == null) {
            return EasyBinder.EMPTY;
        }
        try {
            return constructor.newInstance(target);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Constructor<? extends EasyBinder> findClassConstructor(Class<?> cls) {
        Constructor<? extends EasyBinder> bindingConstructor = BINDINGS.get(cls);
        if (BINDINGS.get(cls) != null) {
            if (DEBUG) Log.d(TAG, "Found constructor in cache");
            return bindingConstructor;
        } else {
            if (DEBUG) Log.d(TAG, "Constructor not found, creating new");
            //noinspection TryWithIdenticalCatches
            try {
                String clsName = cls.getName();
                Class<?> bindingClass = cls.getClassLoader().loadClass(clsName + "Binder");
                //noinspection unchecked
                bindingConstructor = (Constructor<? extends EasyBinder>) bindingClass.getConstructor(cls);
                BINDINGS.put(cls, bindingConstructor);
            } catch (ClassNotFoundException e) {
                if (DEBUG) Log.d(TAG, "Constructor not found");
                return null;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            return bindingConstructor;
        }
    }
}
