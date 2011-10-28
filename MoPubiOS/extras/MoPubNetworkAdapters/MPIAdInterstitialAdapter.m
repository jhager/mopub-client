//
//  MPIAdInterstitialAdapter.m
//  MoPub
//
//  Created by Haydn Dufrene on 10/28/11.
//  Copyright 2011 MoPub. All rights reserved.
//

#import "MPIAdInterstitialAdapter.h"
#import "MPAdView.h"
#import "MPLogging.h"
#import "CJSONDeserializer.h"

@implementation MPIAdInterstitialAdapter

- (void)getAdWithParams:(NSDictionary *)params
{		
	_iAdInterstitialAdView = [[ADInterstitialAd alloc] init];
  _iAdInterstitialAdView.delegate = self;
}

- (void)dealloc {
  _iAdInterstitialAdView.delegate = nil;
  [_iAdInterstitialAdView release];
  [super dealloc];
}

- (void)showInterstitialFromViewController:(UIViewController *)controller
{
	[_iAdInterstitialAdView presentFromViewController:controller];
}

- (void)interstitialAdDidUnload:(ADInterstitialAd *)interstitialAd {
	[_interstitialAdController interstitialWillDisappearForAdapter:self];
	[_interstitialAdController interstitialDidDisappearForAdapter:self];
}

- (void)interstitialAd:(ADInterstitialAd *)interstitialAd didFailWithError:(NSError *)error {
  [_interstitialAdController adapter:self didFailToLoadAdWithError:error];
  
}

- (void)interstitialAdDidLoad:(ADInterstitialAd *)interstitialAd {
  [_interstitialAdController adapterDidFinishLoadingAd:self];
}

@end
