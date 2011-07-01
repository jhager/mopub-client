//
//  ORMMAJavascriptBridge.m
//  RichMediaAds
//
//  Created by Robert Hedin on 9/7/10.
//  Copyright 2010 The Weather Channel. All rights reserved.
//

#import <MessageUI/MessageUI.h>
#import "ORMMAJavascriptBridge.h"
#import "Reachability.h"
#import "UIColor-Expanded.h"
#import "UIDevice-ORMMA.h"



@interface ORMMAJavascriptBridge ()

- (NSDictionary *)parametersFromJSCall:(NSString *)parameterString;
- (BOOL)processCommand:(NSString *)command
			parameters:(NSDictionary *)parameters
			forWebView:(UIWebView *)webView;
- (BOOL)processORMMAEnabledCommand:(NSDictionary *)parameters
						forWebView:(UIWebView *)webView;
- (BOOL)processCloseCommand:(NSDictionary *)parameters
				 forWebView:(UIWebView *)webView;
- (BOOL)processExpandCommand:(NSDictionary *)parameters
				  forWebView:(UIWebView *)webView;
- (BOOL)processHideCommand:(NSDictionary *)parameters
				forWebView:(UIWebView *)webView;
- (BOOL)processResizeCommand:(NSDictionary *)parameters
				  forWebView:(UIWebView *)webView;
- (BOOL)processShowCommand:(NSDictionary *)parameters
				forWebView:(UIWebView *)webView;
- (BOOL)processOpenCommand:(NSDictionary *)parameters
				forWebView:(UIWebView *)webView;
- (BOOL)processRequestCommand:(NSDictionary *)parameters
				   forWebView:(UIWebView *)webView;

- (CGFloat)floatFromDictionary:(NSDictionary *)dictionary
						forKey:(NSString *)key;
- (CGFloat)floatFromDictionary:(NSDictionary *)dictionary
						forKey:(NSString *)key
				   withDefault:(CGFloat)defaultValue; 
- (NSString *)requiredStringFromDictionary:(NSDictionary *)dictionary
									forKey:(NSString *)key;
- (BOOL)booleanFromDictionary:(NSDictionary *)dictionary
					   forKey:(NSString *)key;

@end

@implementation ORMMAJavascriptBridge


#pragma mark -
#pragma mark Constants

// the protocol to use to identify the ORMMA request
NSString * const ORMMAProtocol = @"ormma://";

NSString * const ORMMACommandORMMAEnabled = @"ormmaenabled";

NSString * const ORMMACommandShow = @"show";
NSString * const ORMMACommandHide = @"hide";
NSString * const ORMMACommandClose = @"close";

NSString * const ORMMACommandExpand = @"expand";
NSString * const ORMMACommandResize = @"resize";

NSString * const ORMMACommandOpen = @"open";
NSString * const ORMMACommandRequest = @"request";

#pragma mark -
#pragma mark Properties

@synthesize bridgeDelegate = m_bridgeDelegate;

#pragma mark -
#pragma mark Initializers / Memory Management

- (ORMMAJavascriptBridge *)init
{
	if ( ( self = [super init] ) )
	{		
		// make sure to register for the events that we care about
		NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
		[notificationCenter addObserver:self
							   selector:@selector(orientationChanged:)
								   name:UIDeviceOrientationDidChangeNotification
								 object:nil];
		[notificationCenter addObserver:self 
							   selector:@selector(keyboardWillShow:) 
								   name:UIKeyboardWillShowNotification
								 object:nil];
		[notificationCenter addObserver:self 
							   selector:@selector(keyboardWillHide:) 
								   name:UIKeyboardWillHideNotification
								 object:nil];
	}
	return self;
}


- (void)dealloc
{
	// stop listening for notifications
	NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
	[notificationCenter removeObserver:self];
		
	m_bridgeDelegate = nil;
	[super dealloc];
}

#pragma mark -
#pragma mark Process

- (BOOL)processURL:(NSURL *)url
		forWebView:(UIWebView *)webView
{
	NSString *workingUrl = [url absoluteString];
	if ( [workingUrl hasPrefix:ORMMAProtocol] )
	{
		// the URL is intended for the bridge, so process it
		NSString *workingCall = [workingUrl substringFromIndex:ORMMAProtocol.length];
		
		// get the command
		NSRange r = [workingCall rangeOfString:@"?"];
		if ( r.location == NSNotFound )
		{
			// just a command
			return [self processCommand:workingCall 
							 parameters:nil
							 forWebView:webView];
		}
		NSString *command = [[workingCall substringToIndex:r.location] lowercaseString];
		NSString *parameterValues = [workingCall substringFromIndex:( r.location + 1 )];
		NSDictionary *parameters = [self parametersFromJSCall:parameterValues];
		NSLog( @"ORMMA Command: %@, %@", command, parameters );
		
		// let the callee know
		return [self processCommand:command 
						 parameters:parameters
						 forWebView:webView];
	}
	
	// not intended for the bridge
	return NO;
}


- (NSDictionary *)parametersFromJSCall:(NSString *)parameterString
{
	NSMutableDictionary *parameters = [NSMutableDictionary dictionary];
	
	// find the start of our parameters
	NSArray *parameterList = [parameterString componentsSeparatedByString:@"&"];
	for ( NSString *parameterEntry in parameterList )
	{
		NSArray *kvp = [parameterEntry componentsSeparatedByString:@"="];
		NSString *key = [kvp objectAtIndex:0];
		NSString *encodedValue = [kvp objectAtIndex:1];
		NSString *value = [encodedValue stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];;
		[parameters setObject:value forKey:key];
	}
	
	return parameters;
}


- (BOOL)processCommand:(NSString *)command
			parameters:(NSDictionary *)parameters
			forWebView:(UIWebView *)webView
{
	BOOL processed = NO;
	if ( [command isEqualToString:ORMMACommandORMMAEnabled] )
	{
		// process close
		processed = [self processORMMAEnabledCommand:parameters
										  forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandClose] )
	{
		// process close
		processed = [self processCloseCommand:parameters
								   forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandExpand] )
	{
		// process hide
		processed = [self processExpandCommand:parameters
									forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandHide] )
	{
		// process hide
		processed = [self processHideCommand:parameters
							 forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandResize] )
	{
		// process resize
		processed = [self processResizeCommand:parameters
							   forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandShow] )
	{
		// process show
		processed = [self processShowCommand:parameters
								  forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandOpen] )
	{
		// process show
		processed = [self processOpenCommand:parameters
								  forWebView:webView];
	}
	else if ( [command isEqualToString:ORMMACommandRequest] )
	{
		// process show
		processed = [self processRequestCommand:parameters
									 forWebView:webView];
	}
	if ( !processed ) 
	{
		NSLog( @"Unknown Command: %@", command );
	}

	// notify JS that we've completed the last request
	[self.bridgeDelegate usingWebView:webView
					executeJavascript:@"window.ormmaview.nativeCallComplete( '%@' );", command];

	return processed;
}


- (BOOL)processORMMAEnabledCommand:(NSDictionary *)parameters
						forWebView:(UIWebView *)webView
{
	NSLog( @"Processing ORMMAENABLED Command..." );
	[self.bridgeDelegate adIsORMMAEnabledForWebView:webView];
	return YES;
}


- (BOOL)processShowCommand:(NSDictionary *)parameters
				forWebView:(UIWebView *)webView
{
	NSLog( @"Processing SHOW Command..." );
	[self.bridgeDelegate showAd:webView];
	return YES;
}


- (BOOL)processHideCommand:(NSDictionary *)parameters
				forWebView:(UIWebView *)webView
{
	NSLog( @"Processing HIDE Command..." );
	[self.bridgeDelegate hideAd:webView];
	return YES;
}


- (BOOL)processCloseCommand:(NSDictionary *)parameters
				 forWebView:(UIWebView *)webView
{
	NSLog( @"Processing CLOSE Command..." );
	[self.bridgeDelegate closeAd:webView];
	return YES;
}


- (BOOL)processExpandCommand:(NSDictionary *)parameters
				  forWebView:(UIWebView *)webView
{
	NSLog( @"Processing EXPAND Command..." );
	
	// account for status bar, if needed
	CGFloat yDelta = 0;
	UIApplication *app = [UIApplication sharedApplication];
	if ( !app.statusBarHidden )
	{
		yDelta = app.statusBarFrame.size.height;
	}
	
	// ok, to make it easy on the client, we don't require them to give us all
	// the values all the time.
	// basicallly we're going to take the current real frame information from
	// the ad (translated to window space coordinates) and set the final frame
	// to this information. Then for each data point we receive from the client,
	// we override the appropriate value. this allows the client to say things
	// like "using the current ad position, expand the ad's height to 300px"
	CGRect f = [self.bridgeDelegate getAdFrameInWindowCoordinates];
	CGFloat x = f.origin.x;
	CGFloat y = f.origin.y;
	CGFloat w = f.size.width;
	CGFloat h = f.size.height;
	
	// now get the sizes as specified by the creative
	x = [self floatFromDictionary:parameters
						   forKey:@"x"
					  withDefault:x];
	y = [self floatFromDictionary:parameters
						   forKey:@"y"
					  withDefault:y];
	w = [self floatFromDictionary:parameters
						   forKey:@"w"
					  withDefault:w];
	h = [self floatFromDictionary:parameters
						   forKey:@"h"
					  withDefault:h];
	
	BOOL useBG = [self booleanFromDictionary:parameters
									  forKey:@"useBG"];
	UIColor *blockerColor = [UIColor blackColor];
	CGFloat bgOpacity = 0.20;
	if ( useBG )
	{
		NSString *value = [parameters objectForKey:@"bgColor"];
		if ( value != nil ) 
		{
			value = [value stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
			if ( value.length > 0 )
			{
				// we have what "should" be a color
				if ( [value hasPrefix:@"#"] ) 
				{
					// hex color
					blockerColor = [UIColor colorWithName:[value substringFromIndex:1]];
				}
				else
				{
					// assume it's a named color
					blockerColor = [UIColor colorWithName:value];
				}
			}
		}
		bgOpacity = [self floatFromDictionary:parameters
									   forKey:@"bgOpacity"
								  withDefault:1.0];
	}
	
	NSString *urlString = [parameters valueForKey:@"url"];
	NSURL *url = [NSURL URLWithString:urlString];
	NSLog( @"Expanding to ( %f, %f ) ( %f x %f ) showing %@", x, y, w, h, url );
	CGRect newFrame = CGRectMake( x, ( y + yDelta ), w, h );
	[self.bridgeDelegate expandTo:newFrame
						  withURL:url
						inWebView:webView
					blockingColor:blockerColor
				  blockingOpacity:bgOpacity];
	return YES;
}


- (BOOL)processResizeCommand:(NSDictionary *)parameters
				  forWebView:(UIWebView *)webView
{
	NSLog( @"Processing RESIZE Command..." );
	
	// get the new bounds
	CGFloat w = [self floatFromDictionary:parameters
								   forKey:@"w"];
	CGFloat h = [self floatFromDictionary:parameters
								   forKey:@"h"];
	[self.bridgeDelegate resizeToWidth:w
								height:h
							 inWebView:webView];
	return YES;
}
	
- (BOOL)processOpenCommand:(NSDictionary *)parameters
				forWebView:(UIWebView *)webView
{
	NSLog( @"Processing OPEN Command..." );
	NSString *url = [self requiredStringFromDictionary:parameters 
												forKey:@"url"];
	BOOL back = [self booleanFromDictionary:parameters
									 forKey:@"back"];
	BOOL forward = [self booleanFromDictionary:parameters
										forKey:@"forward"];
	BOOL refresh = [self booleanFromDictionary:parameters
										forKey:@"refresh"];
	[self.bridgeDelegate openBrowser:webView 
					   withUrlString:url 
						  enableBack:back 
					   enableForward:forward 
					   enableRefresh:refresh];
	return YES;
}


- (BOOL)processRequestCommand:(NSDictionary *)parameters
				   forWebView:(UIWebView *)webView
{
	NSLog( @"Processing REQUEST Command..." );
	return YES;
}


#pragma mark -
#pragma mark Notification Center Dispatch Methods

- (void)orientationChanged:(NSNotification *)notification
{
	UIDevice *device = [UIDevice currentDevice];
	UIDeviceOrientation orientation = device.orientation;
	NSInteger orientationAngle = -1;
	switch ( orientation )
	{
		case UIDeviceOrientationPortrait:
			orientationAngle = 0;
			break;
		case UIDeviceOrientationPortraitUpsideDown:
			orientationAngle = 180;
			break;
		case UIDeviceOrientationLandscapeLeft:
			orientationAngle = 270;
			break;
		case UIDeviceOrientationLandscapeRight:
			orientationAngle = 90;
			break;
		default:
			// the device is likely flat
			// since we have no idea what the orientation is
			// don't change it
			return;
	}
	CGSize screenSize = [device screenSizeForOrientation:orientation];
	[self.bridgeDelegate usingWebView:self.bridgeDelegate.webView
					executeJavascript:@"window.ormmaview.fireChangeEvent( { orientation: %i, screenSize: { width: %f, height: %f } } );", orientationAngle,
																																		  screenSize.width, screenSize.height];
}


- (void)keyboardWillShow:(NSNotification *)notification
{
	[self.bridgeDelegate usingWebView:self.bridgeDelegate.webView
	 executeJavascript:@"window.ormmaview.fireChangeEvent( { keyboardState: true } );"];
}


- (void)keyboardWillHide:(NSNotification *)notification
{
	[self.bridgeDelegate usingWebView:self.bridgeDelegate.webView
					executeJavascript:@"window.ormmaview.fireChangeEvent( { keyboardState: false } );"];
}

#pragma mark -
#pragma mark Utility

- (CGFloat)floatFromDictionary:(NSDictionary *)dictionary
						forKey:(NSString *)key
{
	return [self floatFromDictionary:dictionary
							  forKey:key
						 withDefault:0.0];
}


- (CGFloat)floatFromDictionary:(NSDictionary *)dictionary
						forKey:(NSString *)key
				   withDefault:(CGFloat)defaultValue
{
	NSString *stringValue = [dictionary valueForKey:key];
	if ( stringValue == nil )
	{
		return defaultValue;
	}
	CGFloat value = [stringValue floatValue];
	return value;
}


- (BOOL)booleanFromDictionary:(NSDictionary *)dictionary
					   forKey:(NSString *)key
{
	NSString *stringValue = [dictionary valueForKey:key];
	BOOL value = [@"Y" isEqualToString:stringValue] || [@"y" isEqualToString:stringValue];
	return value;
}


- (NSString *)requiredStringFromDictionary:(NSDictionary *)dictionary
									forKey:(NSString *)key
{
	NSString *value = [dictionary objectForKey:key];
	if ( value == nil )
	{
		// error
		NSLog( @"Missing required parameter: %@", key );
		return nil;
	}
	value = [value stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	if ( value.length == 0 )
	{
		NSLog( @"Missing required parameter: %@", key );
		return nil;
	}
	return value;
}

@end
