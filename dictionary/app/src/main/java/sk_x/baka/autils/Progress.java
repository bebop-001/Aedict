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
package sk_x.baka.autils;

import android.app.Activity;

/**
 * Contains data about a progress.
 *
 * @author Martin Vysny
 */
public final class Progress {

    /**
     * Creates new Progress object.
     * @param message the message to show.
     * @param progress the progress, 0..max
     * @param max maximum progress, greater than zero. Next Progress object may override this value.
     */
    public Progress(final String message, final int progress, final int max) {
        this.message = message;
        this.progress = progress;
        error = null;
        this.max = max;
    }
    /**
     * The message to show.
     */
    public final String message;
    /**
     * A progress being made.
     */
    public final int progress;
    /**
     * Optional error (if the task failed).
     */
    public final Throwable error;
    /**
     * the maximum value of the progress parameter.
     */
    public final int max;

    /**
     * Creates a progress object representing an error state.
     * @param t the exception
     * @param activity
     */
    Progress(final Throwable t, final Activity activity) {
        progress = 0;
        message = new DialogUtils(activity).getErrorMsg() + ": " + t;
        error = t;
        max = 100;
    }

    /**
     * Checks if this progress denotes a failure.
     * @return true if this progress shows an error.
     */
    public boolean isError() {
        return error != null;
    }
}
