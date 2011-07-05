//
//  MPAdRequest.m
//  MoPubTests
//
//  Created by Haydn Dufrene on 6/15/11.
//  Copyright 2011 The Falco Initiative. All rights reserved.
//

#import "MPAdManager.h"
#import "MPConstants.h"
#import "MPGlobal.h"
#import "MPAdView.h"
#import "MPTimer.h"
#import "MPBaseAdapter.h"
#import "MPAdapterMap.h"
#import "CJSONDeserializer.h"
#import "MPAdView+MPAdManagerPrivate.h"
#import "UIDevice-Hardware.h"

static NSString * const kTimerNotificationName		= @"Autorefresh";
static NSString * const kErrorDomain				= @"mopub.com";
static NSString * const kMoPubUrlScheme				= @"mopub";
static NSString * const kMoPubCloseHost				= @"close";
static NSString * const kMoPubFinishLoadHost		= @"finishLoad";
static NSString * const kMoPubFailLoadHost			= @"failLoad";
static NSString * const kMoPubInAppHost				= @"inapp";
static NSString * const kMoPubCustomHost			= @"custom";
static NSString * const kMoPubInterfaceOrientationPortraitId	= @"p";
static NSString * const kMoPubInterfaceOrientationLandscapeId	= @"l";
static const CGFloat kMoPubRequestTimeoutInterval	= 10.0;
static const CGFloat kMoPubRequestRetryInterval     = 60.0;

// Ad header key/value constants.
static NSString * const kClickthroughHeaderKey		= @"X-Clickthrough";
static NSString * const kLaunchpageHeaderKey		= @"X-Launchpage";
static NSString * const kFailUrlHeaderKey			= @"X-Failurl";
static NSString * const kImpressionTrackerHeaderKey	= @"X-Imptracker";
static NSString * const kInterceptLinksHeaderKey	= @"X-Interceptlinks";
static NSString * const kScrollableHeaderKey		= @"X-Scrollable";
static NSString * const kWidthHeaderKey				= @"X-Width";
static NSString * const kHeightHeaderKey			= @"X-Height";
static NSString * const kRefreshTimeHeaderKey		= @"X-Refreshtime";
static NSString * const kAnimationHeaderKey			= @"X-Animation";
static NSString * const kAdTypeHeaderKey			= @"X-Adtype";
static NSString * const kNetworkTypeHeaderKey		= @"X-Networktype";
static NSString * const kAdTypeHtml					= @"html";
static NSString * const kAdTypeClear				= @"clear";

//ORMMA constants
NSString * const kAnimationKeyExpand = @"expand";
NSString * const kAnimationKeyCloseExpanded = @"closeExpanded";
NSString * const kInitialORMMAPropertiesFormat = @"{ state: '%@'," \
" size: { width: %f, height: %f },"\
" maxSize: { width: %f, height: %f },"\
" screenSize: { width: %f, height: %f },"\
" defaultPosition: { x: %f, y: %f, width: %f, height: %f },"\
" orientation: %i,"\
" supports: ['level-1', 'orientation', 'screen', 'size' %@ ] }";


@interface MPAdManager (Internal)
- (void)loadAdWithURL:(NSURL *)URL;
- (void)forceRefreshAd;
- (void)registerForApplicationStateTransitionNotifications;
- (void)destroyWebviewPool;
- (NSString *)orientationQueryStringComponent;
- (NSString *)scaleFactorQueryStringComponent;
- (NSString *)timeZoneQueryStringComponent;
- (NSString *)locationQueryStringComponent;
- (NSURLRequest *)serverRequestObjectForUrl:(NSURL *)URL;
- (void)replaceCurrentAdapterWithAdapter:(MPBaseAdapter *)newAdapter;
- (void)scheduleAutorefreshTimer;
- (NSURL *)serverRequestUrl;
- (UIWebView *)makeAdWebViewWithFrame:(CGRect)frame;
- (void)trackClick;
- (void)trackImpression;
- (NSDictionary *)dictionaryFromQueryString:(NSString *)query;
- (void)customLinkClickedForSelectorString:(NSString *)selectorString 
							withDataString:(NSString *)dataString;

- (NSString *)executeJavascript:(NSString *)javascript
			   withVarArgs:(va_list)varargs;
- (void)injectJavaScript;
- (void)injectORMMAJavaScript;
- (void)injectORMMAState;
- (void)injectJavaScriptFile:(NSString *)fileName;

@end

@interface MPAdManager ()

@property (nonatomic, assign) MPAdView *adView;
@property (nonatomic, copy) NSString *adUnitId;
@property (nonatomic, retain) CLLocation *location;
@property (nonatomic, copy) NSString *keywords;
@property (nonatomic, copy) NSURL *URL;
@property (nonatomic, copy) NSURL *clickURL;
@property (nonatomic, copy) NSURL *interceptURL;
@property (nonatomic, copy) NSURL *failURL;
@property (nonatomic, copy) NSURL *impTrackerURL;
@property (nonatomic, assign) BOOL isLoading;
@property (nonatomic, assign) BOOL ignoresAutorefresh;
@property (nonatomic, assign) BOOL adActionInProgress;
@property (nonatomic, assign) BOOL autorefreshTimerNeedsScheduling;	
@property (nonatomic, retain) MPTimer *autorefreshTimer;
@property (nonatomic, retain) MPStore *store;
@property (nonatomic, retain) NSMutableData *data;
@property (nonatomic, retain) NSMutableSet *webviewPool;
@property (nonatomic, retain) MPBaseAdapter *currentAdapter;

@end

@implementation MPAdManager

@synthesize adView = _adView;
@synthesize adUnitId = _adUnitId;
@synthesize location = _location;
@synthesize keywords = _keywords;
@synthesize URL = _URL;
@synthesize clickURL = _clickURL;
@synthesize interceptURL = _interceptURL;
@synthesize failURL = _failURL;
@synthesize impTrackerURL = _impTrackerURL;
@synthesize autorefreshTimer = _autorefreshTimer;
@synthesize isLoading = _isLoading;
@synthesize adActionInProgress = _adActionInProgress;
@synthesize ignoresAutorefresh = _ignoresAutorefresh;
@synthesize autorefreshTimerNeedsScheduling = _autorefreshTimerNeedsScheduling;
@synthesize store = _store;
@synthesize data = _data;
@synthesize webviewPool = _webviewPool;
@synthesize currentAdapter = _currentAdapter;

-(id)initWithAdView:(MPAdView *)adView {
	if ((self = [super init])) {
        _ORMMAView = [[ORMMAView alloc] initWithFrame:adView.frame];
		_adView = adView;
		_data = [[NSMutableData data] retain];
		_webviewPool = [[NSMutableSet set] retain];
		_isLoading = NO;
		_ignoresAutorefresh = NO;
		_store = [MPStore sharedStore];
		_timerTarget = [[MPTimerTarget alloc] initWithNotificationName:kTimerNotificationName];
		[[NSNotificationCenter defaultCenter] addObserver:self
												 selector:@selector(forceRefreshAd)
													 name:kTimerNotificationName
												   object:_timerTarget];		
		[self registerForApplicationStateTransitionNotifications];
	}
	return self;
}

- (void)dealloc 
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];	
		
	[self destroyWebviewPool];
	
	[_currentAdapter unregisterDelegate];
	[_currentAdapter release];
	[_previousAdapter unregisterDelegate];
	[_previousAdapter release];
	[_adUnitId release];
	[_conn cancel];
	[_conn release];
	[_data release];
	[_URL release];
	[_clickURL release];
	[_interceptURL release];
	[_failURL release];
	[_impTrackerURL release];
	[_keywords release];
	[_location release];
	[_autorefreshTimer invalidate];
	[_autorefreshTimer release];
	[_timerTarget release];
	
	_adView = nil;
    [super dealloc];
}

- (void)destroyWebviewPool
{
	for (UIWebView *webview in _webviewPool)
	{
		[webview setDelegate:nil];
		[webview stopLoading];
	}
	[_webviewPool release];
}

- (void)registerForApplicationStateTransitionNotifications
{
	// iOS version > 4.0: Register for relevant application state transition notifications.
	if (&UIApplicationDidEnterBackgroundNotification != nil)
	{
		[[NSNotificationCenter defaultCenter] addObserver:self 
												 selector:@selector(applicationDidEnterBackground) 
													 name:UIApplicationDidEnterBackgroundNotification 
												   object:[UIApplication sharedApplication]];
	}		
	if (&UIApplicationWillEnterForegroundNotification !=  nil)
	{
		[[NSNotificationCenter defaultCenter] addObserver:self 
												 selector:@selector(applicationWillEnterForeground)
													 name:UIApplicationWillEnterForegroundNotification 
												   object:[UIApplication sharedApplication]];
	}
}

- (void)refreshAd
{
	[self.autorefreshTimer invalidate];
	[self loadAdWithURL:nil];
}

- (void)forceRefreshAd
{
	// Cancel any existing request to the ad server.
	[_conn cancel];
	
	_isLoading = NO;
	[self.autorefreshTimer invalidate];
	[self loadAdWithURL:nil];
}

- (void)loadAdWithURL:(NSURL *)URL
{
	if (_isLoading) 
	{
		MPLogWarn(@"Ad view (%p) already loading an ad. Wait for previous load to finish.", self.adView);
		return;
	}
	
	self.URL = (URL) ? URL : [self serverRequestUrl];
	MPLogDebug(@"Ad view (%p) loading ad with MoPub server URL: %@", self.adView, self.URL);
	
	NSURLRequest *request = [self serverRequestObjectForUrl:self.URL];
	[_conn release];
	_conn = [[NSURLConnection connectionWithRequest:request delegate:self] retain];
	_isLoading = YES;
	
	MPLogInfo(@"Ad manager (%p) fired initial ad request.", self);
}

-(NSURL *)serverRequestUrl {
	NSString *urlString = [NSString stringWithFormat:@"http://%@/m/ad?v=4&udid=%@&q=%@&id=%@", 
						   HOSTNAME,
						   hashedMoPubUDID(),
						   [_keywords stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding],
						   [_adUnitId stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]
						   ];
	
	urlString = [urlString stringByAppendingString:[self orientationQueryStringComponent]];
	urlString = [urlString stringByAppendingString:[self scaleFactorQueryStringComponent]];
	urlString = [urlString stringByAppendingString:[self timeZoneQueryStringComponent]];
	urlString = [urlString stringByAppendingString:[self locationQueryStringComponent]];
	
	return [NSURL URLWithString:urlString];
}

- (NSString *)orientationQueryStringComponent
{
	UIInterfaceOrientation orientation = [UIApplication sharedApplication].statusBarOrientation;
	NSString *orientString = UIInterfaceOrientationIsPortrait(orientation) ?
	kMoPubInterfaceOrientationPortraitId : kMoPubInterfaceOrientationLandscapeId;
	return [NSString stringWithFormat:@"&o=%@", orientString];
}

- (NSString *)scaleFactorQueryStringComponent
{
	return [NSString stringWithFormat:@"&sc=%.1f", MPDeviceScaleFactor()];
}

- (NSString *)timeZoneQueryStringComponent
{
	static NSDateFormatter *formatter;
	@synchronized(self)
	{
		if (!formatter) formatter = [[NSDateFormatter alloc] init];
	}
	[formatter setDateFormat:@"Z"];
	NSDate *today = [NSDate date];
	return [NSString stringWithFormat:@"&z=%@", [formatter stringFromDate:today]];
}

- (NSString *)locationQueryStringComponent
{
	NSString *result = @"";
	if (_location)
	{
		result = [result stringByAppendingFormat:
				  @"&ll=%f,%f",
				  _location.coordinate.latitude,
				  _location.coordinate.longitude];
	}
	return result;
}

- (NSURLRequest *)serverRequestObjectForUrl:(NSURL *)URL {
	NSMutableURLRequest *request = [[[NSMutableURLRequest alloc] 
									 initWithURL:URL
									 cachePolicy:NSURLRequestUseProtocolCachePolicy 
									 timeoutInterval:kMoPubRequestTimeoutInterval] autorelease];
	
	// Set the user agent so that we know where the request is coming from (for targeting).
	[request setValue:userAgentString forHTTPHeaderField:@"User-Agent"];			
	
	return request;
}

- (NSDictionary *)dictionaryFromQueryString:(NSString *)query
{
	NSMutableDictionary *queryDict = [[NSMutableDictionary alloc] initWithCapacity:1];
	NSArray *queryElements = [query componentsSeparatedByString:@"&"];
	for (NSString *element in queryElements) {
		NSArray *keyVal = [element componentsSeparatedByString:@"="];
		NSString *key = [keyVal objectAtIndex:0];
		NSString *value = [keyVal lastObject];
		[queryDict setObject:[value stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding] 
					  forKey:key];
	}
	return [queryDict autorelease];
}

- (void)trackClick
{
	NSURLRequest *clickURLRequest = [NSURLRequest requestWithURL:self.clickURL];
	[NSURLConnection connectionWithRequest:clickURLRequest delegate:nil];
	MPLogDebug(@"Ad view (%p) tracking click %@", self, self.clickURL);
}

- (void)trackImpression
{
	NSURLRequest *impTrackerURLRequest = [NSURLRequest requestWithURL:self.impTrackerURL];
	[NSURLConnection connectionWithRequest:impTrackerURLRequest delegate:nil];
	MPLogDebug(@"Ad view (%p) tracking impression %@", self, self.impTrackerURL);
}

- (void)setAdContentView:(UIView *)view
{
	[_adView setAdContentView:view];
}

- (void)customEventDidFailToLoadAd {
	[_adView customEventDidFailToLoadAd];
}

- (UIViewController *)viewControllerForPresentingModalView {
	return [_adView.delegate viewControllerForPresentingModalView];
}

- (void)customLinkClickedForSelectorString:(NSString *)selectorString 
							withDataString:(NSString *)dataString
{
	if (!selectorString)
	{
		MPLogError(@"Custom selector requested, but no custom selector string was provided.",
				   selectorString);
	}
	
	SEL selector = NSSelectorFromString(selectorString);
	
	// First, try calling the no-object selector.
	if ([_adView.delegate respondsToSelector:selector])
	{
		[_adView.delegate performSelector:selector];
	}
	// Then, try calling the selector passing in the ad view.
	else 
	{
		NSString *selectorWithObjectString = [NSString stringWithFormat:@"%@:", selectorString];
		SEL selectorWithObject = NSSelectorFromString(selectorWithObjectString);
		
		if ([_adView.delegate respondsToSelector:selectorWithObject])
		{
			NSData *data = [dataString dataUsingEncoding:NSUTF8StringEncoding];
			NSDictionary *dataDictionary = [[CJSONDeserializer deserializer] deserializeAsDictionary:data
																							   error:NULL];
			[_adView.delegate performSelector:selectorWithObject withObject:dataDictionary];
		}
		else
		{
			MPLogError(@"Ad view delegate does not implement custom selectors %@ or %@.",
					   selectorString,
					   selectorWithObjectString);
		}
	}
}

- (void)adLinkClicked:(NSURL *)URL
{
	_adActionInProgress = YES;
	
	// Construct the URL that we want to load in the ad browser, using the click-tracking URL.
	NSString *redirectURLString = [[URL absoluteString] URLEncodedString];	
	NSURL *desiredURL = [NSURL URLWithString:[NSString stringWithFormat:@"%@&r=%@",
											  _clickURL,
											  redirectURLString]];
	
	// Notify delegate that the ad browser is about to open.
	if ([_adView.delegate respondsToSelector:@selector(willPresentModalViewForAd:)])
		[_adView.delegate willPresentModalViewForAd:_adView];
	
	if ([self.autorefreshTimer isScheduled])
		[self.autorefreshTimer pause];
	
	// Present ad browser.
	MPAdBrowserController *browserController = [[MPAdBrowserController alloc] initWithURL:desiredURL 
																				 delegate:self];
	[[_adView.delegate viewControllerForPresentingModalView] presentModalViewController:browserController 			
																			animated:YES];
	[browserController release];
}

#pragma mark -
#pragma mark MPAdBrowserControllerDelegate

- (void)dismissBrowserController:(MPAdBrowserController *)browserController{
	[self dismissBrowserController:browserController animated:YES];
}

- (void)dismissBrowserController:(MPAdBrowserController *)browserController animated:(BOOL)animated
{
	_adActionInProgress = NO;
	[[_adView.delegate viewControllerForPresentingModalView] dismissModalViewControllerAnimated:animated];
	
	if ([_adView.delegate respondsToSelector:@selector(didDismissModalViewForAd:)])
		[_adView.delegate didDismissModalViewForAd:_adView];
	
	if (_autorefreshTimerNeedsScheduling)
	{
		[self.autorefreshTimer scheduleNow];
		_autorefreshTimerNeedsScheduling = NO;
	}
	else if ([self.autorefreshTimer isScheduled])
		[self.autorefreshTimer resume];
}

# pragma mark -
# pragma mark NSURLConnection delegate

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response 
{
	if ([response respondsToSelector:@selector(statusCode)])
	{
		int statusCode = [((NSHTTPURLResponse *)response) statusCode];
		if (statusCode >= 400)
		{
			[connection cancel];
			NSDictionary *errorInfo = [NSDictionary dictionaryWithObject:[NSString stringWithFormat:
																		  NSLocalizedString(@"Server returned status code %d",@""),
																		  statusCode]
																  forKey:NSLocalizedDescriptionKey];
			NSError *statusError = [NSError errorWithDomain:@"mopub.com"
													   code:statusCode
												   userInfo:errorInfo];
			[self connection:connection didFailWithError:statusError];
			return;
		}
	}
	
	// Parse response headers, set relevant URLs and booleans.
	NSDictionary *headers = [(NSHTTPURLResponse *)response allHeaderFields];
	NSString *urlString = nil;
	
	urlString = [headers objectForKey:kClickthroughHeaderKey];
	self.clickURL = urlString ? [NSURL URLWithString:urlString] : nil;
	
	urlString = [headers objectForKey:kLaunchpageHeaderKey];
	self.interceptURL = urlString ? [NSURL URLWithString:urlString] : nil;
	
	urlString = [headers objectForKey:kFailUrlHeaderKey];
	self.failURL = urlString ? [NSURL URLWithString:urlString] : nil;
	
	urlString = [headers objectForKey:kImpressionTrackerHeaderKey];
	self.impTrackerURL = urlString ? [NSURL URLWithString:urlString] : nil;
	
	NSString *shouldInterceptLinksString = [headers objectForKey:kInterceptLinksHeaderKey];
	if (shouldInterceptLinksString)
		self.adView.shouldInterceptLinks = [shouldInterceptLinksString boolValue];
	
	NSString *scrollableString = [headers objectForKey:kScrollableHeaderKey];
	if (scrollableString)
		self.adView.scrollable = [scrollableString boolValue];
	
	NSString *widthString = [headers objectForKey:kWidthHeaderKey];
	NSString *heightString = [headers objectForKey:kHeightHeaderKey];
	
	// Try to get the creative size from the server or otherwise use the original container's size.
	if (widthString && heightString)
		self.adView.creativeSize = CGSizeMake([widthString floatValue], [heightString floatValue]);
	else
		self.adView.creativeSize = self.adView.originalSize;
	
	// Create the autorefresh timer, which will be scheduled either when the ad appears,
	// or if it fails to load.
	NSString *refreshString = [headers objectForKey:kRefreshTimeHeaderKey];
	if (refreshString && !self.ignoresAutorefresh)
	{
		NSTimeInterval interval = [refreshString doubleValue];
		interval = (interval >= MINIMUM_REFRESH_INTERVAL) ? interval : MINIMUM_REFRESH_INTERVAL;
		self.autorefreshTimer = [MPTimer timerWithTimeInterval:interval
													  target:_timerTarget 
													  selector:@selector(postNotification) 
													  userInfo:nil 
													  repeats:NO];
	}
	
	NSString *animationString = [headers objectForKey:kAnimationHeaderKey];
	if (animationString)
		self.adView.animationType = [animationString intValue];
	
	// Log if the ad is from an ad network
	NSString *networkTypeHeader = [[(NSHTTPURLResponse *)response allHeaderFields] 
								   objectForKey:kNetworkTypeHeaderKey];
	if (networkTypeHeader && ![networkTypeHeader isEqualToString:@""])
	{
		MPLogInfo(@"Fetching Ad Network Type: %@",networkTypeHeader);
	}
	
	// Determine ad type.
	NSString *typeHeader = [headers	objectForKey:kAdTypeHeaderKey];
	
	if (!typeHeader || [typeHeader isEqualToString:kAdTypeHtml]) {
		[self replaceCurrentAdapterWithAdapter:nil];
		
		// HTML ad, so just return. connectionDidFinishLoading: will take care of the rest.
		return;
	}	else if ([typeHeader isEqualToString:kAdTypeClear]) {
		[self replaceCurrentAdapterWithAdapter:nil];
		
		// Show a blank.
		MPLogInfo(@"*** CLEAR ***");
		[connection cancel];
		_isLoading = NO;
		[_adView backFillWithNothing];
		[self scheduleAutorefreshTimer];
		return;
	}
	
	// Obtain adapter for specified ad type.
	NSString *classString = [[MPAdapterMap sharedAdapterMap] classStringForAdapterType:typeHeader];
	Class cls = NSClassFromString(classString);
	if (cls != nil)
	{
		MPBaseAdapter *newAdapter = (MPBaseAdapter *)[[cls alloc] initWithAdManager:self];
		[self replaceCurrentAdapterWithAdapter:newAdapter];
		
		[connection cancel];
		
		// Tell adapter to fire off ad request.
		NSDictionary *params = [(NSHTTPURLResponse *)response allHeaderFields];
		[_currentAdapter getAdWithParams:params];
	}
	// Else: no adapter for the specified ad type, so just fail over.
	else 
	{
		[self replaceCurrentAdapterWithAdapter:nil];
		
		[connection cancel];
		_isLoading = NO;
		
		[self loadAdWithURL:self.failURL];
	}	
}

- (void)replaceCurrentAdapterWithAdapter:(MPBaseAdapter *)newAdapter
{
	// Dispose of the last adapter stored in _previousAdapter.
	[_previousAdapter unregisterDelegate];
	[_previousAdapter release];
	
	_previousAdapter = _currentAdapter;
	_currentAdapter = newAdapter;
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)d
{
	[_data appendData:d];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	MPLogError(@"Ad view (%p) failed to get a valid response from MoPub server. Error: %@", 
			   self, error);
	
	// If the initial request to MoPub fails, replace the current ad content with a blank.
	_isLoading = NO;
	[_adView backFillWithNothing];
	
	// Retry in 60 seconds.
	if (!self.autorefreshTimer || ![self.autorefreshTimer isValid])
	{
		self.autorefreshTimer = [MPTimer timerWithTimeInterval:kMoPubRequestRetryInterval 
														target:_timerTarget 
													  selector:@selector(postNotification) 
													  userInfo:nil 
													   repeats:NO];
	}
	
	[self scheduleAutorefreshTimer];	
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
	// Generate a new webview to contain the HTML and add it to the webview pool.
	UIWebView *webview = [self makeAdWebViewWithFrame:(CGRect){{0, 0}, self.adView.creativeSize}];
	webview.delegate = self;
	[_webviewPool addObject:webview];
	[webview loadData:_data MIMEType:@"text/html" textEncodingName:@"utf-8" baseURL:self.URL];
	
	// Print out the response, for debugging.
	if (MPLogGetLevel() <= MPLogLevelTrace)
	{
		NSString *response = [[NSString alloc] initWithData:_data encoding:NSUTF8StringEncoding];
		MPLogTrace(@"Ad view (%p) loaded HTML content: %@", self, response);
		[response release];
	}
}

- (void)scheduleAutorefreshTimer
{
	if (_adActionInProgress)
	{
		MPLogDebug(@"Ad action in progress: MPTimer will be scheduled after action ends.");
		_autorefreshTimerNeedsScheduling = YES;
	}
	else if ([self.autorefreshTimer isScheduled])
	{
		MPLogDebug(@"Tried to schedule the autorefresh timer, but it was already scheduled.");
	}
	else if (self.autorefreshTimer == nil)
	{
		MPLogDebug(@"Tried to schedule the autorefresh timer, but it was nil.");
	}
	else
	{
		[self.autorefreshTimer scheduleNow];
	}
}

#pragma mark -
#pragma mark MPAdapterDelegate

- (void)adapterDidFinishLoadingAd:(MPBaseAdapter *)adapter shouldTrackImpression:(BOOL)shouldTrack
{
	_isLoading = NO;
	
	if (shouldTrack) [self trackImpression];
	[self scheduleAutorefreshTimer];
	
	if ([_adView.delegate respondsToSelector:@selector(adViewDidLoadAd:)])
		[_adView.delegate adViewDidLoadAd:_adView];	
}

- (void)adapter:(MPBaseAdapter *)adapter didFailToLoadAdWithError:(NSError *)error
{
	// Ignore fail messages from the previous adapter.
	if (_previousAdapter && adapter == _previousAdapter) return;
	
	_isLoading = NO;
	MPLogError(@"Adapter (%p) failed to load ad. Error: %@", adapter, error);
	
	// Dispose of the current adapter, because we don't want it to try loading again.
	[_currentAdapter unregisterDelegate];
	[_currentAdapter release];
	_currentAdapter = nil;
	
	// An adapter will sometimes send this message during a user action (example: user taps on an 
	// iAd; iAd then does an internal refresh and fails). In this case, we schedule a new request
	// to occur after the action ends. Otherwise, just start a new request using the fall-back URL.
	if (_adActionInProgress) [self scheduleAutorefreshTimer];
	else [self loadAdWithURL:self.failURL];
	
}

- (void)userActionWillBeginForAdapter:(MPBaseAdapter *)adapter
{
	_adActionInProgress = YES;
	[self trackClick];
	
	if ([self.autorefreshTimer isScheduled])
		[self.autorefreshTimer pause];
	
	// Notify delegate that the ad will present a modal view / disrupt the app.
	if ([_adView.delegate respondsToSelector:@selector(willPresentModalViewForAd:)])
		[_adView.delegate willPresentModalViewForAd:_adView];	
}

- (void)userActionDidEndForAdapter:(MPBaseAdapter *)adapter
{
	_adActionInProgress = NO;
	
	if (_autorefreshTimerNeedsScheduling)
	{
		[self.autorefreshTimer scheduleNow];
		_autorefreshTimerNeedsScheduling = NO;
	}
	else if ([self.autorefreshTimer isScheduled])
		[self.autorefreshTimer resume];
	
	// Notify delegate that the ad's modal view was dismissed, returning focus to the app.
	if ([_adView.delegate respondsToSelector:@selector(didDismissModalViewForAd:)])
		[_adView.delegate didDismissModalViewForAd:_adView];	
}

- (void)userWillLeaveApplicationFromAdapter:(MPBaseAdapter *)adapter
{
	// TODO: Implement.
}

#pragma mark -
#pragma mark UIWebViewDelegate

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request 
 navigationType:(UIWebViewNavigationType)navigationType
{
	NSURL *URL = [request URL];
	
	// Handle the custom mopub:// scheme.
	if ([[URL scheme] isEqualToString:kMoPubUrlScheme])
	{
		NSString *host = [URL host];
		if ([host isEqualToString:kMoPubCloseHost])
		{
			[self.adView didCloseAd:nil];
		}
		else if ([host isEqualToString:kMoPubFinishLoadHost])
		{
			_isLoading = NO;
			
			[self.adView setAdContentView:webView];
			[self scheduleAutorefreshTimer];
			
			// Notify delegate that an ad has been loaded.
			if ([self.adView.delegate respondsToSelector:@selector(adViewDidLoadAd:)]) 
				[self.adView.delegate adViewDidLoadAd:_adView];
		}
		else if ([host isEqualToString:kMoPubFailLoadHost])
		{
			_isLoading = NO;
			
			// Deallocate this webview by removing it from the pool.
			webView.delegate = nil;
			[webView stopLoading];
			[_webviewPool removeObject:webView];
			
			// Start a new request using the fall-back URL.
			[self loadAdWithURL:self.failURL];
		}
	    else if ([host isEqualToString:kMoPubInAppHost])
		{
			[self trackClick];
			NSDictionary *queryDict = [self dictionaryFromQueryString:[URL query]];
			[_store initiatePurchaseForProductIdentifier:[queryDict objectForKey:@"id"] 
												quantity:[[queryDict objectForKey:@"num"] intValue]];
		}
	    else if ([host isEqualToString:kMoPubCustomHost])
		{
			[self trackClick];
			NSDictionary *queryDict = [self dictionaryFromQueryString:[URL query]];
			[self customLinkClickedForSelectorString:[queryDict objectForKey:@"fnc"]
									  withDataString:[queryDict objectForKey:@"data"]];
		} 
		return NO;
	}     
	// Intercept non-click forms of navigation (e.g. "window.location = ...") if the target URL
	// has the interceptURL prefix. Launch the ad browser.
	if (navigationType == UIWebViewNavigationTypeOther && 
		_adView.shouldInterceptLinks && 
		self.interceptURL &&
		[[URL absoluteString] hasPrefix:[self.interceptURL absoluteString]])
	{
		[self adLinkClicked:URL];
		return NO;
	}
	
	// Launch the ad browser for all clicks (if shouldInterceptLinks is YES).
	if (navigationType == UIWebViewNavigationTypeLinkClicked && _adView.shouldInterceptLinks)
	{
		[self adLinkClicked:URL];
		return NO;
	}
	
	// Other stuff (e.g. JavaScript) should load as usual.
	return YES;	
}

- (void)webViewDidFinishLoad:(UIWebView *)webView
{
	// we've finished loading the URL
	[self injectJavaScript];
}

- (UIWebView *)makeAdWebViewWithFrame:(CGRect)frame
{
	UIWebView *webView = [[UIWebView alloc] initWithFrame:frame];
	if (self.adView.stretchesWebContentToFill)
		webView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
	webView.backgroundColor = [UIColor clearColor];
	webView.opaque = NO;
	return [webView autorelease];
}

# pragma mark -
# pragma UIApplicationNotification responders

- (void)applicationDidEnterBackground
{
	[self.autorefreshTimer pause];
}

- (void)applicationWillEnterForeground
{
	_autorefreshTimerNeedsScheduling = NO;
	if (_ignoresAutorefresh == NO) [self forceRefreshAd];
}

#pragma mark -
#pragma mark JavaScript Injection

- (void)injectJavaScript
{
	// notify app that the ad is preparing to show
	//[self fireAdWillShow];
	
	// always inject the ORMMA code
    NSLog( @"Ad requires ORMMA, inject code" );
    //[self injectORMMAJavaScript];
    
    // now allow the app to inject it's own javascript if needed
    //		if ( self.ormmaDelegate != nil )
    //		{
    //			if ( [self.ormmaDelegate respondsToSelector:@selector(javascriptForInjection)] )
    //			{
    //				NSString *js = [self.ormmaDelegate javascriptForInjection];
    //				[self usingWebView:webView executeJavascript:js];
    //			}
    //		}
    
    // now inject the current state
    [self injectORMMAState];
    
	// Notify app that the ad has been shown
	//[self fireAdDidShow];
}

- (void)injectJavaScriptFile:(NSString *)fileName
{
	if ([self executeJavascript:@"var ormmascr = document.createElement('script');ormmascr.src='%@';ormmascr.type='text/javascript';var ormmahd = document.getElementsByTagName('head')[0];ormmahd.appendChild(ormmascr);return 'OK';", fileName] == nil )
	{
		NSLog( @"Error injecting Javascript!" );
	}
}

- (void)injectORMMAState
{
	NSLog( @"Injecting ORMMA State into creative." );
	
	// setup the default state
	_ORMMAstate = ORMMAViewStateDefault;
	//[self fireAdWillShow];
	
	// add the various features the device supports
	NSMutableString *features = [NSMutableString stringWithCapacity:100];

	
	NSInteger platformType = [[UIDevice currentDevice] platformType];
	switch (platformType)
	{
		case UIDevice4iPhone:
			//[features appendString:@", 'camera'"]; 
			[features appendString:@", 'heading'"]; 
			[features appendString:@", 'rotation'"]; 
			break;
		case UIDevice1GiPad:
			[features appendString:@", 'heading'"]; 
			[features appendString:@", 'rotation'"]; 
			break;
		case UIDevice4GiPod:
			//[features appendString:@", 'camera'"]; 
			[features appendString:@", 'rotation'"]; 
			break;
		default:
			break;
	}
    
    // setup the ad size
	CGSize size = _webView.frame.size;
	
	// setup orientation
	NSInteger angle = angleFromOrientation();
	
	// setup the screen size
	UIDevice *device = [UIDevice currentDevice];
	CGSize screenSize = [device screenSizeForOrientation:[device orientation]];	
	
	// get the key window
	UIApplication *app = [UIApplication sharedApplication];
	UIWindow *keyWindow = [app keyWindow];
	
	// setup the default position information (translated into window coordinates)
	CGRect defaultPosition = [_adView convertRect:_adView.frame toView:keyWindow];	
    
	// build the initial properties
	NSString *properties = [NSString stringWithFormat:kInitialORMMAPropertiesFormat, @"default",
                            size.width, size.height,
                            _maxSize.width, _maxSize.height,
                            screenSize.width, screenSize.height,
                            defaultPosition.origin.x, defaultPosition.origin.y, defaultPosition.size.width, defaultPosition.size.height,
                            angle,
                            features];
	[self executeJavascript:@"window.ormmaview.fireChangeEvent( %@ );", properties];
    
	// make sure things are visible
	//[self fireAdDidShow];
}

#pragma mark -
#pragma ORMMAJavascriptBridgeDelegate Methods

- (void)adIsORMMAEnabled {
    _webView = (UIWebView *)_adView.adContentView;
}

- (NSString *)executeJavascript:(NSString *)javascript, ...
{
	// handle variable argument list
	va_list args;
	va_start( args, javascript );
	NSString *result = [self executeJavascript:javascript withVarArgs:args];
	va_end( args );
	return result;
}


- (NSString *)executeJavascript:(NSString *)javascript withVarArgs:(va_list)args
{
	NSString *js = [[[NSString alloc] initWithFormat:javascript arguments:args] autorelease];
	NSLog(@"Executing Javascript: %@", js );
	return [_webView stringByEvaluatingJavaScriptFromString:js];
}

- (void)showAd{ 	
	_ORMMAstate = ORMMAViewStateDefault;

    /*
    if ([_adView.delegate respondsToSelector:@selector(adViewDidLoadAd:)])
		[_adView.delegate adViewDidLoadAd:_adView];*/
    
	// notify the ad view that the state has changed
	[self executeJavascript:@"window.ormmaview.fireChangeEvent( { state: 'default' } );"];
} 

- (void)hideAd {
	// make sure we're not already hidden
	if (_ORMMAstate == ORMMAViewStateHidden )
	{
		[self executeJavascript:@"window.ormmaview.fireErrorEvent( 'Cannot hide if we're already hidden.', 'hide' );" ]; 
		return;
	}	
	
	// if the ad isn't in the default state, restore it first
    //[_adView didCloseAd:nil];
	
    [self closeAd];
	_ORMMAstate = ORMMAViewStateHidden;
    
	// notify the ad view that the state has changed
	[self executeJavascript:@"window.ormmaview.fireChangeEvent( { state: 'hidden', size: { width: 0, height: 0 } } );"];
}

- (void)closeAd {
    // if we're in the default state already, there is nothing to do
	if (_ORMMAstate == ORMMAViewStateDefault )
	{
		// default ad, nothing to do
		return;
	}
	if (_ORMMAstate == ORMMAViewStateHidden )
	{
		// hidden ad, nothing to do
		return;
	}
	
	// Closing the ad refers to restoring the default state, whatever tasks
	// need to be taken to achieve this state
	
	// notify the app that we're starting
	//[self fireAdWillClose];
	
	// closing the ad differs based on the current state
	if (_ORMMAstate == ORMMAViewStateExpanded )
	{
		// We know we're going to close our state from the expanded state.
		// So we basically want to reverse the steps we took to get to the
		// expanded state as follows: (note: we already know we're in a good
		// state to close)
		//
		// so... here's what we're going to do:
		// step 1: start a new animation, and change our frame
		// step 2: change our frame to the stored translated frame
		// step 3: wait for the animation to complete
		// step 4: restore our frame to the original untranslated frame
		// step 5: get a handle to the key window
		// step 6: get a handle to the previous parent view based on the tag
		// step 7: restore the parent view's original tag
		// step 8: add ourselves to the original parent window
		// step 9: remove the blocking view
		// step 10: fire the size changed ORMMA event
		// step 11: update the state to default
		// step 12: fire the state changed ORMMA event
		// step 13: fire the application did close delegate call
		//
		// Now, let's get started
		//[self fireAppShouldResume];
		
		// step 1: start a new animation, and change our frame
		// step 2: change our frame to the stored translated frame
		[UIView beginAnimations:kAnimationKeyCloseExpanded
						context:nil];
		[UIView setAnimationDuration:0.5];
		[UIView setAnimationDelegate:self];
        
		// step 2: change our frame to the stored translated frame
        _adView.frame = _translatedFrame;
        
		// update the web view as well
		CGRect webFrame = CGRectMake( 0, 0, _translatedFrame.size.width, _translatedFrame.size.height );
		_webView.frame = webFrame;
		
		[UIView commitAnimations];
        
		// step 3: wait for the animation to complete
		// (more happens after the animation completes)
    }
	else
	{
		// animations for resize are delegated to the application
		
		// notify the app that we are resizing
		//[self fireAdWillResizeToSize:m_defaultFrame.size];
		
		// restore the size
		_adView.frame = _defaultFrame;
		
		// update the web view as well
		CGRect webFrame = CGRectMake( 0, 0, _defaultFrame.size.width, _defaultFrame.size.height );
		_webView.frame = webFrame;
		
		// notify the app that we are resizing
		//[self fireAdDidResizeToSize:m_defaultFrame.size];
		
		// notify the app that we're done
		//[self fireAdDidClose];
		
		// update our state
		_ORMMAstate = ORMMAViewStateDefault;
		
		// notify the client
		[self executeJavascript:@"window.ormmaview.fireChangeEvent( { state: 'default', size: { width: %f, height: %f } } );", _defaultFrame.size.width, _defaultFrame.size.height ];
	}

}

- (void)openWithUrlString:(NSString *)urlString enableBack:(BOOL)back enableForward:(BOOL)forward enableRefresh:(BOOL)refresh {
    //Implement variables if/when we display a webview with navigation controls
    NSURLRequest *requestObj = [NSURLRequest requestWithURL:[NSURL URLWithString:urlString]];
    [_webView loadRequest:requestObj];
    //Ad takes control of app
}

-(void)resizeToWidth:(CGFloat)width height:(CGFloat)height { 
    // resize must work within the view hierarchy; all the ORMMA ad view does
	// is modify the frame size while leaving the containing application to 
	// determine how this should be presented (animations).
	
	// note: we can only resize if we are in the default state and only to the
	//       limit specified by the maxSize value.
	
	// verify that we can resize
	if (_ORMMAstate != ORMMAViewStateDefault )
	{
		// we can't resize an expanded ad
		[self executeJavascript:@"window.ormmaview.fireErrorEvent( 'Cannot resize an ad that is not in the default state.', 'resize' );" ]; 
		return;
	}
	
	// Make sure the resize honors our limits
	if ((height > _maxSize.height) || (width > _maxSize.width)) 
	{
		// we can't resize outside our limits
		[self executeJavascript:@"window.ormmaview.fireErrorEvent( 'Cannot resize an ad larger than allowed.', 'resize' );" ]; 
		return;
	}
	
	// store the original frame
	_defaultFrame = CGRectMake(_adView.frame.origin.x, 
                               _adView.frame.origin.y,
                               _adView.frame.size.width,
                               _adView.frame.size.height );
	
	// determine the final frame
	CGSize size = { width, height };
	
	// notify the application that we are starting to resize
	//[self fireAdWillResizeToSize:size];
	
	// now update the size
	CGRect newFrame = CGRectMake(_adView.frame.origin.x, 
                                 _adView.frame.origin.y, 
                                 width,
                                 height );
	_adView.frame = newFrame;
	
	// resize the web view as well
	newFrame.origin.x = 0;
	newFrame.origin.y = 0;
    _webView.frame = newFrame;
	
	// make sure we're on top of everything
	[_adView.superview bringSubviewToFront:_adView];
	
	// notify the application that we are done resizing
	//[self fireAdDidResizeToSize:size];
	
	// update our state
	_ORMMAstate = ORMMAViewStateResized;
	
	// send state changed event
	[self executeJavascript:@"window.ormmaview.fireChangeEvent( { state: 'resized', size: { width: %f, height: %f } } );", width, height ];
}

- (void)expandTo:(CGRect)endingFrame
		 withURL:(NSURL *)url
   blockingColor:(UIColor *)blockingColor
 blockingOpacity:(CGFloat)blockingOpacity
{
    // OK, here's what we have to do when the creative want's to expand
	// Note that this is NOT the same as resize.
	// first, since we have no idea about the surrounding view hierarchy we
	// need to pull our container to the "top" of the view hierarchy. This
	// means that we need to be able to restore ourselves when we're done, so
	// we want to remember our settings from before we kick off the expand
	// function.
	//
	// so... here's what we're going to do:
	// step 0: make sure we're in a valid state to expand
	// step 1: fire the application will expand delegate call
	// step 2: get a handle to the key window
	// step 3: store the current frame for later re-use
	// step 4: create a blocking view that fills the current window
	// step 5: store the current tag for the parent view
	// step 6: pick a random unused tag
	// step 7: change the parent view's tag to the new random tag
	// step 8: create a new frame, based on the current frame but with
	//         coordinates translated to the window space
	// step 9: store this new frame for later use
	// step 10: change our frame to the new one
	// step 11: add ourselves to the key window
	// step 12: start a new animation, and change our frame
	// step 13: wait for the animation to complete
	// step 14: fire the size changed ORMMA event
	// step 15: update the state to expanded
	// step 16: fire the state changed ORMMA event
    // step 17: fire the application did expand delegate call
	//
	// Now, let's get started
	
	// step 0: make sure we're in a valid state to expand
	if (_ORMMAstate != ORMMAViewStateDefault )
	{
		// Already Expanded
		[self executeJavascript:@"window.ormmaview.fireErrorEvent( 'Can only expand from the default state.', 'expand' );" ]; 
		return;
	}	
    
	// step 1: fire the application will expand delegate call
	//[self fireAdWillExpandToFrame:endingFrame];
	//[self fireAppShouldSuspend];
    
	// step 2: get a handle to the key window
	UIApplication *app = [UIApplication sharedApplication];
	UIWindow *keyWindow = [app keyWindow];
	
	// step 3: store the current frame for later re-use
	_defaultFrame = _adView.frame;
    
	// step 4: create a blocking view that fills the current window
	// if the status bar is visible, we need to account for it
	CGRect f = keyWindow.frame;
	UIApplication *a = [UIApplication sharedApplication];
	if ( !a.statusBarHidden )
	{
        // status bar is visible
        endingFrame.origin.y -= 20;
	}
	if (_blockingView != nil )
	{
		[_blockingView removeFromSuperview], _blockingView = nil;
	}
	_blockingView = [[UIView alloc] initWithFrame:f];
	_blockingView.backgroundColor = blockingColor;
	_blockingView.alpha = blockingOpacity;
	[keyWindow addSubview:_blockingView];
	
	// step 5: store the current tag for the parent view
	UIView *parentView = _adView;
	_originalTag = parentView.tag;
	
	// step 6: pick a random unused tag
	_parentTag = 0;
	do 
	{
		_parentTag = arc4random() % 25000;
	} while ( [keyWindow viewWithTag:_parentTag] != nil );
	
	// step 7: change the parent view's tag to the new random tag
	parentView.tag = _parentTag;
    
	// step 8: create a new frame, based on the current frame but with
	//         coordinates translated to the window space
	// step 9: store this new frame for later use
	_translatedFrame = [_adView convertRect:_defaultFrame
								   toView:keyWindow];
	
	// step 10: change our frame to the new one
	_adView.frame = _translatedFrame;
	
	// step 11: add ourselves to the key window
	[keyWindow addSubview:_adView];
	
	// step 12: start a new animation, and change our frame
	[UIView beginAnimations:kAnimationKeyExpand
					context:nil];
	[UIView setAnimationDuration:0.5];
	[UIView setAnimationDelegate:self];
	_adView.frame = endingFrame;
    
	// Create frame for web view
	CGRect webFrame = CGRectMake( 0, 0, endingFrame.size.width, endingFrame.size.height );
	_webView.frame = webFrame;
	
	[UIView commitAnimations];
	
	// step 13: wait for the animation to complete
	// (more happens after the animation completes)
}

-(CGRect)getAdFrameInWindowCoordinates {
    CGRect frame = [_webView convertRect:_webView.frame toView:_webView.window];
	return frame;
}

@end