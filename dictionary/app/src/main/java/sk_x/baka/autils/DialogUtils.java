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

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Gravity;
import android.widget.Toast;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains several Dialog utilities.
 * <p/>
 * Usage note: to show correctly localized strings you'll have to set the {@link #resError} to the correct resource in your application.
 *
 * @author Martin Vysny
 */
public final class DialogUtils {

    /**
     * The activity.
     */
    public final Activity activity;
    /**
     * A string resource to use for the "Error" string message. Set this in the {@link Application#onCreate()}.
     */
    public static int resError = -1;

    /**
     * Creates new utility class.
     *
     * @param activity
     *            owning activity which will show the dialogs. Android 1.6 is
     *            not able to show a dialog belonging to an Application object
     *            so you cannot use {@link Context} anymore.
     */
    public DialogUtils(final Activity activity) {
        this.activity = activity;
    }

    /**
     * Returns a localized "Error" string if available.
     * @return localized error string if available.
     */
    public String getErrorMsg() {
        return resError == -1 ? "Error" : activity.getString(resError);
    }

    /**
     * Shows a simple yes/no dialog. The dialog does nothing and simply
     * disappears when No is clicked.
     *
     * @param message
     *            the message to show
     * @param yesListener
     *            invoked when the Yes button is pressed. The listener is
     *            automatically {@link AedictApp#safe(Activity, Class, Object)
     *            safe-protected}.
     */
    public void showYesNoDialog(final String message, final DialogInterface.OnClickListener yesListener) {
        showYesNoDialog(null, message, yesListener);
    }

    /**
     * Shows a simple yes/no dialog. The dialog does nothing and simply
     * disappears when No is clicked.
     *
     * @param title an optional title of the dialog. When null or blank the title will not be shown.
     * @param message
     *            the message to show
     * @param yesListener
     *            invoked when the Yes button is pressed. The listener is
     *            automatically {@link AedictApp#safe(Activity, Class, Object)
     *            safe-protected}.
     */
    public void showYesNoDialog(final String title, final String message, final DialogInterface.OnClickListener yesListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (!MiscUtils.isBlank(title)) {
            builder.setTitle(title);
        }
        builder.setMessage(message);
        builder.setPositiveButton(R.string.yes, AndroidUtils.safe(activity, DialogInterface.OnClickListener.class, yesListener));
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /**
     * Shows an error dialog.
     *
     * @param messageRes
     *            the message to show.
     */
    public void showErrorDialog(final int messageRes) {
        showErrorDialog(activity.getString(messageRes));
    }

    /**
     * Shows an error dialog.
     *
     * @param message
     *            the message to show.
     */
    public void showErrorDialog(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        builder.setTitle(getErrorMsg());
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.create().show();
    }

    /**
     * Shows a quick info toast at the bottom of the screen.
     *
     * @param messageRes
     *            the message to show.
     */
    public void showToast(final int messageRes) {
        showToast(activity.getString(messageRes));
    }

    /**
     * Shows a quick info toast.
     *
     * @param message
     *            the message to show.
     */
    public void showToast(final String message) {
        final Toast toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
    /**
     * The name of the shared preferences, which are used to store status for e.g. {@link #showInfoOnce(int)} method.
     */
    public static final String SHAREDPREFS_NAME = "autils";

    /**
     * Shows given string in a dialog, but only once.
     * @param dialogId the ID of the dialog. A dialog with this ID will be shown at most once.
     * @param title an optional title. If null then no title will be shown.
     * @param message the string resource ID
     */
    public void showInfoOnce(final String dialogId, final int title, final int message) {
        showInfoOnce(dialogId, title==-1?null:activity.getString(title), activity.getString(message));
    }

    /**
     * Clears the tracking of a "dialog was shown" flag. After the method finishes, the dialogs will again be shown when one of the {@link #showInfoOnce(int, java.lang.String) } is invoked.
     */
    public void clearInfoOccurency() {
        final SharedPreferences prefs = activity.getApplication().getSharedPreferences(SHAREDPREFS_NAME, Context.MODE_PRIVATE);
        final Set<String> keys = new HashSet<String>(prefs.getAll().keySet());
        final Editor e = prefs.edit();
        for (final String key : keys) {
            if (key.startsWith("infoonce")) {
                e.remove(key);
            }
        }
        e.commit();
    }

    /**
     * Shows given string in a dialog, but only once.
     * @param dialogId the ID of the dialog. A dialog with this ID will be shown at most once.
     * @param title an optional title. If null then no title will be shown.
     * @param message the string resource ID
     */
    public void showInfoOnce(final String dialogId, final String title, final String message) {
        final SharedPreferences prefs = activity.getApplication().getSharedPreferences(SHAREDPREFS_NAME, Context.MODE_PRIVATE);
        final String shownDialog = prefs.getString("infoonce" + dialogId, null);
        if (shownDialog == null) {
            prefs.edit().putString("infoonce" + dialogId, "").commit();
            showInfoDialog(title, message);
        }
    }

    /**
     * Shows an information dialog.
     * @param title an optional title. If -1 then no title will be shown.
     * @param message the dialog message.
     */
    public void showInfoDialog(final int title, final int message) {
        showInfoDialog(title == -1 ? null : activity.getString(title), activity.getString(message));
    }

    /**
     * Shows an information dialog.
     * @param title an optional title. If null then no title will be shown.
     * @param message the dialog message.
     */
    public void showInfoDialog(final String title, final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.create().show();
    }
}
