package com.soba.persona.p4clock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RemoteViews;

/**
 * Created by Tobias on 12/12/2016.
 */
public class ClockChoice extends Activity implements OnClickListener{

    private Button[] buttons;
    private int[] clocks;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clock_choice);

        buttons = new Button[2];
        clocks = new int[2];

        for (int i = 0; i < 2; ++i) {
            clocks[i] = this.getResources().getIdentifier("Clock"+i, "id", getPackageName());
            buttons[i] = (Button)findViewById(this.getResources().getIdentifier("button_"+i, "id", getPackageName()));
            buttons[i].setOnClickListener(this);
        }
    }

    public void onClick(View v) {
        int p = -1;
        for (int i = 0; i < 2; ++i) {
            if (v.getId() == buttons[i].getId()) {
                p = i;
                break;
            }
        }


        int picked = clocks[p];
        RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.clock_widget_layout);

        for (int i = 0; i < 2; ++i) {
            if (i != p) {
                remoteViews.setViewVisibility(clocks[i], View.INVISIBLE);
            }
        }
        remoteViews.setViewVisibility(picked, View.VISIBLE);

        //get component name for widget class
        ComponentName comp = new ComponentName(this, ClockWidget.class);
        //get AppWidgetManager
        AppWidgetManager appWidgetManager =
                AppWidgetManager.getInstance(this.getApplicationContext());
        //update
        appWidgetManager.updateAppWidget(comp, remoteViews);
    }
}
