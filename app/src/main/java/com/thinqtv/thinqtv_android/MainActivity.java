package com.thinqtv.thinqtv_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String THINQTV_ROOM_NAME = "ThinqTV";
    private static final String screenNameKey = "com.thinqtv.thinqtv_android.SCREEN_NAME";
    private static String lastScreenNameStr = "";

    private ActionBarDrawerToggle mDrawerToggle; //toggle for sidebar button shown in action bar
    boolean eventsExpanded = false; //used to expand and collapse the Events ScrollView, changes with each click

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeEvents();
        generateSidebar();
        
        // restore screen name using lastInstanceState if possible
        if (savedInstanceState != null) {
            lastScreenNameStr = savedInstanceState.getString(screenNameKey);
        }
        // else try to restore it using SharedPreferences
        else {
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            String defaultValue = lastScreenNameStr;
            lastScreenNameStr = sharedPref.getString(screenNameKey, defaultValue);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // restore text inside screen name field if the user hasn't typed anything to override it
        EditText screenName = findViewById(R.id.screenName);
        String screenNameStr = screenName.getText().toString();
        if (screenNameStr.length() == 0) {
            screenName.setText(lastScreenNameStr);
        }
    }

    @Override
    protected void onDestroy() {
        if (lastScreenNameStr.length() > 0) {
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(screenNameKey, lastScreenNameStr);
            editor.commit();
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // save lastScreenNameStr to savedInstanceState if it exists
        if (lastScreenNameStr.length() > 0) {
            outState.putString(screenNameKey, lastScreenNameStr);
        }

        super.onSaveInstanceState(outState);
    }

    // Button listener for "Join Conversation" button that connects to default ThinQ.TV chatroom
    public void onJoinClick(View v) {
        // extract screen name and conference name from EditText fields
        EditText screenName = findViewById(R.id.screenName);
        lastScreenNameStr = screenName.getText().toString();

        JitsiMeetConferenceOptions.Builder optionsBuilder
                = new JitsiMeetConferenceOptions.Builder()
                .setRoom(THINQTV_ROOM_NAME);

        if (lastScreenNameStr.length() > 0) {
            Log.d("SCREEN_NAME", lastScreenNameStr);
            Bundle userInfoBundle = new Bundle();
            // the string "displayName" is required by the API
            userInfoBundle.putString("displayName", lastScreenNameStr);
            optionsBuilder.setUserInfo(new JitsiMeetUserInfo(userInfoBundle));
        }

        JitsiMeetConferenceOptions options = optionsBuilder.build();

        // build and start intent to start a jitsi meet conference
        Intent intent = new Intent(getApplicationContext(), ConferenceActivity.class);
        intent.setAction("org.jitsi.meet.CONFERENCE");
        intent.putExtra("JitsiMeetConferenceOptions", options);
        startActivity(intent);
        finish();
    }

    // go to get involved page
    public void goGetInvolved(View V){
        Intent i = new Intent(this, GetInvolved.class);
        startActivity(i);
    }

    // listener for when a user clicks an event to go to its page
    private class ViewEventDetails_ClickListener implements View.OnClickListener{
        private Context mContext;
        private String eventCode;

        public ViewEventDetails_ClickListener(Context context, String eventID){
            mContext = context;
            eventCode = eventID;
        }

        @Override
        public void onClick(View v){
            Intent i = new Intent(mContext, EventWebview.class);
            i.putExtra("eventCode", eventCode); //Optional parameters
            startActivity(i);
        }
    }

    // Use EventsJSON file to fill in ScrollView
    public void setUpcomingEvents(String fullEventsJSON)
    {
        try {
            //link layout and JSON file
            LinearLayout linearLayout = findViewById(R.id.upcoming_events_linearView);
            JSONArray json = new JSONArray(fullEventsJSON);

            // Get the selected event filter text
            Spinner eventFilter_spinner = (Spinner)findViewById(R.id.eventsSpinner);
            String eventFilter_selection = eventFilter_spinner.getSelectedItem().toString();

            //For each event in the database, create a new item for it in ScrollView
            for(int i=0; i < json.length(); i++)
            {
                // gets the name and sets its values
                TextView newEvent_name = new TextView(this);
                newEvent_name.setTextSize(22);
                newEvent_name.setPadding(20, 70, 0, 0);
                newEvent_name.setTextColor(getResources().getColor(R.color.colorPrimary));
                newEvent_name.setText(json.getJSONObject(i).getString("name")
                        .substring(0, Math.min(json.getJSONObject(i).getString("name").length(), 18)));
                if (json.getJSONObject(i).getString("name").length() > 18)
                    newEvent_name.setText(newEvent_name.getText() + "...");

                // add listener to the name, so when the user clicks an event it will bring them to the event page
                newEvent_name.setOnClickListener(new ViewEventDetails_ClickListener(this, json.getJSONObject(i).getString("id")));

                // gets the host id and sets its values
                TextView newEvent_host = new TextView(this);    // TODO : CHANGE THIS "name" TO "user_id" WHEN DATABASE INCLUDES PERMALINK
                newEvent_host.setTextSize(15);
                newEvent_host.setWidth(600);
                newEvent_host.setPadding(20, 150, 0, 0);
                newEvent_host.setTextColor(Color.GRAY);
                newEvent_host.setText("Hosted by " + json.getJSONObject(i).getString("username")
                        .substring(0, Math.min(json.getJSONObject(i).getString("username").length(), 40)));
                if (json.getJSONObject(i).getString("username").length() > 40)
                    newEvent_host.setText(newEvent_host.getText() + "...");

                // gets the date of event
                TextView newEvent_time = new TextView(this);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");
                Date date = new Date();
                try {
                    date = dateFormat.parse(json.getJSONObject(i).getString("start_at"));
                } catch (ParseException e) { e.printStackTrace(); }

                // formats the date from above into viewable format
                SimpleDateFormat displayFormat = new SimpleDateFormat("EEE, MMM dd");
                newEvent_time.setText(displayFormat.format(date));
                newEvent_time.setTextSize(20);
                newEvent_time.setPadding(750, 80, 0, 0);
                newEvent_time.setTextColor(getResources().getColor(R.color.colorPrimary));

                // formats the date from above into viewable format (BUT NOW ITS START TIME)
                displayFormat = new SimpleDateFormat("h:mm aa");
                TextView newEvent_starttime = new TextView(this);
                newEvent_starttime.setText(displayFormat.format(date));
                newEvent_starttime.setTextSize(15);
                newEvent_starttime.setPadding(750, 150, 0, 0);
                newEvent_starttime.setTextColor(Color.GRAY);

                // Now you have all your TextViews, create a ConstraintLayout for each one
                ConstraintLayout constraintLayout = new ConstraintLayout(this);
                constraintLayout.setLayoutParams(new LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, getResources().getDisplayMetrics())));

                // Add your TextViews to the ConstraintLayout
                constraintLayout.addView(newEvent_name);
                constraintLayout.addView(newEvent_host);
                constraintLayout.addView(newEvent_time);
                constraintLayout.addView(newEvent_starttime);

                // Add simple divider to put in between ConstraintLayouts (ie events)
                View viewDivider = new View(this);
                viewDivider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 2));
                viewDivider.setBackgroundColor(Color.LTGRAY);

                //get current date and what week it is
                Calendar mCalendar = Calendar.getInstance();

                switch(eventFilter_selection)
                {
                    case ("All Events") :
                    {
                        linearLayout.addView(constraintLayout);
                        linearLayout.addView(viewDivider);
                        break;
                    }
                    case ("This Week") :
                    {
                        mCalendar.set(Calendar.WEEK_OF_MONTH, (mCalendar.get(Calendar.WEEK_OF_MONTH) + 1));
                        Date filterDate = mCalendar.getTime();

                        if (date.before(filterDate))
                        {
                            linearLayout.addView(constraintLayout);
                            linearLayout.addView(viewDivider);
                        }
                        break;
                    }
                    case ("Next Week") :
                    {
                        mCalendar.set(Calendar.WEEK_OF_MONTH, (mCalendar.get(Calendar.WEEK_OF_MONTH) + 1));
                        Date filterDate = mCalendar.getTime();

                        if (date.after(filterDate))
                        {
                            mCalendar.set(Calendar.WEEK_OF_MONTH, (mCalendar.get(Calendar.WEEK_OF_MONTH) + 1));
                            filterDate = mCalendar.getTime();

                            if (date.before(filterDate))
                            {
                                linearLayout.addView(constraintLayout);
                                linearLayout.addView(viewDivider);
                            }
                        }
                        break;
                    }
                    case ("Future") :
                    {
                        mCalendar.set(Calendar.WEEK_OF_MONTH, (mCalendar.get(Calendar.WEEK_OF_MONTH) + 2));
                        Date filterDate = mCalendar.getTime();

                        if (date.after(filterDate))
                        {
                            linearLayout.addView(constraintLayout);
                            linearLayout.addView(viewDivider);
                        }
                        break;
                    }
                }
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void getEventsJSONfile()
    {
        // where you get the JSON file
        final String url = "https://thinqtv.herokuapp.com/events.json";

        //RequestQueue initialized
        RequestQueue mRequestQueue;
        mRequestQueue = Volley.newRequestQueue(this);

        //String Request initialized
        StringRequest mStringRequest;
        mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // If you receive a response, set the fullEventsJSON string to the response
                // Then call setUpcomingEvents() to fill in ScrollView data
                setUpcomingEvents(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO : ADD SOMETHING TO HAPPEN WHEN JSON IS NOT REACHABLE
                // TODO : ie display error message
            }
        });

        //Add the request to the Queue
        //This is essentially telling it to execute
        mRequestQueue.add(mStringRequest);
    }

    public void initializeEvents()
    {
        // get the spinner filter and the layout that's inside of it
        Spinner eventFilter = (Spinner) findViewById(R.id.eventsSpinner);
        LinearLayout layout = (LinearLayout) findViewById(R.id.upcoming_events_linearView);

        // add listener for whenever a user changes filter
        eventFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                layout.removeAllViews();
                getEventsJSONfile();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                // this doesn't ever happen but i need to override the virtual class
            }
        });
    }

    public void expandEventsClick(View v)
    {
        // Link the header TextView
        TextView header = (TextView) findViewById(R.id.upcoming_events_header);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) header.getLayoutParams();

        // Link any buttons
        TextView carrot = (TextView) findViewById(R.id.expandButton);
        Button joinButton = findViewById(R.id.defaultJoinButton);

        // if the Upcoming Events are expanded, minimize
        if (eventsExpanded)
        {
            // convert pixels to dp and set the margin
            float headerMarginSmall = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    450f,
                    getResources().getDisplayMetrics()
            );
            params.topMargin = (int) headerMarginSmall;

            // the buttons are always visible under the ScrollView for some reason
            // because of this, they must be set to invisible when you expand the Events
            joinButton.setVisibility(View.VISIBLE);

            // make it twist
            carrot.setRotation(0);
            ConstraintLayout.LayoutParams lparams = (ConstraintLayout.LayoutParams) carrot.getLayoutParams();
            lparams.verticalBias = 0.33f;
            carrot.setLayoutParams(lparams);

            // for when it gets clicked again
            eventsExpanded = false;
        }
        // when the Upcoming Events are expanded already, this code collapses it
        // it's just the opposite of the above code essentially
        else
        {
            float headerMarginLarge = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    215f,
                    getResources().getDisplayMetrics()
            );
            params.topMargin = (int) headerMarginLarge;

            joinButton.setVisibility(View.INVISIBLE);
            carrot.setRotation(180);
            ConstraintLayout.LayoutParams lparams = (ConstraintLayout.LayoutParams) carrot.getLayoutParams();
            lparams.verticalBias = 0.48f;
            carrot.setLayoutParams(lparams);

            eventsExpanded = true;
        }

        // move header based on the values set in the if-else statement
        // other items are linked to the header so they will move as well
        header.setLayoutParams(params);
    }

    private void generateSidebar() {
        ListView mDrawerList = (ListView)findViewById(R.id.navList);
        DrawerLayout mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        ArrayAdapter<String> mAdapter;

        String[] osArray = getResources().getStringArray(R.array.sidebar_menu);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);

        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();

                if (position == 2)
                {
                    goGetInvolved(view);
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Activate the navigation drawer toggle
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}