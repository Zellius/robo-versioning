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
For each build variant the plugin will select a single **versioningCalculator** based on logic (producFlavor -> buildType -> defaultConfig). In the example above for _flavorRelease_ variant will be choosed TAG_DIGIT_RC implementation and for _flavor2Release_ - TAG_DIGIT.

Where are some brebuild **versioningCalculator**:
- **TAG_DIGIT**: git tag pattern (\d+).(\d+).(\d+). For git tag _1.2.3_ the result will be: versionCode 10203, versionName 1.2.3. If where is no valid git tag the result wull be  versionCode 0, versionName 0.0.0.
- **TAG_DIGIT_RC**: git tag pattern ((\d+).(\d+).(\d+)rc(\d+). For git tag _1.2.3rc4_ the result will be: versionCode 1020304, versionName 1.2.3rc4. If where is no valid git tag the result wull be  versionCode 0, versionName 0.0.0rc0.
- **TAG_DESCRIBE_DIGIT**: git tag pattern (\d+).(\d+).(\d+). For git tag _1.2.3_ the result will be: versionCode 10203, versionName 1.2.3-14-g2414721. If where is no valid git tag the result wull be  versionCode 0, versionName 0.0.0.
- **TAG_DESCRIBE_DIGIT_RC**: git tag pattern ((\d+).(\d+).(\d+)rc(\d+). For git tag _1.2.3rc4_ the result will be: versionCode 1020304, versionName 1.2.3rc4-14-g2414721. If where is no valid git tag the result wull be  versionCode 0, versionName 0.0.0rc0.

##### Customize
You can customize digit tag **versioningCalculator** like this:
```gradle
roboVersioningFlavor {
    versioningCalculator customDigitTag {
        //you can customize standard digit versioning calculator
        
        //required. Regex pattern.
        pattern = "(\\d+).(\\d+).(\\d+)" 
        //optional. Additional tag validation. You can set boolean on Closure.
        valid = {tag -> tag.hash == "123"} 
        //optional. You can calculate your own code based on parsed int array. You can set Integer or Closure.
        versionCode = {tagParts -> tagParts[0]}
         //optional. You can calculate your own name based on git tag. You can set String or Closure.
        versionName = {tag -> "custom_version_name"}
         //required. Used in the case if where is no valid tag for this calculator. You can set Map or Closure.
        emptyVersion = ["name": "empty", "code": 123]
    }
}
```
Or you can write your own **versioningCalculator**:
```gradle
roboVersioningFlavor {
    versioningCalculator { git ->
        //you can extract data from git here
        def tags = git.tags()
        def describeTags = git.execute("describe --tags")
        //do something and reurn result as below
        ['name': 'custom', 'code': 123]
    }
}
```
##### Global plugin customization
Use it to change plugin's logger or git settings.
```gradle
roboVersioning {
    //configure git
    git {
        //change path to the git executable file
        gitPath = 'path_to_git'
        //add additional params to get tags command
        tagsParams = '--first-parent'
        //add additional params to describe command
        describeParams = '--first-parent'
    }
    //or you can set your own Git implementation
    git new Git() {
        @Override
        List<Git.Tag> tags() {
            return null
        }

        @Override
        String describe(String s) {
            return null
        }
    }
    
    //use it to disable plugin's logger
    quiet()
    //or to set your own logger
    logger {message -> println(message)}
}

android {}
```
