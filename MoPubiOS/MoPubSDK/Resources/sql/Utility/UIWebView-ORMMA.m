//
//  UIWebView-TWC.m
//
//  Created by Patrick Childers on 8/26/10.
//  Copyright 2010 The Weather Channel. All rights reserved.
//

#import "UIWebView-ORMMA.h"


@implementation UIWebView (ORMMA)

- (void)disableBouncesAndScrolling
{
	for ( id subview in self.subviews )
	{
		if ( [[subview class] isSubclassOfClass:[UIScrollView class]] )
		{
			UIScrollView *sv = (UIScrollView *)subview;
			sv.scrollEnabled = NO;
			sv.bounces = NO;
		}
	}
}

@end
