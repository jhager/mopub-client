//
//  ORMMAJavascriptBridge.h
//  RichMediaAds
//
//  Created by Robert Hedin on 9/7/10.
//  Copyright 2010 The Weather Channel. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@protocol ORMMAJavascriptBridgeDelegate;

@class Reachability;

@interface ORMMAJavascriptBridge : NSObject 
{
@private
	id<ORMMAJavascriptBridgeDelegate> m_bridgeDelegate;
}
@property( nonatomic, assign ) id<ORMMAJavascriptBridgeDelegate> bridgeDelegate;

// parses the passed URL; if it is handleable by the bridge, it will be handled 
// otherwise no action will be taken
// returns- TRUE  if the URL was processed, FALSE otherwise
- (BOOL)processURL:(NSURL *)url
		forWebView:(UIWebView *)webView;

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

- (CGRect)getAdFrameInWindowCoordinates;

@end