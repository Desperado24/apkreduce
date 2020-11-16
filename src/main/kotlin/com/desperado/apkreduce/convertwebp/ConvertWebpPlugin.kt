package com.desperado.apkreduce.convertwebp

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.utils.StringHelper

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.internal.os.OperatingSystem

import com.desperado.apkreduce.convertwebp.util.Logger
import com.desperado.apkreduce.convertwebp.tasks.ConvertTask
import com.desperado.apkreduce.convertwebp.tasks.DecompressTask
import com.desperado.apkreduce.convertwebp.tasks.DownloadLibTask

class ConvertWebpPlugin : Plugin<Project> {
    companion object {
        val APP_PLUGIN = "com.android.application"
        val LIB_PLUGIN = "com.android.library"

        private val HOST_URL =
            "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/"
    }

    private val libFileName: String?
        get() {
            val os = OperatingSystem.current()
            if (os.isMacOsX()) {
                return "libwebp-1.0.0-rc3-mac-10.13.tar.gz"
            } else if (os.isWindows()) {
                val arch = System.getProperty("os.arch")
                if ("x86".equals(arch)) {
                    return "libwebp-1.0.0-windows-x86-no-wic.zip"
                } else if ("x86_64".equals(arch)) {
                    return "libwebp-1.0.0-windows-x64-no-wic.zip"
                }
            }
            return null
        }

    override fun apply(project: Project) {
        project.getExtensions().create("convertWebpConfig", ConvertWebpConfigExtension::class.java)
        Logger.initialize(project.getLogger())

        val libFileName = libFileName
        Logger.i("libFileName" + libFileName)

        if (libFileName != null) {
            project.afterEvaluate({ p ->
                val extension = p.getExtensions().findByType(ConvertWebpConfigExtension::class.java)
                val downloadUrl = HOST_URL + libFileName
                val downloadDir = getDefaultDownloadDir(p.getGradle())
                val downloadFilePath = "$downloadDir/$libFileName"

                val downloadTask = createDownloadTask(p, downloadUrl, downloadFilePath)
                val decompressTask = createDecompressTask(p, downloadFilePath)

                val variantsName = getVariantsName(p)
                Logger.i("variantsName" + variantsName)
                for (variantName in variantsName) {
                    val convertTask = createConvertTask(
                        p,
                        variantName, getCWebPPath(downloadDir),
                        extension.isAutoConvert, extension.quality
                    )
                    Logger.i("variantsName" + convertTask.name)
                    convertTask.dependsOn(decompressTask.dependsOn(downloadTask))

                    if (extension.isAutoConvert) {
                        attachCovertTaskToBuild(p, variantName, convertTask)
                    }
                }
            })
        } else {
            Logger.i("Can not support your operating system.")
        }
    }

    private fun createDownloadTask(
        project: Project,
        downloadUrl: String,
        downloadFilePath: String
    ): Task {
        val task = project.getTasks().create(
            "downloadLibWebP", DownloadLibTask::class.java,
            DownloadLibTask.ConfigAction(downloadUrl, downloadFilePath)
        )
        task.setGroup("apkreduce")
        return task
    }

    private fun createDecompressTask(project: Project, downloadFilePath: String): Task {
        val task = project.getTasks().create(
            "decompressDownloadFile", DecompressTask::class.java,
            DecompressTask.ConfigAction(downloadFilePath)
        )
        task.setGroup("apkreduce")
        return task
    }

    private fun createConvertTask(
        project: Project,
        variant: String,
        cwebpPath: String,
        autoConvert: Boolean,
        quality: Int
    ): Task {
        val task = project.getTasks().create(
            "convert" + StringHelper.capitalize(variant) + "WebP",
            ConvertTask::class.java,
            ConvertTask.ConfigAction(project, cwebpPath, autoConvert, quality)
        )
        task.setGroup("apkreduce")
        return task
    }

    private fun attachCovertTaskToBuild(project: Project, variant: String, convertTask: Task) {
        val nameOfMergeResources = "merge" + StringHelper.capitalize(variant) + "Resources"
        Logger.i("attachCovertTaskToBuild variant" + variant + "nameOfMergeResources:" + nameOfMergeResources)
        val mergeResources = project.getTasks().findByName(nameOfMergeResources)
        Logger.i("attachCovertTaskToBuild mergeResources" + mergeResources.name)
        mergeResources.dependsOn(convertTask)
    }

    private fun getDefaultDownloadDir(gradle: Gradle): String {
        val gradleUserHome = gradle.getGradleUserHomeDir()
        val dependencyCacheDir = "/caches/modules-2/files-2.1/"
        val packageName = "com.desperado.apkreduce"
        return gradleUserHome.getAbsolutePath() + dependencyCacheDir + packageName
    }

    private fun getCWebPPath(downloadDir: String): String {
        var path: String? = null
        val os = OperatingSystem.current()
        if (os.isMacOsX()) {
            path = "libwebp-1.0.0-rc3-mac-10.13/bin/cwebp"
        } else if (os.isWindows()) {
            val arch = System.getProperty("os.arch")
            if ("x86".equals(arch)) {
                path = "libwebp-1.0.0-windows-x86-no-wic/bin/cwebp.exe"
            } else if ("x86_64".equals(arch)) {
                path = "libwebp-1.0.0-windows-x64-no-wic/bin/cwebp.exe"
            }
        }
        return "$downloadDir/$path"
    }

    private fun getVariantsName(project: Project): List<String> {
        val variantsName = mutableListOf<String>()
        val androidExtension = project.getExtensions().findByName("android")
        if (project.getPlugins().hasPlugin(APP_PLUGIN)) {
            val appExtension = androidExtension as AppExtension
            appExtension.getApplicationVariants().forEach({ applicationVariant ->
                val variantData = getVariantData(applicationVariant)
                if (variantData != null) {
                    variantsName.add(variantData!!.getScope().getVariantConfiguration().getFullName())
                }
            })
        } else if (project.getPlugins().hasPlugin(LIB_PLUGIN)) {
            val libraryExtension = androidExtension as LibraryExtension
            libraryExtension.getLibraryVariants().forEach({ libraryVariant ->
                val variantData = getVariantData(libraryVariant)
                if (variantData != null) {
                    variantsName.add(variantData!!.getVariantConfiguration().getFullName())
                }
            })
        }
        return variantsName
    }

    private fun getVariantData(variant: BaseVariant): BaseVariantData<*>? {
        try {
            val methodGetVariantData = variant.javaClass.getMethod("getVariantData")
            methodGetVariantData.setAccessible(true)
            return methodGetVariantData.invoke(variant) as BaseVariantData<*>
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
