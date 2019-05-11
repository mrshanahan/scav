package itme.localcalendarexporter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    public static final String[] CALENDAR_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 1
            CalendarContract.Calendars.ACCOUNT_TYPE,                  // 2
            CalendarContract.Calendars.ACCOUNT_NAME,                    // 3
    };

    public static final int CALENDAR_PROJECTION_ID = 0;

    private static final int _MAX_RESULTS_DISPLAYED = 50;

    private static final String _QUERY_RESULTS_ITEM = "queryResults";

    private TextView mDisplayText;
    private EditText mEditQuery;
    //private ArrayList<String> mQueryResults = null;
    private JSONArray mQueryResults = null;
    private Lock mQueryLock = null;
    private boolean mQueryInProgress = false;
    private File outputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDisplayText = (TextView) findViewById(R.id.display);
        mEditQuery = (EditText) findViewById(R.id.edit_query);
        mQueryLock = (mQueryLock == null) ? new ReentrantLock() : mQueryLock;

        if (savedInstanceState != null) {
            try {
                mQueryResults = new JSONArray(savedInstanceState.getString(_QUERY_RESULTS_ITEM));
            }
            catch (JSONException ex) {
                Toast.makeText(
                        this,
                        "Failed to load state information: " + ex.getLocalizedMessage(),
                        Toast.LENGTH_LONG)
                    .show();
                mQueryResults = null;
            }

            if (mQueryResults != null) {
                setDisplayText(mQueryResults);
            }
        }

        FloatingActionButton fab_search = findViewById(R.id.fab_search);
        fab_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mQueryInProgress)
                {
                    if (mQueryLock.tryLock())
                    {
                        try
                        {
                            if (!mQueryInProgress)
                            {
                                mQueryInProgress = true;
                                String queryString = mEditQuery.getText().toString().trim();
                                CalendarQuery query;
                                try {
                                    query = CalendarQuery.parse(queryString);
                                    AsyncTask<CalendarQuery, Void, JSONArray> queryTask = new CalendarQueryTask();
                                    queryTask.execute(query);
                                }
                                catch (QueryParseException ex) {
                                    mQueryInProgress = false;
                                    Snackbar.make(view, ex.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                                }
                                catch (Exception ex) {
                                    mQueryInProgress = false;
                                    throw ex;
                                }
                            }
                            else
                            {
                                Snackbar.make(view, "Query already in progress", Snackbar.LENGTH_INDEFINITE)
                                        .show();
                            }
                        }
                        finally
                        {
                            mQueryLock.unlock();
                        }
                    }
                }
                else
                {
                    Snackbar.make(view, "Query already in progress", Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });
    }

    //public void setDisplayText(ArrayList<String> results) {
    public void setDisplayText(JSONArray results) {
        int maxResults = Math.min(_MAX_RESULTS_DISPLAYED, results.length());

        try {
            for (int i = 0; i < maxResults; i++) {
                mDisplayText.append(results.get(i).toString());
                mDisplayText.append("\n\n");
            }
        }
        catch (JSONException ex) {
            Toast.makeText(this, "Unable to fully display results: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(_QUERY_RESULTS_ITEM, mQueryResults.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private boolean onActionSendSelected() {
        if (mQueryResults == null)
        {
            Toast.makeText(this, "No data to send!", Toast.LENGTH_LONG).show();
            return false;
        }
        Uri resultsUri;
        File outputFile;
        try
        {
            String outputFileName = String.format("%d.json", System.currentTimeMillis());
            File outputDir = new File(getCacheDir(), "results");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
            outputFile = new File(outputDir, outputFileName);
            PrintWriter writer = new PrintWriter(outputFile.getAbsolutePath());
            try {
                String serializedResults = mQueryResults.toString();
                writer.println(serializedResults);
            }
            finally {
                writer.close();
            }

            // Gotta go through FilerProvider b/c we have to share this URI
            // with whatever email app the user chooses, which requires going
            // through the FileProvider API starting with API version 17 (?).
            // Check out the manifest to see the configured FileProvider for
            // this application.
            resultsUri = FileProvider.getUriForFile(
                    this,
                    "itme.localcalendarexporter.fileprovider",
                    outputFile);
        }
        catch (IOException ex)
        {
            Toast.makeText(this,
                    String.format(Locale.getDefault(), "Failed to create results file: %s", ex.getLocalizedMessage()),
                    Toast.LENGTH_LONG)
                    .show();
            return false;
        }

        // Using ACTION_SEND here instead of ACTION_SENDTO b/c we are
        // attaching the results as a document. If you want to specifically
        // send an email w/o an attachment, use ACTION_SENDTO.
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Results from LocalCalendarExporter");
        sendIntent.putExtra(Intent.EXTRA_STREAM, resultsUri);

        Intent chooseSendApp = Intent.createChooser(sendIntent, "Emailing results:");
        startActivity(chooseSendApp);
        return true;
    }

    private boolean onActionHelpSelected() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        boolean retVal;

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send) {
            retVal = onActionSendSelected();
        }
        else if (id == R.id.action_help) {
            retVal = onActionHelpSelected();
        }
        else {
            retVal = super.onOptionsItemSelected(item);
        }

        return retVal;
    }

    private class CalendarQueryTask extends AsyncTask<CalendarQuery, Void, JSONArray> {
        @Override
        protected void onPostExecute(JSONArray results) {
            if (results == null) {
                Snackbar.make(findViewById(R.id.root_layout),
                            "Could not find local calendar",
                            Snackbar.LENGTH_SHORT)
                        .show();
            }
            else {
                setDisplayText(results);
            }
        }

        @Override
        protected JSONArray doInBackground(CalendarQuery... queries) {
            CalendarQuery query = queries[0];
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                    + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
            String[] selectionArgs = new String[] {"My calendar", "LOCAL"};

            Cursor calCur = null, eventsCur = null;
            try
            {
                calCur = query(uri, CALENDAR_PROJECTION, selection, selectionArgs);
                long localCalId = 0;
                while (calCur.moveToNext())
                {
                    localCalId = calCur.getLong(CALENDAR_PROJECTION_ID);
                }

                if (localCalId == 0)
                {
                    return null;
                }

                eventsCur = query(CalendarContract.Events.CONTENT_URI,
                        CalendarQuery.getEventProjection(),
                        query.getSelectionText(),
                        query.getSelectionArguments(localCalId));

                JSONArray jsonEvents = new JSONArray();
                while (eventsCur.moveToNext())
                {
                    long id = eventsCur.getLong(0);
                    int allDay = eventsCur.getInt(1);
                    int availability = eventsCur.getInt(2);
                    String description = eventsCur.getString(3);
                    long eventEnd = eventsCur.getLong(4);
                    long eventStart = eventsCur.getLong(5);
                    String duration = eventsCur.getString(6);
                    String eventLocation = eventsCur.getString(7);
                    String eventTimezone = eventsCur.getString(8);
                    String exceptionDate = eventsCur.getString(9);
                    String exceptionRule = eventsCur.getString(10);
                    long recurrenceEnd = eventsCur.getLong(11);
                    int originalAllDay = eventsCur.getInt(12);
                    long originalId = eventsCur.getLong(13);
                    long originalInstanceTime = eventsCur.getLong(14);
                    String recurrenceDates = eventsCur.getString(15);
                    String recurrenceRule = eventsCur.getString(16);
                    int status = eventsCur.getInt(17);
                    String title = eventsCur.getString(18);

                    JSONObject thisEvent = new JSONObject();
                    try
                    {
                        thisEvent.put(CalendarContract.Events._ID, id);
                        thisEvent.put(CalendarContract.Events.ALL_DAY, allDay);
                        thisEvent.put(CalendarContract.Events.AVAILABILITY, availability);
                        thisEvent.put(CalendarContract.Events.DESCRIPTION, description);
                        thisEvent.put(CalendarContract.Events.DTEND, eventEnd);
                        thisEvent.put(CalendarContract.Events.DTSTART, eventStart);
                        thisEvent.put(CalendarContract.Events.DURATION, duration);
                        thisEvent.put(CalendarContract.Events.EVENT_LOCATION, eventLocation);
                        thisEvent.put(CalendarContract.Events.EVENT_TIMEZONE, eventTimezone);
                        thisEvent.put(CalendarContract.Events.EXDATE, exceptionDate);
                        thisEvent.put(CalendarContract.Events.EXRULE, exceptionRule);
                        thisEvent.put(CalendarContract.Events.LAST_DATE, recurrenceEnd);
                        thisEvent.put(CalendarContract.Events.ORIGINAL_ALL_DAY, originalAllDay);
                        thisEvent.put(CalendarContract.Events.ORIGINAL_ID, originalId);
                        thisEvent.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, originalInstanceTime);
                        thisEvent.put(CalendarContract.Events.RDATE, recurrenceDates);
                        thisEvent.put(CalendarContract.Events.RRULE, recurrenceRule);
                        thisEvent.put(CalendarContract.Events.STATUS, status);
                        thisEvent.put(CalendarContract.Events.TITLE, title);

                        jsonEvents = jsonEvents.put(thisEvent);
                    }
                    catch (JSONException ex)
                    {
                    }

                    // sb.append("Event Start: ").append(eventStart).append(String.format(" (%s)", new java.util.Date(eventStart).toString())).append("\n");
                }

                mQueryResults = jsonEvents;

                return jsonEvents;
            }
            finally
            {
                if (calCur != null)
                {
                    calCur.close();
                }
                if (eventsCur != null)
                {
                    eventsCur.close();
                }
            }
        }

        private Cursor query(Uri uri, String[] projections, String selection, String[] selectionArgs)
        {
            ContentResolver cr = getContentResolver();
            return getContentResolver().query(uri, projections, selection, selectionArgs, null);
        }
    }
}
