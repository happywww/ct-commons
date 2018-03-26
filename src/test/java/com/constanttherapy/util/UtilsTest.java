package com.constanttherapy.util;

import com.constanttherapy.util.TimeUtil;
import com.constanttherapy.util.TimeUtil.TimeUnits;
import org.junit.Test;

import java.io.File;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilsTest
{
	@SuppressWarnings("deprecation")
    @Test
	public void testTimeDiff()
	{
		java.util.Date date = new java.util.Date(2012,9,1);
		Timestamp startTime = new Timestamp(date.getTime());

		Timestamp newTime = TimeUtil.addTime(startTime, 60.0);
		assertTrue(TimeUtil.timeDiff(startTime, newTime) == 60000);

		date = new java.util.Date(2012,9,5);
		Timestamp endTime = new Timestamp(date.getTime());

		long diff = TimeUtil.timeDiff(startTime, endTime);
		assertTrue(diff > 0);

		diff = TimeUtil.timeDiff(startTime, endTime, TimeUnits.DAYS);
		assertEquals(4, diff);

		startTime = new Timestamp(2012, 8, 20, 9, 15, 45, 0);
		endTime = new Timestamp(2012, 8, 20, 10, 55, 55, 0);

		diff = TimeUtil.timeDiff(startTime, endTime, TimeUnits.DAYS);
		assertEquals(0, diff);

		diff = TimeUtil.timeDiff(startTime, endTime, TimeUnits.HOURS);
		assertEquals(1, diff);

		diff = TimeUtil.timeDiff(startTime, endTime, TimeUnits.MINUTES);
		assertEquals(100, diff);

		diff = TimeUtil.timeDiff(startTime, endTime, TimeUnits.SECONDS);
		assertEquals(6010, diff);
	}

	@Test
	public void testDateDaysAgo()
	{
		String d = TimeUtil.DateDaysAgo(1);
		System.out.println(d);
		d = TimeUtil.DateDaysAgo(2);
		System.out.println(d);
		d = TimeUtil.DateDaysAgo(3);
		System.out.println(d);
		d = TimeUtil.DateDaysAgo(7);
		System.out.println(d);
		d = TimeUtil.DateDaysAgo(30);
		System.out.println(d);
		d = TimeUtil.DateDaysAgo(365);
		System.out.println(d);
	}

	@Test
	public void testTempDir()
	{
		String tmpdir = System.getProperty("java.io.tmpdir") + "test";

		assertTrue(!tmpdir.isEmpty());
		System.out.println(tmpdir);

		File f = new File(tmpdir);

		if (f.exists())
			f.delete();

		boolean ret = f.mkdir();

		assertTrue(ret);
	}


	@Test
	public void test_timeNowUTC()
	{
		Timestamp ts = TimeUtil.timeNowUTC();
		System.out.println(ts);
	}

}
