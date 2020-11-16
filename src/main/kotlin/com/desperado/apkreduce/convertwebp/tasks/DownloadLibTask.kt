package com.desperado.apkreduce.convertwebp.tasks

import com.desperado.apkreduce.convertwebp.util.Logger
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


open class DownloadLibTask : DefaultTask() {
    private var downloadUrl: String? = null
    @get:Input
    var downloadFilePath: String? = null
        private set

    @OutputFile
    fun getDownloadFile(): File {
        return File(downloadFilePath)
    }

    @TaskAction
    @Throws(Exception::class)
    fun downloadLib() {
        if (downloadUrl != null && isFilePathValid(downloadFilePath)
            && !File(downloadFilePath).exists()
        ) {
            var bis: BufferedInputStream? = null
            var bos: BufferedOutputStream? = null
            try {
                Logger.i("Start download lib of WebP, " + downloadUrl!!)
                val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                connection.setConnectTimeout(30000)
                connection.setReadTimeout(30000)
                connection.connect()
                if (connection.getResponseCode() === 200) {
                    if (createDownloadFileIfNeed()) {
                        bis = BufferedInputStream(connection.getInputStream())
                        bos = BufferedOutputStream(FileOutputStream(downloadFilePath))
                        val bytes = ByteArray(1024)
                        var length = bis!!.read(bytes)
                        while (length != -1) {
                            bos!!.write(bytes, 0, length)
                            length = bis!!.read(bytes)
                        }
                        bos!!.flush()
                        Logger.i("Download finished.")
                    } else {
                        throw Exception("Download failed, can not create download file.")
                    }
                } else {
                    throw Exception("Download failed, request has been refused.")
                }
            } finally {
                if (bis != null) {
                    try {
                        bis!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
                if (bos != null) {
                    try {
                        bos!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }

    private fun createDownloadFileIfNeed(): Boolean {
        val file = File(downloadFilePath)
        val parent = file.getParentFile()
        if (parent != null && !parent!!.exists()) {
            if (!parent!!.mkdirs()) {
                return false
            }
        }
        if (file.exists() && !file.delete()) {
            return false
        }
        try {
            if (!file.createNewFile()) {
                return false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private fun isFilePathValid(path: String?): Boolean {
        if (path != null) {
            val dir = File(path).getParentFile()
            return dir.isDirectory() && dir.canWrite()
        } else {
            return false
        }
    }

    open class ConfigAction(private val downloadUrl: String, private val downloadFilePath: String) :
        Action<DownloadLibTask> {

        override fun execute(downloadLibTask: DownloadLibTask) {
            downloadLibTask.downloadUrl = downloadUrl
            downloadLibTask.downloadFilePath = downloadFilePath
        }
    }
}
