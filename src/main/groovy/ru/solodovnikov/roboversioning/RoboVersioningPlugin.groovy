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

class RoboVersioningPlugin implements Plugin<Project> {
    private static final String TAG = 'RoboVersioning'

    private static final String ANDROID_EXTENSION_NAME = 'android'

    static final String FLAVOR_EXTENSION_NAME = 'roboFlavor'
    static final String GLOBAL_EXTENSION_NAME = 'roboGlobal'

    /**
     * Warning: The greatest value Google Play allows for versionCode is 2100000000
     *
     * https://developer.android.com/studio/publish/versioning.html
     *
     */
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

            List<Git.Tag> gitTags

            getAndroidVariants().all { variant ->
                def resultVersioning = ((variant.productFlavors[FLAVOR_EXTENSION_NAME]*.versioning +
                        [variant.buildType[FLAVOR_EXTENSION_NAME].versioning,
                         getAndroidExt().defaultConfig[FLAVOR_EXTENSION_NAME].versioning]) as List<Versioning>)
                        .find { it }

                println("$TAG: <${variant.name}> versioning type ${resultVersioning?.class?.simpleName ?: 'null'}")

                if (resultVersioning != null) {
                    if (gitTags == null) {
                        gitTags = git.tags()
                        println("$TAG: tags ${gitTags?.name ?: 'empty'}")
                    }

                    final def version = (gitTags?.find {
                        resultVersioning.isTagValid(it)
                    } ?: null).with {
                        final RoboVersion calculatedVersion
                        if (!it) {
                            println("$TAG: <${variant.name}> there is no valid tag for build variant")
                            calculatedVersion = resultVersioning.empty()
                        } else {
                            println("$TAG: <${variant.name}> Valid tag is ${it.name}")
                            calculatedVersion = resultVersioning.calculate(it)
                        }
                        println("$TAG: <${variant.name}> tag calculated version $calculatedVersion")
                        if (calculatedVersion.code > MAX_VERSION_CODE) {
                            throw new IllegalStateException("Calculated versionCode ${it.code} is too big for Google Play")
                        }
                        return calculatedVersion
                    }

                    //put version on BuildConfig
                    variant.preBuild.dependsOn.add(project.tasks.create("RoboVersioning${variant.name.toUpperCase()}BuildConfig", {
                        it.doLast {
                            variant.mergedFlavor.with {
                                it.versionName = version.name
                                it.versionCode = version.code

                            }
                        }
                    }))

                    //put versioning to the manifest
                    variant.outputs.all {
                        if (it.metaClass.respondsTo(it, "getApkData")) {
                            it.apkData.with {
                                it.versionCode = version.code
                                it.versionName = version.name
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
