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
package sk.baka.autils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;

/**
 * An {@link AsyncTask} which shows a dialog and allows user to cancel the task. Invoke {@link #execute(Params[])} from the UI thread to run the task in background.
 *
 * @author Martin Vysny
 * @param <P>
 *            the parameter type
 * @param <R>
 *            the result type
 */
public final class DialogAsyncTask<P, R> extends AsyncTask<P, Progress, R> {

    /**
     * A context reference.
     */
    protected final Activity context;
    protected final DialogUtils utils;
    protected final AbstractTask<P, R> task;

    /**
     * Creates the task instance.
     *
     * @param context
     *            a context reference, used to create a dialog.
     */
    DialogAsyncTask(final Activity context, AbstractTask<P, R> task) {
        this.context = context;
        utils = new DialogUtils(context);
        this.task = task;
    }
    private ProgressDialog dlg;

    @Override
    protected final void onPreExecute() {
        exceptionThrownByTask = null;
        dlg = new ProgressDialog(context);
        dlg.setCancelable(true);
        dlg.setOnCancelListener(AndroidUtils.safe(context, new OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                cancel(true);
                dlg.setTitle("Cancelling");
            }
        }));
        dlg.setIndeterminate(false);
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // we have to call this method otherwise the title will never be shown
        // (on android 1.5)
        dlg.setTitle("Please wait");
        dlg.show();
    }

    /**
     * Returns true if the task failed with an exception.
     *
     * @return true if the task failed with an exception, false if the task is
     *         still running, finished successfully or was cancelled.
     */
    public final boolean isError() {
        return exceptionThrownByTask != null;
    }

    @Override
    protected final R doInBackground(P... params) {
        try {
            final R result = task.impl(params);
            exceptionThrownByTask = null;
            if (isCancelled()) {
                Log.i(task.getClass().getSimpleName(), "Cancelled");
                return null;
            }
            return result;
        } catch (Exception ex) {
            if (!isCancelled()) {
                exceptionThrownByTask = ex;
                Log.e(task.getClass().getSimpleName(), "Task execution failed", ex);
                publishProgress(new Progress(ex, context));
            } else {
                exceptionThrownByTask = null;
                Log.i(task.getClass().getSimpleName(), "Interrupted", ex);
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        // hide the dialog
        dlg.dismiss();
        task.cleanupAfterError(null);
    }
    /**
     * If not null then the task execution failed and the exception was thrown.
     */
    private volatile Exception exceptionThrownByTask = null;

    @Override
    protected final void onPostExecute(R result) {
        // memory effect explanation: the exceptionThrownByTask field actually forms a happens-before relation:
        // after a task completes, the variable is written to, and it is read from in the isError() method.
        if (isError()) {
            // leave the dialog open to show the error.
            task.cleanupAfterError(exceptionThrownByTask);
        } else if (isCancelled()) {
            // the onPostExecute method is actually NOT invoked when the task is cancelled, contrary to the Javadoc. Thus, this shouldn't happen.
        } else {
            dlg.dismiss();
            task.onSucceeded(result);
        }
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        int p = values[0].progress;
        dlg.setProgress(p);
        dlg.setMax(values[0].max);
        String msg = values[0].message;
        final Throwable t = values[0].error;
        if (t != null) {
            dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            String message = msg;
            if (message == null) {
                message = t.toString();
            }
            // This throws NPE on android 1.5???
            // dlg.setMessage(message);
            // the title is too short to display a complex exception. Dismiss the dialog and show a new one.
            dlg.dismiss();
            utils.showErrorDialog(t.toString());
        } else {
            if (msg != null) {
                dlg.setTitle(msg);
            }
        }
    }

    /**
     * Publishes a progress. Intended to be invoked from an {@link ITask} implementation.
     * @param progress the progress to publish, must not be null.
     */
    void publish(final Progress progress) {
        publishProgress(progress);
    }
}
