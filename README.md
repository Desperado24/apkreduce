一款支持自动将图片转换成WebP格式的Android构建插件

### 功能：
1. 支持将资源文件夹中的jpg、png图片转换成webp格式；
2. 构建过程中自动下载libwebp，无需再配置环境；
3. 支持手动与自动两种构建模式，祥见 `配置`；
4. 自动构建过程中，图片转换时机位于mergeResources之前，避免图片被重复处理多次；

### 接入：
1. 引用插件
    ```
    buildscript {
        dependencies {
            classpath "com.desperado.apkreduce:apkreduce:1.0.6"
        }
    }
     apply plugin: 'apkreduce.convertwebp'
     convertWebpConfig {
             autoConvert true
             quality 75
         }

2. 构建
    ```
    autoConvert = true:         ./gradlew build

    autoConvert = false:        ./gradlew convert***WebP
    ```

### 配置：
配置名称 |  类型 | 默认值 | 说明
------- | ------- | ------- | -------
quality | int | 75 | 指定转换过程中RGB通道的压缩因，取值范围0到100。值越小，压缩率越高，图片质量越低。
autoConvert | boolean | false | 若设置为true，转换任务将自动加入构建过程，执行gradle build即可；若设置为false，需手动执行gradle convertWebP。

### 构建产物：
- ```autoConvert = true```，sourSets.res指定资源文件夹中所有jpg、png（非.9）图片将被转换为webp格式，原有图片将移动到```projectDir/before_convertwebp_res```目录中。
- ```autoConvert = false```，原有图片路径不改变，转换后的webp图片位于```projectDir/before_convertwebp_res```目录中。
