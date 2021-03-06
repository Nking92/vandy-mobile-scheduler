package com.vmat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;



/**
 * DetailActivity is called when an item from the main meetings viewer
 * is selected. An intent is passed in to DetailActivity containing the 
 * _id field from the corresponding item in the database with which to 
 * populate the activity. 
 */
public class DetailActivity extends SherlockMapActivity{

	private boolean alarmActive; 

	// if -1, it will launch the dialog box when the alarm button is pressed and
	// cancel it if already set.
	// if anything else, the alarm will be set to that time.
	public long millisPrior = -1;

	private EventsDB hasDatabase; 
	private Cursor myInfo;
	private TextView topic;
	private TextView speaker;
	private TextView date;
	private TextView time;
	private TextView food_speaker;
	private TextView description;
	private TextView details;
	private MapView mapthumb;
	private GeoPoint center;
	private MyLocationOverlay me = null;
	

    @Override
	public void onCreate(Bundle savedInstantState){
		setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        super.onCreate(savedInstantState);
        setContentView(R.layout.detail_activity);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Initialize Textviews
		topic = (TextView)findViewById(R.id.topic);
		speaker = (TextView)findViewById(R.id.speaker);
		date = (TextView)findViewById(R.id.date);
		time = (TextView)findViewById(R.id.time);
		food_speaker = (TextView)findViewById(R.id.food_speaker);
		description = (TextView)findViewById(R.id.description);
		mapthumb = (MapView)findViewById(R.id.mapthumb);

		// Initialize database and cursor
		startManagingData();

		alarmActive = (1==myInfo.getInt(myInfo.getColumnIndex(EventsDB.ALARM_ACTIVE)));

		// Find center geopoint and create map
		center = getCenter();
		initMap();

		fillTextViews(myInfo);

		// If this activity is started in response to the alarm, 
		// show a dialog box saying so.
		if (getIntent().getIntExtra("alarmReceived", 0) != 0){
			// make dialog for receiving alarm
			alarmTriggeredDialog();
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		me.enableMyLocation();
		if (myInfo.isClosed()){
			// Initialize database and cursor
			startManagingData();
		}
	}

	@Override
	public void onPause(){
		super.onPause();
		// Clean up
		me.disableMyLocation();
		myInfo.close();
		hasDatabase.close();
	}



	@Override
	protected boolean isRouteDisplayed(){
		return false;
	}

	private void startManagingData(){
		hasDatabase = new EventsDB(this);
		String[] index = new String[1];
		index[0] = "" + getIntent().getIntExtra("id", -1);
		myInfo = hasDatabase.getReadableDatabase()
				.rawQuery("SELECT * FROM meetings WHERE _id=? LIMIT 1", index);
        myInfo.moveToFirst();

	}

	///////////////////////////////////////
	// Button click listeners
	///////////////////////////////////////

	/**
	 * Called when the "Set Alarm" button is selected
	 */
	public void alarmSet(View view){
		
		if ( !alarmActive )
			// Will create a dialog box listener that will call switchAlarm
			// if an option is selected. 
			setAlarmMillisPrior();
		else 
			// This will cancel an active alarm
			switchAlarm();
	}

	/**
	 * Called when the user opens a map for directions.
	 * Since google maps should be optional, this will attempt to open 
	 * a browser and populate the address field in Maps with the destination
	 * if not available.
	 */
	public void openMap(View view){
		String url = new String("http://maps.google.com/maps?daddr=" +
			center.getLatitudeE6()/1000000.0 + "," + center.getLongitudeE6()/1000000.0);
		Log.i("DetailActivity", url);
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
			Uri.parse(url));
		startActivity(intent);
	}

	/**
	 * Private function used to set the alarm and set the appropriate fields in the 
	 * database based on their current state.
	 */
	 private void switchAlarm(){

		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);	

		// Reads in the stored date, parses it, and stores it in a Calendar object
		// for the alarm service.
		String UTCdate = myInfo.getString(myInfo.getColumnIndex(EventsDB.DATE));
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date parsedDate = new Date();
		try{
			parsedDate = format.parse(UTCdate);
		}catch(ParseException e){
			e.printStackTrace();
		}
		Calendar alarmCalendar = Calendar.getInstance();
		alarmCalendar.setTime(parsedDate);

		Intent intent = new Intent(this, DetailActivity.class);
		intent.putExtra("id", myInfo.getInt(myInfo.getColumnIndex("_id"))).
			putExtra("alarmReceived", 1);
		PendingIntent pi = PendingIntent.getActivity(
			getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (!alarmActive){

			long alarmTime = alarmCalendar.getTimeInMillis() - millisPrior;

			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pi);
			Button btn = (Button)findViewById(R.id.alarm_button);
			btn.setText("Cancel Alarm");
			alarmActive = true;
		
		}
		else{
			millisPrior = 0;
			alarmManager.cancel(pi);
			Button btn = (Button)findViewById(R.id.alarm_button);
			btn.setText("Set Alarm");
			alarmActive = false;
		}

		// Set the database ALARM_ACTIVE value to 1 if alarm is set, 
		// otherwise, set to 0.
		EventsDB db = new EventsDB(this);
		int numChanged = db.updateAlarm(getIntent().getIntExtra("id", -1), 
			alarmActive, millisPrior);
		db.close();
		Log.i("DetailActivity", "Number of alarms changed: " + numChanged);

	 }

	///////////////////////////////////////
	// Helper functions and classes
	///////////////////////////////////////

	/**
	 * Private helper function used for updating the values of the TextViews
	 * with the values in the cursor. 
	 * This assumes that the cursor points to the currect data.
	 */
	private void fillTextViews(Cursor c){
		// Fill Strings with cursor data
		String topicText, speakerText, dateText, descrText;
		boolean isFood;
		topicText = c.getString(c.getColumnIndex(EventsDB.TOPIC));
		speakerText = c.getString(c.getColumnIndex(EventsDB.SPEAKER_NAME));
		dateText = c.getString(c.getColumnIndex(EventsDB.DATE));
		isFood = (1==c.getInt(c.getColumnIndex(EventsDB.FOOD)));
		descrText = c.getString(c.getColumnIndex(EventsDB.DESCRIPTION));

		// Fill TextViews with Strings
		topic.setText(topicText);
		speaker.setText(speakerText);
		description.setText(descrText);	
		if (isFood)
			food_speaker.setText("There's food too!");

		// Format the date
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date parsed = new Date();
		try{
			parsed = format.parse(dateText);
		}catch(ParseException e){
			e.printStackTrace();
		}

		// ex - Wednesday, January 10 @ 7:30 PM
		date.setText(DateFormat.format("EEEE, MMMM d", parsed));
		time.setText(DateFormat.format("h:mm a", parsed));

		if (alarmActive){
			Button btn = (Button)findViewById(R.id.alarm_button);
			btn.setText("Cancel Alarm");
		}
	}

	/**
	 * Creates a dialog box which presents the user with a number of time intervals.
	 * The time interval they select will be stored in the database with the 
	 * corresponding entry, and the alarm will go off that number of minutes
	 * before the start of the meeting/event.
	 * @return the number of minutes before the event at which the alarm will go off.
	 * or -1 if the dialog was cancelled without a time set.
	 */
	private void setAlarmMillisPrior(){
		final CharSequence[] items = {"On time", "5 minutes before", "10 minutes before",
			"15 minutes before", "30 minutes before", "1 hour before"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("At what time?")
			.setItems(items, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int item){
					switch (item){
						case 0: millisPrior = 0; break;
						case 1: millisPrior = (5 * 1000 * 60) ; break;
						case 2: millisPrior = (10 * 1000 * 60) ; break;
						case 3: millisPrior = (15 * 1000 * 60) ; break;
						case 4: millisPrior = (30 * 1000 * 60) ; break;
						case 5: millisPrior = (60 * 1000 * 60) ; break;
						default:millisPrior = -1;
					}
					switchAlarm();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Creates a dialog box in response to the event alarm going off.
	 */
	private void alarmTriggeredDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("This event is starting soon!")
			.setPositiveButton("Okay", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int id){
					dialog.cancel();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Initializes the map with the destination and the current location overlays.
	 */
	private void initMap(){
		mapthumb.getController().setCenter(center);
		mapthumb.getController().setZoom(16);
		//add destination marker
		Drawable marker = getResources().getDrawable(R.drawable.pushpin);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		mapthumb.getOverlays().add(new SiteOverlay(marker));
		// Add location marker
		me = new MyLocationOverlay(this, mapthumb);
		mapthumb.getOverlays().add(me);

	}

	private GeoPoint getCenter(){
		double latitude, longitude;
		latitude = myInfo.getDouble(myInfo.getColumnIndex(EventsDB.XCOORD));
		longitude = myInfo.getDouble(myInfo.getColumnIndex(EventsDB.YCOORD));
		return new GeoPoint((int)(latitude*1000000.0), (int)(longitude*1000000.0));
	}


	/**
	 * Contains the logic for displaying an overlay for the destination
	 * and the current position on the minimap
	 */
	private class SiteOverlay extends ItemizedOverlay<OverlayItem>{
		private OverlayItem location;

		public SiteOverlay(Drawable marker){
			super(marker);
			boundCenterBottom(marker);
			location = new OverlayItem(center, "Destination", 
				topic.getText().toString());
			populate();
		}

		@Override
		public int size(){
			return 1;
		}

		@Override
		protected OverlayItem createItem(int index){
			return location;
		}
	}
}
