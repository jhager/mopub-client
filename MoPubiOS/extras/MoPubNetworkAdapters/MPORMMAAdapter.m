//
//  MPORMMAAdapter.m
//  SimpleAds
//
//  Created by Haydn Dufrene on 7/8/11.
//  Copyright 2011 Mopub/Stanford. All rights reserved.
//

#import "MPORMMAAdapter.h"
#import "MPAdManager+MPAdaptersPrivate.h"
#import "MPAdManager.h"
#import "MPAdView.h"

@implementation MPORMMAAdapter


- (id)initWithAdManager:(MPAdManager *)adManager
{
	if (self = [super initWithAdManager:adManager])
	{
        _ORMMAView = [[ORMMAView alloc] initWithFrame:adManager.adView.frame];
        _ORMMAView.ormmaDelegate = self;
        _ORMMAView.maxSize = CGSizeMake(320, 480);
        [_ORMMAView loadCreative:adManager.URL];
        [adManager.adView setAdContentView:_ORMMAView];
	}
	return self;
}

-(void)dealloc {
    [super dealloc];
}

#pragma mark - 
#pragma mark ORMMAViewDelegate Methods

-(UIViewController *)ormmaViewController {
    return [_adManager.adView.delegate viewControllerForPresentingModalView];
}

- (void)failureLoadingAd:(ORMMAView *)adView{
    [self.adManager adapter:self didFailToLoadAdWithError:nil];
}

// Called before the ad is resized in place to allow the parent application to
// animate things if desired.
- (void)willResizeAd:(ORMMAView *)adView
			  toSize:(CGSize)size{
    if ([_adManager.adView.delegate respondsToSelector:@selector(adView:willResizeTo:)]) {
        [_adManager.adView.delegate adView:_adManager.adView willResizeTo:size];
    } 
}
// Called after the ad is resized in place to allow the parent application to
// animate things if desired.
- (void)didResizeAd:(ORMMAView *)adView
             toSize:(CGSize)size{
    [self.adManager userActionWillBeginForAdapter:self];
}

// Called just before to an ad is displayed
- (void)adWillShow:(ORMMAView *)adView{
    _adManager.isLoading = NO;
    [self.adManager adapterDidFinishLoadingAd:self shouldTrackImpression:YES];
}

// Called just after to an ad is displayed
- (void)adDidShow:(ORMMAView *)adView{
    
}

// Called just before to an ad is Hidden
- (void)adWillHide:(ORMMAView *)adView{
    
}

// Called just after to an ad is Hidden
- (void)adDidHide:(ORMMAView *)adView{
    
}

// Called just before an ad expands
- (void)willExpandAd:(ORMMAView *)adView
			 toFrame:(CGRect)frame{
    [self.adManager userActionWillBeginForAdapter:self];
    [_ORMMAView setFrame:frame];
}

// Called just after an ad expands
- (void)didExpandAd:(ORMMAView *)adView
			toFrame:(CGRect)frame{
    if (![[UIApplication sharedApplication] isStatusBarHidden]) {
        [[UIApplication sharedApplication] setStatusBarHidden:YES];
        _statusBarShouldShow = YES;
    }
}

- (void)showURLFullScreen:(NSURL *)url
			   sourceView:(UIView *)view {
    [self.adManager userActionWillBeginForAdapter:self];
    [self.adManager userWillLeaveApplicationFromAdapter:self];
    [[UIApplication sharedApplication] openURL:url]; 
}

// Called just before an ad closes
- (void)adWillClose:(ORMMAView *)adView{
    if (_statusBarShouldShow) {
        [[UIApplication sharedApplication] setStatusBarHidden:NO];
    }
}

// Called just after an ad closes
- (void)adDidClose:(ORMMAView *)adView{
    [self.adManager userActionDidEndForAdapter:self];
}

@end
