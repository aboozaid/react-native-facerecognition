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
import Camera from './src/index';
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
      faces: faces
    };
    
  }
  onTrained = () => {
      ToastAndroid.show("Trained", ToastAndroid.SHORT);
  }
  onUntrained = ({error}) => {
    ToastAndroid.show(error, ToastAndroid.SHORT);
  }
  onRecognized = ({name, confidence}) => {
    ToastAndroid.show("Recognized: " + name + " and Confidence " + confidence, ToastAndroid.LONG)
  }
  onUnrecognized = ({error}) => {
    ToastAndroid.show(error, ToastAndroid.SHORT)
  }
  render() {
    return (
      <View style={styles.container}>
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
        <View style={{flex: 0, flexDirection: 'row', justifyContent: 'center'}}>
        <TouchableOpacity
            onPress={this.takePicture.bind(this)}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> SNAP </Text>
        </TouchableOpacity>
        <TouchableOpacity
            onPress={() => this.camera.identify()}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> Recognize </Text>
        </TouchableOpacity>
        <TouchableOpacity
            onPress={this.clear.bind(this)}
            style = {styles.capture}
        >
            <Text style={{fontSize: 14}}> Clear </Text>
        </TouchableOpacity>
        </View>
      </View>
    );
  }
  takePicture = async function() {
    if (this.camera) {
      try {
        await this.camera.takePicture()
        this.showPanel()
      } catch(err) {
        ToastAndroid.show(err.toString(), ToastAndroid.SHORT);
      }
    }
  }
  clear() {
    try {
      this.camera.clear()
      ToastAndroid.show("Cleared", ToastAndroid.SHORT);
    } catch(err) {
      ToastAndroid.show(err.toString(), ToastAndroid.SHORT);
    }
  }
  showPanel() {
    if(this.state.faces.length == 0)
      this.newFacePanel()
    else {
      DialogManager.show({
        title: 'Trained Faces',
        titleAlign: 'center',
        haveOverlay: false,
        animationDuration: 200,
        SlideAnimation: new SlideAnimation({slideFrom: 'top'}),
        children: (
          <DialogContent>
              <View>
                <ListView dataSource = {this.state.dataSource} renderRow = {this.renderRow.bind(this)} />
              </View>
              <DialogButton text = "Close" align = 'right' onPress = {() => DialogManager.dismiss()} />
              <DialogButton text = "New Face" align = 'right' onPress = {() => this.newFacePanel()} />
          </DialogContent>
        ),
      }, () => {
        console.log('callback - show')
      });
    }
  }
  newFacePanel() {
    DialogManager.show({
      title: 'Train Face',
      titleAlign: 'center',
      haveOverlay: false,
      animationDuration: 200,
      SlideAnimation: new SlideAnimation({slideFrom: 'top'}),
      children: (
        <DialogContent>
            <View>
              <TextInput placeholder="face name" onChangeText={(fname) => this.setState({fname})} />
            </View>
          <DialogButton text = "Save" onPress= {() => this.faceDetails()}/>
        </DialogContent>
      ),
    }, () => {
      console.log('callback - show');
    });
  }
  faceDetails() {
    const faces = [...this.state.faces, {fname: this.state.fname, captured: this.state.captured}]
    const info = {fname: this.state.fname}
    this.camera.train(info)
    this.setState({dataSource: this.ds.cloneWithRows(faces), faces})
    DialogManager.dismissAll()
  }
  saveCaptureImage(faceData) {
    if(faceData.captured == 2) // you can set photos limit per face
      ToastAndroid.show("More photos are not allowed", ToastAndroid.SHORT)
    else {
      const slice = this.state.faces.slice()
      slice.map((face) => {
        if(face.fname == faceData.fname)
          face.captured++
      })
      this.setState({dataSource: this.ds.cloneWithRows(slice)})
      const info = {fname: faceData.fname}
      this.camera.train(info)
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
                  <Text style = {{fontSize: 18}}>{rowData.fname}</Text>
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
    backgroundColor: 'black'
  },
  preview: {
    flex: 1,
  },
  capture: {
    flex: 0,
    backgroundColor: '#fff',
    borderRadius: 5,
    padding: 15,
    paddingHorizontal: 20,
    alignSelf: 'center',
    margin: 20
  }
});
