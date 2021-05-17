@file:Suppress("SpellCheckingInspection")

package sk_x.baka.aedict.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import sk_x.baka.aedict.R
import java.io.File
import java.lang.RuntimeException

class TocInstallException(mess: String) : Exception(mess)

class InstallFromToc private constructor() {
    interface BooleanCB {
        fun booleanCb(completed: Boolean)
    }

    class InstallStatusUpdateCB (val totalBytes:Int, val pb:ProgressBar, val tv:TextView, val dialog:AlertDialog) {
        var currentPct = 0
        var currentBytes = 0.0
        fun updatePb(bytesLoaded:Int) {
            currentBytes += bytesLoaded
            val newPct = ((currentBytes/totalBytes.toFloat()) * 100.0).toInt()
            // Log.d("newPct", "$newPct, $currentBytes, $totalBytes")
            if (newPct > currentPct + 5) {
                currentPct = newPct
                Log.d("updatePb", newPct.toString())
                MainScope().launch {
                    pb.progress = newPct
                }
            }
        }
        fun updateTv(updateFileName:String) {
            val name = if (zipFiles[updateFileName] != null) zipFiles[updateFileName]!!.name
                else if (normalFiles[updateFileName] != null) normalFiles[updateFileName]!!.name
                else updateFileName
            MainScope().launch {
                val newMess = "${tv.text}\n$name"
                tv.text = newMess
            }
        }
        fun dismiss() {
            MainScope().launch { dialog.dismiss() }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun Activity.installStatusDialog(totalBytesToDownload:Int) : InstallStatusUpdateCB {
        var alertDialog : AlertDialog
        val downloadView: LinearLayout = layoutInflater.inflate(R.layout.dict_install_status, null) as LinearLayout

        val dialog = AlertDialog.Builder(this).setTitle("Install Status")
        dialog.setView(downloadView)
        val pb : ProgressBar = downloadView.findViewById(R.id.download_PB)
        val tv  : TextView = downloadView.findViewById(R.id.download_files)
        dialog.setCancelable(false)
        alertDialog = dialog.create()
        alertDialog.show()
        return InstallStatusUpdateCB(totalBytesToDownload, pb, tv, alertDialog)
    }
    private fun doInstall(app:Application) :Pair<Boolean, String> {
        installOrder.forEach{ installName ->
            if (installName.endsWith(".zip")) {
                var rv:HashMap<String, Pair<Long, String>>
                with (zipFiles[installName]!!) {
                    rv = AssetFileUtils.unzipFromAssets(app,
                        installName, app.filesDir, destination
                    )
                    val srcNames = fileInfo.keys.sorted().joinToString(",")
                    val destNames = rv.keys.sorted().joinToString(",")
                    if (srcNames != destNames) {
                        throw TocInstallException("""doInstall
                            |file = $installName
                            |src names differ from dest names
                            |"$srcNames" vs "$destNames"
                        """.trimMargin())
                    }
                    val srcSize = fileInfo.keys.sorted().map{"$it:${fileInfo[it]!!.first}"}
                        .joinToString(",")
                    val destSize = rv.keys.sorted().map{"$it:${rv[it]!!.first}"}
                        .joinToString(",")
                    if (srcSize != destSize) {
                        throw TocInstallException("""doInstall
                            |file = $installName
                            |src names differ from dest names
                            |"$srcSize" 
                            |vs 
                            |"$destSize"
                        """.trimMargin())
                    }
                    val srcMD5 = fileInfo.keys.sorted().map{"$it:${fileInfo[it]!!.second}"}
                        .joinToString(",")
                    val destMD5 = rv.keys.sorted().map{"$it:${rv[it]!!.second}"}
                        .joinToString(",")
                    if (srcMD5 != destMD5) {
                        throw TocInstallException("""doInstall
                            |file = $installName
                            |src names differ from dest names
                            |"$srcMD5" 
                            |vs
                            |"$destMD5"
                        """.trimMargin())
                    }
                    Log.d("installed", installName)
                }
            }
            else {
                var rv : Pair<Long, String>
                with(normalFiles[installName]!!) {
                    rv = AssetFileUtils.copyFromAssets(app,
                        installName, app.filesDir, destination
                    )
                }
                val (size, mess) = rv
                if (size < 0) throw TocInstallException("""doInstall
                        |file =                     $installName
                        |install failed: $mess
                    """.trimMargin("|"))
            }
        }

        return true to "Successful Install"
    }

    fun install(activity: Activity, cb: BooleanCB) {
        val files = zipFiles.keys.map { key ->
            zipFiles[key]!!.destination
        }.toMutableList()
        normalFiles.keys.map { key ->
            files.add(normalFiles[key]!!.destination)
        }
        val missingFiles =
            files.filterNot { File(activity.filesDir, it).exists() }
        cb.booleanCb(missingFiles.isEmpty())
        if (missingFiles.isEmpty())
            return
        var totalDownloadBytes = 0
        zipFiles.keys.map{key ->
            totalDownloadBytes += zipFiles[key]!!.totalSize
        }
        normalFiles.keys.map{key ->
            totalDownloadBytes += normalFiles[key]!!.fileSize
        }
        val installUpdateCB = activity.installStatusDialog(totalDownloadBytes)
        AssetFileUtils.updateCb = installUpdateCB

        GlobalScope.launch {
            try {
                val (success, message) = doInstall(activity.application)
                MainScope().launch {
                    cb.booleanCb(success)
                    installUpdateCB.dismiss()
                    AssetFileUtils.updateCb = null
                    Thread.sleep(5000)

                }
                Log.d("DoInstall", "success = $success, message = \"$message\"")
            }
            catch (e: java.lang.Exception) {
                Log.d("DictInstall", "Install failed\n$e")
                Thread.sleep(15000)
                installUpdateCB.dismiss()
                AssetFileUtils.updateCb = null
            }
        }
    }
    companion object {
        private val installOrder = mutableListOf<String>()
        private data class ZipFileInfo(
            val name:String,
            val destination: String,
            val zipMd5: String,
        ) {
            val fileInfo = mutableMapOf<String, Pair<Int, String>>()
            var totalFiles = 0
            var totalSize = 0
        }
        private val zipFiles = mutableMapOf<String, ZipFileInfo>()

        private data class NormalFileInfo(
            val name:String,
            val destination: String,
            val md5Sum: String,
            val fileSize: Int,
        )
        private val normalFiles = mutableMapOf<String, NormalFileInfo>()

        private class NextLine(application: Application, tocName: String) {
            val br = application.assets.open(tocName).bufferedReader()
            var lineCount = 0
            var curLine: String = ""
            var eof = false
            fun next(): String? {
                lineCount++
                var cl: String?
                do {
                    cl = br.readLine()
                    eof = cl == null
                    curLine = if (!eof) cl!! else ""
                } while (cl != null && cl.isEmpty()) // skip empty lines.
                return cl
            }
        }

        private lateinit var nl: NextLine
        private fun createFileInfo(application: Application, tocName: String) {
            fun String.startsWith(string: String): Boolean =
                "^\\s*$string.*".toRegex().matches(this)
            var nameResult : MatchResult? = null
            fun isName() : Boolean {
                nameResult = """^\s*Name:\s*(.*)""".toRegex().find(nl.curLine)
                return nameResult != null
            }
            fun getName() : String {
                return nameResult!!.groupValues.last()
            }

            fun parseArchive(name:String) {
                if (name.isEmpty()) throw TocInstallException("name missing.")
                val start = 0
                val parsEntries = 1
                val lookForEnd = 3
                
                var ea: MatchResult? = null
                fun isEndArchive(): Boolean {
                    ea = "^\\s*(\\d+)\\s+(\\d+)\\s+files".toRegex().find(nl.curLine)
                    return ea != null
                }
                // 9705598  2020-01-27 19:02   _2.frq          e200afc3d14927efc4c4f3da2dcaf058
                var ize: MatchResult? = null
                fun isZipEntry(): Boolean {
                    ize = """^\s+(\d+)\s+[\d-]+\s+\d+:\d+\s+([a-zA-Z0-9_.]+)\s+([0-9a-f]+)"""
                        .toRegex().find(nl.curLine)
                    return ize != null
                }

                val (_, zipName, md5Sum) = """^[A_Za-z]+:\s+(\S+.zip)\s+(.*)""".toRegex()
                    .find(nl.curLine)!!.groupValues
                val destination = """^\s*Destination:\s+(.*)""".toRegex()
                    .find(nl.next()!!)!!.groupValues.last()
                val zif = ZipFileInfo(name, destination, md5Sum)
                var state = start
                while (!nl.eof) {
                    if (nl.curLine.startsWith("---+")) {
                        state = when (state) {
                            start -> parsEntries
                            parsEntries -> lookForEnd
                            else -> throw TocInstallException("uexpecded line")
                        }
                    }
                    else if (isZipEntry()) {
                        if (state != parsEntries) throw TocInstallException("Not expecting zip entry")
                        val (_, size, zipEntry, md5) = ize!!.groupValues
                        if (zif.fileInfo[zipEntry] != null) throw TocInstallException(
                            """line ${nl.lineCount}: file $zipEntry already found."""
                        )
                        zif.fileInfo[zipEntry] = size.toInt() to md5
                    }
                    else if (isEndArchive()) {
                        if (state != lookForEnd) throw TocInstallException("Not expecting end.")
                        val (_, totalSize, filesCount) = ea!!.groupValues
                        zif.totalFiles = filesCount.toInt()
                        zif.totalSize = totalSize.toInt()
                        if (zif.fileInfo.keys.size != zif.totalFiles) throw TocInstallException(
                            """line ${nl.lineCount}: count of $filesCount and files found
                                |of ${zif.fileInfo.size} don't agree.""".trimMargin()
                        )
                        zipFiles[zipName] = zif
                        break
                    }
                    nl.next()
                }
                installOrder.add(zipName)
            }

            fun parseSource(name:String) {
                if (name.isEmpty()) throw TocInstallException("name missing.")
                // Source: dictionaries/sod-20110313.dat 4584605 48cebba17fd984523d6a03d174e74e51
                val (_, sourceName, sourceSize, md5) =
                    """^\s*Source:\s+(\S+)\s+(\d+)\s+([\da-f]+)""".toRegex()
                        .find(nl.curLine)!!.groupValues
                val destination = """^\s*Destination:\s+(.*)""".toRegex()
                    .find(nl.next()!!)!!.groupValues.last()
                normalFiles[sourceName] = NormalFileInfo(
                    name, destination, md5, sourceSize.toInt()
                )
                installOrder.add(sourceName)
            }

            nl = NextLine(application, tocName)
            var curName = ""
            while (nl.next() != null) {
                when {
                    isName() -> curName = getName()
                    nl.curLine.startsWith("Archive:") -> run {
                        try {
                            parseArchive(curName)
                            curName = ""
                        } catch (e: java.lang.Exception) {
                            throw TocInstallException(
                                """Parse archive FAILED:line ${nl.lineCount}
                                        |line="${nl.curLine}
                                        |Exception:$e
                                    """.trimMargin()
                            )
                        }
                    }
                    nl.curLine.startsWith("Source:") -> run {
                        parseSource(curName)
                        curName = ""
                    }
                    else -> throw TocInstallException(
                        """$tocName:${nl.lineCount}:Expected "Archive" or "Source:"
                                        |Found "${nl.curLine}"
                                    """.trimMargin()
                    )
                }
            }
        }

        private var installFromToc: InstallFromToc? = null
        fun getInstance(application: Application, assetsToc: String): InstallFromToc {
            synchronized(this) {
                if (installFromToc == null) {
                    installFromToc = InstallFromToc()
                    createFileInfo(application, assetsToc)
                    Log.d(
                        "installFromToc",
                        "Found ${zipFiles.keys.size} zip " +
                                "files and ${normalFiles.keys.size} normal files."
                    )
                }
                return installFromToc!!
            }
        }
    }
}

