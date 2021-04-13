/*
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
import android.util.Log;
import java.io.Closeable;

/**
 * Contains misc utility methods.
 *
 * @author Martin Vysny
 */
public final class MiscUtils {

    private MiscUtils() {
        throw new AssertionError();
    }

    /**
     * Closes given closeable quietly. Any errors are logged as warnings to the
     * android log.
     *
     * @param in the closeable to close. Nothing is done if null.
     */
    public static void closeQuietly(Closeable in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (Exception ex) {
            Log.w("MiscUtils", "Failed to close closeable", ex);
        }
    }

    /**
     * Checks if given string is null, empty or whitespace-only.
     *
     * @param str the string to check
     * @return true if given string is null, empty or consists of whitespaces
     * only.
     */
    public static boolean isBlank(final String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Checks if given string array is null, empty or contains whitespace-only
     * strings only.
     *
     * @param str the string to check
     * @return true if given string is null, empty or consists of whitespaces
     * only.
     */
    public static boolean isBlank(final String[] str) {
        if (str == null || str.length == 0) {
            return true;
        }
        for (final String s : str) {
            if (!isBlank(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if given character is an ascii letter (a-z, A-Z).
     *
     * @param c the character to check
     * @return true if the character is a letter, false otherwise.
     */
    public static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}