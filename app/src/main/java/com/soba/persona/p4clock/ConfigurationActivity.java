package com.soba.persona.p4clock;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.w3c.dom.Text;

public class ConfigurationActivity extends AppCompatActivity {

    AppWidgetManager manager;
    SharedPreferences prefs;
    public static final String comName = "com.soba.persona.p4clock.prefs";
    public static final String disabled = "com.soba.persona.p4clock.disabled";
    public static final String useLoc = "com.soba.persona.p4clock.useLoc";
    public static final String lat = "com.soba.persona.p4clock.lat";
    public static final String lon = "com.soba.persona.p4clock.lon";
    public static final String force = "com.soba.persona.p4clock.force";
    int id;

    public void initEditTexts() {
        EditText latitude = (EditText)findViewById(R.id.latText);
        latitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0 || s.toString().equals("-")) {
                    removeKey(lat);
                }
                else {
                    try {
                        putInt(lat, Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        EditText longitude = (EditText)findViewById(R.id.lonText);
        longitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0 || s.toString().equals("-")) {
                    removeKey(lon);
                }
                else {
                    try {
                        putInt(lon, Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if (prefs.contains(lat)) {
            String latit = ""+prefs.getInt(lat, 0);
            latitude.setText(latit);
        }
        if (prefs.contains(lon)) {
            String longi = ""+prefs.getInt(lon, 0);
            longitude.setText(longi);
        }
    }

    public void initRadios() {
        if (prefs.getBoolean(useLoc, false)) {
            RadioButton useLoc = (RadioButton)findViewById(R.id.curLocationButton);
            useLoc.setChecked(true);
            useLoc.callOnClick();
        }
        else {
            RadioButton chooseLoc = (RadioButton) findViewById(R.id.chooseButton);
            chooseLoc.setChecked(true);
            chooseLoc.callOnClick();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_main);
        manager = AppWidgetManager.getInstance(this);
        prefs = getSharedPreferences(comName, MODE_PRIVATE);

        Button start = (Button) findViewById(R.id.updateButton);
        start.setText(R.string.confirm);
        start.setCompoundDrawables(null, null, null, null);

        TextView textView = (TextView)findViewById(R.id.infoText);
        textView.setVisibility(View.GONE);

        initEditTexts();
        initRadios();

        if (prefs.getBoolean(disabled, false)) {
            CheckBox disableBox = (CheckBox) findViewById(R.id.disableBox);
            disableBox.setChecked(true);
            disableBox.callOnClick();
        }

        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.clock_widget_layout);
        awm.updateAppWidget(id, views);
    }

    public void updateWidget(boolean force) {
        Intent intent = new Intent(this, ClockWidget.class);
        intent.setAction(MainActivity.update);
        intent.putExtra(MainActivity.force, force);
        sendBroadcast(intent);

        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.clock_widget_layout);
        awm.updateAppWidget(id, views);
    }

    public void removeKey(String key) {
        if (prefs.contains(key)) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.remove(key);
            edit.commit();
            updateWidget(false);
        }
    }

    public void putInt(String key, int value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(key, value);
        edit.commit();
        updateWidget(false);
    }

    public void putBoolean(String key, boolean value, boolean force) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(key, value);
        edit.commit();
        updateWidget(force);
    }

    public void disableWeather(View view) {
        CheckBox disableBox = (CheckBox)findViewById(R.id.disableBox);
        boolean enabled = !disableBox.isChecked();
        RadioButton button2 = (RadioButton) findViewById(R.id.curLocationButton);
        button2.setEnabled(enabled);
        RadioButton button3 = (RadioButton) findViewById(R.id.chooseButton);
        button3.setEnabled(enabled);
        EditText lat = (EditText)findViewById(R.id.lonText);
        EditText lon = (EditText)findViewById(R.id.latText);
        lat.setEnabled(enabled);
        lon.setEnabled(enabled);

        if (getCurrentFocus() != null) {
            if (!enabled) {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        putBoolean(disabled, !enabled, false);
    }

    public void updateButton(View view) {
        updateWidget(true);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            boolean accepted = false;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    accepted = true;
                }
            }

            if (accepted) {
                putBoolean(useLoc, true, true);
            }
            else {
                RadioButton choose = (RadioButton)findViewById(R.id.chooseButton);
                choose.setChecked(true);
            }
        }
    }

    public void useLocation(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        else {
            putBoolean(useLoc, true, false);
        }
    }

    public void setLocation(View view) {
        putBoolean(useLoc, false, false);
    }
}
