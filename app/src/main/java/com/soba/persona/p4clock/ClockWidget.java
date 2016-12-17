package com.soba.persona.p4clock;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

/**
 * Logic controlling the Widget, including updates
 * Created by Tobias on 12/12/2016.
 */
public class ClockWidget extends AppWidgetProvider {

    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        context.startService(new Intent(context, UpdateService.class));
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
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

        private class GetWeatherTask extends AsyncTask<URL, Integer, Integer> {

            RemoteViews views;
            Context context;

            GetWeatherTask(RemoteViews views, Context context) {
                this.views = views;
                this.context = context;
            }

            @Override
            protected Integer doInBackground(URL... params) {
                URL url = params[0];
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                    BufferedReader read = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = read.readLine()) != null) {
                        result.append(line);
                    }

                    String output = result.toString();
                    JSONObject jsonOut = new JSONObject(output);
                    JSONArray weather = jsonOut.getJSONArray("weather");
                    return weather.getJSONObject(0).getInt("id");

                } catch (IOException | JSONException e) {
                    e.printStackTrace(); }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                int icon;
                switch (result / 100) {
                    case 2: icon = R.drawable.thunderstorms; break;
                    case 3:
                    case 5: icon = R.drawable.rain; break;
                    case 6: icon = R.drawable.snow; break;
                    case 7: icon = R.drawable.fog; break;
                    case 8: icon = R.drawable.clear; break;
                    default: icon = R.drawable.unknown;
                }
                if (result > 800) {
                    icon = R.drawable.cloudy;
                }
                if (result >= 900) {
                    icon = R.drawable.unknown;
                }
                views.setImageViewResource(R.id.weatherIcon, icon);

                ComponentName widget = new ComponentName(context, ClockWidget.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                manager.updateAppWidget(widget, views);

            }
        }

        Typeface clock = null, clock2 = null;
        static int hour = -1;

        static GoogleApiClient mGoogleApiClient;
        int lat = -1, lon = -1;
        boolean located = false, permissed = false;

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

            return super.onStartCommand(intent, flags, startId);
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
            else if (hour < 18) {
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
            if (clock == null) {
                clock = Typeface.createFromAsset(context.getAssets(), "fonts/Days.ttf");
            }
            if (clock2 == null) {
                clock2 = Typeface.createFromAsset(context.getAssets(),"fonts/RobotoCondensed-Regular.ttf");
            }

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setSubpixelText(true);
            paint.setTypeface(clock);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(100);
            paint.setLetterSpacing(0.25f);

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
            paint2.setTextSize(65);

            int dateWidth = (int)paint.measureText(date);
            int dayWidth = (int)paint2.measureText(day);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            Bitmap myBitmap = Bitmap.createBitmap(dateWidth + 75 + dayWidth + 100,
                    75, Bitmap.Config.ARGB_8888);
            Canvas myCanvas = new Canvas(myBitmap);

            myCanvas.drawText(date, 0, 75, paint);
            myCanvas.drawText(day, dateWidth + 75, 52, paint2);

            views.setImageViewBitmap(R.id.dayDate, myBitmap);

            return views;
        }
        private void update() {
            //On minute tick, if we now have permissions where we didn't before, force the update
            if (!permissed && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                hour = -1;
            }

            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int h = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour == h) {
                return;
            }
            hour = h;

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
                        permissed = true;
                    }
                }
                else {
                    //No access to either :(
                    permissed = false;
                }
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
                    permissed = true;
                }
            }

            String date = (String) DateFormat.format("MM/dd", mCalendar);
            String day = (String) DateFormat.format("EEE", mCalendar);
            RemoteViews dateView = buildUpdate(this, date, day.toUpperCase());
            RemoteViews timeView = buildTime(this, hour);

            RemoteViews weatherView = new RemoteViews(this.getPackageName(), R.layout.clock_widget_layout);

            //located = false;
            if (located) {
                try {
                    String url = "http://api.openweathermap.org/data/2.5/weather?"
                            + "lat=" + lat + "&lon=" + lon
                            + "&APPID=" + getString(R.string.openweatherKey);

                    new GetWeatherTask(weatherView, this).execute(new URL(url));
                } catch (MalformedURLException e) { e.printStackTrace(); }
            }

            ComponentName widget = new ComponentName(this, ClockWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);

            manager.updateAppWidget(widget, dateView);
            manager.updateAppWidget(widget, timeView);

        }
        private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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
