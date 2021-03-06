//
//  OMEInvite.h
//  ToPlay
//
//  Created by Ozzie Zhang on 8/15/14.
//  Copyright (c) 2014 OneME. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <MapKit/MapKit.h>
#import <Parse/Parse.h>

@interface OMEInvite : NSObject <MKAnnotation>

//@protocol MKAnnotation <NSObject>

// Center latitude and longitude of the annotion view.
// The implementation of this property must be KVO compliant.
@property (nonatomic, readonly) CLLocationCoordinate2D coordinate;

// @optional
// Title and subtitle for use by selection UI.
@property (nonatomic, readonly, copy) NSString *title;
@property (nonatomic, readonly, copy) NSString *subtitle;

@property (nonatomic, readonly, copy) NSString *sportType;
@property (nonatomic, readonly, copy) NSString *time;

// @end

// Other properties:
@property (nonatomic, readonly, strong) PFObject *object;
@property (nonatomic, readonly, strong) PFGeoPoint *geopoint;
@property (nonatomic, readonly, strong) PFUser *user;
@property (nonatomic, assign) BOOL animatesDrop;
@property (nonatomic, readonly) MKPinAnnotationColor pinColor;

// Designated initializer.
- (id)initWithCoordinate:(CLLocationCoordinate2D)coordinate
				andTitle:(NSString *)title
			 andSubtitle:(NSString *)subtitle
	   andSportTypeTitle:(NSString *)sporttypetitle
			andTimeTitle:(NSString *)timetitle;
- (id)initWithPFObject:(PFObject *)object;
- (BOOL)equalToInvite:(OMEInvite *)invite;

- (void)setTitleAndSubtitleOutsideDistance:(BOOL)outside;

@end
