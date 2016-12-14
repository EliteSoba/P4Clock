package com.soba.persona.p4clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.widget.RemoteViews;
import android.widget.TextClock;

import java.util.Calendar;

/**
 * Created by Tobias on 12/12/2016.
 */
public class ClockWidget extends AppWidgetProvider{
    RemoteViews views;
    private PendingIntent service = null;
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
        //context.startService(new Intent(UpdateService.ACTION_UPDATE));

        final AlarmManager m = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        final Calendar TIME = Calendar.getInstance();
        final Intent i = new Intent(context, UpdateService.class);

        if (service == null) {
            service = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        context.startService(new Intent(context, UpdateService.class));
        //m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), 60000, service);
    }

    public void onDisabled(Context context) {
        super.onDisabled(context);
        //context.stopService(new Intent(context, UpdateService.class));
        final AlarmManager m = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (service != null) {
            m.cancel(service);
        }
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    public static final class UpdateService extends Service {
        static final String ACTION_UPDATE = "com.soba.persona4.p4clock.action.UPDATE";

        static Typeface clock = null, clock2 = null;

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

            mCalendar = Calendar.getInstance();
            registerReceiver(mTimeChangedReceiver, sIntentFilter);
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mTimeChangedReceiver);
        }

        public int onStartCommand(Intent intent, int flags, int startId) {
            update();

            /*if (ACTION_UPDATE.equals(intent.getAction())) {
                update();
            }*/

            return super.onStartCommand(intent, flags, startId);
        }

        public static RemoteViews buildTime(Context context, int hour) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            Bitmap myBitmap = Bitmap.createBitmap(350, 80, Bitmap.Config.ARGB_8888);
            Canvas myCanvas = new Canvas(myBitmap);

            if (clock2 == null) {
                clock2 = Typeface.createFromAsset(context.getAssets(),"fonts/RobotoCondensed-Regular.ttf");
            }

            String time = "";
            if (hour < 5) {
                time = "Night";
            }
            else if (hour < 8) {
                time = "Early Morning";
            }
            else if (hour < 12) {
                time = "Morning";
            }
            else if (hour < 2) {
                time = "Lunch";
            }
            else if (hour < 7) {
                time = "Afternoon";
            }
            else if (hour < 10) {
                time = "Evening";
            }
            else {
                time = "Night";
            }

            Paint stroke = new Paint();
            stroke.setAntiAlias(true);
            stroke.setSubpixelText(true);
            stroke.setTypeface(clock2);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(10);
            stroke.setColor(Color.BLACK);
            stroke.setTextSize(60);

            int width = (int)stroke.measureText(time);


            myCanvas.drawText(time, 325-width, 60, stroke);

            Paint paint2 = new Paint();
            paint2.setAntiAlias(true);
            paint2.setSubpixelText(true);
            paint2.setTypeface(clock2);
            paint2.setStyle(Paint.Style.FILL);
            paint2.setColor(Color.WHITE);
            paint2.setTextSize(60);
            myCanvas.drawText(time, 325-width, 60, paint2);

            views.setImageViewBitmap(R.id.timeOfDay, myBitmap);

            return views;
        }

        public static RemoteViews buildUpdate(Context context, String date, String day)
        {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            Bitmap myBitmap = Bitmap.createBitmap(235, 50, Bitmap.Config.ARGB_8888);
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
            myCanvas.drawText(date, 0, 50, paint);

            if (clock2 == null) {
                clock2 = Typeface.createFromAsset(context.getAssets(),"fonts/RobotoCondensed-Regular.ttf");
            }

            Paint paint3 = new Paint();
            paint3.setAntiAlias(true);
            paint3.setSubpixelText(true);
            paint3.setTypeface(clock2);
            paint3.setStyle(Paint.Style.STROKE);
            paint3.setStrokeWidth(6);
            paint3.setColor(Color.BLACK);
            paint3.setTextSize(38);
            myCanvas.drawText(day, 165, 47, paint3);

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
            paint2.setTextSize(38);
            myCanvas.drawText(day, 165, 47, paint2);

            views.setImageViewBitmap(R.id.maybeSomething, myBitmap);

            return views;
        }
        private void update() {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            //final CharSequence time = DateFormat.format(mTimeFormat, mCalendar);

            String date = (String) DateFormat.format("MM/dd", mCalendar);
            String day = (String) DateFormat.format("EEE", mCalendar);
            RemoteViews views = buildUpdate(this, date, day.toUpperCase());
            /*RemoteViews views = new RemoteViews(getPackageName(), R.layout.clock_widget_layout);
            views.setTextViewText(R.id.Time, time);
            views.setTextViewText(R.id.Day, day);
            views.setTextViewText(R.id.Date, date);*/
            RemoteViews views2 = buildTime(this, mCalendar.get(Calendar.HOUR_OF_DAY));
            ComponentName widget = new ComponentName(this, ClockWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(widget, views);
            manager.updateAppWidget(widget, views2);

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
    }

}
