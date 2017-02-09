package com.futurepress.visiblescroll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.views.scroll.FpsListener;
import com.futurepress.visiblescroll.FPHorizontalVisibleScrollView;
import com.futurepress.visiblescroll.FPHorizontalVisibleScrollViewManager;
import com.futurepress.visiblescroll.FPVisibleScrollView;
import com.futurepress.visiblescroll.FPVisibleScrollViewManager;

public class FPVisibleScrollViewPackage implements ReactPackage {

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
      return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                new FPVisibleScrollViewManager(),
                new FPHorizontalVisibleScrollViewManager()
        );
    }
}
