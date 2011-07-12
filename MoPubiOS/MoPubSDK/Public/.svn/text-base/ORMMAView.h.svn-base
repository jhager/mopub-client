/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import <UIKit/UIKit.h>
#import <MessageUI/MessageUI.h>
#import <EventKit/EventKit.h>
#import "ORMMAWebBrowserViewController.h"
#import "ORMMAAVPlayer.h"

@class ORMMAJavascriptBridge;
@class ORMMALocalServer;

@protocol ORMMAViewDelegate;
@protocol ORMMAJavascriptBridgeDelegate;

typedef enum ORMMAViewStateEnum
{
	ORMMAViewStateHidden = -1,
	ORMMAViewStateDefault = 0,
	ORMMAViewStateResized,
	ORMMAViewStateExpanded
} ORMMAViewState;



@interface ORMMAView : UIView <MFMailComposeViewControllerDelegate,
							   MFMessageComposeViewControllerDelegate,
							   ORMMAWebBrowserViewControllerDelegate,
								ORMMAAVPlayerDelegate>
{
@private
	UIDevice *m_currentDevice;
	ORMMAJavascriptBridge *m_javascriptBridge;
	id<ORMMAViewDelegate> m_ormmaDelegate;
	ORMMAViewState m_currentState;
	NSError *m_lastError;
	BOOL m_adVisible;
	
	// for resize
	CGSize m_maxSize;

	UIWebView *m_webView;
	
	CGRect m_defaultFrame;
	
	CGRect m_translatedFrame;
	NSInteger m_originalTag;
	NSInteger m_parentTag;
	
	UIButton *m_blockingView;
	
	ORMMAWebBrowserViewController *m_webBrowser;
	ORMMAAVPlayer *m_moviePlayer;
	
	NSURL *m_creativeURL;
	NSString *m_creativeId;	

	BOOL m_applicationReady;
	
	BOOL m_allowLocationServices;
	
	BOOL m_isOrmmaAd;
	NSURL *m_launchURL;
	BOOL m_loadingAd;
	
	NSInteger m_modalityCounter;
    
	
	NSMutableArray *m_externalProtocols;
	BOOL allowAdOrientation;
	
	EKEventStore *eventStore;
	EKEvent *event;
    
    // Save the frame that the host application wants us to apply
    // and apply it to the ormmaView when it is not in expanded state
    CGRect m_originalFrame;
    CGAffineTransform m_originalTransform;
    // The ORMMAView frame in the expanded state
    CGRect m_expandedFrame;
    BOOL m_bIsDisplayed;
}
@property( nonatomic, assign ) id<ORMMAViewDelegate> ormmaDelegate;
@property( nonatomic, copy ) NSString *htmlStub;
@property( nonatomic, copy ) NSURL *creativeURL;
@property( nonatomic, retain, readonly ) NSError *lastError;
@property( nonatomic, assign, readonly ) ORMMAViewState currentState;
@property( nonatomic, assign ) CGSize maxSize;
@property( nonatomic, assign ) BOOL allowLocationServices;
@property( nonatomic, assign, readonly ) BOOL isOrmmaAd;

- (void)loadCreative:(NSURL *)url;

- (void)loadHTMLCreative:(NSString *)htmlFragment
			 creativeURL:(NSURL *)url;

// registers a protocol scheme for external handling
- (void)registerProtocol:(NSString *)protocol;

// removes a protocol scheme from external handling
- (void)deregisterProtocol:(NSString *)protocol;


// used to force an ad to revert to its default state
- (void)restoreToDefaultState;


- (void)doneWithBrowser;


// Returns the html string for the current creative
- (NSString *)cachedHtmlForCreative;

// Returns the computed creative id
- (NSString *)creativeId;


// These method let the app indicate whether it considers the ORMMAView to be visible or not.  
// This is useful when ORMMAView are embedded in a scrolling view and need to be loaded in advance.
// Calling these method will let the ORMMAView set the proper default ad position based on the currently displayed view.
- (void)ormmaViewDisplayed:(BOOL)isDisplayed;
@end



@protocol ORMMAViewDelegate <NSObject>

@required

// retrieves the owning view controller
- (UIViewController *)ormmaViewController;


@optional

// called to allow the application to inject javascript into the creative
- (NSString *)javascriptForInjection;

// notifies the consumer that it should handle the specified request
// NOTE: REQUIRED IF A PROTOCOL IS REGISTERED
- (void)handleRequest:(NSURLRequest *)request
				forAd:(ORMMAView *)adView;


// called to allow the application to execute javascript on the creative at the
// time the creative is loaded
- (NSString *)onLoadJavaScriptForAd:(ORMMAView *)adView;

// called when an ad fails to load
- (void)failureLoadingAd:(ORMMAView *)adView;

// Called before the ad is resized in place to allow the parent application to
// animate things if desired.
- (void)willResizeAd:(ORMMAView *)adView
			  toSize:(CGSize)size;

// Called after the ad is resized in place to allow the parent application to
// animate things if desired.
- (void)didResizeAd:(ORMMAView *)adView
			  toSize:(CGSize)size;



// Called just before to an ad is displayed
- (void)adWillShow:(ORMMAView *)adView;

// Called just after to an ad is displayed
- (void)adDidShow:(ORMMAView *)adView;

// Called just before to an ad is Hidden
- (void)adWillHide:(ORMMAView *)adView;

// Called just after to an ad is Hidden
- (void)adDidHide:(ORMMAView *)adView;

// Called just before an ad expands
- (void)willExpandAd:(ORMMAView *)adView
			 toFrame:(CGRect)frame;

// Called just after an ad expands
- (void)didExpandAd:(ORMMAView *)adView
			toFrame:(CGRect)frame;

// Called just before an ad closes
- (void)adWillClose:(ORMMAView *)adView;

// Called just after an ad closes
- (void)adDidClose:(ORMMAView *)adView;

// called when the ad will begin heavy content (usually when the ad goes full screen)
- (void)appShouldSuspendForAd:(ORMMAView *)adView;

// called when the ad is finished with it's heavy content (usually when the ad returns from full screen)
- (void)appShouldResumeFromAd:(ORMMAView *)adView;

// allows the application to override the phone call process to, for example
// display an alert to the user before hand
- (void)placePhoneCall:(NSString *)number;

// allows the application to override the click to app store, for example
// display an alert to the user before hand
- (void)placeCallToAppStore:(NSString *)urlString;

// allows the application to override the create calendar event process to, for 
// example display an alert to the user before hand
- (void)createCalendarEntryForDate:(NSDate *)date
							 title:(NSString *)title
							  body:(NSString *)body;

// allows the application to inject itself into the full screen browser menu 
// to handle the "go" method (for example, send to safari, facebook, etc)
- (void)showURLFullScreen:(NSURL *)url
			   sourceView:(UIView *)view;

- (void)emailNotSetupForAd:(ORMMAView *)adView;


@end
