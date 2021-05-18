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
import android.os.AsyncTask;
import android.util.Log;

/**
 * Implements a task. The task is intended to be executed asynchronously by {@link DialogAsyncTask}. Use {@link #execute(Activity, P[])} to run the task.
 * @author Martin Vysny
 * @param <P>
 *            the parameter type
 * @param <R>
 *            the result type
 */
public abstract class AbstractTask<P, R> {

    /*
     * Publishes a progress. Intended to be invoked from an {@link ITask} implementation.
     * @param progress the progress to publish, must not be null.
     */
    protected final void publish(final Progress progress) {
        if (!sync) {
            executor.publish(progress);
        }
    }
    /*
     * This object is the executor of this task. Valid after the {@link #execute(P[])} is invoked.
     */
    DialogAsyncTask<P, R> executor;
    /**
     * If true then this task is executed in a synchronous manner.
     */
    private boolean sync = false;

    /**
     * The implementation of the task. The task should periodically invoke
     * {@link DialogAsyncTask#publish(Progress)} to update the progress. The task
     * should periodically check for {@link #isCancelled()} - it should
     * terminate ASAP when canceled, even by throwing an exception.
     * <p/>
     * The method is NOT executed in the UI thread.
     * @param params
     *            the parameters.
     * @return the result.
     * @throws Exception
     */
    public abstract R impl(final P... params) throws Exception;

    /*
     * Performs a cleanup when the task fails (throws an exception), or is canceled. The method is run in the UI thread - it is NOT executed in the same thread as the {@link #execute(null, params)} method.
     * The exception is already logged using the Android logger.
     * <h3>Memory effects</h3>
     * Invocation of {@link #impl(P[])} happens-before invocation of {@link #cleanupAfterError(java.lang.Exception)}.
     * @param ex the exception, may be null - in this case the execution was canceled.
     */
    protected abstract void cleanupAfterError(final Exception ex);

    /**
     * Invoked when the task finished successfully, from the UI thread. Not invoked when the task was cancelled.
     * <h3>Memory effects</h3>
     * Invocation of {@link #impl(P[])} happens-before invocation of {@link #cleanupAfterError(java.lang.Exception)}.
     *
     * @param result
     *            the task product.
     */
    protected abstract void onSucceeded(final R result);

    /**
     * Invoke this method from the UI thread to run the task. The task is run in the background.
     * @param activity the owner activity, not null.
     * @param parameters the parameter list.
     * @return the async task.
     */
    public final AsyncTask<P, Progress, R> execute(final Activity activity, P... parameters) {
        return execute(false, activity, parameters);
    }

    /*
     * Invoke this method from the UI thread to run the task. The task will be run entirely in UI thread and the dialog will not be shown.
     * This helps the testing.
     * @param sync if true then the task is run in the UI thread. The method blocks until the task finished running. If the task fails the exception is thrown.
     * @param activity the owner activity, not null.
     * @param parameters the parameter list.
     * @return the asynctask or null if synchronous execution was requested.
     */
    private final String pParamaters(P...parameters) {
        String rv = "<" + parameters.getClass().getSimpleName() + ":";
        for (P p:parameters) {
            rv += p.toString() + ",";
        }
        rv += ">";
        return rv;
    }
    public final AsyncTask<P, Progress, R> execute(final boolean sync, final Activity activity, P... parameters) {
        Log.d("AbstractClass", String.format("execute:sync=%b, activity=%s, parameters=%s",
                sync, activity.getClass().getSimpleName(), pParamaters(parameters)));
        if (executor != null) {
            throw new IllegalStateException("The task has already been run. Use new instance.");
        }
        this.sync = sync;
        if (!sync) {
            executor = new DialogAsyncTask<>(activity, this);
            return executor.execute(parameters);
        }
        final R result;
        try {
            result = impl(parameters);
        } catch (Exception ex) {
            try {
                Log.e(getClass().getSimpleName(), "Task failed", ex);
                cleanupAfterError(ex);
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Task failed and the cleanup failed, too", e);
            }
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException("Task execution failed", ex);
        }
        onSucceeded(result);
        return null;
    }

    /**
     * Checks if the task has been cancelled.
     * @return true if the task was cancelled.
     */
    protected boolean isCancelled() {
        if (sync) {
            return false;
        }
        return executor.isCancelled();
    }
}
