//
//  MPIAdInterstitialAdapter.h
//  MoPubTests
//
//  Created by Haydn Dufrene on 7/12/11.
//  Copyright 2011 Mopub/Stanford. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "MPBaseInterstitialAdapter.h"
#import <iAd/iAd.h>

@interface MPIAdInterstitialAdapter : MPBaseInterstitialAdapter <ADInterstitialAdDelegate> {
    ADInterstitialAd *_interstitial;
}
@end
