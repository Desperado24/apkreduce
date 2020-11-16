package com.desperado.apkreduce.convertwebp.tasks

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import com.desperado.apkreduce.convertwebp.ConvertWebpPlugin
import com.desperado.apkreduce.convertwebp.util.Logger
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.io.IOException
import java.util.regex.Pattern



open class ConvertTask : DefaultTask() {
    @get:Input
    var quality: Int = 0
        private set
    @get:Input
    var isAutoConvert: Boolean = false/**/
        private set
    private var cwebpPath: String? = null
    private var projectRootPath: String? = null
    @get:InputFiles
    var drawableDirs: Collection<File>? = null
        private set

    /*@TaskAction
    public void convert(IncrementalTaskInputs inputs) {
        if (!inputs.isIncremental()) {
            cleanPreOutput();
        }

        inputs.outOfDate(inputFileDetails -> {

        });

        inputs.removed(inputFileDetails -> {

        });
    }*/

    @TaskAction
    @Throws(Exception::class)
    fun convert() {
        getPermissionForMac()
        Logger.i("attachCovertTaskToBuild drawableDirs" + drawableDirs.toString())
        for (drawableDir in drawableDirs!!) {
            val drawableFiles = drawableDir.listFiles()
            if (drawableFiles != null) {
                for (drawableFile in drawableFiles!!) {
                    Logger.i("attachCovertTaskToBuild drawableFile" + drawableFile.absolutePath)
                    if (canConvert(drawableFile)) {
                        val srcName = drawableFile.getName()
                        val dotIndex = srcName.lastIndexOf(".")
                        val dstName = srcName.substring(0, dotIndex) + ".webp"

                        val parent = drawableFile.getParentFile()

                        val srcFilePath = drawableFile.getAbsolutePath()
                        val dstFilePath: String
                        if (isAutoConvert) {
                            dstFilePath = parent.getAbsolutePath() + "/" + dstName
                        } else {
                            dstFilePath =
                                projectRootPath + "/webp/" + parent.getName() + "/" + dstName
                            val dstFile = File(dstFilePath)
                            if (!dstFile.getParentFile().exists()) {
                                dstFile.getParentFile().mkdirs()
                            }
                        }

                        try {
                            val script = (cwebpPath + " -q " + quality + " "
                                    + srcFilePath + " -o " + dstFilePath)
                            val process = Runtime.getRuntime().exec(script)
                            if (process.waitFor() !== 0) {
                                Logger.i("convert failed: $srcFilePath")
                                continue
                            }
                            if (isAutoConvert) {
                                moveFileTo(
                                    srcFilePath,
                                    projectRootPath + "/ori_res/" + parent.getName() + "/" + srcName
                                )
                            }
                        } catch (e: IOException) {
                            Logger.i("convert failed: $srcFilePath")
                        }

                    }
                }
            }
        }
    }

    private fun canConvert(drawableFile: File): Boolean {
        val fileName = drawableFile.getName()
        return fileName.endsWith(".png") && !fileName.contains(".9") || fileName.endsWith(".jpg")
    }

    @Throws(Exception::class)
    private fun getPermissionForMac() {
        val os = OperatingSystem.current()
        if (os.isMacOsX()) {
            val script = "chmod a+x " + cwebpPath!!
            val process = Runtime.getRuntime().exec(script)
            if (process.waitFor() !== 0) {
                throw Exception("Can not get executive permission, please execute 'chmod a+x $cwebpPath' on terminal")
            }
        }
    }

    private fun moveFileTo(src: String, dst: String) {
        val dstFile = File(dst)
        if (!dstFile.getParentFile().exists()) {
            dstFile.getParentFile().mkdirs()
        }
        File(src).renameTo(dstFile)
    }

    open class ConfigAction(
        private val project: Project,
        private val cwebpPath: String,
        private val autoConvert: Boolean,
        private val quality: Int
    ) : Action<ConvertTask> {

        override fun execute(convertTask: ConvertTask) {
            val androidResDirs = getAndroidResDirectories(project)
            convertTask.drawableDirs = getDrawableDirsFromRes(androidResDirs)
            convertTask.projectRootPath = project.getProjectDir().getAbsolutePath()
            convertTask.cwebpPath = cwebpPath
            convertTask.isAutoConvert = autoConvert
            convertTask.quality = quality
        }

        private fun getDrawableDirsFromRes(resDirs: Collection<File>?): Collection<File> {
            val drawableDirs = mutableListOf<File>()
            if (resDirs != null) {
                val pattern = Pattern.compile("^drawable.*|^mipmap.*")
                for (resDir in resDirs) {
                    val subResDirs = resDir.listFiles()
                    if (subResDirs != null) {
                        for (subResDir in subResDirs!!) {
                            if (subResDir != null) {
                                val dirName = subResDir!!.getName()
                                Logger.i("dirName" + dirName)
                                if (pattern.matcher(dirName).matches()) {
                                    Logger.i("dirName drawableDirs add" + dirName)
                                    drawableDirs.add(subResDir)
                                }
                            }
                        }
                    }
                }
            }
            return drawableDirs
        }

        private fun getAndroidResDirectories(project: Project): Collection<File>? {
            var sourceSet: DefaultAndroidSourceSet? = null
            val androidExtension = project.getExtensions().findByName("android")
            if (project.getPlugins().hasPlugin(ConvertWebpPlugin.APP_PLUGIN)) {
                val appExtension = androidExtension as AppExtension
                sourceSet =
                    appExtension.getSourceSets().getByName("main") as DefaultAndroidSourceSet
            } else if (project.getPlugins().hasPlugin(ConvertWebpPlugin.LIB_PLUGIN)) {
                val libraryExtension = androidExtension as LibraryExtension
                sourceSet =
                    libraryExtension.getSourceSets().getByName("main") as DefaultAndroidSourceSet
            }

            return if (sourceSet != null) {
                sourceSet!!.getResDirectories()
            } else null
        }
    }
}
