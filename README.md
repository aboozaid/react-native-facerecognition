<h2 align="center">
  <img src="https://www.genetec.com/assets/Images/Global%2FProducts%2FColumn%2FPRD-Column-Add-Ons-Reduce-Risk-of-Breaches.png" /><br>
  React Native Face Recognition [Android]
</h2>

[![NPM Version](https://img.shields.io/badge/npm-1.0.0-red.svg)](https://www.npmjs.com/package/react-native-facerecognition)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#)
[![Join us on Slack](https://img.shields.io/badge/Slack-react--native--facerecognition-orange.svg)](https://join.slack.com/t/rn-facerecognition/shared_invite/enQtNDAzNTQzMzczMzUwLThlMzhkMDU0ZGMyZjJmYzEwZTVjZmQzYjBiNmIwZDgyNDZkMmYwZWQzOGYwNzE3YmRkMTZmMmQxMGQ3YmY1OTg)

## Important
We are in `BETA` period to keep sure that the library works as expected in all devices as well as the user can have all of its features work as a charm!
* New to Face Recognition? We recommend you start with the default values which described below.
* Looking to contribute? feel free to join our community on [Slack](https://join.slack.com/t/rn-facerecognition/shared_invite/enQtNDAzNTQzMzczMzUwLThlMzhkMDU0ZGMyZjJmYzEwZTVjZmQzYjBiNmIwZDgyNDZkMmYwZWQzOGYwNzE3YmRkMTZmMmQxMGQ3YmY1OTg), and take a look into coming updates.
* Currently, we rely on [react-native-camera](https://github.com/react-native-community/react-native-camera) library to open camera and do the process but in the next updates we'll have our own component.
* If you're trying to use the library on the last version of react native (0.56.0) it won't work as this version still has many bugs to fix.

<br><br>Don't be afraid to use this `BETA` version. It supports react-native >= 0.41.2.
>⚠️Since we're focusing our efforts on next updates, we are really welcome for any issues/pulls to improve the way we go.

## Features
* Fast Detection
* Fast Recognition (LBPH Algorithm only)
* High Accuracy
* Easy to use
* Without any internet!
* All devices supported except the ones with mips cpu.

## Upcoming updates
* Add EagenFace Algorithm
* Add Fisherface Algorithm
* Expo support
* Automatic Recognition and Detection
* UI Component

### Real world examples
<img src="https://preview.ibb.co/kmDT6y/Screenshot_20180720_151322.png" width="240">&nbsp;&nbsp;&nbsp;&nbsp;
<img src="https://preview.ibb.co/n5VxzJ/Screenshot_20180720_151359.png" width="240">&nbsp;&nbsp;&nbsp;&nbsp;
<img src="https://preview.ibb.co/ho8HzJ/Screenshot_20180720_151424.png" width="240">

🔥 Video will be available ASAP.
<hr>

### Code Example

🔥 [Checkout](https://github.com/assemmohamedali/react-native-facerecognition/tree/master/Example) our main example to get the right way to start your own recognition.

## Get Started

~~~ 
npm install react-native-facerecognition --save
or
yarn add react-native-facerecognition
~~~
>⚠️ We are highly recommend you to use Deamon gradle for building faster and ignore any error may happen.
>> Install the package using npm or yarn may take a while for 10 ~ 15 mins we'll fix this ASAP
* Inside `build.gradle` put this line in `projectName/android/build.gradle`.
~~~ 
repositories {
        ...
        google()
   }
   ...
   classpath 'com.android.tools.build:gradle:3.0.1'
   ...
   allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
        maven { url "https://maven.google.com" }
        ...
        }
       }
       
~~~
* And go to `projectName/android/gradle/wrapper/gradle-wrapper.properties` and change this.
~~~
distributionUrl=https\://services.gradle.org/distributions/gradle-4.4-all.zip
~~~

### 1.1 Direct Setup

* You would need to link the library once you install it.
~~~ 
react-native link react-native-facerecognition 
~~~
* Add this line into `build.gradle` and the path `projectName/android/app/build.gradle`.
~~~
compileSdkVersion 26
buildToolsVersion "26.0.2"
....
....
~~~
* Then put this lines into `settings.gradle` and the path `projectName/android/settings.gradle`.
~~~
include ':openCVLibrary'
project(':openCVLibrary').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-facerecognition/android/openCVLibrary')
~~~
* The last step is to install the camera package from [react-native-camera](https://github.com/react-native-community/react-native-camera) you can find the documentation for how to install it there.

### 1.2 Manual Setup

* First, put this line into `MainApplication.java` and its path `projectName/android/app/src/main/java/com/projectName/MainApplication.java`.
~~~
import opencv.android.FaceModulePackage;
...
return Arrays.<ReactPackage>asList(
  new FaceModulePackage()
);
~~~
* Then add this line into `build.gradle` and the path `projectName/android/app/build.gradle`.
~~~
compileSdkVersion 26
buildToolsVersion "26.0.2"
....
....
....
dependencies {
  compile project(':react-native-facerecognition')
  }
~~~
* Inside `settings.gradle ` put these lines you can find the path at `projectName/android/settings.gradle` 
~~~
include ':react-native-facerecognition'
project(':react-native-facerecognition').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-facerecognition/android/app')
include ':openCVLibrary'
project(':openCVLibrary').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-facerecognition/android/openCVLibrary')
~~~
* The last step is to install the camera package from [react-native-camera](https://github.com/react-native-community/react-native-camera) you can find the documentation for how to install it there.

🔥 You're ready to go!

## Documentation
How it works:
> There's three main process you have to follow the first one is to detect the face first on the image, Then you do some training on every face (Person1 & Person2 & Person3) you will need to recognize later which is the second process. Then, you can now after training, Recognize all these faces by their names.

### 1.3 Initialize
* Before going to do anything you have to initialize the data which used by detection and recognition methods.

~~~
import Face from 'react-native-facerecognition'
~~~

* `Start(options, success callback, error callback)`<br>
If you're newbie to face recognition you will use this method as we put our improved data for you by default and you have two options: <br><br>
`Face.Detection.DEEP:` It's highly recommend to use that constant if you need a good result as well as accurate recognition but sometimes it may be a little bit slow[Depend on camera resolution]<br><br>
`Face.Detection.FAST:` Should be faster than DEEP and still you get a good result but you may miss some recognition <br>

* `Initialize(DetectionObj, RecognitionObj, success callback, error callback)`<br>
If you're not going to use our default data, You will need to define yours by using this function instead of Start, It takes two arguments:
~~~ 
const detection = {scaleFactor: 1.1, minNeighbors: 4, minWidth: 30, minHeight: 30, flag: options, module: options}
~~~
We support three types of flags which described below:<br>
`flag: Face.Detection.Scale: Downscale the image by 1.1 (scaleFactor) rather than zoom`<br>
`flag: Face.Detection.Biggest: Usually to find the biggest object on the image`<br>
`flag: Face.Detection.Canny: Enable canny detector which rejects some regions on image that contain few edges which cannot contain the searched object`<br>
We have three main modules to detect faces called LBP & Cascade:<br>
`module: Face.Detection.Module.Default: This module contain many trained faces and higher recall of faces`<br>
`module: Face.Detection.Module.Cascade: Like default but with less trained faces and high precision`<br>
`module: Face.Detection.Module.LBP: Less faces trained but very precision for small face and faster`<br>
>⚠️ Bear in mind that choose your module is very important as it effects the recognition step significantly
>> [Checkout](https://stackoverflow.com/questions/20801015/recommended-values-for-opencv-detectmultiscale-parameters) this article will help you to understand how to initialize the other variables
~~~ 
const recognition = {radius: 3, neighbors: 8, grid_x: 8, grid_y: 8, threshold: 200, maxConfidence: 125}
~~~
Till now we support one recognition algorithm Local Binary Patterns Histograms(LBPH) you can read more about it here [LBPH](https://towardsdatascience.com/face-recognition-how-lbph-works-90ec258c3d6b). <br>

`maxConfidence: The distance between the recognized face and the mobile so if the captured image exceeds this number it will unrecognized` <br>

### 1.4 Detection

* `Detect(imageAsBase64, success callback, error callback)`<br>
Check whether if there's face inside the image or not<br>
> react-native-camera automatically generates a base64 image when taking the picture check out the example

* `Training(arguments, success callback, error callback)` <br>
Train the detected faces so that we can recognize them later<br>
__Arguments__
  - `ImageAsBase64` - `String` - The detected face image
  - `Face name` - `String` - The detected face name
~~~
const arguments = {ImageAsBase64: String, Name: String}
~~~

### 1.5 Recognition

* `Identify(imageAsBase64, error callback)` <br>
Recognize the detected face from all trained faces <br>

### 1.6 Events

* `onFaceRecognized(callback)` <br>
__Callback Contain__
  - `name` - `String` - The recognized face name
  - `distance` - `String` - The distance between the face and the mobile

* `onClean()` <br>
Clean all trained face from the module.

## Recommendations

>If you're not familiar with OpenCV and face recognition you have to be in safe and use our default arguments as we care about all of the details for you. In case of using your own arguments please note that you may effect the performance depend on your settings so hopefully read the articles were mentioned.

* `Face.Detection.DEEP Default arguments` <br>
__Arguments__
  - scaleFactor = 1.1
  - minNeighbors = 4
  - minWidth = 30, minHeight = 30
  - flag = Scale
  - module = Default
  
* `Face.Detection.FAST Default arguments` <br>
__Arguments__
  - scaleFactor = 1.35
  - minNeighbors = 5
  - minWidth = 30, minHeight = 30
  - flag = Canny
  - module = Default
 
* Recognition default arguments <br>
__Arguments__
  - radius = 3
  - neighbors = 8
  - grid_x = 8, grid_y = 8
  - threshold = 200
  - maxConfidence = 125
  
* Training images count<br>
__Minimum__
  - Two photos per face (Recommended if you're training for few faces like 2 or 3)
  - There's no maximum but the average is 3~4 photos per face to guarantee high accuracy
  
## Licence

MIT
