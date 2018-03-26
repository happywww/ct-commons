package com.constanttherapy.users;

import com.constanttherapy.enums.UserEventType;
import com.constanttherapy.util.CTTestBase;
import com.constanttherapy.util.TimeUtil;
import org.junit.Test;

@SuppressWarnings("unused")
public class UserEventLoggerTest extends CTTestBase
{

	@Test
	public void testLogEvent1() throws Exception
	{
		//UserEventLogger.logEvent(UserEventType.ScheduleChanged, 10, "abcdefg");
		UserEventLogger.logEvent(_sql, UserEventType.ScheduleChanged, "Test", 10, "12345",
				TimeUtil.getTimestampFromDate("2013-01-01"), null);
	}

	@Test
	public void test_setEventDataOnLatestEvent()
	{
		int userId = 19990;
		UserEventType type = UserEventType.EmailSent;
		String subType = "email_to_clinician_patient_trial_15_days";
		String eventData = "This is a test";
		UserEventLogger.setEventDataOnLatestEvent(_sql, userId, type, subType, eventData, null);

		String notes = "This is a note";
		UserEventLogger.setEventDataOnLatestEvent(_sql, userId, type, subType, eventData, notes);
	}

}

