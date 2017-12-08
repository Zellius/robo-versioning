package ru.solodovnikov.roboversioning

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class RoboVersioningPlugin implements Plugin<Project> {
    private static final String ANDROID_EXTENSION_NAME = 'android'

    private Project project

    @Override
    void apply(Project project) {
        //only if it is Android project
        if (!isAndroidProject()) {
            throw new GradleException('Please apply com.android.application or com.android.library plugin before apply this plugin!')
        }

        this.project = project

        def ext = project.extensions.create('gitSettings', RoboVersioningGitExtension)

        project.afterEvaluate {
            getAndroidVariants().all {
                println("Hello ${it.name}")
            }
        }
    }

    private BaseExtension getAndroidExt() {
        return project[ANDROID_EXTENSION_NAME]
    }

    private DomainObjectSet<BaseVariant> getAndroidVariants() {
        def androidExtension = getAndroidExt()
        if (isAndroidApplication()) {
            return (androidExtensiona as AppExtension).getApplicationVariants()
        } else {
            return (androidExtension as LibraryExtension).getLibraryVariants()
        }
    }

    private boolean isAndroidProject() {
        return isAndroidApplication() || isAndroidLibrary() || isAndroidTest()
    }

    private boolean isAndroidApplication() {
        return project.plugins.hasPlugin("com.android.application")
    }

    private boolean isAndroidLibrary() {
        return project.plugins.hasPlugin("com.android.library")
    }

    private boolean isAndroidTest() {
        return project.plugins.hasPlugin("com.android.test")
    }
}
