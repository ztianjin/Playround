/*
* Copyright 2014 OneME
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.oneme.toplay.local;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.oneme.toplay.Application;
import com.oneme.toplay.base.AppConstant;
import com.oneme.toplay.base.InviteToIntentExtra;
import com.oneme.toplay.base.LoadImageFromParseCloud;
import com.oneme.toplay.base.third.RoundedTransformationBuilder;
import com.oneme.toplay.base.Time;
import com.oneme.toplay.CnMapActivity;
import com.oneme.toplay.database.Invite;
import com.oneme.toplay.database.Sport;
import com.oneme.toplay.invite.InviteNextActivity;
import com.oneme.toplay.join.JoinNextActivity;
import com.oneme.toplay.LoginActivity;
import com.oneme.toplay.me.MeActivity;
import com.oneme.toplay.MessageListActivity;
import com.oneme.toplay.R;
import com.oneme.toplay.service.CoreService;
import com.oneme.toplay.weather.RemoteFetch;
import com.oneme.toplay.weather.WeatherActivity;

import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.map.Circle;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.SupportMapFragment;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;


public class CnLocalWithoutMapActivity extends ActionBarActivity {

    private static final String TAG = "CnLocalWithoutMapActivity";

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;

    // Ozzie Zhang 2014-11-07 modify interval values
    // The update interval default value is 5
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    // A fast interval ceiling
    private static final int FAST_CEILING_IN_SECONDS = 1;

    // Update interval in milliseconds
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    private static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * FAST_CEILING_IN_SECONDS;

    // Conversion from feet to meters
    private static final float METERS_PER_FEET = 0.3048f;

    // Conversion from kilometers to meters
    private static final int METERS_PER_KILOMETER = 1000;

    // Initial offset for calculating the map bounds
    private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;

    // Accuracy for calculating the map bounds
    private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;

    // Maximum results returned from a Parse query
    private static final int MAX_Invite_SEARCH_RESULTS = 100;

    // Maximum post search radius for map in kilometers
    private static final int MAX_Invite_SEARCH_DISTANCE = 50;

    // Map fragment
    private SupportMapFragment mapFragment;

    // Represents the circle around a map
    private Circle mapCircle;

    // Fields for the map radius in feet
    private float radius;
    private float lastRadius;

    // Fields for helping process map and location changes
    private final Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
    private int mostRecentMapUpdate;
    private boolean hasSetUpInitialLocation;
    private String selectedInviteObjectId;
    private BDLocation lastLocation;
    private BDLocation currentLocation;

    private Marker marker;

    // private LocationManagerProxy locationManager;

    // Adapter for the Parse query
    private ParseQueryAdapter<Invite> inviteQueryAdapter;

    // user last location
    private ParseGeoPoint userLastLocation;

    private static ParseGeoPoint mGeoPoint;

    private int mcount = 0;

    // locate
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;

    MapView mMapView;
    BaiduMap mBaiduMap;

    // UI
    OnCheckedChangeListener radioButtonListener;
    Button requestLocButton;
    boolean isFirstLoc = true;

    private int hourpart      = 3;

    private ParseUser muser   = ParseUser.getCurrentUser();

    private String musername  = null;

    private ListView localInvitationListView;

    Handler handler;


    public final class SDKReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            //Log.d(TAG, "action: " + s);
            if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {

            } else if (s
                    .equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {

            }
        }
    }


    private SDKReceiver mReceiver;

    public CnLocalWithoutMapActivity(){
        handler = new Handler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register sdk broadcast
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);

        setContentView(R.layout.ome_activity_bd_without_map);


        // init map

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.cn_without_map_fragment);

        mMapView  = mapFragment.getMapView(); //  findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // enable my location
        mBaiduMap.setMyLocationEnabled(true);
        // locate init
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        // time span  1s = 1000 ms
        option.setScanSpan(1000*60*5);
        mLocClient.setLocOption(option);
        mLocClient.start();



        // query data from parse cloud

        radius     = 3 * Application.getSearchDistance();
        lastRadius = radius;

            // Set up a customized query
        ParseQueryAdapter.QueryFactory<Invite> factory =
                new ParseQueryAdapter.QueryFactory<Invite>() {
                    public ParseQuery<Invite> create() {
                        // update user newest location
                        BDLocation myLocation = (currentLocation == null) ? lastLocation : currentLocation;
                        Application.setCurrentLatitude(Double.toString(myLocation.getLatitude()));
                        Application.setCurrentLongitude(Double.toString(myLocation.getLongitude()));

                        ParseQuery<Invite> query = Invite.getQuery();
                        query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
                        query.include(AppConstant.OMEPARSEUSERKEY);
                        query.orderByDescending(AppConstant.OMEPARSECREATEDATKEY);
                        //query.whereWithinKilometers(AppConstant.OMEPARSELOCATIONKEY, geoPointFromLocation(myLocation), radius
                        //        * METERS_PER_FEET / METERS_PER_KILOMETER);
                        query.setLimit(MAX_Invite_SEARCH_RESULTS);
                        return query;
                    }
                };

        // Set up a progress dialog
        final ProgressDialog listLoadDialog = new ProgressDialog(CnLocalWithoutMapActivity.this);
        listLoadDialog.show();

        // get current geo point
        mGeoPoint = new ParseGeoPoint(Double.valueOf(Application.getCurrentLatitude()), Double.valueOf(Application.getCurrentLongitude()));

        // Set up the query adapter
        inviteQueryAdapter = new ParseQueryAdapter<Invite>(CnLocalWithoutMapActivity.this, factory) {

            int listPinCount = 0;

            @Override
            public View getItemView(Invite invite, View view, ViewGroup parent) {
                if (view == null) {
                    view = View.inflate(getContext(), R.layout.ome_activity_local_list, null);
                }

                ImageView sporttypeiconView = (ImageView) view.findViewById(R.id.local_sport_type_icon);
                TextView workoutnameView    = (TextView) view.findViewById(R.id.local_workoutname_view);
                TextView venueaddressView   = (TextView) view.findViewById(R.id.local_venue_address);
                TextView playtimeView       = (TextView) view.findViewById(R.id.local_play_time);
                TextView distanceView       = (TextView) view.findViewById(R.id.local_distance_to_me);

                String mplaytime = invite.getPlayTime();
                workoutnameView.setText(invite.getWorkoutName());
                venueaddressView.setText(invite.getCourt());
                playtimeView.setText(invite.getPlayTime());

                if (mplaytime.contains(AppConstant.OMEPARSESLASHSTRING)) {
                    // the old version time format contains slash
                    playtimeView.setText(invite.getPlayTime());
                } else {
                    // the new version time format contains space
                    // reformat the play time, original format is MMM dd yyyy HH:mm
                    String[] mpart   = mplaytime.split(AppConstant.OMEPARSESPACESTRING);
                    // MMM's first month january is 0, then when show it, need add 1
                    String mmonth    = Integer.toString(Integer.parseInt(mpart[0]) + 1);
                    String mday      = mpart[1];
                    String mhour     = mpart[hourpart];
                    playtimeView.setText(mday + AppConstant.OMEPARSESLASHSTRING + mmonth + AppConstant.OMEPARSESPACESTRING + mhour);
                }

                // calculate distance between me and venue
                if (invite.getLocation() != null) {
                    Double mdistance = mGeoPoint.distanceInMilesTo(invite.getLocation());
                    DecimalFormat df = new DecimalFormat(AppConstant.OMEPARSEDISTANCEFORMATSTRING);
                    String distancenote = null;
                    if (mdistance < 1) {
                        mdistance = mdistance * AppConstant.OMEMETERSINAKILOMETER;
                        distancenote = getResources().getString(R.string.OMEPARSEMEMYVENUEDISTANCEMETERNOTE);
                    } else {
                        distancenote = getResources().getString(R.string.OMEPARSEMEMYVENUEDISTANCEKMNOTE);
                    }

                    String mdistancestring = df.format(mdistance) + AppConstant.OMEPARSESPACESTRING + distancenote;
                    distanceView.setText(mdistancestring);
                }


                String sporttypevalue = invite.getSportTypeValue();
                sporttypeiconView.setImageDrawable(getResources().getDrawable(Sport.msporticonarray[Integer.parseInt(sporttypevalue)]));


                listLoadDialog.dismiss();

                return view;
            }
        };


        // Disable automatic loading when the adapter is attached to a view.
        inviteQueryAdapter.setAutoload(false);

        // Enable pagination, we'll not manage the query limit ourselves
        inviteQueryAdapter.setPaginationEnabled(true);

        // Attach the query adapter to the view
        localInvitationListView = (ListView) findViewById(R.id.local_without_map_listview);
        localInvitationListView.setAdapter(inviteQueryAdapter);


        // Set up the handler for an item's selection
        localInvitationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final Invite invite    = inviteQueryAdapter.getItem(position);
                ParseUser hostuser     = invite.getFromUser();
                selectedInviteObjectId = invite.getObjectId();

                //BDLocation clickedItemUserLocation = (currentLocation == null) ? lastLocation : currentLocation;
                //clickedItemUserLocation.setLatitude(invite.getLocation().getLatitude());
                //clickedItemUserLocation.setLongitude(invite.getLocation().getLongitude());

                //Location passLocation = new Location("");
                //passLocation.setLatitude(invite.getLocation().getLatitude());
                //passLocation.setLongitude(invite.getLocation().getLongitude());

                Intent invokeJoinActivityIntent = new Intent(getBaseContext(), JoinNextActivity.class);//LocalActivity.this, JoinActivity.class);

                invokeJoinActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //invokeJoinActivityIntent.putExtra(Application.INTENT_EXTRA_LOCATION, passLocation);
                InviteToIntentExtra.putExtra(invokeJoinActivityIntent, invite);

                startActivity(invokeJoinActivityIntent);


            }

        });

    }


    private void doListQuery() {
        BDLocation myLoc = (currentLocation == null) ? lastLocation : currentLocation;
        // If location info is available, load the data
        if (myLoc != null) {
            // Refreshes the list view with new data based
            // usually on updated location data.
            inviteQueryAdapter.loadObjects();
        }
    }


    //
    // Helper method to get the Parse GEO point representation of a location
    //
    private ParseGeoPoint geoPointFromLocation(BDLocation loc) {
        return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
    }


    //
    // locate sdk listen
    //
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            lastLocation    = location;
            currentLocation = location;


            // map view
            if (location == null || mMapView == null)
                return;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                            // map directory
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll  = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }

            // set current location
            Application.setCurrentLatitude(Double.toString(location.getLatitude()));
            Application.setCurrentLongitude(Double.toString(location.getLongitude()));
            doListQuery();
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        //unbindService(YOUR_SERVICE);
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();

        // Get the latest search distance preference
        radius = Application.getSearchDistance();
        // Checks the last saved location to show cached data if it's available
        if (lastLocation != null) {
            LatLng myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            // If the search distance preference has been changed, move
            // map to new bounds.
            if (lastRadius != radius) {
               // updateZoom(myLatLng);
            }
            // Update the circle map
            //updateCircle(myLatLng);
        }
        // Save the current radius
        lastRadius = radius;
        // Query for the latest data to update the views.
        //doMapQuery();
        doListQuery();


        //FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.local_without_fab);
        //fab.setSize(FloatingActionButton.SIZE_NORMAL);
        //  fab.setColor(Color.parseColor("#" + AppConstant.OMETOPLAYOLYMPICGREEN));
        //fab.initBackground();
        //fab.setImageResource(R.drawable.mic);
        //localInvitationListView.setOnTouchListener(new ShowHideOnScroll(fab));
        //fab.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        Toast.makeText(getBaseContext(), "voice turned on ", Toast.LENGTH_LONG).show();
        //    }
        //});
    }

    @Override
    protected void onDestroy() {
        // stop locate
        mLocClient.stop();
        // close locate lay
        mBaiduMap.setMyLocationEnabled(false);
        //mMapView.onDestroy();
        //mMapView = null;
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();

        appData.putBoolean(Application.INTENT_EXTRA_VENUESEARCH, true);
        // appData.putString(Application.INTENT_EXTRA_VENUESEARCH, venuequery);
        startSearch(null, false, appData, false);
        return true;
    }

    private void updateWeatherData(final TextView weatherIcon, final String city){
        new Thread(){
            public void run(){
                final JSONObject json = RemoteFetch.getJSON(CnLocalWithoutMapActivity.this, city);
                if(json == null){
                    handler.post(new Runnable(){
                        public void run(){
                            Toast.makeText(CnLocalWithoutMapActivity.this,
                                    getResources().getString(R.string.OMWPARSEWEATHERPLACENOTFOUND),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable(){
                        public void run(){
                            renderWeather(weatherIcon, json);
                        }
                    });
                }
            }
        }.start();
    }

    private void newsetWeatherIcon(TextView weatherIcon, int actualId, int day){
        int id = actualId / 100;
        String icon = "";
        if(actualId == 800){
            long currentTime = new Date().getTime();
            // if(currentTime>=sunrise && currentTime<sunset) {
            icon = getResources().getString(R.string.OMEPARSEWEATHERSUNNY);
            //} else {
            //    icon = getActivity().getString(R.string.weather_clear_night);
            //}
        } else {
            switch(id) {
                case 2 : icon = getResources().getString(R.string.OMEPARSEWEATHERTHUNDER);
                    break;
                case 3 : icon = getResources().getString(R.string.OMEPARSEWEATHERDRIZZLE);
                    break;
                case 7 : icon = getResources().getString(R.string.OMEPARSEWEATHERFOGGY);
                    break;
                case 8 : icon = getResources().getString(R.string.OMEPARSEWEATHERCLOUDY);
                    break;
                case 6 : icon = getResources().getString(R.string.OMEPARSEWEATHERSNOWY);
                    break;
                case 5 : icon = getResources().getString(R.string.OMEPARSEWEATHERRAINY);
                    break;
            }
        }
        switch(day) {
            case 0 : weatherIcon.setText(icon);
                break;
        }
    }

    private void renderWeather(TextView weatherIcon, JSONObject json){
        try {
            JSONObject detailsindex = json.getJSONArray("list").getJSONObject(0);
            JSONObject details      = detailsindex.getJSONArray("weather").getJSONObject(0);
            newsetWeatherIcon(weatherIcon, details.getInt("id"), 0);
        }catch(Exception e){
            Log.e("SimpleWeather", "One or more fields not found in the JSON data  " + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();

        menuInflater.inflate(R.menu.ome_local_menu, menu);

        Typeface weatherFont;
        weatherFont = Typeface.createFromAsset(CnLocalWithoutMapActivity.this.getAssets(), "fonts/weather.ttf");

        TextView weathertv = new TextView(this);
        weathertv.setTypeface(weatherFont);
        updateWeatherData(weathertv, "beijing");
        weathertv.setPadding(5, 0, 5, 0);
        //tv.setTypeface(null, Typeface.BOLD);
        weathertv.setTextSize(20);
        weathertv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent invokeWeatherActivityIntent = new Intent(CnLocalWithoutMapActivity.this, WeatherActivity.class);
                invokeWeatherActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(invokeWeatherActivityIntent);
            }
        });
        menu.add(0, 1001, 1, weathertv.getText()).setActionView(weathertv).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);


        if (muser != null) {
            MenuItem settingItem = menu.add(getResources().getString(R.string.meactivity_title));

            // menu.findItem(R.id.action_setting).
            settingItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {

                    Intent invokeMeActivityIntent = new Intent(CnLocalWithoutMapActivity.this, MeActivity.class);
                    invokeMeActivityIntent.putExtra(AppConstant.OMEPARSEPARENTCLASSNAME, AppConstant.OMEPARSELOCALWITHOUTMAPACTIVITYCLASS);
                    startActivity(invokeMeActivityIntent);


                    // intent.putExtra("ParentClassName","A");
                    // startActivity(new Intent(LocalWithoutMapActivity.this, MeActivity.class));
                    return true;
                }
            });

        }

        if (muser != null){
            MenuItem logoutItem = menu.add(getResources().getString(R.string.OMEPARSELOGOUT));
            //menu.findItem(R.id.action_login)
            logoutItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {

                    // update user last time and last location

                    //get point according to  current latitude and longitude
                    userLastLocation = new ParseGeoPoint(Double.valueOf(Application.getCurrentLatitude()), Double.valueOf(Application.getCurrentLongitude()));

                    if (muser != null) {
                        muser.put(AppConstant.OMEPARSEUSERLASTTIMEKEY, Time.currentTime());
                        muser.put(AppConstant.OMEPARSEUSERLASTLOCATIONKEY, userLastLocation);

                        muser.saveInBackground();

                    }

                    // preference logout
                    SharedPreferences preferences   = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("loggedin", false);
                    editor.apply();


                    // Stop the Core Service
                    Intent stopCoreServiceIntent = new Intent(CnLocalWithoutMapActivity.this, CoreService.class);
                    CnLocalWithoutMapActivity.this.stopService(stopCoreServiceIntent);



                    // Call the Parse log out method
                    ParseUser.logOut();

                    finish();


                    return true;
                }
            });
        } else {
            MenuItem loginItem = menu.add(getResources().getString(R.string.OMEPARSELOGIN));
            //menu.findItem(R.id.action_login)
            loginItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    startActivity(new Intent(CnLocalWithoutMapActivity.this, LoginActivity.class));
                    finish();
                    return true;
                }
            });
        }


        return super.onCreateOptionsMenu(menu);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search_venue:
                 onSearchRequested();
                return true;
            case R.id.action_map:
                Intent invokeMapActivityIntent = new Intent(CnLocalWithoutMapActivity.this, CnMapActivity.class);
                invokeMapActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(invokeMapActivityIntent);
                return true;
            case R.id.action_message:
                // Check username
                Intent invokeMessageIntent = new Intent(CnLocalWithoutMapActivity.this, MessageListActivity.class);
                invokeMessageIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(invokeMessageIntent);
                return true;
            case R.id.action_invite:
                invokeInviteActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    //
    // invoke Map activity intent.
    //
    private void invokeInviteActivity() {

            // Only allow posts if we have a location
            BDLocation mLocation = (currentLocation == null) ? lastLocation : currentLocation;
            if (mLocation == null) {
                //Toast.makeText(LocalActivity.this,
                //        getResources().getString(R.string.current_location_unavailable), Toast.LENGTH_LONG).show();
                return;
            }

            //set current latitude and longitude
            Application.setCurrentLatitude(Double.toString(mLocation.getLatitude()));
            Application.setCurrentLongitude(Double.toString(mLocation.getLongitude()));

            //Ozzie Zhang 10-29-2014 please change JoinActivity to InviteActivity, now only for test
            Intent intent = new Intent(CnLocalWithoutMapActivity.this, InviteNextActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

    }



}

