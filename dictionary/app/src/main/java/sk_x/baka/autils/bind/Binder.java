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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sk_x.baka.autils.MiscUtils;
import sk_x.baka.autils.bind.validator.Validator;
import sk_x.baka.autils.bind.validator.ValidatorException;

/**
 * Binds a value from a bean to given target component using given binder.
 * @author Martin Vysny
 */
public final class Binder {

    /**
     * Binds values from given bean to the context using given mapper.
     * @param <T> the context type
     * @param <A> the expected annotation. Fields lacking this annotation type will be skipped silently.
     * @param bean the bean to bind values from
     * @param mapper the mapper to use when mapping values to the context
     * @param context the context
     * @param validate if true then the validation will be performed
     * @throws ValidatorException if validation fails, if a type conversion is required and the conversion fails (e.g. cannot parse String to int etc).
     */
    public <T, A extends Annotation> void bindFromBean(final Object bean, final IValueMapper<T, A> mapper, T context, final boolean validate) throws ValidatorException {
        mapper.setContext(context);
        try {
            for (final Field f : bean.getClass().getFields()) {
                final A annotation = f.getAnnotation(mapper.getBindAnnotationType());
                if (annotation == null) {
                    continue;
                }
                final Object value = f.get(bean);
                if (validate) {
                    Validator.fromField(f).validate(value, f);
                }
                final Object converted;
                try {
                    converted = transform(value, mapper.getValueClass(annotation));
                } catch (ParseException ex) {
                    throw new ValidatorException(ex.getMessage(), ex, f);
                }
                mapper.setValue(annotation, converted);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Shouldn't happen", ex);
        }
    }

    /**
     * Binds values from given context to the bean using given mapper.
     * @param <T> the context type
     * @param <A> the expected annotation. Fields lacking this annotation type will be skipped silently.
     * @param bean the bean to bind values to
     * @param mapper the mapper to use when mapping values to the context
     * @param context the context
     * @param validate if true then the validation will be performed
     * @throws ValidatorException if validation fails, or if a type conversion is required and the conversion fails (e.g. cannot parse String to int etc).
     */
    public <T, A extends Annotation> void bindToBean(final Object bean, final IValueMapper<T, A> mapper, T context, final boolean validate) throws ValidatorException {
        mapper.setContext(context);
        try {
            for (final Field f : bean.getClass().getFields()) {
                final A annotation = f.getAnnotation(mapper.getBindAnnotationType());
                if (annotation == null) {
                    continue;
                }
                final Object value = mapper.getValue(annotation, primitiveToClass(f.getType()));
                final Object converted;
                try {
                    converted = transform(value, Collections.<Class<?>>singleton(f.getType()));
                } catch (ParseException ex) {
                    throw new ValidatorException(ex.getMessage(), ex, f);
                }
                if (validate) {
                    Validator.fromField(f).validate(converted, f);
                }
                f.set(bean, converted);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Shouldn't happen", ex);
        }
    }

    /**
     * Transforms given source object to required type. Only several combinations are supported. Performs parsing as necessary. Supports
     * primitive types, their object counterparts, enums and strings only.
     * @param source the source object to convert, must not be null.
     * @param targetClass convert to one of these classes
     * @return transformed object.
     * @throws IllegalArgumentException if such conversion is not possible because of incompatible types
     * @throws ParseException if the conversion fails
     */
    private Object transform(final Object source, final Set<Class<?>> targetClass) throws ParseException {
        if (source == null) {
            return null;
        }
        final Class<?> sourceClass = source.getClass();
        final Set<Class<?>> _targetClass = primitiveToClass(targetClass);
        // handle simple case
        if (_targetClass.contains(sourceClass)) {
            return source;
        }
        final IConverter converter = CONVERTERS.get(MiscUtils.isEnum(sourceClass) ? Enum.class : sourceClass);
        if (converter == null) {
            throw new IllegalArgumentException("Source class " + sourceClass + " is not supported");
        }
        final Object result = converter.convert(source, _targetClass);
        if (result == null) {
            throw new IllegalArgumentException("Unsupported conversion: from " + sourceClass + " to " + targetClass);
        }
        return result;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_CLASS = new HashMap<Class<?>, Class<?>>();

    static {
        PRIMITIVE_TO_CLASS.put(int.class, Integer.class);
        PRIMITIVE_TO_CLASS.put(double.class, Double.class);
        PRIMITIVE_TO_CLASS.put(float.class, Float.class);
        PRIMITIVE_TO_CLASS.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_CLASS.put(byte.class, Byte.class);
        PRIMITIVE_TO_CLASS.put(char.class, Character.class);
        PRIMITIVE_TO_CLASS.put(long.class, Long.class);
        PRIMITIVE_TO_CLASS.put(short.class, Short.class);
    }

    private static Class<?> primitiveToClass(final Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        }
        return PRIMITIVE_TO_CLASS.get(clazz);
    }

    private static Set<Class<?>> primitiveToClass(final Set<Class<?>> clazz) {
        final Set<Class<?>> result = new HashSet<Class<?>>();
        for (final Class<?> c : clazz) {
            result.add(primitiveToClass(c));
        }
        return result;
    }

    /**
     * Transforms object of a particular type to another type.
     */
    private static interface IConverter {

        /**
         * Converts given source object.
         * @param source the source value, must be of correct type. Must not be null.
         * @param targetClass the target class, must not be null nor empty. Must not contain classes of primitives. Must not be of same class as the source value.
         * @return transformed object. Returns null if the conversion is impossible.
         * @throws ParseException if the transformation fails.
         */
        Object convert(final Object source, final Set<Class<?>> targetClass) throws ParseException;
    }

    private static final class StringConverter implements IConverter {

        public Object convert(Object source, Set<Class<?>> targetClass) throws ParseException {
            final String src = (String) source;
            if (hasNumber(targetClass)) {
                if (src.trim().length() == 0) {
                    throw new ParseException("Please fill in a number", 0);
                }
                try {
                    final Long parsed = Long.valueOf(src);
                    return new NumberConverter().convert(parsed, targetClass);
                } catch (NumberFormatException ex) {
                    throw new ParseException("Failed to parse number [" + src + "]: " + ex.getMessage(), 0);
                }
            }
            if (targetClass.contains(Boolean.class)) {
                return Boolean.valueOf(src);
            }
            if (targetClass.contains(Character.class)) {
                return src.length() > 0 ? src.charAt(0) : 0;
            }
            final Class<?> e = hasEnum(targetClass);
            if (e != null) {
                try {
                    return Enum.valueOf((Class<Enum>) e, src);
                } catch (IllegalArgumentException ex) {
                    throw new ParseException("Unknown enum " + targetClass + " constant [" + source + "]: " + ex.toString(), 0);
                }
            }
            return null;
        }
    }

    private static final class NumberConverter implements IConverter {

        public Object convert(Object source, Set<Class<?>> targetClass) throws ParseException {
            final Number num = (Number) source;
            final Class<?> targetType = getBestConversion(num.getClass(), targetClass);
            if (targetType == Integer.class) {
                return num.intValue();
            }
            if (targetType == Long.class) {
                return num.longValue();
            }
            if (targetType == Short.class) {
                return num.shortValue();
            }
            if (targetType == Byte.class) {
                return num.byteValue();
            }
            if (targetType == Double.class) {
                return num.doubleValue();
            }
            if (targetType == Float.class) {
                return num.floatValue();
            }
            if (targetClass.contains(String.class)) {
                return source.toString();
            }
            final Class<?> e = hasEnum(targetClass);
            if (e != null) {
                final Object[] enumConstants = e.getEnumConstants();
                final int value = num.intValue();
                if (value < 0 || value >= enumConstants.length) {
                    throw new ParseException("No enum " + targetClass + " constant for index " + value, 0);
                }
                return enumConstants[value];
            }
            return null;
        }

        private Class<?> getBestConversion(final Class<? extends Number> clazz, final Set<Class<?>> targetClasses) {
            final List<Class<?>> conversions = getConversionTypes(clazz);
            for (final Class<?> c : conversions) {
                if (targetClasses.contains(c)) {
                    return c;
                }
            }
            return null;
        }

        private List<Class<?>> getConversionTypes(final Class<? extends Number> clazz) {
            if (clazz == Integer.class) {
                return Arrays.<Class<?>>asList(Integer.class, Long.class, Double.class, Float.class, Short.class, Byte.class);
            }
            if (clazz == Short.class) {
                return Arrays.<Class<?>>asList(Short.class, Integer.class, Long.class, Double.class, Float.class, Byte.class);
            }
            if (clazz == Byte.class) {
                return Arrays.<Class<?>>asList(Byte.class, Short.class, Integer.class, Long.class, Double.class, Float.class);
            }
            if (clazz == Long.class) {
                return Arrays.<Class<?>>asList(Long.class, Double.class, Float.class, Integer.class, Short.class, Byte.class);
            }
            if (clazz == Float.class) {
                return Arrays.<Class<?>>asList(Float.class, Double.class, Long.class, Integer.class, Short.class, Byte.class);
            }
            if (clazz == Double.class) {
                return Arrays.<Class<?>>asList(Double.class, Float.class, Long.class, Integer.class, Short.class, Byte.class);
            }
            throw new IllegalArgumentException("Unsupported number type: " + clazz);
        }
    }

    private static final class BooleanConverter implements IConverter {

        public Object convert(Object source, Set<Class<?>> targetClass) throws ParseException {
            final Boolean b = (Boolean) source;
            if (targetClass.contains(String.class)) {
                return b.toString();
            }
            return null;
        }
    }

    private static final class EnumConverter implements IConverter {

        public Object convert(Object source, Set<Class<?>> targetClass) throws ParseException {
            if (hasNumber(targetClass)) {
                return new NumberConverter().convert(((Enum) source).ordinal(), targetClass);
            }
            if (targetClass.contains(String.class)) {
                return source.toString();
            }
            return null;
        }
    }

    private static final class CharConverter implements IConverter {

        public Object convert(Object source, Set<Class<?>> targetClass) throws ParseException {
            if (targetClass.contains(String.class)) {
                return source.toString();
            }
            return null;
        }
    }
    private static final Map<Class<?>, IConverter> CONVERTERS = new HashMap<Class<?>, IConverter>();

    static {
        CONVERTERS.put(String.class, new StringConverter());
        CONVERTERS.put(Integer.class, new NumberConverter());
        CONVERTERS.put(Long.class, new NumberConverter());
        CONVERTERS.put(Short.class, new NumberConverter());
        CONVERTERS.put(Byte.class, new NumberConverter());
        CONVERTERS.put(Double.class, new NumberConverter());
        CONVERTERS.put(Float.class, new NumberConverter());
        CONVERTERS.put(Boolean.class, new BooleanConverter());
        CONVERTERS.put(Enum.class, new EnumConverter());
        CONVERTERS.put(Character.class, new CharConverter());
    }

    private static final boolean isNumber(final Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz);
    }

    private static final boolean hasNumber(final Collection<Class<?>> clazz) {
        for (final Class<?> c : clazz) {
            if (isNumber(c)) {
                return true;
            }
        }
        return false;
    }

    private static final Class<?> hasEnum(final Collection<Class<?>> clazz) {
        for (final Class<?> c : clazz) {
            if (c.isEnum()) {
                return c;
            }
        }
        return null;
    }
}
