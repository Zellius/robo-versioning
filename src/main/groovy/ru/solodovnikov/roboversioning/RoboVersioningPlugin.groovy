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

class RoboVersioningPlugin implements Plugin<Project> {
    private static final String ANDROID_EXTENSION_NAME = 'android'

    static final String PROJECT_EXTENSION_NAME = 'roboVersioningFlavor'
    static final String GLOBAL_EXTENSION_NAME = 'roboVersioning'

    private Project project

    /**
     * Warning: The greatest value Google Play allows for versionCode is 2100000000
     *
     * https://developer.android.com/studio/publish/versioning.html
     *
     */
    private static final int MAX_VERSION_CODE = 2_100_000_000

    @Override
    void apply(Project project) {
        this.project = project

        //only if it is Android project
        if (!isAndroidProject()) {
            throw new GradleException('Please apply com.android.application or com.android.library plugin before apply this plugin!')
        }

        final ProjectExtension projectExtension = project.extensions.create(GLOBAL_EXTENSION_NAME, ProjectExtension, project)

        prepareFlavorExtension()

        project.afterEvaluate {
            final Git git = projectExtension.git
            final Logger logger = projectExtension.logger

            getAndroidVariants().all { variant ->
                final VersionCalculator resultVersioning = ((variant.productFlavors[PROJECT_EXTENSION_NAME]*.versioningCalculator +
                        [variant.buildType[PROJECT_EXTENSION_NAME].versioningCalculator,
                         getAndroidExt().defaultConfig[PROJECT_EXTENSION_NAME].versioningCalculator]) as List<VersionCalculator>)
                        .find { it }

                logger.log("<${variant.name}> versioning type ${resultVersioning?.class?.typeName ?: 'null'}")

                if (resultVersioning != null) {
                    final def version = resultVersioning.calculate(git)

                    checkVersion(version)

                    logger.log("<${variant.name}> versionCode ${version.code}, versionName ${version.name}")

                    //put version on BuildConfig
                    variant.preBuild.dependsOn.add(project.tasks.create("roboVersioning${variant.name.capitalize()}BuildConfig", {
                        it.doLast {
                            variant.mergedFlavor.with {
                                it.versionName = version.name
                                it.versionCode = version.code
                            }
                        }
                    }))

                    //put versioning to the manifest (bad implementation...)
                    variant.outputs.all {
                        if (it.metaClass.respondsTo(it, "getApkData")) {
                            it.apkData.with {
                                it.versionCode = version.code
                                it.versionName = version.name + variant.mergedFlavor.versionNameSuffix
                            }
                        }
                    }
                }
            }
        }
    }


    private void checkVersion(RoboVersion version) {
        if (!version) {
            throw new IllegalStateException("Calculated version is null")
        }
        if (version.code > MAX_VERSION_CODE) {
            throw new IllegalStateException("Calculated versionCode ${version.code} is too big for Google Play")
        }
    }

    /**
     * Prepare flavor extension for all current and future flavors
     */
    private void prepareFlavorExtension() {
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
        if (!flavor.extensions.findByName(PROJECT_EXTENSION_NAME)) {
            flavor.extensions.create(PROJECT_EXTENSION_NAME, FlavorExtension, project, project[GLOBAL_EXTENSION_NAME])
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
