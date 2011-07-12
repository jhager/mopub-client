//
//  MPORMMAAdapter.h
//  SimpleAds
//
//  Created by Haydn Dufrene on 7/8/11.
//  Copyright 2011 Mopub/Stanford. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "MPBaseAdapter.h"
#import "ORMMAView.h"

@interface MPORMMAAdapter : MPBaseAdapter <ORMMAViewDelegate> {
    ORMMAView *_ORMMAView;
}

@end
