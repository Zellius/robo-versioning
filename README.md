# RoboVersioning
It is Android Gradle versioning plugin based on Git tags. You can set versioning on BuildTypes and ProductFlavors. Works well with git-flow and merge-commits.
### How it works
Set **versioningCalculator** to calculate versionName and versionCode like this:
```gradle
android {
    defaultConfig {
        roboVersioningFlavor {
            versioningCalculator TAG_DESCRIBE_DIGIT_RC
        }
    }
    buildTypes {
        release {
            roboVersioningFlavor {
                versioningCalculator TAG_DIGIT
            }
        }
    }

    flavorDimensions 'dimension'

    productFlavors {
        flavor {
            dimension "dimension"
            roboVersioningFlavor {
                versioningCalculator TAG_DIGIT_RC
            }
        }
        flavor2{
            dimension "dimension"
        }
    }
}
```
For each build variant plugin will select single **versioningCalculator** based on logic (producFlavor -> buildType -> defaultConfig). In the example above for _flavorRelease_ variant will be choosed TAG_DIGIT_RC implementation and for _flavor2Release_ - TAG_DIGIT.

Where are some brebuild **versioningCalculator**:
- **TAG_DIGIT**: git tag pattern (\d+).(\d+).(\d+). For git tag _1.2.3_ the result will be: versionCode 10203, versionName 1.2.3. If where is no valid git tag the result wull be  versionCode 0, versionName 0.0.0
- **TAG_DIGIT_RC**: git tag pattern ((\d+).(\d+).(\d+)rc(\d+). For git tag _1.2.3rc4_ the result will be: versionCode 1020304, versionName 1.2.3rc4. If where is no valid git tag the result wull be  versionCode 0, versionName 0.0.0rc0
