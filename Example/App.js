/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  ToastAndroid,
  ListView,
  TextInput,
  TouchableHighlight,
  DeviceEventEmitter
} from 'react-native';
import { RNCamera } from 'react-native-camera';
import Face from 'react-native-facerecognition'
import DialogManager, { SlideAnimation, DialogContent, DialogTitle, DialogButton } from 'react-native-dialog-component';

export default class App extends Component {
  constructor(props) {
    super(props);
    this.ds = new ListView.DataSource({
      rowHasChanged:(r1,r2) => r1 !== r2
    });
    const faces = [];
    this.state = {
      dataSource: this.ds.cloneWithRows(faces),
      captured: 1,
      faces: faces,
      type: 'front'
    };
  }
  componentDidMount() {
    DeviceEventEmitter.addListener("onFaceRecognized", this.onFaceRecognized.bind(this))
    DeviceEventEmitter.addListener("onClean", this.onClean.bind(this))
  }
  render() {
    return (
      <View style={styles.container}>
        <RNCamera
            ref={ref => {
              this.camera = ref;
            }}
            style = {styles.preview}
            type={this.state.type}
            permissionDialogTitle={'Permission to use camera'}
            permissionDialogMessage={'We need your permission to use your camera phone'}
            onMountError={(msg) => this.cameraError(msg)}
            onCameraReady={() => {this.onCameraReady()}}
        />
        <View style={{flex: 0, flexDirection: 'row', justifyContent: 'center',}}>
        <TouchableOpacity
            onPress={this.takePicture.bind(this)}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> SHOT </Text>
        </TouchableOpacity>
        <TouchableOpacity
            onPress={this.recognizePicture.bind(this)}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> RECOGNIZE </Text>
        </TouchableOpacity>
        <TouchableOpacity
            onPress={() => this.setState({type: this.state.type === 'back' ? 'front' : 'back',})}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> FLIP </Text>
        </TouchableOpacity>
        <TouchableOpacity
            onPress={() => this.clean()}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> CLEAN </Text>
        </TouchableOpacity>
        </View>
      </View>
    );
  }
  
  onFaceRecognized(data) {
    ToastAndroid.show("Recognized: " + data.name + " Distance: " + data.distance, ToastAndroid.LONG)
  }
  onClean(msg) {
    this.setState({faces: []})
    ToastAndroid.show(msg, ToastAndroid.SHORT)
  }
  clean() {
    Face.Clean();
  }
  onCameraReady() {
    Face.Start(Face.Detection.DEEP, (success) => {
      ToastAndroid.show("Train initialized", ToastAndroid.SHORT)
    }, (error) => {
      ToastAndroid.show(error, ToastAndroid.LONG)
    })
  }

  takePicture = async function() {
    if (this.camera) {
      const options = { width: 200, base64: true }
      const data = await this.camera.takePictureAsync(options)
      Face.Detect(data.base64, (detected) => {
        ToastAndroid.show(detected, ToastAndroid.SHORT)
        this.setState({image64: data.base64})
        this.onFaceDetect()
      }, (error) => {
        ToastAndroid.show(error, ToastAndroid.SHORT)
      })
    }
  };
  recognizePicture = async function() {
    if (this.camera) {
      const options = { width: 200, base64: true };
      const data = await this.camera.takePictureAsync(options)
      Face.Detect(data.base64, (detected) => {
        ToastAndroid.show(detected, ToastAndroid.SHORT)
        Face.Identify(data.base64, (unrecognized) => {
          ToastAndroid.show(unrecognized, ToastAndroid.SHORT)
        })
      }, (error) => {
        ToastAndroid.show(error, ToastAndroid.SHORT)
      })
      console.log(data.uri);
    }
  };
  onFaceDetect() {
    if(this.state.faces.length == 0)
      this.newFaceDetected();
    else {
      DialogManager.show({
        title: 'Trained Faces',
        titleAlign: 'center',
        haveOverlay: false,
        animationDuration: 200,
        SlideAnimation: new SlideAnimation({slideFrom: 'top'}),
        children: (
          <DialogContent >
              <View>
                <ListView dataSource = {this.state.dataSource} renderRow = {this.renderRow.bind(this)} />
              </View>
              <DialogButton text = "Close" align = 'right' onPress = {() => DialogManager.dismiss()} />
              <DialogButton text = "New Face" align = 'right' onPress = {() => this.newFaceDetected()} />
          </DialogContent>
        ),
      }, () => {
        console.log('callback - show')
      });
    }
  }
  newFaceDetected() {
    DialogManager.show({
      title: 'Train Face',
      titleAlign: 'center',
      haveOverlay: false,
      animationDuration: 200,
      SlideAnimation: new SlideAnimation({slideFrom: 'top'}),
      children: (
        <DialogContent>
            <View>
              <TextInput placeholder="face name" onChangeText={(Fname) => this.setState({Fname})} />
            </View>
          <DialogButton text = "Save" onPress= {() => this.newFaceImage()}/>
        </DialogContent>
      ),
    }, () => {
      console.log('callback - show');
    });
  }
  newFaceImage() {
    const faces = [...this.state.faces, {Fname: this.state.Fname, captured: this.state.captured}]
    const images = {image64: this.state.image64, Fname: this.state.Fname}
    Face.Training(images, (result) => alert(result), (err) => alert(err))
    this.setState({dataSource: this.ds.cloneWithRows(faces), faces})
    DialogManager.dismissAll()
  }
  saveCaptureImage(faceData) {
    if(faceData.captured == 5)
      ToastAndroid.show("More photos are not allowed", ToastAndroid.SHORT)
    else {
      const slice = this.state.faces.slice()
      slice.map((face) => {
        if(face.Fname == faceData.Fname)
          face.captured++
      })
      this.setState({dataSource: this.ds.cloneWithRows(slice)})
      const images = {image64: this.state.image64, Fname: faceData.Fname}
      Face.Training(images, (result) => alert(result), (err) => alert(err))
    }
    DialogManager.dismiss()
  }
  renderRow(rowData) {
    return(
          <TouchableHighlight onPress= {() => this.saveCaptureImage(rowData)} underlayColor='transparent' >
            <View style = {{
                flex:1,
                flexDirection: 'row',
                padding: 15,
                alignItems: 'center',
                borderColor: '#D7D7D7',
                borderBottomWidth: 1
            }}>
            <Text style = {{fontSize: 16}}>{rowData.captured}</Text>
                <View style = {{paddingLeft: 20}}>
                  <Text style = {{fontSize: 18}}>{rowData.Fname}</Text>
                </View>
            </View>
          </TouchableHighlight>
    );
  }
}


const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    backgroundColor: 'white'
  },
  preview: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center'
  },
  capture: {
    flex: 0,
    backgroundColor: '#D7D7D7',
    borderRadius: 5,
    padding: 10,
    paddingHorizontal: 10,
    alignSelf: 'center',
    margin: 10
  }
});
