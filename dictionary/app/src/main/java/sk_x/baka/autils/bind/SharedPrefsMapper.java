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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps values from and to {@link SharedPreferences}.
 * @author Martin Vysny
 */
public class SharedPrefsMapper implements IValueMapper<SharedPreferences, SharedPref> {

    private SharedPreferences context;

    public void setContext(SharedPreferences context) {
        this.context = context;
    }

    public Class<SharedPref> getBindAnnotationType() {
        return SharedPref.class;
    }

    public Object getValue(SharedPref bindAnnotation, final Class<?> preferred) {
        return context.getAll().get(bindAnnotation.key());
    }

    public void setValue(SharedPref bindAnnotation, Object value) {
        if (!bindAnnotation.removeOnNull() && value == null) {
            // do nothing
            return;
        }
        final Editor edit = context.edit();
        final String key = bindAnnotation.key();
        if (value == null) {
            edit.remove(key);
        } else if (value instanceof Boolean) {
            edit.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            edit.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            edit.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            edit.putLong(key, (Long) value);
        } else {
            edit.putString(key, (String) value);
        }
        edit.commit();
    }
    private static final Set<Class<?>> VALUE_CLASS = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.<Class<?>>asList(
            Boolean.class, Float.class, Integer.class, Long.class, String.class)));

    public Set<Class<?>> getValueClass(SharedPref bindAnnotation) {
        return VALUE_CLASS;
    }
}
