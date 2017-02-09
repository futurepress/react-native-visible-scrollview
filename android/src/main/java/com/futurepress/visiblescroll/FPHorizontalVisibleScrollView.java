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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.uimanager.MeasureSpecAssertions;
import com.facebook.react.uimanager.events.NativeGestureUtil;
import com.facebook.react.uimanager.ReactClippingViewGroup;
import com.facebook.react.uimanager.ReactClippingViewGroupHelper;

import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.views.scroll.FpsListener;
import com.facebook.react.views.scroll.ReactScrollViewHelper;
import com.facebook.react.views.scroll.ReactHorizontalScrollView;

import android.util.Log;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import com.facebook.react.views.view.ReactViewGroup;
/**
 * Similar to {@link ReactScrollView} but only supports horizontal scrolling.
 */
public class FPHorizontalVisibleScrollView extends ReactHorizontalScrollView implements
    ReactClippingViewGroup {

  private View contentView;

  private int counterX = 0;
  private int counterY = 0;

  public FPHorizontalVisibleScrollView(Context context) {
    this(context, null);
  }

  public FPHorizontalVisibleScrollView(Context context, @Nullable FpsListener fpsListener) {
    super(context, fpsListener);
  }


  private final View.OnLayoutChangeListener
          mContentLayoutChangeListener = new View.OnLayoutChangeListener() {

    @Override
    public void onLayoutChange(
            View v,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {

      if (counterX > 0 || counterY > 0){
        scrollTo(counterX, counterY);
        updateClippingRect();
        counterX = 0;
        counterY = 0;
      }
    }
  };

  private final View.OnLayoutChangeListener
          mChildLayoutChangeListener = new View.OnLayoutChangeListener() {

    @Override
    public void onLayoutChange(
            View v,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {

      int oldHeight = (oldBottom - oldTop);
      int newHeight = (bottom - top);
      int changedHeight = (newHeight - oldHeight);

      int oldWidth = (oldRight - oldLeft);
      int newWidth = (right - left);
      int changedWidth = (newWidth - oldWidth);

      int offsetX = getScrollX();
      int offsetY = getScrollX();

      int changedLeft = (left - oldLeft);
      int changedTop = (left - oldLeft);

      if (left <= offsetX && right < changedLeft + contentView.getWidth() && newWidth > 0) {
        counterX = counterX + offsetX + newWidth;
      } else if (oldLeft <= offsetX && changedLeft != 0) {
        counterX = counterX + offsetX + changedLeft;
      }

      if (top <= offsetY && bottom < changedTop + contentView.getHeight() && newHeight > 0) {
        counterY = counterY + offsetY + newHeight;
      } else if (oldLeft <= offsetY && changedTop != 0) {
        counterY = counterY + offsetY + changedTop;
      }

    }
  };

  private final ViewGroup.OnHierarchyChangeListener
          mChildHierarchyChangeListener = new ViewGroup.OnHierarchyChangeListener() {

    @Override
    public void onChildViewAdded(View parent,
                                 View child) {

      child.addOnLayoutChangeListener(mChildLayoutChangeListener);

    }

    @Override
    public void onChildViewRemoved(View parent, View child) {

      child.removeOnLayoutChangeListener(mChildLayoutChangeListener);
    }
  };

  @Override
  public void addView(View child, int index) {
    child.addOnLayoutChangeListener(mContentLayoutChangeListener);

    ViewGroup viewGroup = (ViewGroup) child;

    viewGroup.setOnHierarchyChangeListener(mChildHierarchyChangeListener);


    super.addView(child, index);

    contentView = child;
  }

  @Override
  public void removeView(View child) {
    child.removeOnLayoutChangeListener(mChildLayoutChangeListener);

    super.removeView(child);
  }
}
