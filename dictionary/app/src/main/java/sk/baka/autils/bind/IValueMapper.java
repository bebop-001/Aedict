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
package sk.baka.autils.bind;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * A value mapper between a field value and a bound component. Able to get or set a value to a component.
 * The value will always be of a correct type - binder is not required to perform any type conversions.
 * @param <T> the context type, see {@link #setContext(java.lang.Object)} for details.
 * @param <A> the bind annotation type
 * @author Martin Vysny
 */
public interface IValueMapper<T, A extends Annotation> {

    /**
     * Invoked directly after binder instantiation, before any other methods are invoked. Sets the binder context,
     * e.g. a form instance which holds all components, a map instance etc.
     * @param context the context. May be null if the binder does not require any context.
     */
    void setContext(final T context);

    /**
     * Returns the annotation required by this binder to perform its binding tasks.
     * @return the binding annotation class.
     */
    Class<A> getBindAnnotationType();

    /**
     * Retrieves a value from the component. The value must be of correct type as returned by {@link #getValueClass(java.lang.annotation.Annotation)}.
     * @param bindAnnotation the binding annotation instance
     * @param preferred the preferred class, may be null. Never a primitive type. May not be from the set returned by the {@link #getValueClass(java.lang.annotation.Annotation)}.
     * @return the value of the component. The mapper may try to match the class as requested by the <code>preferred</code> parameter (e.g. if it is able to provide
     * the value in several types), however it should not go as far as provide dynamic type conversion - this is performed automatically by the binder.
     */
    Object getValue(final A bindAnnotation, final Class<?> preferred);

    /**
     * Sets a value to the component. The value will be of correct type as returned by {@link #getValueClass(java.lang.annotation.Annotation)}.
     * @param bindAnnotation the binding annotation instance
     * @param value the value of the component.
     */
    void setValue(final A bindAnnotation, final Object value);

    /**
     * Returns the type of the component model. May return multiple types: in such case, {@link #getValue(java.lang.annotation.Annotation)}
     * may produce object of any of listed types, and {@link #setValue(java.lang.annotation.Annotation, java.lang.Object)} must accept
     * object of any of listed types.
     * @param bindAnnotation the binding annotation.
     * @return the component model type. Must not be null nor empty. Must not contain classes of primitives (e.g. int.class)
     */
    Set<Class<?>> getValueClass(final A bindAnnotation);
}
