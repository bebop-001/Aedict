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
package sk.baka.autils.bind.validator;

import java.lang.reflect.Field;

/**
 * Thrown if validation fails.
 * @author Martin Vysny
 */
public class ValidatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /**
     * The field in error.
     */
    public final Field field;

    public ValidatorException(String message, Throwable cause, Field field) {
        super(message, cause);
        this.field = field;
    }

    public ValidatorException(String message, final Field field) {
        super(message);
        this.field = field;
    }

    @Override
    public String getMessage() {
        // do not use field name in the error message: the field name is an implementation detail and is not to be shown to the user
        return "Validation failed: " + super.getMessage();
    }
}
