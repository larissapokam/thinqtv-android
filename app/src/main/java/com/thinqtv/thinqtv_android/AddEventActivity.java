package com.thinqtv.thinqtv_android;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.thinqtv.thinqtv_android.data.DataSource;
import com.thinqtv.thinqtv_android.data.UserRepository;

import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        setContentView(R.layout.activity_add_event);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Button setTimeButton = findViewById(R.id.time);
        setTimeButton.setOnClickListener(view -> {
            DialogFragment newFragment = new TimePickerFragment((view1, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                setTimeButton.setText(calendar.getTime().toString().substring(11, 16));
            });
            newFragment.show(getSupportFragmentManager(), "timePicker");
        });
        Button setDateButton = findViewById(R.id.date);
        setDateButton.setOnClickListener(view -> {
            DialogFragment newFragment = new DatePickerFragment((datePicker, year, month, day) -> {
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DATE, day);
                calendar.set(Calendar.YEAR, year);
                setDateButton.setText(calendar.getTime().toString().substring(0, 10));
            });
            newFragment.show(getSupportFragmentManager(), "datePicker");
        });

        SeekBar lengthSlider = findViewById(R.id.length_slider);
        TextView lengthDisplay = findViewById(R.id.length_display);
        lengthSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                        i += 1;
                                                        int hours = i / 4;
                                                        int minutes = (i % 4) * 15;
                                                        String time = hours + ":" + minutes;
                                                        if (minutes == 0) {
                                                            time += "0";
                                                        }
                                                        lengthDisplay.setText(time);
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {}

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {}
                                                }
        );

        Button sendButton = findViewById(R.id.send);
        Context context = this;
        sendButton.setOnClickListener(view -> {
            String name = ((EditText) findViewById(R.id.event_title)).getText().toString();
            String desc = ((EditText) findViewById(R.id.description)).getText().toString();
            Date date = calendar.getTime();
            LocalDateTime start = DateTimeUtils.toInstant(date).atZone(ZoneId.systemDefault()).toLocalDateTime();
            start = start.minusMinutes(start.getMinute());
            start = start.minusSeconds(start.getSecond());
            start = start.minusNanos(start.getNano());
            int length = (lengthSlider.getProgress() + 1) * 15;
            ZonedDateTime zoned_start = start.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("America/Phoenix"));
            ZonedDateTime zoned_end = zoned_start.plusMinutes(length);

            String start_string = zoned_start.toLocalDateTime().toString();
            String end_string = zoned_end.toLocalDateTime().toString();
            makeRequest(context, name, start_string, end_string, desc);
        });
    }

    public static class TimePickerFragment extends DialogFragment {
        private TimePickerDialog.OnTimeSetListener listener;
        public TimePickerFragment(TimePickerDialog.OnTimeSetListener listener) {
            super();
            this.listener = listener;
        }
        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // default values are the current time
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            return new TimePickerDialog(getActivity(), listener, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }
    }

    public static class DatePickerFragment extends DialogFragment {
        private DatePickerDialog.OnDateSetListener listener;
        public DatePickerFragment(DatePickerDialog.OnDateSetListener listener) {
            super();
            this.listener = listener;
        }
        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // default values are the current date
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), listener, year, month, day);
        }
    }

    public void makeRequest(Context context, String name, String start, String end, String desc) {
        JSONObject params = new JSONObject();
        try {
            params.put("name", name);
            params.put("start_at", start);
            params.put("desc", desc);
        } catch(JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                getString(R.string.events_url), params, response -> {
            ((Activity)context).finish();
        }, error -> {
            Log.e("create event", "server error");
            Log.e("create event", error.toString());
            try {
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    JSONObject errors = new JSONObject(new String(error.networkResponse.data)).getJSONObject("errors");
                    Log.e("create event", errors.toString());
                }
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                if (UserRepository.getInstance().getLoggedInUser() != null) {
                    headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + UserRepository.getInstance().getLoggedInUser().getUserInfo().get("token"));
                }
                return headers;
            }
        };
        DataSource.getInstance().addToRequestQueue(request, context);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

