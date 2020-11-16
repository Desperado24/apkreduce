package com.desperado.apkreduce.convertwebp.tasks

import com.desperado.apkreduce.convertwebp.util.Logger
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile


open class DecompressTask : DefaultTask() {
    private var srcFilePath: String? = null
    private var dstDirPath: String? = null
    private var succeedFlagPath: String? = null

    @InputFile
    fun getSrcFile(): File {
        return File(srcFilePath);
    }

    @OutputDirectory
    fun getDstDir(): File {
        return File(dstDirPath);
    }

    @TaskAction
    @Throws(Exception::class)
    fun decompressFile() {
        val succeedFlag = File(succeedFlagPath)
        if (!succeedFlag.exists()) {
            Logger.i("Start decompress file " + srcFilePath!!)
            if (srcFilePath!!.endsWith("tar.gz")) {
                decompressGzip(srcFilePath!!)
            } else {
                decompressZip(srcFilePath!!)
            }
            succeedFlag.createNewFile()
            Logger.i("Decompress finished.")
        }
    }

    @Throws(Exception::class)
    private fun decompressGzip(gzipFilePath: String) {
        var tis: TarArchiveInputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            val gzipFile = File(gzipFilePath)
            tis = TarArchiveInputStream(
                GZIPInputStream(
                    BufferedInputStream(
                        FileInputStream(gzipFile)
                    )
                )
            )
            val fileFolder = gzipFile.getParentFile().getAbsolutePath()
            var tae = tis!!.getNextTarEntry()
            while (tae != null) {
                val tmpFile = File(fileFolder, tae.getName())
                if (tae.isDirectory()) {
                    tmpFile.mkdirs()
                } else {
                    if (!tmpFile.getParentFile().exists()) {
                        tmpFile.getParentFile().mkdirs()
                    }
                    try {
                        bos = BufferedOutputStream(FileOutputStream(tmpFile))
                        val b = ByteArray(1024)
                        var length = tis!!.read(b)
                        while (length != -1) {
                            bos!!.write(b, 0, length)
                            length = tis!!.read(b)
                        }
                    } finally {
                        try {
                            if (bos != null) {
                                bos!!.close()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        } finally {
            if (tis != null) {
                try {
                    tis!!.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    @Throws(Exception::class)
    private fun decompressZip(zipFilePath: String) {
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(zipFilePath)
            val enums = zipFile!!.entries()
            val fileFolder = File(zipFilePath).getParentFile().getAbsolutePath()
            while (enums.hasMoreElements()) {
                val entry = enums.nextElement()
                val tmpFile = File(fileFolder, entry.getName())
                if (entry.isDirectory()) {
                    tmpFile.mkdirs()
                } else {
                    if (!tmpFile.getParentFile().exists()) {
                        tmpFile.getParentFile().mkdirs()
                    }

                    var inputStream: InputStream? = null
                    var out: OutputStream? = null
                    try {
                        inputStream = zipFile!!.getInputStream(entry)
                        out = FileOutputStream(tmpFile)
                        val b = ByteArray(1024)
                        var length = inputStream!!.read(b)
                        while (length != -1) {
                            out!!.write(b, 0, length)
                            length = inputStream!!.read(b)
                        }

                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream!!.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                        if (out != null) {
                            try {
                                out!!.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }

        } finally {
            try {
                if (zipFile != null) {
                    zipFile!!.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    open class ConfigAction(private val downloadFilePath: String) : Action<DecompressTask> {

        override fun execute(decompressTask: DecompressTask) {
            decompressTask.srcFilePath = downloadFilePath
            decompressTask.dstDirPath = File(downloadFilePath).getParent()
            decompressTask.succeedFlagPath = decompressTask.dstDirPath!! + "/succeed"
        }
    }
}
