<h2 align="center">
  <img src="https://www.genetec.com/assets/Images/Global%2FProducts%2FColumn%2FPRD-Column-Add-Ons-Reduce-Risk-of-Breaches.png" /><br>
  React Native Face Recognition [Android]
</h2>

[![NPM Version](https://img.shields.io/badge/npm-2.0.0-red.svg)](https://www.npmjs.com/package/react-native-facerecognition)
[![NPM Version](https://img.shields.io/badge/yarn-2.0.0-blue.svg)](https://yarnpkg.com/en/package/react-native-facerecognition)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#)
[![Join us on Slack](https://img.shields.io/badge/Slack-react--native--facerecognition-orange.svg)](https://join.slack.com/t/rn-facerecognition/shared_invite/enQtNDAzNTQzMzczMzUwLThlMzhkMDU0ZGMyZjJmYzEwZTVjZmQzYjBiNmIwZDgyNDZkMmYwZWQzOGYwNzE3YmRkMTZmMmQxMGQ3YmY1OTg)

# Summary
💥 We've released new version of the library @2.0.0!
<hr>
Detect and Recognize faces are essential on the mobile world especially when you need to deal with computer vision on your app and it also gives you more flexibility to detect/track multiple faces on screen as well as high accuracy to recognize every face.

This library makes it easier for you to do your smarter idea if it depends on people faces or even to do hard tasks such as:
* Authentication
* Tracking
* Make something when specific person appears

## Important
* New to Face Recognition? We recommend you start with the default values which described below.
* Looking to contribute? feel free to join our community on [Slack](https://join.slack.com/t/rn-facerecognition/shared_invite/enQtNDAzNTQzMzczMzUwLThlMzhkMDU0ZGMyZjJmYzEwZTVjZmQzYjBiNmIwZDgyNDZkMmYwZWQzOGYwNzE3YmRkMTZmMmQxMGQ3YmY1OTg), and take a look into coming updates.
* If you're trying to use the library on the last version of react native (0.56.0) it won't work as this version still has many bugs to fix.

>⚠️ Since we're focusing our efforts on next updates, we are really welcome for any issues/pulls to improve the way we go.

## Features
* Multiple Detection
* Fast Recognition (LBPH Algorithm only)
* Tracking faces on screen
* Easy to use
* Without any internet!
* All devices supported.

## Upcoming updates
* Add EagenFace Algorithm ❌
* Add Fisherface Algorithm ❌
* Expo support ❌
* Automatic Recognition and Detection ❌
* UI Component ✔️

### Real world examples
<img src="https://preview.ibb.co/kmDT6y/Screenshot_20180720_151322.png" width="240">&nbsp;&nbsp;&nbsp;&nbsp;
<img src="https://preview.ibb.co/n5VxzJ/Screenshot_20180720_151359.png" width="240">&nbsp;&nbsp;&nbsp;&nbsp;
<img src="https://preview.ibb.co/ho8HzJ/Screenshot_20180720_151424.png" width="240">

🔥 Video will be available ASAP.
<hr>

### Code Example
~~~
import Camera from 'react-native-facerecognition';
...
<Camera
    ref={(cam) => {
      this.camera = cam;
    }}
    style={styles.preview}
    aspect={Camera.constants.Aspect.fill}
    captureQuality={Camera.constants.CaptureQuality.medium}
    cameraType={Camera.constants.CameraType.front}
    model = {Camera.constants.Model.cascade}
    onTrained = {this.onTrained}
    onRecognized = {this.onRecognized}
    onUntrained = {this.onUntrained}
    onUnrecognized = {this.onUnrecognized}
  />
~~~
🔥 [Checkout](https://github.com/assemmohamedali/react-native-facerecognition/tree/master/Example) our main example to get the right way to start your own recognition.

## Get Started

~~~ 
npm install react-native-facerecognition --save
or
yarn add react-native-facerecognition
~~~
>⚠️ We are highly recommend you to use Deamon gradle for building faster and ignore any error may happen.
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
include ':openCV'
project(':openCV').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-facerecognition/android/openCV')
~~~
* You need to put permissions to use Camera on `AndroidManifest.xml` and remove the line below.
~~~
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true"/>
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false"/>
<uses-feature android:name="android.hardware.camera.front" android:required="true"/>
<uses-feature android:name="android.hardware.camera.front.autofocus" android:required="false"/>
...
<activity
  android:windowSoftInputMode="adjustResize" // remove this in order to make UI works well
~~~

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
include ':openCV'
project(':openCV').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-facerecognition/android/openCV')
~~~
* You need to put permissions to use Camera on `AndroidManifest.xml` and remove the line below.
~~~
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true"/>
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false"/>
<uses-feature android:name="android.hardware.camera.front" android:required="true"/>
<uses-feature android:name="android.hardware.camera.front.autofocus" android:required="false"/>
...
<activity
  android:windowSoftInputMode="adjustResize" // remove this in order to make UI works well
~~~

🔥 You're ready to go!

## Documentation
How it works:
> There's three main process you have to follow the first one is to detect the face first on the image, Then you do some training on every face (Person1 & Person2 & Person3) you will need to recognize later which is the second process. Then, you can now after training, Recognize all these faces by their names.

### 1.3 Props

`model`
* Camera.constants.Model.cascade
  * Higher recall & More trained faces
* Camera.constants.Model.lbp
  * Higher precision & Faster & Less trained faces
  
`cameraType`
* Camera.constants.CameraType.front
* Camera.constants.CameraType.back

`captureQuality`
* Camera.constants.CaptureQuality.low
* Camera.constants.CaptureQuality.medium
  * Highly recommended
* Camera.constants.CaptureQuality.high
  * May work slow on multiple detection
  
`aspect`
* Camera.constants.Aspect.fit
* Camera.constants.Aspect.fill
* Camera.constants.Aspect.stretch

`distance`
~~~
<Camera distance = {200} />
~~~
* Distance between the face and the camera. This is very important to keep recognition always works and to help make the result mainly true.

`rotateMode` - (Landscape/Portrait)
* Camera.constants.RotateMode.on
* Camera.constants.RotateMode.off

### 1.4 Functions

* takePicture()
 > Take a picture then processing it to detect face inside

* train(Object)
> Train the algorithm with the new detected face

* identify()
> Take a picture then predict whose face belongs to

* clear()
> Clear all previous trained faces

### 1.5 Events

`onTrained`
* Called after success training

`onUntrained`
* If training fails this function will be called with the error

`onRecognized`
You recieved details about recognized face:
* `name` The face name
* `confidence` This number indicates how much the result is true. Usually low number < 100 means a good result

`onUnrecognized`
* If recognition fails this function will be called with the error

## Recommendations

>If you're not familiar with OpenCV and face recognition you have to be in safe and use our default arguments as we care about all of the details for you. In case of using your own arguments please note that you may effect the accuracy depend on your settings.

* Recognition default arguments <br>
__Arguments__
  - distance = {200}
  
* Training images count<br>
__Minimum__
  - Two photos per face (Recommended if you're training for few faces like 2 or 3)
  - There's no maximum but the average is 3~4 photos per face to guarantee high accuracy
  
## Licence

MIT
