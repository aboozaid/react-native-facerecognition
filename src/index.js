import React, { Component } from 'react';
import PropTypes from 'prop-types';
import {NativeModules, requireNativeComponent, View, findNodeHandle} from 'react-native';

const CameraManager = NativeModules.Face;
const CAMERA_REF = 'camera';

type DetectOption = {
  fname?: string
};

function convertNativeProps(props) {
    const newProps = { ...props };
    if (typeof props.aspect === 'string') {
      newProps.aspect = Camera.constants.Aspect[props.aspect];
    }
    if (typeof props.torchMode === 'string') {
        newProps.torchMode = Camera.constants.TorchMode[props.torchMode];
    }
    if (typeof props.captureQuality === 'string') {
      newProps.captureQuality = Camera.constants.CaptureQuality[props.captureQuality];
    }
    if (typeof props.cameraType === 'string') {
        newProps.cameraType = Camera.constants.CameraType[props.cameraType];
    }
    if (typeof props.model === 'string') {
        newProps.model = Camera.constants.Model[props.model];
    }
    if (typeof props.rotateMode === 'string') {
      newProps.rotateMode = Camera.constants.RotateMode[props.rotateMode];
    } 
    delete newProps.onTrained;
    delete newProps.onRecognized;
    delete newProps.onUntrained;
    delete newProps.onUnrecognized;
    return newProps;
  }
  export default class Camera extends Component {

    static constants = {
      Aspect: CameraManager.Aspect,
      CaptureQuality: CameraManager.CaptureQuality,
      TorchMode: CameraManager.TorchMode,
      CameraType: CameraManager.CameraType,
      Model: CameraManager.Model,
      RotateMode: CameraManager.RotateMode,
    };
  
    static propTypes = {
      ...View.propTypes,
      aspect: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      captureQuality: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      torchMode: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      cameraType: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      rotateMode: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      model: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      distance: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
      ]),
      dataset: PropTypes.bool,
      touchToFocus: PropTypes.bool,
      onTrained: PropTypes.func,
      onRecognized: PropTypes.func,
      onUntrained: PropTypes.func,
      onUnrecognized: PropTypes.func
    };
  
    static defaultProps: Object = {
      aspect: CameraManager.Aspect.fill,
      captureQuality: CameraManager.CaptureQuality.medium,
      torchMode: CameraManager.TorchMode.off,
      cameraType: CameraManager.CameraType.back,
      model: CameraManager.Model.cascade,
      touchToFocus: false,
      distance: 200,
      rotateMode: CameraManager.RotateMode.off,
      dataset: false
    };

    _cameraRef: ?Object;
    _cameraHandle: ?number;
  
    _setReference = (ref: ?Object) => {
      if (ref) {
        this._cameraRef = ref;
        this._cameraHandle = findNodeHandle(ref);
      } else {
        this._cameraRef = null;
        this._cameraHandle = null;
      }
    };
  
    constructor() {
      super();
      this.state = {
        mounted: false
      };
    }
  
    onTrained = () => {
      if(this.props.onTrained) {
        this.props.onTrained();
      }
    }
    onUntrained = (event) => {
      if(this.props.onUntrained) {
        this.props.onUntrained(event.nativeEvent);
      }
    }

    onRecognized = (event) => {
      if(this.props.onRecognized) {
        this.props.onRecognized(event.nativeEvent);
      }
    }
    onUnrecognized = (event) => {
      if(this.props.onUnrecognized) {
        this.props.onUnrecognized(event.nativeEvent);
      }
    }
  
    componentDidMount() {
        this.setState({
          mounted: true
        })
    
    }
    componentWillUnmount() {
        this.setState({
          mounted: false
        })
    }

    async takePicture() {
      return await CameraManager.detection(this._cameraHandle);
    }
    train(info?: DetectOption) {
       CameraManager.train(info, this._cameraHandle);
    }
    async identify() {
      return await CameraManager.detection(this._cameraHandle).then(() => {
        CameraManager.recognize(this._cameraHandle);
      });
    }
    clear() {
      CameraManager.clear(this._cameraHandle);
    }

    render() {
      const nativeProps = convertNativeProps(this.props);
  
      return <FaceCamera 
              mounted={this.state.mounted} 
              ref={this._setReference} 
              {...nativeProps} 
              onTrained = {this.onTrained} 
              onRecognized = {this.onRecognized}
              onUntrained = {this.onUntrained}
              onUnrecognized = {this.onUnrecognized} />;
    }
  }
  export const constants = Camera.constants;
  const FaceCamera = requireNativeComponent(
    'FaceCamera',
     Camera,
     {
        nativeOnly: {
          'mounted': true,
        }
      }
  );


