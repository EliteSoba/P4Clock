package com.soba.persona.p4clock;

import android.*;
import android.Manifest;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

/**
 * Created by Tobias on 12/12/2016.
 */
public class ClockWidget extends AppWidgetProvider {
    RemoteViews views;
    public static String ACTION_WIDGET_RECEIVER = "ActionReceiverWidget";

    public void onEnabled(Context context) {
        super.onEnabled(context);
        context.startService(new Intent(context, UpdateService.class));
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        /*String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            //views = buildUpdate(context, time);
            Intent choiceIntent = new Intent(context, ClockChoice.class);
            PendingIntent clickPendIntent = PendingIntent.getActivity(context, 0, choiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.custom_clock_widget, clickPendIntent);
            AppWidgetManager.getInstance(context).updateAppWidget(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS), views);
        }*/
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        context.startService(new Intent(context, UpdateService.class));
    }

    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context, UpdateService.class));
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    public static final class UpdateService extends Service implements  GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {
        static final String ACTION_UPDATE = "com.soba.persona4.p4clock.action.UPDATE";

        Typeface clock = null, clock2 = null;
        static int hour = -1;

        static GoogleApiClient mGoogleApiClient;
        double lat = -1, lon = -1;

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
            hour = -1;
            update();

            /*if (ACTION_UPDATE.equals(intent.getAction())) {
                update();
            }*/

            return super.onStartCommand(intent, flags, startId);
        }

        public RemoteViews buildWeather(Context context) {
            String date = ":(";
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            Bitmap myBitmap = Bitmap.createBitmap(275, 50, Bitmap.Config.ARGB_8888);
            Canvas myCanvas = new Canvas(myBitmap);
            Paint paint = new Paint();
            if (clock == null) {
                clock = Typeface.createFromAsset(context.getAssets(), "fonts/Days.ttf");
            }

            paint.setAntiAlias(true);
            paint.setSubpixelText(true);
            paint.setTypeface(clock);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);
            paint.setLetterSpacing(0.25f);
            myCanvas.drawText(date, 0, 50, paint);

            views.setImageViewBitmap(R.id.weatherIcon, myBitmap);

            return views;
        }

        public RemoteViews buildTime(Context context, int hour) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);

            if (hour == 0) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.midnight);
            }
            else if (hour < 5) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.night);
            }
            else if (hour < 8) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.earlymorning);
            }
            else if (hour < 12) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.morning);
            }
            else if (hour < 14) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.lunchtime);
            }
            else if (hour < 19) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.afternoon);
            }
            else if (hour < 22) {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.evening);
            }
            else {
                views.setImageViewResource(R.id.timeOfDay, R.drawable.night);
            }

            return views;
        }

        public RemoteViews buildUpdate(Context context, String date, String day)
        {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            Bitmap myBitmap = Bitmap.createBitmap(360, 50, Bitmap.Config.ARGB_8888);
            Canvas myCanvas = new Canvas(myBitmap);
            Paint paint = new Paint();
            if (clock == null) {
                clock = Typeface.createFromAsset(context.getAssets(), "fonts/Days.ttf");
            }
            paint.setAntiAlias(true);
            paint.setSubpixelText(true);
            paint.setTypeface(clock);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);
            paint.setLetterSpacing(0.25f);
            myCanvas.drawText(date, 0, 50, paint);

            if (clock2 == null) {
                clock2 = Typeface.createFromAsset(context.getAssets(),"fonts/RobotoCondensed-Regular.ttf");
            }
            Paint paint2 = new Paint();
            paint2.setAntiAlias(true);
            paint2.setSubpixelText(true);
            paint2.setTypeface(clock2);
            paint2.setStyle(Paint.Style.FILL);
            paint2.setColor(Color.WHITE);
            //Special colors for weekends. Holidays should probably also be red, but that's much
            //harder to set up and I'm not feeling it right now
            if ("SAT".equals(day)) {
                paint2.setColor(Color.rgb(165,194,218));
            }
            else if ("SUN".equals(day)) {
                paint2.setColor(Color.rgb(215,157,167));
            }
            paint2.setTextSize(36);
            myCanvas.drawText(day, 230, 43, paint2);

            views.setImageViewBitmap(R.id.dayDate, myBitmap);

            return views;
        }
        private void update() {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            else {
                LocationManager man = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                long time_update = 30*60*1000;
                long dist_update = 10;
                man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time_update, dist_update, this);
                Location mLastLocation = man.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (mLastLocation == null) {

                    lat = 1;
                    lon = 2;
                }
                else {
                    lat = mLastLocation.getLatitude();
                    lon = mLastLocation.getLongitude();
                }
            }

            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int h = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour == h) {
                return;
            }
            hour = h;

            String date = (String) DateFormat.format("MM/dd", mCalendar);
            String day = (String) DateFormat.format("EEE", mCalendar);
            RemoteViews views = buildUpdate(this, date, day.toUpperCase());
            RemoteViews views2 = buildTime(this, hour);
            //RemoteViews views3 = buildWeather(this);

            ComponentName widget = new ComponentName(this, ClockWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(widget, views);
            manager.updateAppWidget(widget, views2);
            //manager.updateAppWidget(widget, views3);

        }
        private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(Intent.ACTION_TIME_CHANGED) ||
                        action.equals(Intent.ACTION_TIMEZONE_CHANGED))
                {
                }

                update();
            }
        };

        @Override
        public void onConnected(Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

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
