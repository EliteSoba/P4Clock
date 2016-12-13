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
        context.startService(new Intent(UpdateService.ACTION_UPDATE));
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

        m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), 60000, service);
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

        private Calendar mCalendar;
        @Override
        public void onCreate() {
            super.onCreate();
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public int onStartCommand(Intent intent, int flags, int startId) {
            update();

            return super.onStartCommand(intent, flags, startId);
        }

        public static RemoteViews buildUpdate(Context context, String time)
        {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.clock_widget_layout);
            Bitmap myBitmap = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_4444);
            Canvas myCanvas = new Canvas(myBitmap);
            Paint paint = new Paint();
            Typeface clock = Typeface.createFromAsset(context.getAssets(),"fonts/Days.ttf");
            paint.setAntiAlias(true);
            paint.setSubpixelText(true);
            paint.setTypeface(clock);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);
            myCanvas.drawText(time, 0, 50, paint);
            views.setImageViewBitmap(R.id.maybeSomething, myBitmap);

            return views;
        }
        private void update() {
            mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            //final CharSequence time = DateFormat.format(mTimeFormat, mCalendar);

            String time = (String) DateFormat.format("h:mm", mCalendar);
            RemoteViews views = buildUpdate(this, time);
            /*RemoteViews views = new RemoteViews(getPackageName(), R.layout.clock_widget_layout);
            views.setTextViewText(R.id.Time, time);
            views.setTextViewText(R.id.Day, day);
            views.setTextViewText(R.id.Date, date);*/

            ComponentName widget = new ComponentName(this, ClockWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(widget, views);

        }
    }

}
