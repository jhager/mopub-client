//
//  UIDevice-ORMMA.m
//  ORMMA
//
//  Created by Robert Hedin on 11/15/10.
//  Copyright 2010 The Weather Channel. All rights reserved.
//

#import "UIDevice-ORMMA.h"



@implementation UIDevice (ORMMA)


- (CGSize)screenSizeForOrientation:(UIDeviceOrientation)orientation
{
	CGSize size;
	UIScreen *screen = [UIScreen mainScreen];
	CGSize screenSize = screen.bounds.size;	
	if ( UIDeviceOrientationIsLandscape( orientation ) )
	{
		// Landscape Orientation, reverse size values
		size.width = screenSize.height;
		size.height = screenSize.width;
	}
	else
	{
		// portrait orientation, use normal size values
		size.width = screenSize.width;
		size.height = screenSize.height;
	}
	return size;
}

@end
