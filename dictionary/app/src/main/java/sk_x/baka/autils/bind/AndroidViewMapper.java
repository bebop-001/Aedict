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
package sk_x.baka.autils.bind;

import android.app.Activity;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Binds values to Android {@link View} objects.
 * @author Martin Vysny
 */
public class AndroidViewMapper implements IValueMapper<Activity, BindToView> {

    private final boolean allowAdapterViewStringLookup;

    /**
     * Creates new mapper instance.
     * @param allowAdapterViewStringLookup if true then descendants of {@link AdapterView} will allow a
     * String mapping besides the usual int mapping. In such case the {@link Adapter} is searched and
     * item's {@link Object#toString()} is matched against given string.
     */
    public AndroidViewMapper(boolean allowAdapterViewStringLookup) {
        this.allowAdapterViewStringLookup = allowAdapterViewStringLookup;

    }
    private Activity context;

    public void setContext(Activity context) {
        this.context = context;
    }

    private View getView(final BindToView bindAnnotation) {
        return context.findViewById(bindAnnotation.value());
    }

    public Object getValue(BindToView bindAnnotation, final Class<?> preferred) {
        final View view = getView(bindAnnotation);
        if (view instanceof Checkable) {
            return ((Checkable) view).isChecked();
        }
        if (view instanceof TextView) {
            return ((TextView) view).getText().toString();
        }
        if (view instanceof AdapterView<?>) {
            final AdapterView<?> a = (AdapterView<?>) view;
            if (preferred != String.class || !allowAdapterViewStringLookup) {
                return a.getSelectedItemPosition();
            }
            final Object item = a.getSelectedItem();
            return item == null ? "" : item.toString();
        }
        throw new UnsupportedOperationException("The view type " + view.getClass() + " is not supported");
    }

    public void setValue(BindToView bindAnnotation, Object value) {
        final View view = getView(bindAnnotation);
        // this has to be before TextView as all Checkables (e.g. CheckBox) are TextViews
        if (view instanceof Checkable) {
            ((Checkable) view).setChecked(value == null ? false : (Boolean) value);
            return;
        }
        if (view instanceof TextView) {
            ((TextView) view).setText(value == null ? "" : (String) value);
            return;
        }
        if (view instanceof AdapterView<?>) {
            final AdapterView<?> a = (AdapterView<?>) view;
            if (value == null) {
                a.setSelection(0);
                return;
            }
            if (allowAdapterViewStringLookup && value instanceof String) {
                final Adapter adapter = a.getAdapter();
                final int count = adapter.getCount();
                for (int i = 0; i < count; i++) {
                    final Object item = adapter.getItem(i);
                    if (item == null) {
                        continue;
                    }
                    if (value.equals(item.toString())) {
                        a.setSelection(i);
                        return;
                    }
                }
                return;
            }
            a.setSelection((Integer) value);
            return;
        }
        throw new UnsupportedOperationException("The view type " + view.getClass() + " is not supported");
    }

    public Set<Class<?>> getValueClass(BindToView bindAnnotation) {
        final View view = getView(bindAnnotation);
        if (view instanceof Checkable) {
            return Collections.<Class<?>>singleton(Boolean.class);
        }
        if (view instanceof TextView) {
            return Collections.<Class<?>>singleton(String.class);
        }
        if (view instanceof AdapterView<?>) {
            if (allowAdapterViewStringLookup) {
                return new HashSet<Class<?>>(Arrays.<Class<?>>asList(Integer.class, String.class));
            }
            return Collections.<Class<?>>singleton(Integer.class);
        }
        throw new UnsupportedOperationException("The view type " + view.getClass() + " is not supported");
    }

    public Class<BindToView> getBindAnnotationType() {
        return BindToView.class;
    }
}
