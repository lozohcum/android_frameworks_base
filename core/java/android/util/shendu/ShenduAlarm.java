package android.util.shendu;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;

/*
 * Add by fwsun
 * 2012.8.28
 * add the class for turn device off alarm
 * 
 * @hide
 */

public class ShenduAlarm {
	private static boolean DEBUG = true;

	private static class Columns implements BaseColumns {
		/**
		 * The content:// style URL for this table
		 */
		private static final Uri CONTENT_URI = Uri
				.parse("content://com.android.deskclock/alarm");

		/**
		 * Hour in 24-hour localtime 0 - 23.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		private static final String HOUR = "hour";

		/**
		 * Minutes in localtime 0 - 59
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		private static final String MINUTES = "minutes";

		/**
		 * Days of week coded as integer
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		private static final String DAYS_OF_WEEK = "daysofweek";

		/**
		 * Alarm time in UTC milliseconds from the epoch.
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		private static final String ALARM_TIME = "alarmtime";

		/**
		 * True if alarm is active
		 * <P>
		 * Type: BOOLEAN
		 * </P>
		 */
		private static final String ENABLED = "enabled";

		/**
		 * True if alarm should vibrate
		 * <P>
		 * Type: BOOLEAN
		 * </P>
		 */
		private static final String VIBRATE = "vibrate";

		/**
		 * Message to show when alarm triggers Note: not currently used
		 * <P>
		 * Type: STRING
		 * </P>
		 */
		private static final String MESSAGE = "message";

		/**
		 * Audio alert to play when alarm triggers
		 * <P>
		 * Type: STRING
		 * </P>
		 */
		private static final String ALERT = "alert";

		/**
		 * The default sort order for this table
		 */
		private static final String DEFAULT_SORT_ORDER = HOUR + ", " + MINUTES
				+ " ASC";

		// Used when filtering enabled alarms.
		private static final String WHERE_ENABLED = ENABLED + "=1";

		static final String[] ALARM_QUERY_COLUMNS = { _ID, HOUR, MINUTES,
				DAYS_OF_WEEK, ALARM_TIME, ENABLED, VIBRATE, MESSAGE, ALERT };

		/**
		 * These save calls to cursor.getColumnIndexOrThrow() THEY MUST BE KEPT
		 * IN SYNC WITH ABOVE QUERY COLUMNS
		 */
		private static final int ALARM_ID_INDEX = 0;
		private static final int ALARM_HOUR_INDEX = 1;
		private static final int ALARM_MINUTES_INDEX = 2;
		private static final int ALARM_DAYS_OF_WEEK_INDEX = 3;
		private static final int ALARM_TIME_INDEX = 4;
		private static final int ALARM_ENABLED_INDEX = 5;
		private static final int ALARM_VIBRATE_INDEX = 6;
		private static final int ALARM_MESSAGE_INDEX = 7;
		private static final int ALARM_ALERT_INDEX = 8;
	}

	private int id;
	private boolean enabled;
	private int hour;
	private int minutes;
	private DaysOfWeek daysOfWeek;
	private long time;
	private boolean vibrate;
	private String label;
	private Uri alert;
	private boolean silent;

	public ShenduAlarm(Cursor c) {
		id = c.getInt(Columns.ALARM_ID_INDEX);
		enabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
		hour = c.getInt(Columns.ALARM_HOUR_INDEX);
		minutes = c.getInt(Columns.ALARM_MINUTES_INDEX);
		daysOfWeek = new DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
		time = c.getLong(Columns.ALARM_TIME_INDEX);
		vibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
		label = c.getString(Columns.ALARM_MESSAGE_INDEX);
		String alertString = c.getString(Columns.ALARM_ALERT_INDEX);
		if ("silent".equals(alertString)) {
			silent = true;
		} else {
			if (alertString != null && alertString.length() != 0) {
				alert = Uri.parse(alertString);
			}

			// If the database alert is null or it failed to parse, use the
			// default alert.
			if (alert == null) {
				alert = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_ALARM);
			}
		}
		if(DEBUG){
			Log.d("ShenDuAlarm","-------"+toString());
		}
	}

	public static Cursor getAlarmsCursor(ContentResolver contentResolver) {
		return contentResolver.query(Columns.CONTENT_URI, Columns.ALARM_QUERY_COLUMNS,
				Columns.ENABLED + "= 1", null, Columns.DEFAULT_SORT_ORDER);
	}
	
	public static long getClockTime(Context context) {
		Cursor cur = getAlarmsCursor(context.getContentResolver());
		if (cur != null && cur.getCount() != 0) {
		long[] time = new long[cur.getCount()];
		int i = 0;		
			for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {

				ShenduAlarm am = new ShenduAlarm(cur);
				time[i] = am.calculateAlarm(am);
				i++;
			}
			Arrays.sort(time);
			return time[0];
		}
		return 0;
		
	}
	
	public String toString() {
		return "id= " + id + "enabled= " + enabled + " hour= " + hour + " minutes= "
				+ minutes + " daysOfWeek= " + daysOfWeek + " time= "
				+ time + " vibrate= " + vibrate + " label= " + label + " silent= "
				+ silent + " alert= " + alert;
	}

	private long calculateAlarm(ShenduAlarm alarm) {
		return calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek)
				.getTimeInMillis();
	}

	private Calendar calculateAlarm(int hour, int minute,
			ShenduAlarm.DaysOfWeek daysOfWeek) {

		// start with now
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());

		int nowHour = c.get(Calendar.HOUR_OF_DAY);
		int nowMinute = c.get(Calendar.MINUTE);

		// if alarm is behind current time, advance one day
		if (hour < nowHour || hour == nowHour && minute <= nowMinute) {
			c.add(Calendar.DAY_OF_YEAR, 1);
		}
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		int addDays = daysOfWeek.getNextAlarm(c);
		if (addDays > 0)
			c.add(Calendar.DAY_OF_WEEK, addDays);
		return c;
	}

	static final class DaysOfWeek {

		private int mDays;

		DaysOfWeek(int days) {
			mDays = days;
		}

		private boolean isSet(int day) {
			return ((mDays & (1 << day)) > 0);
		}

		public int getNextAlarm(Calendar c) {
			if (mDays == 0) {
				return -1;
			}

			int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

			int day = 0;
			int dayCount = 0;
			for (; dayCount < 7; dayCount++) {
				day = (today + dayCount) % 7;
				if (isSet(day)) {
					break;
				}
			}
			return dayCount;
		}
	}
}
