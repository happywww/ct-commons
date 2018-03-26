package com.constanttherapy.util;

import java.util.TimeZone;

/** helps understand the client characteristics such as OS, version, timezone that are
 * sent down via an HTTP request */
public class ClientCharacteristicsHelper {

	public static final Integer CLIENT_OS_IOS = 1;
	public static final Integer CLIENT_OS_ANDROID = 2;
	
	public static boolean isAndroidClientQueryParam(Integer theParam) {
		if (theParam == null) return false;
		return (theParam.compareTo(CLIENT_OS_ANDROID) == 0);
	}
	
	public static boolean isIosClientQueryParam(Integer theParam) {
		if (theParam == null) return false;
		return (theParam.compareTo(CLIENT_OS_IOS) == 0);
	}
	
	public static TimeZone getTimeZoneFromClientQueryParam(String theParam) {
		return TimeZone.getTimeZone(theParam);
	}
}
