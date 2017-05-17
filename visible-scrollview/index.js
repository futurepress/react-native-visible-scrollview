/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule VisibleScrollView
 * @flow
 */
'use strict';
const ScrollView = require('react-native/Libraries/Components/ScrollView/ScrollView');

const Animated = require('react-native/Libraries/Animated/src/Animated');
const ColorPropType = require('react-native/Libraries/StyleSheet/ColorPropType');
const EdgeInsetsPropType = require('react-native/Libraries/StyleSheet/EdgeInsetsPropType');
const Platform = require('react-native/Libraries/Utilities/Platform');
const PointPropType = require('react-native/Libraries/StyleSheet/PointPropType');
const React = require('react');
const ReactNative = require('react-native');
const ScrollResponder = require('./VisibleScrollResponder');
const ScrollViewStickyHeader = require('react-native/Libraries/Components/ScrollView/ScrollViewStickyHeader');
const StyleSheet = require('react-native/Libraries/StyleSheet/StyleSheet');
const StyleSheetPropType = require('react-native/Libraries/StyleSheet/StyleSheetPropType');
const View = require('react-native/Libraries/Components/View/View');
const ViewPropTypes = require('react-native/Libraries/Components/View/ViewPropTypes');
const ViewStylePropTypes = require('react-native/Libraries/Components/View/ViewStylePropTypes');

const dismissKeyboard = require('react-native/Libraries/Utilities/dismissKeyboard');
const flattenStyle = require('react-native/Libraries/StyleSheet/flattenStyle');
const invariant = require('fbjs/lib/invariant');
const processDecelerationRate = require('react-native/Libraries/Components/ScrollView/processDecelerationRate');
const PropTypes = React.PropTypes;
const requireNativeComponent = require('react-native/Libraries/ReactNative/requireNativeComponent');

class VisibleScrollView extends ScrollView {
  constructor(props) {
    super(props);
  }

  render() {
    let ScrollViewClass;
    let ScrollContentContainerViewClass;
    if (Platform.OS === 'ios') {
      ScrollViewClass = RCTScrollView;
      ScrollContentContainerViewClass = RCTScrollContentView;
    } else if (Platform.OS === 'android') {
      if (this.props.horizontal) {
        ScrollViewClass = AndroidHorizontalScrollView;
      } else {
        ScrollViewClass = AndroidScrollView;
      }
      ScrollContentContainerViewClass = View;
    }

    invariant(
      ScrollViewClass !== undefined,
      'ScrollViewClass must not be undefined'
    );

    invariant(
      ScrollContentContainerViewClass !== undefined,
      'ScrollContentContainerViewClass must not be undefined'
    );

    const contentContainerStyle = [
      this.props.horizontal && styles.contentContainerHorizontal,
      this.props.contentContainerStyle,
    ];
    let style, childLayoutProps;
    if (__DEV__ && this.props.style) {
      style = flattenStyle(this.props.style);
      childLayoutProps = ['alignItems', 'justifyContent']
        .filter((prop) => style && style[prop] !== undefined);
      invariant(
        childLayoutProps.length === 0,
        'ScrollView child layout (' + JSON.stringify(childLayoutProps) +
          ') must be applied through the contentContainerStyle prop.'
      );
    }

    let contentSizeChangeProps = {};
    if (this.props.onContentSizeChange) {
      contentSizeChangeProps = {
        onLayout: this._handleContentOnLayout,
      };
    }

    const {stickyHeaderIndices} = this.props;
    const hasStickyHeaders = stickyHeaderIndices && stickyHeaderIndices.length > 0;
    const childArray = hasStickyHeaders && React.Children.toArray(this.props.children);
    const children = hasStickyHeaders ?
      childArray.map((child, index) => {
        const indexOfIndex = child ? stickyHeaderIndices.indexOf(index) : -1;
        if (indexOfIndex > -1) {
          const key = child.key;
          const nextIndex = stickyHeaderIndices[indexOfIndex + 1];
          return (
            <ScrollViewStickyHeader
              key={key}
              ref={(ref) => this._setStickyHeaderRef(key, ref)}
              nextHeaderLayoutY={
                this._headerLayoutYs.get(this._getKeyForIndex(nextIndex, childArray))
              }
              onLayout={(event) => this._onStickyHeaderLayout(index, event, key)}
              scrollAnimatedValue={this._scrollAnimatedValue}>
              {child}
            </ScrollViewStickyHeader>
          );
        } else {
          return child;
        }
      }) :
      this.props.children;
    const contentContainer =
      <ScrollContentContainerViewClass
        {...contentSizeChangeProps}
        ref={this._setInnerViewRef}
        style={contentContainerStyle}
        removeClippedSubviews={
          hasStickyHeaders && Platform.OS === 'android' ? false : this.props.removeClippedSubviews
        }
        collapsable={false}>
        {children}
      </ScrollContentContainerViewClass>;

    const alwaysBounceHorizontal =
      this.props.alwaysBounceHorizontal !== undefined ?
        this.props.alwaysBounceHorizontal :
        this.props.horizontal;

    const alwaysBounceVertical =
      this.props.alwaysBounceVertical !== undefined ?
        this.props.alwaysBounceVertical :
        !this.props.horizontal;

    const baseStyle = this.props.horizontal ? styles.baseHorizontal : styles.baseVertical;
    const props = {
      ...this.props,
      alwaysBounceHorizontal,
      alwaysBounceVertical,
      style: ([baseStyle, this.props.style]: ?Array<any>),
      // Override the onContentSizeChange from props, since this event can
      // bubble up from TextInputs
      onContentSizeChange: null,
      onMomentumScrollBegin: this.scrollResponderHandleMomentumScrollBegin,
      onMomentumScrollEnd: this.scrollResponderHandleMomentumScrollEnd,
      onResponderGrant: this.scrollResponderHandleResponderGrant,
      onResponderReject: this.scrollResponderHandleResponderReject,
      onResponderRelease: this.scrollResponderHandleResponderRelease,
      onResponderTerminate: this.scrollResponderHandleTerminate,
      onResponderTerminationRequest: this.scrollResponderHandleTerminationRequest,
      onScroll: this._handleScroll,
      onScrollBeginDrag: this.scrollResponderHandleScrollBeginDrag,
      onScrollEndDrag: this.scrollResponderHandleScrollEndDrag,
      onScrollShouldSetResponder: this.scrollResponderHandleScrollShouldSetResponder,
      onStartShouldSetResponder: this.scrollResponderHandleStartShouldSetResponder,
      onStartShouldSetResponderCapture: this.scrollResponderHandleStartShouldSetResponderCapture,
      onTouchEnd: this.scrollResponderHandleTouchEnd,
      onTouchMove: this.scrollResponderHandleTouchMove,
      onTouchStart: this.scrollResponderHandleTouchStart,
      scrollEventThrottle: hasStickyHeaders ? 1 : this.props.scrollEventThrottle,
      sendMomentumEvents: (this.props.onMomentumScrollBegin || this.props.onMomentumScrollEnd) ?
        true : false,
    };

    const { decelerationRate } = this.props;
    if (decelerationRate) {
      props.decelerationRate = processDecelerationRate(decelerationRate);
    }

    const refreshControl = this.props.refreshControl;

    if (refreshControl) {
      if (Platform.OS === 'ios') {
        // On iOS the RefreshControl is a child of the ScrollView.
        // tvOS lacks native support for RefreshControl, so don't include it in that case
        return (
          <ScrollViewClass {...props} ref={this._setScrollViewRef}>
            {Platform.isTVOS ? null : refreshControl}
            {contentContainer}
          </ScrollViewClass>
        );
      } else if (Platform.OS === 'android') {
        // On Android wrap the ScrollView with a AndroidSwipeRefreshLayout.
        // Since the ScrollView is wrapped add the style props to the
        // AndroidSwipeRefreshLayout and use flex: 1 for the ScrollView.
        // Note: we should only apply props.style on the wrapper
        // however, the ScrollView still needs the baseStyle to be scrollable

        return React.cloneElement(
          refreshControl,
          {style: props.style},
          <ScrollViewClass {...props} style={baseStyle} ref={this._setScrollViewRef}>
            {contentContainer}
          </ScrollViewClass>
        );
      }
    }
    return (
      <ScrollViewClass {...props} ref={this._setScrollViewRef}>
        {contentContainer}
      </ScrollViewClass>
    );
  }
}

const styles = StyleSheet.create({
  baseVertical: {
    flexGrow: 1,
    flexShrink: 1,
    flexDirection: 'column',
    overflow: 'scroll',
  },
  baseHorizontal: {
    flexGrow: 1,
    flexShrink: 1,
    flexDirection: 'row',
    overflow: 'scroll',
  },
  contentContainerHorizontal: {
    flexDirection: 'row',
  },
});

let nativeOnlyProps,
  AndroidScrollView,
  AndroidHorizontalScrollView,
  RCTScrollView,
  RCTScrollContentView;
if (Platform.OS === 'android') {
  nativeOnlyProps = {
    nativeOnly: {
      sendMomentumEvents: true,
    }
  };
  AndroidScrollView = requireNativeComponent(
      'FPVisibleScrollView',
      (VisibleScrollView: ReactClass<any>),
      nativeOnlyProps
    );
  AndroidHorizontalScrollView = requireNativeComponent(
    'FPAndroidHorizontalVisibleScrollView',
    (VisibleScrollView: ReactClass<any>),
    nativeOnlyProps
  );
} else if (Platform.OS === 'ios') {
  nativeOnlyProps = {
    nativeOnly: {
      onMomentumScrollBegin: true,
      onMomentumScrollEnd : true,
      onScrollBeginDrag: true,
      onScrollEndDrag: true,
    }
  };
  RCTScrollView = requireNativeComponent(
      'FPVisibleScrollView',
      (VisibleScrollView: ReactClass<any>),
      nativeOnlyProps,
    );
  RCTScrollContentView = requireNativeComponent('RCTScrollContentView', View);
}

export default VisibleScrollView;
