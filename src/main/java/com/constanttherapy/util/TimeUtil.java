/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */

package com.constanttherapy.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtil
{
	public static Timestamp timeNow()
	{
		return new java.sql.Timestamp(new java.util.Date().getTime());
	}

	public static Timestamp timeNowUTC()
	{
        // too convoluted.  find a better way!
        Timestamp ts = timeNow();
        Long tsl = ts.getTime();
        String dateString = getUTCtimeFromLong(tsl);


        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

        try
        {
            Date dt = df.parse(dateString);
            return new Timestamp(dt.getTime());
        }
        catch (ParseException e)
        {
            return null;
        }
	}

	/**
	 * Gets current time in unix epoch (long int) format.  This can be used
	 * to timestamp files for version control.
	 *
	 * @return Milliseconds elapsed since Jan 1, 1970
	 */
	public static long getEpochFromCurrentTime()
	{
		return System.currentTimeMillis() / 1000L;
	}

	/**
	 * Get Unix epoch long int from provided Timestamp
	 * @param ts
	 * @return
	 */
	public static long getEpochFromTimestamp(Timestamp ts)
	{
		return ts.getTime();
	}

	/**
	 * Return Unix epoch long int from provided date string
	 * @param dateString
	 * @return
	 */
	public static long getEpochFromDate(String dateString)
	{
		try
		{
			Timestamp ts = TimeUtil.getTimestampFromDate(dateString);
			return ts.getTime();
		}
		catch(Exception ex)
		{
			// default to current time
			return getEpochFromCurrentTime();
		}
	}

	/**
	 * Returns current time in yyyy-MM-dd_HH-mm-ss format
	 * @return
	 */
	public static String timeNowString()
	{
		return timeNowString(null);
	}

	public static String timeNowString(String format)
	{
        if (format == null)
            format = "yyyy-MM-dd_HH-mm-ss";

		return timeStampToFormattedString(null, format);
	}

    /*
    public static String timeNowString(String format)
	{
		return new SimpleDateFormat(format).format(new java.util.Date().getTime());
	}
    */

	public static String timeStampToFormattedString(Timestamp ts, String format)
	{
		if (ts == null)
            ts = timeNow();

		if (format == null)
			format = "yyyy-MM-dd_HH-mm-ss";

		return new SimpleDateFormat(format).format(ts);
	}

	public static String DateDaysAgo(long days)
	{
		long timeThen = new java.util.Date().getTime() - (days * 86400000);
		return new SimpleDateFormat("yyyy-MM-dd").format(timeThen);
	}

	public static enum TimeUnits
	{
		WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS
	};

	public static Long timeSince(Timestamp time, TimeUnits timeUnit)
	{
		return timeDiff(time, null, timeUnit);
	}

	/**
	 * Time difference in milliseconds.
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static Long timeDiff(Timestamp startTime, Timestamp endTime)
	{
		if (endTime == null)
			endTime = timeNow();

		return (endTime.getTime() - startTime.getTime());
	}

	/**
	 * Time difference in specified units
	 * @param startTime
	 * @param endTime
	 * @param timeUnit
	 * @return
	 */
	public static Long timeDiff(Timestamp startTime, Timestamp endTime, TimeUnits timeUnit)
	{
		if (endTime == null)
			endTime = new Timestamp(new java.util.Date().getTime());
		Long diff = timeDiff(startTime, endTime);

		switch (timeUnit)
		{
			case WEEKS: // TODO: 11:59pm to 12:01am should count as 1 day
				diff = (long) (diff / (86400000*7));
				break;
			case DAYS: // TODO: 11:59pm to 12:01am should count as 1 day
				diff = (long) (diff / 86400000);
				break;
			case HOURS:
				diff = (long) (diff / 3600000);
				break;
			case MINUTES:
				diff = (long) (diff / 60000);
				break;
			case SECONDS:
				diff = (long) (diff / 1000);
				break;
			case MILLISECONDS:
				break;
		}

		return diff;
	}

	/**
	 * Offsets the timestamp by the specified number of seconds
	 *
	 * @param timestamp
	 * @param nSeconds
	 * @return
	 */
	public static Timestamp addTime(Timestamp timestamp, Double nSeconds)
	{
		long time = timestamp.getTime() + (long) (nSeconds * 1000);
		return new Timestamp(time);
	}

	public static Timestamp subtractTime(Timestamp timestamp, Double offset)
	{
		return addTime(timestamp, (-1.0 * offset));
	}

	public static Timestamp getTimestampFromDate(String dateString)
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

		try
		{
			Date dt = df.parse(dateString);
			return new Timestamp(dt.getTime());
		}
		catch (ParseException e)
		{
			return null;
		}
	}

	public static Timestamp getTimestampFromLong(long ts)
	{
		Long ms = ts * 1000;

		// java.util.Date dt = new java.util.Date(ms);
		// convert to UTC
		Date dt = convertLocalTimestamp(ms);
		return new Timestamp(dt.getTime());
	}

	public static String getUTCtimeFromLong(long ts)
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		java.util.Date dt = new java.util.Date(ts);

		return df.format(dt);
	}

	public static Integer getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static boolean isToday(Timestamp ts)
	{
		return TimeUtil.isSameDay(ts, TimeUtil.timeNow());
	}

	@SuppressWarnings("deprecation")
	public static boolean isSameDay(Timestamp ts1, Timestamp ts2)
	{
		return (ts1.getDate() == ts2.getDate() && ts1.getMonth() == ts2.getMonth() && ts1.getYear() == ts2.getYear());
	}

    public static String getDateFromTimestamp(Timestamp ts)
    {
        return getDateFromTimestamp(ts, DateFormat.LONG);
    }

	public static String getDateFromTimestamp(Timestamp ts, int format)
	{
		if (ts == null) return null;
		DateFormat df = DateFormat.getDateInstance(format);
		return df.format(ts);
	}

	public static Date convertLocalTimestamp(long millis)
	{
		TimeZone tz = TimeZone.getDefault();
		Calendar c = Calendar.getInstance(tz);
		long localMillis = millis;
		int offset, time;

		c.set(1970, Calendar.JANUARY, 1, 0, 0, 0);

		// Add milliseconds
		while (localMillis > Integer.MAX_VALUE)
		{
			c.add(Calendar.MILLISECOND, Integer.MAX_VALUE);
			localMillis -= Integer.MAX_VALUE;
		}
		c.add(Calendar.MILLISECOND, (int) localMillis);

		// Stupidly, the Calendar will give us the wrong result if we use getTime() directly.
		// Instead, we calculate the offset and do the math ourselves.
		time = c.get(Calendar.MILLISECOND);
		time += c.get(Calendar.SECOND) * 1000;
		time += c.get(Calendar.MINUTE) * 60 * 1000;
		time += c.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
		offset = tz.getOffset(c.get(Calendar.ERA), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.DAY_OF_WEEK), time);

		return new Date(millis - offset);
	}

	private static Calendar utcCalendar = null;

	public static Calendar getUtcCalendar()
	{
		if (utcCalendar == null)
			utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		return 	utcCalendar;
	}
}
