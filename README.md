# RoboVersioning
It is Android Gradle versioning plugin based on Git tags. You can set different versioning on BuildTypes and ProductFlavors. So you can have different versioning on a same git branch. Works well with git-flow and merge-commits. Tested on **com.android.tools.build:gradle:3.0.1**.
## Base usage
Set **versioningCalculator** to calculate versionName and versionCode like this:
```gradle
android {
    defaultConfig {
        //optional
        roboVersioningFlavor {
            versioningCalculator TAG_DESCRIBE_DIGIT_RC
        }
    }
    buildTypes {
        release {
            //optional
            roboVersioningFlavor {
                versioningCalculator TAG_DIGIT
            }
        }
    }

    flavorDimensions 'dimension'

    productFlavors {
        flavor {
            dimension "dimension"
            //optional
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

Plugin uses **git log --first-parent** command to parse and get list of tags. You can customize it.

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
## License

```
IT License

Copyright (c) 2017 Sergey Solodovnikov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
