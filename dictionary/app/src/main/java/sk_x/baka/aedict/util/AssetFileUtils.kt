
package sk_x.baka.aedict.util

import android.app.Application
import android.util.Log
import java.io.*
import java.lang.RuntimeException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val TAG = "DictInstaller"
private const val BUFFER_SIZE = 0x1000

object AssetFileUtils {

    var updateCb : InstallFromToc.InstallStatusUpdateCB? = null
    // Create any files needed below the user's filesDir (from Context.getFiles).
    private fun mkdirs(
        root: File, dirName: String
    ): File {
        val dirs = dirName.split("/+".toRegex()).toMutableList()
        var next: File = root
        while (dirs.isNotEmpty()) {
            next = File(next, dirs.removeAt(0))
            if (!next.exists() && !next.mkdir())
                throw RuntimeException(
                    """$TAG: directory doesn't and mkdir FAILED:$next"""
                )
        }
        return next
    }
    private fun updateTotalBytes(bytes:Int) {
        updateCb?.updatePb(bytes)
    }
    private fun updateCurrentFile(file:String) {
        updateCb?.updateTv(file)
    }
    private fun copyFromAssets(
        inStream:InputStream, baseDir:File, outFile:String
    ) : Pair<Long, String> {
        var md5Sum: String
        var fileBytes = 0L
        val (_, dirName, fileName) = "^(.*/)*(?:([^/]+))*$".toRegex()
            .find(outFile)!!.groupValues
        val outDir = mkdirs(baseDir, dirName)
        val outStream = FileOutputStream("$outDir/$fileName")
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead:Int
        while(inStream.read(buffer).also { bytesRead = it } > 0) {
            digest.update(buffer, 0, bytesRead)
            outStream.write(buffer, 0, bytesRead)
            updateTotalBytes(bytesRead)
        }
        md5Sum = digest.digest()
            .joinToString("") { "%02x".format(it) }
        return fileBytes to md5Sum
    }
    @Suppress("UsePropertyAccessSyntax")
    // using ZipInputStream:getNextEntry instead of ZipInputStream:nextEntry.
    // behaviour is different.  getNextEntry returns null if next entry is null
    // where as nextEntry throws an exception when null.
    private fun unzipFile(
        inStream: InputStream,
        baseDir: File,
        unzipDir: String
    ): HashMap<String, Pair<Long, String>> {
        val buffer = ByteArray(BUFFER_SIZE)
        val rv = mutableMapOf<String, Pair<Long, String>>()
        var unzippedBytes = 0L
        val zipStream = ZipInputStream(BufferedInputStream(inStream))
        val unzipRoot = mkdirs(baseDir, unzipDir)
        var ze: ZipEntry? = zipStream.getNextEntry()
        if (ze == null)
            throw RuntimeException("unzipFile:inStream:Empty or not a zip file.")
        while (ze != null) {
            val zippedFileName = ze.name
            val (_, dirName, fileName) = "^(.*/)*(?:([^/]+))*$".toRegex()
                .find(zippedFileName)!!.groupValues
            var md5Sum = ""
            rv[zippedFileName] = 0L to md5Sum
            if (dirName.isNotEmpty()) {
                mkdirs(unzipRoot, dirName)
            }
            if (fileName.isNotEmpty()) {
                val digest = MessageDigest.getInstance("MD5")
                val zipOut = FileOutputStream("$unzipRoot/$dirName$fileName")
                var bytesRead: Int
                var fileBytes = 0L
                while (zipStream.read(buffer).also { bytesRead = it } > 0) {
                    digest.update(buffer, 0, bytesRead)
                    zipOut.write(buffer, 0, bytesRead)
                    fileBytes += bytesRead.toLong()
                    updateTotalBytes(bytesRead)
                }
                unzippedBytes += fileBytes
                md5Sum = digest.digest()
                    .joinToString("") { "%02x".format(it) }
                rv[zippedFileName] = fileBytes to md5Sum
                zipOut.close()
            }
            zipStream.closeEntry()
            ze = zipStream.getNextEntry()
        }
        zipStream.close()
        return rv as HashMap<String, Pair<Long, String>>
    }

    // on success, pair will contain file size in bytes
    // and an MD5 checksum for the file.  On fail,
    // it will contain a -1 and an error string.
    // assumes local file is under baseDir
    fun copyFromAssets(app:Application,
        srcFile: String, baseDir:File, destFile: String
    ) : Pair<Long, String> {
        updateCurrentFile(srcFile)
        var rv: Pair<Long, String>
        try {
            val inStream = app.assets.open(srcFile)
            rv = copyFromAssets(inStream, baseDir, destFile)
        }
        catch (e:Exception) {
            val mess =  """
                |Exception occurred while trying assets file:
                |     Source: $srcFile
                |     Destination: $destFile
                |     Exception:$e               
            """.trimMargin("|")
            updateCurrentFile(mess)
            return -1L to  mess
        }
        /*
        Log.d("copy result",
            "%-30s -> %8s %s".format(destFile, "%,d".format(rv.first.toInt()), rv.second)
        )
        */
        return rv
    }

    fun unzipFromAssets(
        app: Application,
        zipFile: String, baseDir:File, unzipDir: String
    ) : HashMap<String, Pair<Long, String>> {
        updateCurrentFile(zipFile)
        val resultMap: HashMap<String, Pair<Long, String>>
        try {
            val inStream = app.assets.open(zipFile)
            resultMap = unzipFile(inStream, baseDir, unzipDir)
        }
        catch (e: java.lang.Exception) {
            throw TocInstallException("""
            |Exception occurred while trying to unzip asset file.
            |    File = assets.$zipFile
            |    Destination = $unzipDir
            |    Exception:$e.
            |""".trimMargin("|")
            )
        }
        /*
        resultMap.keys.sorted().forEach { fileName ->
            val (size, md5Sum) = resultMap[fileName]!!
            Log.d(
                "unzip result",
                "%-30s -> %8s %s".format(fileName, "%,d".format(size.toInt()), md5Sum)
            )
        }
        */
        return resultMap
    }
}
