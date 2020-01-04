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

/**
 * Checks that given object is a number in given range.
 * @author Martin Vysny
 */
public class RangeValidator extends Validator {

    public final long min;
    public final long max;

    public RangeValidator(final long min, final long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public void validate(Object object, Field field) throws ValidatorException {
        if (object == null) {
            return;
        }
        if (!(object instanceof Number)) {
            throw new ValidatorException("Expected number but got " + object.getClass(), field);
        }
        final long value = ((Number) object).longValue();
        if (value < min || value > max) {
            throw new ValidatorException(valueInRange(value), field);
        }
    }

    private String valueInRange(final long value) {
        if (max == Long.MAX_VALUE) {
            return value + " should be at least " + min;
        }
        if (min == Long.MAX_VALUE) {
            return value + " should be at most " + max;
        }
        return value + " should be between " + min + " and " + max + " inclusive";
    }
}
