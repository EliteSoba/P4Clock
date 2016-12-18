package com.soba.persona.p4clock;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.RemoteViews;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

/**
 * Logic controlling the Widget, including updates
 * Created by Tobias on 12/12/2016.
 */
public class ClockWidget extends AppWidgetProvider {

    public void onEnabled(Context context) {
        super.onEnabled(context);
        /*if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }*/
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        context.startService(new Intent(context, UpdateService.class));
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Intent i = new Intent(context, UpdateService.class);

        if (intent.getBooleanExtra(MainActivity.force, false)) {
            i.putExtra(MainActivity.force, true);
        }

        context.startService(i);

    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
        Intent configIntent = new Intent(context, MainActivity.class);
        PendingIntent clickPendIntent = PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.custom_clock_widget, clickPendIntent);
        appWidgetManager.updateAppWidget(appWidgetIds, views);

        Intent i = new Intent(context, UpdateService.class);
        i.putExtra(MainActivity.force, true);

        context.startService(i);
    }

    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context, UpdateService.class));
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    public static final class UpdateService extends Service implements  GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

        static int hour = -1;

        static GoogleApiClient mGoogleApiClient;
        int lat = -1, lon = -1;
        boolean located = false;
        boolean setLocation = false;
        boolean disabled = false;
        SharedPreferences prefs = null;

        private final static IntentFilter sIntentFilter;
        static {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(Intent.ACTION_TIME_TICK);
            sIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            sIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        }
        private Calendar mCalendar;
        @Override
        public void onCreate() {
            super.onCreate();
            hour = -1;
            mCalendar = Calendar.getInstance();
            registerReceiver(mTimeChangedReceiver, sIntentFilter);

            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
            mGoogleApiClient.connect();
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mTimeChangedReceiver);
            mGoogleApiClient.disconnect();
        }

        public int onStartCommand(Intent intent, int flags, int startId) {
            //hour = -1;
            if (prefs == null) {
                prefs = getSharedPreferences(MainActivity.comName, MODE_PRIVATE);
            }

            setLocation = !prefs.getBoolean(MainActivity.useLoc, true);
            disabled = prefs.getBoolean(MainActivity.disabled, false);

            if (setLocation
                    && prefs.contains(MainActivity.lat)
                    && prefs.contains(MainActivity.lon)) {
                lat = prefs.getInt(MainActivity.lat, 0);
                lon = prefs.getInt(MainActivity.lon, 0);
                located = true;
            }

            Bundle extras = null;
            if (intent != null) {
                extras = intent.getExtras();
            }

            if (extras != null && extras.getBoolean(MainActivity.force, false)) {
                update();
            }
            else {
                checkUpdate();
            }

            return super.onStartCommand(intent, flags, startId);
        }

        private void update() {
            WidgetUpdater.update(this);

            //If weather updates are disabled
            if (disabled) {
                return;
            }

            if (!setLocation) {
                //IDK if this actually changes anything, but try to get location from network provider first, then gps
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        LocationManager man = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                        long time_update = 30*60*1000;
                        long dist_update = 10;
                        man.requestLocationUpdates(LocationManager.GPS_PROVIDER, time_update, dist_update, this);
                        Location mLastLocation = man.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (mLastLocation != null) {
                            lat = (int)mLastLocation.getLatitude();
                            lon = (int)mLastLocation.getLongitude();
                            located = true;
                        }
                    }
                    //Else no access to either :(
                }
                else {
                    LocationManager man = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                    long time_update = 30*60*1000;
                    long dist_update = 10;
                    man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time_update, dist_update, this);
                    Location mLastLocation = man.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (mLastLocation != null) {
                        lat = (int)mLastLocation.getLatitude();
                        lon = (int)mLastLocation.getLongitude();
                        located = true;
                    }
                }
            }

            if (located) {
                WidgetUpdater.updateWeather(this, lat, lon);
            }
        }

        private void checkUpdate() {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int h = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour == h) {
                return;
            }
            hour = h;
            update();

        }
        private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkUpdate();
            }
        };

        @Override
        public void onConnected(Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

}
