#import "RCTScrollView+RNVisibleScrollView.h"

@implementation RCTScrollView (RNVisibleScrollView)

//NSUInteger _previousVisibleSubViewId;
UIView *_previousVisibleSubView;

- (NSArray<NSNumber *> *)visibleChildFrames
{
    //    RCTCustomScrollView *_scrollView = [self valueForKey:@"_scrollView"];
    UIView *_contentView = [self valueForKey:@"_contentView"];
    
    const CGSize contentSize = [[self valueForKey:@"_scrollView"] contentSize];
    const CGRect bounds = [[self valueForKey:@"_scrollView"] bounds];
    // const BOOL scrollsHorizontally = contentSize.width > bounds.size.width;
    const BOOL scrollsVertically = contentSize.height > bounds.size.height;
    
    const CGPoint offset = [[self valueForKey:@"_scrollView"] contentOffset];
    const CGFloat visibleMin = scrollsVertically ? offset.y : offset.x;
    const CGFloat visibleMax = visibleMin + bounds.size.width;
    
    NSMutableArray<NSNumber *> *visibleChildFrames = [NSMutableArray new];
    [[_contentView valueForKey:@"reactSubviews"] enumerateObjectsUsingBlock:
     ^(UIView *subview, NSUInteger idx, __unused BOOL *stop) {
         
         const CGFloat min = scrollsVertically ? subview.frame.origin.y : subview.frame.origin.x;
         // const CGFloat max = min + (scrollsVertically ? subview.frame.size.height : subview.frame.size.width);
         
         if (subview.frame.size.width > 0 &&
             subview.frame.size.height > 0 &&
             min >= visibleMin && min < visibleMax) {
             [visibleChildFrames addObject: [NSNumber numberWithUnsignedLong:idx]];
         }
     }];
    
    return visibleChildFrames;
}


- (CGPoint)calculateOffsetForContentSize:(CGSize)newContentSize
{
    UIScrollView* _scrollView = [self valueForKey:@"_scrollView"];
    UIView* _contentView = [self valueForKey:@"_contentView"];
    
    CGPoint oldOffset = _scrollView.contentOffset;
    CGPoint newOffset = oldOffset;
    
    CGSize oldContentSize = _scrollView.contentSize;
    CGSize *viewportSize = (__bridge CGSize *)[self valueForKey:@"_calculateViewportSize"]; //[self _calculateViewportSize];
    
    BOOL fitsinViewportY = oldContentSize.height <= viewportSize->height && newContentSize.height <= viewportSize->height;
    if (newContentSize.height < oldContentSize.height && !fitsinViewportY) {
        CGFloat offsetHeight = oldOffset.y + viewportSize->height;
        if (oldOffset.y < 0) {
            // overscrolled on top, leave offset alone
        } else if (offsetHeight > oldContentSize.height) {
            // overscrolled on the bottom, preserve overscroll amount
            newOffset.y = MAX(0, oldOffset.y - (oldContentSize.height - newContentSize.height));
        } else if (offsetHeight > newContentSize.height) {
            // offset falls outside of bounds, scroll back to end of list
            newOffset.y = MAX(0, newContentSize.height - viewportSize->height);
        }
    }
    
    BOOL fitsinViewportX = oldContentSize.width <= viewportSize->width && newContentSize.width <= viewportSize->width;
    
    if (newContentSize.width < oldContentSize.width && !fitsinViewportX) {
        CGFloat offsetHeight = oldOffset.x + viewportSize->width;
        
        if (oldOffset.x < 0) {
            // overscrolled at the beginning, leave offset alone
        } else if (offsetHeight > oldContentSize.width && newContentSize.width > viewportSize->width) {
            // overscrolled at the end, preserve overscroll amount as much as possible
            newOffset.x = MAX(0, oldOffset.x - (oldContentSize.width - newContentSize.width));
        } else if (offsetHeight > newContentSize.width) {
            // offset falls outside of bounds, scroll back to end
            newOffset.x = MAX(0, newContentSize.width - viewportSize->width);
        }
    }
    
    // Get all visible childFrames
    NSArray<NSNumber *> *visible = [self visibleChildFrames];
    NSUInteger subViewId;
    
    
    if (visible.count > 0) {
        subViewId = [visible[0] intValue];
        UIView *subView = [[self valueForKey:@"_contentView"] reactSubviews][subViewId];
        int indexValue = [[[self valueForKey:@"_contentView"] reactSubviews] indexOfObject:_previousVisibleSubView];
        
        // if the position of the first visible subview changed, offset to counter the change and return
        // to the previous position
        if (indexValue >= 0 && indexValue > subViewId) {
            
            // TODO: handle size of prev subview shrinking
            newOffset.x = newOffset.x + (newContentSize.width - oldContentSize.width);
            
            // Fire a new scroll event
            //            _allowNextScrollNoMatterWhat = YES;
            [self setValue:[NSNumber numberWithBool:YES] forKey:@"_allowNextScrollNoMatterWhat"];
            [self scrollViewDidScroll:_scrollView];
        } else {
            // otherwise reset the previous subview
            _previousVisibleSubView = subView;
        }
    }
    
    
    
    // all other cases, offset doesn't change
    return newOffset;
}

@end
