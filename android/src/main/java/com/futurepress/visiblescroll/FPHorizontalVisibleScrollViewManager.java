/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.futurepress.visiblescroll;

import javax.annotation.Nullable;

import android.graphics.Color;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.Spacing;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.ReactClippingViewGroupHelper;
import com.facebook.react.uimanager.annotations.ReactPropGroup;
import com.facebook.yoga.YogaConstants;

import com.facebook.react.views.scroll.ReactScrollViewCommandHelper;
import com.facebook.react.views.scroll.FpsListener;
import com.facebook.react.views.scroll.ReactScrollViewHelper;

/**
 * View manager for {@link FPHorizontalVisibleScrollView} components.
 *
 * <p>Note that {@link ReactScrollView} and {@link FPHorizontalVisibleScrollView} are exposed to JS
 * as a single ScrollView component, configured via the {@code horizontal} boolean property.
 */
@ReactModule(name = FPHorizontalVisibleScrollViewManager.REACT_CLASS)
public class FPHorizontalVisibleScrollViewManager
    extends ViewGroupManager<FPHorizontalVisibleScrollView>
    implements ReactScrollViewCommandHelper.ScrollCommandHandler<FPHorizontalVisibleScrollView> {

  protected static final String REACT_CLASS = "FPAndroidHorizontalVisibleScrollView";

  private static final int[] SPACING_TYPES = {
      Spacing.ALL, Spacing.LEFT, Spacing.RIGHT, Spacing.TOP, Spacing.BOTTOM,
  };

  private @Nullable FpsListener mFpsListener = null;

  public FPHorizontalVisibleScrollViewManager() {
    this(null);
  }

  public FPHorizontalVisibleScrollViewManager(@Nullable FpsListener fpsListener) {
    mFpsListener = fpsListener;
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public FPHorizontalVisibleScrollView createViewInstance(ThemedReactContext context) {
    return new FPHorizontalVisibleScrollView(context, mFpsListener);
  }

  @ReactProp(name = "scrollEnabled", defaultBoolean = true)
  public void setScrollEnabled(FPHorizontalVisibleScrollView view, boolean value) {
    view.setScrollEnabled(value);
  }

  @ReactProp(name = "showsHorizontalScrollIndicator")
  public void setShowsHorizontalScrollIndicator(FPHorizontalVisibleScrollView view, boolean value) {
    view.setHorizontalScrollBarEnabled(value);
  }

  @ReactProp(name = ReactClippingViewGroupHelper.PROP_REMOVE_CLIPPED_SUBVIEWS)
  public void setRemoveClippedSubviews(FPHorizontalVisibleScrollView view, boolean removeClippedSubviews) {
    view.setRemoveClippedSubviews(removeClippedSubviews);
  }

  /**
   * Computing momentum events is potentially expensive since we post a runnable on the UI thread
   * to see when it is done.  We only do that if {@param sendMomentumEvents} is set to true.  This
   * is handled automatically in js by checking if there is a listener on the momentum events.
   *
   * @param view
   * @param sendMomentumEvents
   */
  @ReactProp(name = "sendMomentumEvents")
  public void setSendMomentumEvents(FPHorizontalVisibleScrollView view, boolean sendMomentumEvents) {
    view.setSendMomentumEvents(sendMomentumEvents);
  }

  /**
   * Tag used for logging scroll performance on this scroll view. Will force momentum events to be
   * turned on (see setSendMomentumEvents).
   *
   * @param view
   * @param scrollPerfTag
   */
  @ReactProp(name = "scrollPerfTag")
  public void setScrollPerfTag(FPHorizontalVisibleScrollView view, String scrollPerfTag) {
    view.setScrollPerfTag(scrollPerfTag);
  }

  @ReactProp(name = "pagingEnabled")
  public void setPagingEnabled(FPHorizontalVisibleScrollView view, boolean pagingEnabled) {
    view.setPagingEnabled(pagingEnabled);
  }

  /**
   * Controls overScroll behaviour
   */
  @ReactProp(name = "overScrollMode")
  public void setOverScrollMode(FPHorizontalVisibleScrollView view, String value) {
    view.setOverScrollMode(ReactScrollViewHelper.parseOverScrollMode(value));
  }

  @Override
  public void receiveCommand(
      FPHorizontalVisibleScrollView scrollView,
      int commandId,
      @Nullable ReadableArray args) {
    ReactScrollViewCommandHelper.receiveCommand(this, scrollView, commandId, args);
  }

  @Override
  public void scrollTo(
      FPHorizontalVisibleScrollView scrollView,
      ReactScrollViewCommandHelper.ScrollToCommandData data) {
    if (data.mAnimated) {
      scrollView.smoothScrollTo(data.mDestX, data.mDestY);
    } else {
      scrollView.scrollTo(data.mDestX, data.mDestY);
    }
  }

  @Override
  public void scrollToEnd(
      FPHorizontalVisibleScrollView scrollView,
      ReactScrollViewCommandHelper.ScrollToEndCommandData data) {
    // ScrollView always has one child - the scrollable area
    int right =
      scrollView.getChildAt(0).getWidth() + scrollView.getPaddingRight();
    if (data.mAnimated) {
      scrollView.smoothScrollTo(right, scrollView.getScrollY());
    } else {
      scrollView.scrollTo(right, scrollView.getScrollY());
    }
  }

  /**
   * When set, fills the rest of the scrollview with a color to avoid setting a background and
   * creating unnecessary overdraw.
   * @param view
   * @param color
   */
  @ReactProp(name = "endFillColor", defaultInt = Color.TRANSPARENT, customType = "Color")
  public void setBottomFillColor(FPHorizontalVisibleScrollView view, int color) {
    view.setEndFillColor(color);
  }

  @ReactPropGroup(names = {
      ViewProps.BORDER_RADIUS,
      ViewProps.BORDER_TOP_LEFT_RADIUS,
      ViewProps.BORDER_TOP_RIGHT_RADIUS,
      ViewProps.BORDER_BOTTOM_RIGHT_RADIUS,
      ViewProps.BORDER_BOTTOM_LEFT_RADIUS
  }, defaultFloat = YogaConstants.UNDEFINED)
  public void setBorderRadius(FPHorizontalVisibleScrollView view, int index, float borderRadius) {
    if (!YogaConstants.isUndefined(borderRadius)) {
      borderRadius = PixelUtil.toPixelFromDIP(borderRadius);
    }

    if (index == 0) {
      view.setBorderRadius(borderRadius);
    } else {
      view.setBorderRadius(borderRadius, index - 1);
    }
  }

  @ReactProp(name = "borderStyle")
  public void setBorderStyle(FPHorizontalVisibleScrollView view, @Nullable String borderStyle) {
    view.setBorderStyle(borderStyle);
  }

  @ReactPropGroup(names = {
      ViewProps.BORDER_WIDTH,
      ViewProps.BORDER_LEFT_WIDTH,
      ViewProps.BORDER_RIGHT_WIDTH,
      ViewProps.BORDER_TOP_WIDTH,
      ViewProps.BORDER_BOTTOM_WIDTH,
  }, defaultFloat = YogaConstants.UNDEFINED)
  public void setBorderWidth(FPHorizontalVisibleScrollView view, int index, float width) {
    if (!YogaConstants.isUndefined(width)) {
      width = PixelUtil.toPixelFromDIP(width);
    }
    view.setBorderWidth(SPACING_TYPES[index], width);
  }

  @ReactPropGroup(names = {
      "borderColor", "borderLeftColor", "borderRightColor", "borderTopColor", "borderBottomColor"
  }, customType = "Color")
  public void setBorderColor(FPHorizontalVisibleScrollView view, int index, Integer color) {
    float rgbComponent =
        color == null ? YogaConstants.UNDEFINED : (float) ((int)color & 0x00FFFFFF);
    float alphaComponent = color == null ? YogaConstants.UNDEFINED : (float) ((int)color >>> 24);
    view.setBorderColor(SPACING_TYPES[index], rgbComponent, alphaComponent);
  }
}
