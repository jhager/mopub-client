//
//  ORMMAJavascriptBridge.h
//  RichMediaAds
//
//  Created by Robert Hedin on 9/7/10.
//  Copyright 2010 The Weather Channel. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <CoreLocation/CoreLocation.h>
#import <CoreMotion/CoreMotion.h>



@protocol ORMMAJavascriptBridgeDelegate;

@class Reachability;

@interface ORMMAJavascriptBridge : NSObject <UIAccelerometerDelegate,
											 CLLocationManagerDelegate>
{
@private
	id<ORMMAJavascriptBridgeDelegate> m_bridgeDelegate;
	
	Reachability *m_reachability;
	CLLocationManager *m_locationManager;
	CMMotionManager *m_motionManager;
	UIAccelerometer *m_accelerometer;
	NSTimer *m_timer;
	
	BOOL m_accelerometerEnableCount;
	BOOL m_compassEnableCount;
	BOOL m_gyroscopeEnableCount;
	BOOL m_locationEnableCount;
	
	BOOL m_processAccelerometer;
	BOOL m_processShake;
	CGFloat m_shakeIntensity;
}
@property( nonatomic, assign ) id<ORMMAJavascriptBridgeDelegate> bridgeDelegate;
@property( nonatomic, retain ) CMMotionManager *motionManager;
@property( nonatomic, copy, readonly ) NSString *networkStatus;



// parses the passed URL; if it is handleable by the bridge, it will be handled 
// otherwise no action will be taken
// returns- TRUE  if the URL was processed, FALSE otherwise
- (BOOL)processURL:(NSURL *)url
		forWebView:(UIWebView *)webView;


- (void)restoreServicesToDefaultState;

@end



@protocol ORMMAJavascriptBridgeDelegate

@required

@property( nonatomic, assign, readonly ) UIWebView *webView;

- (void)adIsORMMAEnabledForWebView:(UIWebView *)webView;

- (NSString *)usingWebView:(UIWebView *)webView
		 executeJavascript:(NSString *)javascript, ...;

- (void)showAd:(UIWebView *)webView;
- (void)hideAd:(UIWebView *)webView;

- (void)closeAd:(UIWebView *)webView;

- (void)openBrowser:(UIWebView *)webView
	  withUrlString:(NSString *)urlString
		 enableBack:(BOOL)back
	  enableForward:(BOOL)forward
	  enableRefresh:(BOOL)refresh;

- (void)resizeToWidth:(CGFloat)width
			   height:(CGFloat)height
	   inWebView:(UIWebView *)webView;

- (void)expandTo:(CGRect)newFrame
		 withURL:(NSURL *)url
	   inWebView:(UIWebView *)webView
   blockingColor:(UIColor *)blockingColor
 blockingOpacity:(CGFloat)blockingOpacity;

- (void)sendEMailTo:(NSString *)to
		withSubject:(NSString *)subject
		   withBody:(NSString *)body
			 isHTML:(BOOL)html;
- (void)sendSMSTo:(NSString *)to
		 withBody:(NSString *)body;
- (void)placeCallTo:(NSString *)phoneNumber;
- (void)addEventToCalanderForDate:(NSDate *)date
						withTitle:(NSString *)title
						 withBody:(NSString *)body;

- (CGRect)getAdFrameInWindowCoordinates;

@end