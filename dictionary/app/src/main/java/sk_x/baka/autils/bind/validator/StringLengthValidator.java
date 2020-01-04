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
 * Validates the string length.
 * @author Martin Vysny
 */
public class StringLengthValidator extends Validator {

    public final int min;
    public final int max;

    public StringLengthValidator(final int min, final int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public void validate(Object object, Field field) throws ValidatorException {
        if (object == null) {
            return;
        }
        if (!(object instanceof String)) {
            throw new ValidatorException("Expected String but got " + object.getClass(), field);
        }
        final int length = ((String) object).length();
        if (length < min || length > max) {
            throw new ValidatorException(valueInRange(), field);
        }
    }

    private String valueInRange() {
        if (max == Integer.MAX_VALUE) {
            return "length should be at least " + min;
        }
        if (min <= 0) {
            return "length should be at most " + max;
        }
        return "length should be between " + min + " and " + max + " inclusive";
    }
}
