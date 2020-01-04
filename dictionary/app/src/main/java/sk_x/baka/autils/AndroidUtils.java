/**
 *     AUtils - A collection of utility classes for the Android system.
Copyright (C) 2009 Martin Vysny

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk_x.baka.autils;

import android.app.Activity;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import sk_x.baka.autils.bind.BindToView;
import sk_x.baka.autils.bind.validator.ValidatorException;

/**
 * Contains various Android utilities.
 * @author Martin Vysny
 */
public class AndroidUtils {

    /**
     * Returns a safe wrapper for given interface. Any exceptions thrown from
     * interface methods are catched, logged and shown in a dialog.
     *
     * @param <T>
     *            the interface type
     * @param intf
     *            the interface class
     * @param instance
     *            the instance
     * @param activity owning activity which will show the error dialog. Android 1.6 is not able to show a dialog belonging to an Application object.
     * @return a protected proxy
     */
    public static <T> T safe(final Activity activity, final Class<T> intf, final T instance) {
        if (!intf.isInterface()) {
            throw new IllegalArgumentException("Must be an interface: " + intf);
        }
        return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), new Class<?>[]{intf}, new Safe(activity, instance)));
    }

    /**
     * Returns a safe wrapper for given interface. Any exceptions thrown from
     * interface methods are catched, logged and shown in a dialog.
     *
     * @param <T>
     *            the interface type
     * @param activity owning activity which will show the error dialog. Android 1.5 is not able to show a dialog belonging to an Application object.
     * @param instance
     *            the instance. The object must implement exactly one interface.
     * @return a protected proxy
     */
    @SuppressWarnings("unchecked")
    public static <T> T safe(final Activity activity, final T instance) {
        final Class<?>[] intfs = instance.getClass().getInterfaces();
        if (intfs.length == 0) {
            throw new IllegalArgumentException("Given class " + instance.getClass() + " does not implement any interfaces");
        }
        if (intfs.length > 1) {
            throw new IllegalArgumentException("Given class " + instance.getClass() + " implements multiple interfaces");
        }
        final Class<Object> intf = (Class) intfs[0];
        final Object safe = safe(activity, intf, instance);
        // this is a bit ugly. The safe object will not of type T anymore, but
        // this cast will succeed (because it is silently ignored by Java).
        return (T) safe;
    }

    private static class Safe implements InvocationHandler {

        private final Object instance;
        private final Activity activity;

        public Safe(final Activity activity, final Object instance) {
            this.activity = activity;
            this.instance = instance;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(instance, args);
            } catch (Exception ex) {
                handleError(ex, activity, instance.getClass(), method);
                if (method.getReturnType() == Boolean.class || method.getReturnType() == boolean.class) {
                    return false;
                }
                return null;
            }
        }
    }

    /**
     * Handles an application error by logging it and displaying an error dialog.
     * @param t the throwable
     * @param activity owning activity
     * @param problematicClass the problematic class where the exception was thrown
     * @param method the method where the problem occurred, may be null.
     */
    public static void handleError(final Throwable t, final Activity activity, final Class<?> problematicClass, final Method method) {
        final Throwable cause = unwrap(t);
        final boolean isValidatorError = cause instanceof ValidatorException;
        if (isValidatorError) {
            Log.i(activity.getClass().getSimpleName(), "Validation failed: " + cause.getMessage(), cause);
            final BindToView btw = ((ValidatorException) cause).field.getAnnotation(BindToView.class);
            if (btw != null) {
                activity.findViewById(btw.value()).requestFocus();
            }
            new DialogUtils(activity).showErrorDialog(cause.getMessage());
        } else {
            Log.e(problematicClass.getSimpleName(), "Exception thrown" + method == null ? "" : " while invoking " + method, cause);
            new DialogUtils(activity).showErrorDialog("An application problem occured: " + cause.toString());
        }
    }

    /**
     * Unwrap all {@link RuntimeException}s and
     * {@link InvocationTargetException}s.
     *
     * @param t
     *            the exception type
     * @return unwrapped throwable.
     */
    public static Throwable unwrap(final Throwable t) {
        Throwable current = t;
        while (current.getClass() == RuntimeException.class || current instanceof InvocationTargetException) {
            if (current.getCause() == null) {
                return current;
            }
            current = current.getCause();
        }
        return current;
    }
}
