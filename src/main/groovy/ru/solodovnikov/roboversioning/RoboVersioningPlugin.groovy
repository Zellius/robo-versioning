package ru.solodovnikov.roboversioning

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.solodovnikov.roboversioning.ext.FlavorExtension
import ru.solodovnikov.roboversioning.ext.GlobalExtension
import sun.security.krb5.internal.ccache.Tag

class RoboVersioningPlugin implements Plugin<Project> {
    private static final String ANDROID_EXTENSION_NAME = 'android'

    static final String FLAVOR_EXTENSION_NAME = 'roboFlavor'
    static final String GLOBAL_EXTENSION_NAME = 'roboGlobal'


    private static final int MAX_VERSION_CODE = 2_100_000_000

    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        //only if it is Android project
        if (!isAndroidProject()) {
            throw new GradleException('Please apply com.android.application or com.android.library plugin before apply this plugin!')
        }
        final GlobalExtension globalExtension = getAndroidExt().extensions.create(GLOBAL_EXTENSION_NAME, GlobalExtension, project)

        prepareSetupExtension()

        project.afterEvaluate {
            final Git git = new Git(new GitExecutorImpl(globalExtension.git?.path ?: 'git'))

            List<Tag> gitTags

            getAndroidVariants().all { variant ->
                final Versioning defaultVersioning = getAndroidExt().defaultConfig[FLAVOR_EXTENSION_NAME].versioning
                final Versioning buildTypeVersioning = variant.buildType[FLAVOR_EXTENSION_NAME].versioning
                final Versioning flavorVersioning = variant.productFlavors[FLAVOR_EXTENSION_NAME]*.versioning[0]

                final Versioning resultVersioning = flavorVersioning ?: buildTypeVersioning ?: defaultVersioning

                if (resultVersioning != null) {
                    if (gitTags == null) {
                        gitTags = git.tags()
                        println("Tags: ${gitTags.name}")
                    }
                    if (!gitTags?.empty ?: true) {
                        for (tag in gitTags) {
                            if (resultVersioning.isTagValid(tag)) {
                                println("Valid tag for ${variant.name} build variant is ${tag.name}")

                                final def version = resultVersioning.calculate(tag)

                                //put version on BuildConfig
                                def buildConfigVersioningTask = project.tasks.create("${variant.name.toUpperCase()}BuildConfigRoboVersioning", {
                                    it.doLast {
                                        variant.mergedFlavor.with {
                                            it.versionName = version.name
                                            it.versionCode = version.code

                                        }
                                    }
                                })

                                variant.preBuild.dependsOn.add(buildConfigVersioningTask)

                                //put versioning to the manifest
                                variant.outputs.all {
                                    if (it.metaClass.respondsTo(it, "getApkData")) {
                                        it.apkData.with {
                                            it.versionCode = version.code
                                            it.versionName = version.name
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Prepare setup extension for all current and future flavors
     */
    private void prepareSetupExtension() {
        def android = getAndroidExt()

        //add extensions to default flavor
        registerFlavorExtension(android.defaultConfig)
        //add extensions to custom flavors
        android.productFlavors.whenObjectAdded {
            registerFlavorExtension(it)
        }
        //add extensions for release and debug build types
        android.buildTypes.all {
            registerFlavorExtension(it)
        }
        //add extensions for custom build types
        android.buildTypes.whenObjectAdded {
            registerFlavorExtension(it)
        }
    }

    private void registerFlavorExtension(flavor) {
        if (!flavor.extensions.findByName(FLAVOR_EXTENSION_NAME)) {
            flavor.extensions.create(FLAVOR_EXTENSION_NAME, FlavorExtension)
        }
    }

    private BaseExtension getAndroidExt() {
        return project[ANDROID_EXTENSION_NAME]
    }

    private DomainObjectSet<BaseVariant> getAndroidVariants() {
        def androidExtension = getAndroidExt()
        if (isAndroidApplication()) {
            return (androidExtension as AppExtension).getApplicationVariants()
        } else if (isAndroidLibrary()) {
            return (androidExtension as LibraryExtension).getLibraryVariants()
        } else if (isAndroidTest()) {
            return (androidExtension as TestExtension).getApplicationVariants()
        } else {
            throw new GradleException("Cant find Android extension.groovy")
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
