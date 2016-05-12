/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.oneme.toplay.track.fragments;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;


import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.Polyline;



import com.oneme.toplay.R;
import com.oneme.toplay.track.CnMapOverlay;
import com.oneme.toplay.track.MarkerDetailActivity;
import com.oneme.toplay.track.TrackDetailNextActivity;
import com.oneme.toplay.track.content.Track;
import com.oneme.toplay.track.content.TrackDataHub;
import com.oneme.toplay.track.content.TrackDataListener;
import com.oneme.toplay.track.content.TrackDataType;
import com.oneme.toplay.track.content.TracksProviderUtils;
import com.oneme.toplay.track.content.TracksProviderUtils.Factory;
import com.oneme.toplay.track.content.Waypoint;
import com.oneme.toplay.track.services.CnTracksLocationManager;
import com.oneme.toplay.track.stats.TripStatistics;
import com.oneme.toplay.track.util.ApiAdapterFactory;
import com.oneme.toplay.track.util.GoogleLocationUtils;
import com.oneme.toplay.track.util.IntentUtils;
import com.oneme.toplay.track.util.LocationUtils;
import com.oneme.toplay.track.util.PreferencesUtils;
import com.oneme.toplay.track.util.TrackIconUtils;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * A fragment to display map to the user.
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class CnTracksMapFragment extends SupportMapFragment implements TrackDataListener {

  public static final String MAP_FRAGMENT_TAG = "CnTracksMapFragment";

  private static final String CURRENT_LOCATION_KEY = "current_location_key";
  private static final String
      KEEP_CURRENT_LOCATION_VISIBLE_KEY = "keep_current_location_visible_key";

  private static final float DEFAULT_ZOOM_LEVEL = 18f;

  // Google's latitude and longitude
  private static final double DEFAULT_LATITUDE = 37.423;
  private static final double DEFAULT_LONGITUDE = -122.084;

  private static final int MAP_VIEW_PADDING = 32;

  // States from TrackDetailActivity, set in onResume
  private TrackDataHub trackDataHub;

  // Current location
  private Location currentLocation;
  private Location lastTrackPoint;
  private int recordingGpsAccuracy = PreferencesUtils.RECORDING_GPS_ACCURACY_DEFAULT;

  LocationClient mLocClient;
  public MyLocationListenner myListener = new MyLocationListenner();
  private MyLocationConfiguration.LocationMode mCurrentMode;
  BitmapDescriptor mCurrentMarker;
  boolean isFirstLoc = true;

  /**
   * True to continue keeping the current location visible on the screen.
   * <p>
   * Set to true when <br>
   * 1. user clicks on the my location button <br>
   * 2. first location during a recording <br>
   * Set to false when <br>
   * 1. showing a marker <br>
   * 2. user manually zooms/pans
   */
  private boolean keepCurrentLocationVisible;
  private CnTracksLocationManager myCnTracksLocationManager;

  // BDLocationListener for periodic location request
  private BDLocationListener bdlocationListener;

 // private OnLocationChangedListener onLocationChangedListener;

  // Current track
  private Track currentTrack;

  // Current paths
  private ArrayList<Polyline> paths = new ArrayList<Polyline>();
  boolean reloadPaths = true;

  // UI elements
  private BaiduMap mBaiduMap;
  private CnMapOverlay mapOverlay;
  private View mapView;
  private ImageButton myLocationImageButton;
  private TextView messageTextView;
  private MapView mMapView;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setHasOptionsMenu(true);
    mapOverlay = new CnMapOverlay(getActivity());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mapView = super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.ome_track_bd_map, container, false);
   // RelativeLayout mapContainer = (RelativeLayout) layout.findViewById(R.id.bd_map_container);
    mMapView = (MapView)layout.findViewById(R.id.bmapView);
    //mBaiduMap = mMapView.getMap();

    //mapContainer.addView(mapView, 0);

    mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;


   // mLocClient = new LocationClient(getActivity());
   // mLocClient.registerLocationListener(myListener);
   // LocationClientOption option = new LocationClientOption();
   // option.setOpenGps(true);
   // option.setCoorType("bd09ll");
   // option.setScanSpan(1000);
   // mLocClient.setLocOption(option);
   // mLocClient.start();

    /*
     * For Froyo (2.2) and Gingerbread (2.3), need a transparent FrameLayout on
     * top for view pager to work correctly.
     */
    FrameLayout frameLayout = new FrameLayout(getActivity());
    frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
  //  mapContainer.addView(frameLayout,
  //      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    myLocationImageButton = (ImageButton) layout.findViewById(R.id.bd_map_my_location);


    messageTextView = (TextView) layout.findViewById(R.id.map_message);
    return layout;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      keepCurrentLocationVisible = savedInstanceState.getBoolean(
          KEEP_CURRENT_LOCATION_VISIBLE_KEY, false);
      if (keepCurrentLocationVisible) {
        Location location = (Location) savedInstanceState.getParcelable(CURRENT_LOCATION_KEY);
        if (location != null) {
          setCurrentLocation(location);
        }
      }
    }

    /*
     * At this point, after onCreateView, getMap will not return null and we can
     * initialize googleMap. However, onActivityCreated can be called multiple
     * times, e.g., when the user switches tabs. With
     * GoogleMapOptions.useViewLifecycleInFragment == false, googleMap lifecycle
     * is tied to the fragment lifecycle and the same googleMap object is
     * returned in getMap. Thus we only need to initialize googleMap once, when
     * it is null.
     */
    if (mBaiduMap == null) {
      mBaiduMap = mMapView.getMap();
      mBaiduMap.setMyLocationEnabled(true);


      /*
       * My Tracks needs to handle the onClick event when the my location button
       * is clicked. Currently, the API doesn't allow handling onClick event,
       * thus hiding the default my location button and providing our own.
       */
     // googleMap.getUiSettings().setMyLocationButtonEnabled(false);
     // googleMap.setIndoorEnabled(true);
      mBaiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {

          @Override
        public boolean onMarkerClick(Marker marker) {
          if (isResumed()) {
            String title = marker.getTitle();
            if (title != null && title.length() > 0) {
              long id = Long.valueOf(title);
              Context context = getActivity();
              Intent intent = IntentUtils.newIntent(context, MarkerDetailActivity.class)
                  .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, id);
              context.startActivity(intent);
            }
          }
          return true;
        }
      });
    //  googleMap.setLocationSource(new LocationSource() {

    //      @Override
    //    public void activate(OnLocationChangedListener listener) {
    //      onLocationChangedListener = listener;
    //    }

    //      @Override
    //    public void deactivate() {
    //      onLocationChangedListener = null;
    //    }
    //  });
    //  googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {

    //      @Override
    //    public void onCameraChange(CameraPosition cameraPosition) {
    //      if (isResumed() && keepCurrentLocationVisible && currentLocation != null
    //          && !isLocationVisible(currentLocation)) {
    //        keepCurrentLocationVisible = false;
    //      }
    //    }
    //  });
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // First obtain the states from TrackDetailActivity
    long trackId = ((TrackDetailNextActivity) getActivity()).getTrackId();
    long markerId = ((TrackDetailNextActivity) getActivity()).getMarkerId();
    resumeTrackDataHub();

    myCnTracksLocationManager = new CnTracksLocationManager(getActivity(), Looper.myLooper(), true);
    boolean isGpsProviderEnabled = myCnTracksLocationManager.isGpsProviderEnabled();

    if (mBaiduMap != null) {

      // Disable my location if gps is disabled
      mBaiduMap.setMyLocationEnabled(isGpsProviderEnabled);
    }

    // setWarningMessage depends on resumeTrackDataHub being invoked beforehand
    setWarningMessage(isGpsProviderEnabled);

    currentTrack = Factory.get(getActivity()).getTrack(trackId);
    mapOverlay.setShowEndMarker(!isSelectedTrackRecording());
    if (markerId != -1L) {
      showMarker(markerId);
    } else {
      if (keepCurrentLocationVisible && currentLocation != null && isSelectedTrackRecording()) {
        updateCurrentLocation(true);
      } else {
        /*
         * Clear the current location in case the current location is no longer
         * being updated continuously.
         */
       // if (onLocationChangedListener != null) {
        //  onLocationChangedListener.onLocationChanged(new Location(""));
        //}
        showTrack();
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean(KEEP_CURRENT_LOCATION_VISIBLE_KEY, keepCurrentLocationVisible);
    if (currentLocation != null) {
      /*
       * currentLocation is a MyTracksLocation object, which cannot be
       * unmarshalled. Thus creating a Location object before placing it in the
       * bundle.
       */
      outState.putParcelable(CURRENT_LOCATION_KEY, new Location(currentLocation));
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
    if (bdlocationListener != null) {
      myCnTracksLocationManager.removeLocationUpdates(bdlocationListener);
      bdlocationListener = null;
    }
    myCnTracksLocationManager.close();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflator) {
    menuInflator.inflate(R.menu.map, menu);
    TrackIconUtils.setMenuIconColor(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    if (menuItem.getItemId() == R.id.menu_map_layer) {
      ((TrackDetailNextActivity) getActivity()).showMapLayerDialog();
      return true;
    }
    return super.onOptionsItemSelected(menuItem);
  }

  @Override
  public void onTrackUpdated(Track track) {
    currentTrack = track;
  }

  @Override
  public void clearTrackPoints() {
    lastTrackPoint = null;
    if (isResumed()) {
      mapOverlay.clearPoints();
      reloadPaths = true;
    }
  }

  @Override
  public void onSampledInTrackPoint(final Location location) {
    lastTrackPoint = location;
    if (isResumed()) {
      mapOverlay.addLocation(location);
    }
  }

  @Override
  public void onSampledOutTrackPoint(Location location) {
    lastTrackPoint = location;
  }

  @Override
  public void onSegmentSplit(Location location) {
    if (isResumed()) {
      mapOverlay.addSegmentSplit();
    }
  }

  @Override
  public void onNewTrackPointsDone() {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        public void run() {
          if (isResumed() && mBaiduMap != null && currentTrack != null) {
            boolean hasStartMarker = mapOverlay.update(
                    mBaiduMap, paths, currentTrack.getTripStatistics(), reloadPaths);

            /*
             * If has the start marker, then don't need to reload the paths each
             * time
             */
            if (hasStartMarker) {
              reloadPaths = false;
            }

            if (lastTrackPoint != null && isSelectedTrackRecording()) {
              boolean firstLocation = setCurrentLocation(lastTrackPoint);
              if (firstLocation) {
                keepCurrentLocationVisible = true;
              }
              updateCurrentLocation(firstLocation);
              setWarningMessage(true);
            }
          }
        }
      });
    }
  }

  @Override
  public void clearWaypoints() {
    if (isResumed()) {
      mapOverlay.clearWaypoints();
    }
  }

  @Override
  public void onNewWaypoint(Waypoint waypoint) {
    if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
      mapOverlay.addWaypoint(waypoint);
    }
  }

  @Override
  public void onNewWaypointsDone() {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        public void run() {
          if (isResumed() && mBaiduMap != null && currentTrack != null) {
            mapOverlay.update(mBaiduMap, paths, currentTrack.getTripStatistics(), true);
          }
        }
      });
    }
  }

  @Override
  public boolean onMetricUnitsChanged(boolean metric) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onReportSpeedChanged(boolean reportSpeed) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onRecordingGpsAccuracy(int newValue) {
    recordingGpsAccuracy = newValue;
    return false;
  }

  @Override
  public boolean onRecordingDistanceIntervalChanged(int minRecordingDistance) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onMapTypeChanged(final int mapType) {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        public void run() {
          if (isResumed() && mBaiduMap != null) {
            mBaiduMap.setMapType(mapType);
          }
        }
      });
    }
    return false;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((TrackDetailNextActivity) getActivity()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.TRACKS_TABLE,
        TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
        TrackDataType.SAMPLED_OUT_TRACK_POINTS_TABLE, TrackDataType.PREFERENCE));
  }

  /**
   * Pauses the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void pauseTrackDataHub() {
    if (trackDataHub != null) {
      trackDataHub.unregisterTrackDataListener(this);
    }
    trackDataHub = null;
  }

  /**
   * Returns true if the selected track is recording. Needs to be synchronized
   * because the trackDataHub can be accessed by multiple threads.
   */
  private synchronized boolean isSelectedTrackRecording() {
    return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
  }

  /**
   * Sets the current location.
   *
   * @param location the location
   * @return true if this is the first location
   */
  private boolean setCurrentLocation(Location location) {
    boolean isFirst = false;
    if (currentLocation == null && location != null) {
      isFirst = true;
    }
    currentLocation = location;
    return isFirst;
  }

  /**
   * Updates the current location.
   *
   * @param forceZoom true to force zoom to the current location regardless of
   *          the keepCurrentLocationVisible policy
   */
  private void updateCurrentLocation(final boolean forceZoom) {
    getActivity().runOnUiThread(new Runnable() {
      public void run() {
        if (!isResumed() || mBaiduMap == null
            || currentLocation == null) { //|| onLocationChangedListener == null
          return;
        }
      //  onLocationChangedListener.onLocationChanged(currentLocation);
        if (forceZoom || (keepCurrentLocationVisible && !isLocationVisible(currentLocation))) {
          LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
         // CameraUpdate cameraUpdate = forceZoom ? CameraUpdateFactory.newLatLngZoom(
         //         latLng, DEFAULT_ZOOM_LEVEL)
         //     : CameraUpdateFactory.newLatLng(latLng);
         // googleMap.animateCamera(cameraUpdate);
        }
      };
    });
  }

  /**
   * Shows the current track by moving the camera over the track.
   */
  private void showTrack() {
    if (mBaiduMap == null || currentTrack == null) {
      return;
    }
    if (currentTrack.getNumberOfPoints() < 2) {
     // googleMap.moveCamera(
     //     CameraUpdateFactory.newLatLngZoom(getDefaultLatLng(), googleMap.getMinZoomLevel()));
      return;
    }

    if (mapView == null) {
      return;
    }

    if (mapView.getWidth() == 0 || mapView.getHeight() == 0) {
      if (mapView.getViewTreeObserver().isAlive()) {
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
          public void onGlobalLayout() {
            if (!isResumed()) {
              return;
            }
            ApiAdapterFactory.getApiAdapter()
                    .removeGlobalLayoutListener(mapView.getViewTreeObserver(), this);
            getActivity().runOnUiThread(new Runnable() {
                @Override
              public void run() {
                if (isResumed()) {
                  moveCameraOverTrack();
                }
              }
            });
          }
        });
      }
      return;
    }
    moveCameraOverTrack();
  }

  /**
   * Moves the camera over the current track.
   */
  private void moveCameraOverTrack() {
    /**
     * Check all the required variables.
     */
    if (mBaiduMap == null || currentTrack == null || currentTrack.getNumberOfPoints() < 2
        || mapView == null || mapView.getWidth() == 0 || mapView.getHeight() == 0) {
      return;
    }

    TripStatistics tripStatistics = currentTrack.getTripStatistics();
    int latitudeSpanE6 = tripStatistics.getTop() - tripStatistics.getBottom();
    int longitudeSpanE6 = tripStatistics.getRight() - tripStatistics.getLeft();
    if (latitudeSpanE6 > 0 && latitudeSpanE6 < 180E6 && longitudeSpanE6 > 0
        && longitudeSpanE6 < 360E6) {
      LatLng southWest = new LatLng(
          tripStatistics.getBottomDegrees(), tripStatistics.getLeftDegrees());
      LatLng northEast = new LatLng(
              tripStatistics.getTopDegrees(), tripStatistics.getRightDegrees());
      //LatLngBounds bounds = LatLngBounds.Builder();//.include(southWest).include(northEast).build();

      /**
       * Note cannot call CameraUpdateFactory.newLatLngBounds(LatLngBounds
       * bounds, int padding) if the map view has not undergone layout. Thus
       * calling CameraUpdateFactory.newLatLngBounds(LatLngBounds bounds, int
       * width, int height, int padding) after making sure that mapView is valid
       * in the above code.
       */
     // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(
     //         bounds, mapView.getWidth(), mapView.getHeight(), MAP_VIEW_PADDING);
     // googleMap.moveCamera(cameraUpdate);
    }
  }

  /**
   * Shows a marker by moving the camera over the marker.
   *
   * @param id the marker id
   */
  private void showMarker(final long id) {
    getActivity().runOnUiThread(new Runnable() {
        @Override
      public void run() {
        if (!isResumed() || mBaiduMap == null) {
          return;
        }
        TracksProviderUtils MyTracksProviderUtils = Factory.get(getActivity());
        Waypoint waypoint = MyTracksProviderUtils.getWaypoint(id);
        if (waypoint == null) {
          return;
        }
        Location location = waypoint.getLocation();
        if (location == null) {
          return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        keepCurrentLocationVisible = false;
       // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL);
       // googleMap.moveCamera(cameraUpdate);
      }
    });
  }

  /**
   * Gets the default LatLng.
   */
  private LatLng getDefaultLatLng() {
    TracksProviderUtils myTracksProviderUtils = Factory.get(getActivity());
    Location location = myTracksProviderUtils.getLastValidTrackPoint();
    if (location != null) {
      return new LatLng(location.getLatitude(), location.getLongitude());
    }
    return new LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
  }

  /**
   * Returns true if the location is visible. Needs to run on the UI thread.
   * 
   * @param location the location
   */
  private boolean isLocationVisible(Location location) {
    if (location == null || mBaiduMap == null) {
      return false;
    }
    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
    return false;//googleMap.getProjection().getVisibleRegion().latLngBounds.contains(latLng);
  }

  /**
   * Sets the warning message.
   * 
   * @param isGpsProviderEnabled true if gps provider is enabled
   */
  private void setWarningMessage(boolean isGpsProviderEnabled) {
    String message = getWarningMessage(isGpsProviderEnabled);

    if (message == null) {
      messageTextView.setVisibility(View.GONE);
      return;
    }
    messageTextView.setText(message);
    messageTextView.setVisibility(View.VISIBLE);
    if (isGpsProviderEnabled) {
      messageTextView.setOnClickListener(null);
    } else {
      Toast.makeText(getActivity(), R.string.gps_not_found, Toast.LENGTH_LONG).show();

      // Click to show the location source settings
      messageTextView.setOnClickListener(new OnClickListener() {

          @Override
        public void onClick(View v) {
          if (isResumed()) {
            startActivity(GoogleLocationUtils.newLocationSettingsIntent(getActivity()));
          }
        }
      });
    }
  }

  /**
   * Gets the warning message.
   * 
   * @param isGpsProviderEnabled true if gps provider is enabled
   */
  private String getWarningMessage(boolean isGpsProviderEnabled) {
    if (!isSelectedTrackRecording()) {
      return null;
    }
    if (!isGpsProviderEnabled) {
      return GoogleLocationUtils.getGpsDisabledMessage(getActivity());
    }

    boolean hasFix;
    boolean hasGoodFix;
    if (currentLocation == null) {
      hasFix = false;
      hasGoodFix = false;
    } else {
      hasFix = !LocationUtils.isLocationOld(currentLocation);
      hasGoodFix = currentLocation.hasAccuracy()
          && currentLocation.getAccuracy() < recordingGpsAccuracy;
    }
    if (!hasFix) {
      return getString(R.string.gps_wait_for_signal);
    } else if (!hasGoodFix) {
      return getString(R.string.gps_wait_for_better_signal);
    } else {
      return null;
    }
  }

  @Override
  public void onDestroy() {
    // 退出时销毁定位
    //mLocClient.stop();
    // 关闭定位图层
    mBaiduMap.setMyLocationEnabled(false);
    mMapView.onDestroy();
    mMapView = null;
    super.onDestroy();
  }

  /**
   * 定位SDK监听函数
   */
  public class MyLocationListenner implements BDLocationListener {

    @Override
    public void onReceiveLocation(BDLocation location) {
      // map view 销毁后不在处理新接收的位置
      if (location == null || mMapView == null)
        return;
      MyLocationData locData = new MyLocationData.Builder()
              .accuracy(location.getRadius())
                      // 此处设置开发者获取到的方向信息，顺时针0-360
              .direction(100).latitude(location.getLatitude())
              .longitude(location.getLongitude()).build();
      mBaiduMap.setMyLocationData(locData);
      if (isFirstLoc) {
        isFirstLoc = false;
        LatLng ll = new LatLng(location.getLatitude(),
                location.getLongitude());
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
        mBaiduMap.animateMapStatus(u);
      }
    }

    public void onReceivePoi(BDLocation poiLocation) {
    }
  }
}