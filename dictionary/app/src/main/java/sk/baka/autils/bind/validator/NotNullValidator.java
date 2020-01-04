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
 * Checks that the value is not null.
 * @author Martin Vysny
 */
public class NotNullValidator extends Validator {

    @Override
    public void validate(Object object, Field field) throws ValidatorException {
        if (object == null) {
            throw new ValidatorException("Expected non-null value but got null", field);
        }
    }
}
