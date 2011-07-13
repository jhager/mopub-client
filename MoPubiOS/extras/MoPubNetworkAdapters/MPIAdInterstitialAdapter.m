//
//  MPIAdAdapter.m
//  MoPub
//
//  Created by Nafis Jamal on 1/19/11.
//  Copyright 2011 MoPub, Inc. All rights reserved.
//

#import "MPIAdInterstitialAdapter.h"
#import "MPAdManager.h"
#import "MPAdView.h"
#import "MPLogging.h"
#import "MPAdManager+MPAdaptersPrivate.h"

@implementation MPIAdInterstitialAdapter

- (void)getAdWithParams:(NSDictionary *)params
{
	Class cls = NSClassFromString(@"ADInterstitialAd");
	if (cls != nil) 
	{		
		_interstitial = [[ADInterstitialAd alloc] init];
        _interstitial.delegate = self;
	} 
	else 
	{
		// iAd not supported in iOS versions before 4.0.
        [_interstitialAdController adapter:self didFailToLoadAdWithError:nil];
	}
}

- (void)dealloc
{
	if (_interstitial.delegate == self) _interstitial.delegate = nil;
	[_interstitial release];
	[super dealloc];
}

- (void)showInterstitialFromViewController:(UIViewController *)controller
{
    if (_interstitial.loaded) {
        [_interstitial presentFromViewController:controller];
    } else {
        MPLogInfo(@"iAd was not prepared to load yet. Wait for interstitialDidLoadAd call.");
    }
}

#pragma mark -
#pragma	mark ADInterstitialAdDelegate Methods

- (void)interstitialAdDidUnload:(ADInterstitialAd *)interstitialAd {
    
}

- (void)interstitialAdDidLoad:(ADInterstitialAd *)interstitialAd{
    [_interstitialAdController adapterDidFinishLoadingAd:self];
}

// This method will be invoked when an error has occurred attempting to get ad content. 
// The ADError enum lists the possible error codes.
- (void)interstitialAd:(ADInterstitialAd *)interstitialAd didFailWithError:(NSError *)error {
    MPLogInfo(@"iAd failed in trying to load or refresh an ad.");
    
	[_interstitialAdController adapter:self didFailToLoadAdWithError:error];
}

// This message will be sent when the user chooses to interact with the ad unit for the interstitial ad.
// The delegate may return NO to block the action from taking place, but this
// should be avoided if possible because most advertisements pay significantly more when 
// the action takes place and, over the longer term, repeatedly blocking actions will 
// decrease the ad inventory available to the application. Applications should reduce
// their own activity while the advertisement's action executes.
- (BOOL)interstitialAdActionShouldBegin:(ADInterstitialAd *)interstitialAd willLeaveApplication:(BOOL)willLeave {
    MPLogInfo(@"iAd should begin interstitial action.");
	return YES;
}

// This message is sent when the action has completed and control is returned to the application. 
// Games, media playback, and other activities that were paused in response to the beginning
// of the action should resume at this point.
- (void)interstitialAdActionDidFinish:(ADInterstitialAd *)interstitialAd {
    MPLogInfo(@"iAd finished executing interstitial action.");
}

@end