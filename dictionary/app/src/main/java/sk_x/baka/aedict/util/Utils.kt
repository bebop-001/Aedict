/*
 *  Copyright 2018 Steven Smith kana-tutor.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied.
 *
 *  See the License for the specific language governing permissions
 *  and limitations under the License.
 */

package sk_x.baka.aedict.util

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import sk_x.baka.aedict.BuildConfig
import sk_x.baka.aedict.R
import java.io.File
import java.text.SimpleDateFormat

// Return a spanned html string using the appropriate call for
// the user's device.
fun htmlString(htmlString:String) : Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    else {
        @Suppress("DEPRECATION")
        Html.fromHtml(htmlString)
    }
}

// Display info about the build using an AlertDialog.
fun displayBuildInfo(activity : Activity) : Boolean {
    val appInfo = activity.packageManager
        .getApplicationInfo(BuildConfig.APPLICATION_ID, 0)
    val installTimestamp = File(appInfo.sourceDir).lastModified()

    val webview = WebView(activity)
    webview.setBackgroundColor(ContextCompat.getColor(activity, R.color.file_edit_window_bg))
    webview.loadData(
        activity.getString(R.string.build_info_query,
        ContextCompat.getColor(activity, R.color.file_edit_window_font_color) and 0x00FFFFFF,
        activity.getString(R.string.app_name),
        BuildConfig.VERSION_CODE,
        BuildConfig.VERSION_NAME,
        SimpleDateFormat.getInstance().format(
            java.util.Date(BuildConfig.BUILD_TIMESTAMP)),
        SimpleDateFormat.getInstance().format(
            java.util.Date(installTimestamp)),
        if(BuildConfig.DEBUG) "debug" else "release"
        ),
        "text/html", "utf-8")
    AlertDialog.Builder(activity)
        .setView(webview)
        .show()
    return true
}

