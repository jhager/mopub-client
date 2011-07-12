/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import "ORMMAAVPlayer.h"
#include <objc/runtime.h>


@implementation LoadingView

-(id)initWithFrame:(CGRect)frame
{
	if (self = [super initWithFrame:frame]) {
		self.backgroundColor = [UIColor blackColor];
		
		actIndicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
		actIndicator.frame = CGRectMake((frame.size.width-20)/2.0, (frame.size.height-20)/2.0, 20, 20);
		[actIndicator startAnimating];
		[self addSubview:actIndicator];
	}
	return self;
}

-(void)dealloc
{
	[actIndicator release];
	[super dealloc];
}

@end

@implementation ORMMAAVPlayer
@synthesize delegate;
@synthesize ormmaPlayer;

-(void)playAudio:(NSURL *)audioURL attachTo:(UIView*)parentView autoPlay:(BOOL)autoplay showControls:(BOOL)showcontrols repeat:(BOOL)autorepeat playInline:(BOOL)Inline fullScreenMode:(BOOL)fullScreen autoExit:(BOOL)exit
{
	isAudio = YES;
	oldStyle = [UIApplication sharedApplication].statusBarStyle;
	self.backgroundColor = [UIColor blackColor];

	avPlayer = [[MPMoviePlayerViewController alloc] initWithContentURL:audioURL];
	self.ormmaPlayer = avPlayer.moviePlayer;
	

	self.ormmaPlayer.controlStyle = showcontrols ? MPMovieControlStyleFullscreen : MPMovieControlStyleNone;
	self.ormmaPlayer.repeatMode = autorepeat ? MPMovieRepeatModeOne : MPMovieRepeatModeNone;	
	self.ormmaPlayer.shouldAutoplay = autoplay;
	self.ormmaPlayer.view.frame = [self frame];
	
	//call to show the loading screen consisting activity indicator
	[self showLoadingScreen:ormmaPlayer.view.frame];
	
	[[NSNotificationCenter defaultCenter] addObserver:self 
											 selector:@selector(moviePlayerLoadStateChanged:) 
												 name:MPMoviePlayerLoadStateDidChangeNotification 
											   object:nil];		
	[self.ormmaPlayer prepareToPlay];
	
	[[NSNotificationCenter defaultCenter] addObserver:self 
											 selector:@selector(moviePlayBackDidFinish:) 
												 name:MPMoviePlayerPlaybackDidFinishNotification 
											   object:nil];
	autoPlay = autoplay;
	exitOnComplete = exit;
	inlinePlayer = Inline;
}

-(void)playVideo:(NSURL *)videoURL attachTo:(UIView*)parentView autoPlay:(BOOL)autoplay showControls:(BOOL)showcontrols repeat:(BOOL)autorepeat fullScreenMode:(BOOL)fullScreen autoExit:(BOOL)exit
{	
	isAudio = NO;
	oldStyle = [UIApplication sharedApplication].statusBarStyle;
	inlinePlayer = NO;
	self.backgroundColor = [UIColor blackColor];
	ormmaPlayer = [[MPMoviePlayerController alloc] initWithContentURL:videoURL];
	ormmaPlayer.scalingMode = MPMovieScalingModeAspectFit;

	ormmaPlayer.controlStyle = showcontrols ? MPMovieControlStyleFullscreen : MPMovieControlStyleNone;
	ormmaPlayer.repeatMode = autorepeat ? MPMovieRepeatModeOne : MPMovieRepeatModeNone;	
	ormmaPlayer.shouldAutoplay = autoplay;
	isFullScreen = fullScreen;
	if (isFullScreen) 
	{
		ormmaPlayer.view.frame = [UIScreen mainScreen].bounds;
	}
	else 
	{
		ormmaPlayer.view.frame = [self frame];
		
		CGRect frameRect = ormmaPlayer.view.frame;
		if (frameRect.origin.y < 20) 
		{
			frameRect.origin.y = 20;
		} 
		ormmaPlayer.view.frame = frameRect;
		
		[[NSNotificationCenter defaultCenter] addObserver:self
												 selector:@selector(movingScalingModeChanged) name:MPMoviePlayerScalingModeDidChangeNotification 
												   object:nil];		
	}
	//call to show the loading screen consisting activity indicator
	[self showLoadingScreen:ormmaPlayer.view.frame];
	
	statusBarAvailable = [[UIApplication sharedApplication] isStatusBarHidden];
	[[NSNotificationCenter defaultCenter] addObserver:self 
											 selector:@selector(moviePlayerLoadStateChanged:) 
												 name:MPMoviePlayerLoadStateDidChangeNotification 
											   object:nil];
	[ormmaPlayer prepareToPlay];
	[[NSNotificationCenter defaultCenter] addObserver:self 
											 selector:@selector(moviePlayBackDidFinish:) 
												 name:MPMoviePlayerPlaybackDidFinishNotification 
											   object:nil];
	autoPlay = autoplay;
	exitOnComplete = exit;
}

-(void)movingScalingModeChanged
{
	if (!isFullScreen) {
		ormmaPlayer.view.frame = [UIScreen mainScreen].bounds;
		isFullScreen = YES;
	}
	else 
	{
		ormmaPlayer.view.frame = [self frame];
		CGRect frameRect = ormmaPlayer.view.frame;
		if (frameRect.origin.y < 20) 
		{
			frameRect.origin.y = 20;
		} 
		ormmaPlayer.view.frame = frameRect;
		isFullScreen = NO;
	}
}

- (void) moviePlayerLoadStateChanged:(NSNotification*)notification 
{
	if ([ormmaPlayer loadState] != MPMovieLoadStateUnknown)
	{
		[[NSNotificationCenter 	defaultCenter] removeObserver:self 
														 name:MPMoviePlayerLoadStateDidChangeNotification 
													   object:nil];
		[loadingView removeFromSuperview];
		[loadingView release];
		loadingView = nil;
		
        if (!statusBarAvailable && !isAudio) 
        {
            [[UIApplication sharedApplication] setStatusBarHidden:YES];
        }
		
		if (autoPlay) 
		{
			// Play the movie
			[ormmaPlayer play];			
		}

		if (!inlinePlayer) 
		{
			UIWindow* window = [[UIApplication sharedApplication] keyWindow];
			[window addSubview:ormmaPlayer.view];	
			[window bringSubviewToFront:ormmaPlayer.view];			
		}
	}
	else 
	{
		NSLog(@"Player received unknown error");
	}
}

- (void) moviePlayBackDidFinish:(NSNotification*)notification
{
	NSDictionary* userinfo = [notification userInfo];
	NSLog(@"%@",userinfo);
	NSNumber* status = [userinfo objectForKey:MPMoviePlayerPlaybackDidFinishReasonUserInfoKey];
	if ([status intValue] == MPMovieFinishReasonPlaybackError) 
	{
		if (loadingView) 
		{
			[loadingView removeFromSuperview];
			[loadingView release];
			loadingView = nil;
		}
		
		[ormmaPlayer release];
		ormmaPlayer = nil;
		if(self.delegate)
		{
			[self.delegate playerCompleted];
		}	
		[[NSNotificationCenter defaultCenter] removeObserver:self];
	}
	else if ([status intValue] == MPMovieFinishReasonUserExited || (exitOnComplete && [status intValue] == MPMovieFinishReasonPlaybackEnded))
	{
		[[NSNotificationCenter defaultCenter] removeObserver:self];
		
		[ormmaPlayer stop];
		if (!inlinePlayer) 
		{
			[ormmaPlayer.view removeFromSuperview];			
		}
		if (!statusBarAvailable) 
        {
            [[UIApplication sharedApplication] setStatusBarHidden:NO];
            [[UIApplication sharedApplication] setStatusBarStyle:oldStyle];
        }
		ormmaPlayer.initialPlaybackTime = -1;
		[ormmaPlayer release];
		ormmaPlayer = nil;
		if(self.delegate)
		{
			[self.delegate playerCompleted];
		}
	}
}

- (void)dealloc {		
	if (avPlayer) 
	{
		[avPlayer release];
		avPlayer = nil;		
	}
    [super dealloc];
}

-(void)showLoadingScreen:(CGRect)frame
{
	loadingView = [[LoadingView alloc] initWithFrame:frame];
	UIWindow* window = [[UIApplication sharedApplication] keyWindow];
	[window addSubview:loadingView];
}

@end
