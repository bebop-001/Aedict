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
package sk_x.baka.autils.bind.validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The validator contract.
 * @author Martin Vysny
 */
public abstract class Validator {

    /**
     * Validates given value and throws an exception if the value is incorrect.
     * @param object the value to validate
     * @param field the field reference.
     * @throws ValidatorException the exception
     */
    public abstract void validate(final Object object, final Field field) throws ValidatorException;

    /**
     * Creates new validator which simply invokes given validators.
     * @param validators the validator list
     * @return the validator instance.
     */
    public static final Validator and(final Validator... validators) {
        return new Validator() {

            @Override
            public void validate(Object object, Field field) throws ValidatorException {
                for (final Validator validator : validators) {
                    validator.validate(object, field);
                }
            }
        };
    }

    /**
     * Returns a no-op validator which never throws an exception.
     * @return a no-op validator.
     */
    public static final Validator noop() {
        return new Validator() {

            @Override
            public void validate(Object object, Field field) throws ValidatorException {
            }
        };
    }

    /**
     * Creates validator for given annotated field.
     * @param f the field
     * @return non-null validator.
     */
    public static final Validator fromField(final Field f) {
        final List<Validator> v = new ArrayList<Validator>();
        if (f.getAnnotation(NotNull.class) != null || f.getType().isPrimitive()) {
            v.add(new NotNullValidator());
        }
        final Range range = f.getAnnotation(Range.class);
        if (range != null) {
            v.add(new RangeValidator(range.min(), range.max()));
        }
        final CustomValidator custom = f.getAnnotation(CustomValidator.class);
        if (custom != null && custom.value() != null) {
            for (final Class<? extends Validator> clazz : custom.value()) {
                try {
                    v.add(clazz.newInstance());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        final Length length = f.getAnnotation(Length.class);
        if (length != null) {
            v.add(new StringLengthValidator(length.min(), length.max()));
        }
        return and(v.toArray(new Validator[0]));
    }
}
