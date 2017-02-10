/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "FPVisibleScrollView.h"

#import <UIKit/UIKit.h>

#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTLog.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>
#import <React/UIView+Private.h>
#import <React/UIView+React.h>

#if !TARGET_OS_TV
#import <React/RCTRefreshControl.h>
#endif


@implementation FPVisibleScrollView
{
    NSMutableArray<NSValue *> *_cachedChildFrames;

    // FP Added:
    UIView *_previousVisibleSubView;
    CGFloat _previousVisibleSubViewX;
    CGFloat _previousVisibleSubViewY;
}

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{

    self = [super initWithEventDispatcher:eventDispatcher];

    return self;
}


// FP Added:
- (NSArray<NSNumber *> *)visibleChildFrames
{

    const CGSize contentSize = [self.scrollView contentSize];
    const CGRect bounds = [self.scrollView bounds];
    // const BOOL scrollsHorizontally = contentSize.width > bounds.size.width;
    const BOOL scrollsVertically = contentSize.height > bounds.size.height;

    const CGPoint offset = [self.scrollView contentOffset];
    const CGFloat visibleMin = scrollsVertically ? offset.y : offset.x;
    const CGFloat visibleMax = visibleMin + bounds.size.width;

    NSMutableArray<NSNumber *> *visibleChildFrames = [NSMutableArray new];
    [[self.contentView reactSubviews] enumerateObjectsUsingBlock:
     ^(UIView *subview, NSUInteger idx, __unused BOOL *stop) {

         const CGFloat min = scrollsVertically ? subview.frame.origin.y : subview.frame.origin.x;
         const CGFloat max = min + (scrollsVertically ? subview.frame.size.height : subview.frame.size.width);

         if (subview.frame.size.width > 0 &&
             subview.frame.size.height > 0 &&
             max >= visibleMin && min < visibleMax) {
             [visibleChildFrames addObject: [NSNumber numberWithUnsignedLong:idx]];
         }
     }];

    return visibleChildFrames;
}


- (CGSize)_calculateViewportSize
{
    CGSize viewportSize = self.bounds.size;
    if (self.automaticallyAdjustContentInsets) {
        UIEdgeInsets contentInsets = [RCTView contentInsetsForView:self];
        viewportSize = CGSizeMake(self.bounds.size.width - contentInsets.left - contentInsets.right,
                                  self.bounds.size.height - contentInsets.top - contentInsets.bottom);
    }
    return viewportSize;
}

- (CGPoint)calculateOffsetForContentSize:(CGSize)newContentSize
{
  CGPoint oldOffset = self.scrollView.contentOffset;
  CGPoint newOffset = oldOffset;

  CGSize oldContentSize = self.scrollView.contentSize;
  CGSize viewportSize = [self _calculateViewportSize];

  BOOL fitsinViewportY = oldContentSize.height <= viewportSize.height && newContentSize.height <= viewportSize.height;
  if (newContentSize.height < oldContentSize.height && !fitsinViewportY) {
    CGFloat offsetHeight = oldOffset.y + viewportSize.height;
    if (oldOffset.y < 0) {
      // overscrolled on top, leave offset alone
    } else if (offsetHeight > oldContentSize.height) {
      // overscrolled on the bottom, preserve overscroll amount
      newOffset.y = MAX(0, oldOffset.y - (oldContentSize.height - newContentSize.height));
    } else if (offsetHeight > newContentSize.height) {
      // offset falls outside of bounds, scroll back to end of list
      newOffset.y = MAX(0, newContentSize.height - viewportSize.height);
    }
  }

  BOOL fitsinViewportX = oldContentSize.width <= viewportSize.width && newContentSize.width <= viewportSize.width;
  if (newContentSize.width < oldContentSize.width && !fitsinViewportX) {
    CGFloat offsetHeight = oldOffset.x + viewportSize.width;
    if (oldOffset.x < 0) {
      // overscrolled at the beginning, leave offset alone
    } else if (offsetHeight > oldContentSize.width && newContentSize.width > viewportSize.width) {
      // overscrolled at the end, preserve overscroll amount as much as possible
      newOffset.x = MAX(0, oldOffset.x - (oldContentSize.width - newContentSize.width));
    } else if (offsetHeight > newContentSize.width) {
      // offset falls outside of bounds, scroll back to end
      newOffset.x = MAX(0, newContentSize.width - viewportSize.width);
    }
  }

  // FP Added:
  // Get all visible childFrames
  NSArray<NSNumber *> *visible = [self visibleChildFrames];
  NSUInteger subViewId;

    // NSLog(@"visible now is %i", visible.count);
    if (visible.count > 0) {
      subViewId = [visible[visible.count-1] intValue];
      UIView *subView;

      subView = [self.contentView reactSubviews][subViewId];


      // if the position of the first visible subview changed, offset to counter the change and return
      // to the previous position
      if (_previousVisibleSubView.frame.origin.x > _previousVisibleSubViewX) {

          newOffset.x = newOffset.x + (_previousVisibleSubView.frame.origin.x - _previousVisibleSubViewX);

          // NSLog(@"countered to %f", newOffset.x );

          _previousVisibleSubViewX = _previousVisibleSubView.frame.origin.x;

      } else if (_previousVisibleSubView.frame.origin.y > _previousVisibleSubViewY) {

          newOffset.y = newOffset.y + (_previousVisibleSubView.frame.origin.y - _previousVisibleSubViewY);

          _previousVisibleSubViewY = _previousVisibleSubView.frame.origin.y;

      } else {
          // otherwise reset the previous subview
          // NSLog(@"reset to %i", subView);
          _previousVisibleSubView = subView;
          _previousVisibleSubViewX = subView.frame.origin.x;
          _previousVisibleSubViewY = subView.frame.origin.y;

      }
  }

  // all other cases, offset doesn't change
  return newOffset;
}

@end
