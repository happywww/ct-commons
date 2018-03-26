package com.constanttherapy.util;

import javax.security.sasl.AuthenticationException;

public class MsgUtil
{
	public static String constructSimpleMessage(String key, String message)
	{
		return String.format("{\"%s\":\"%s\"}", key, message);
	}

	public static String constructDoubleMessage(String key1, String message1, String key2, String message2)
	{
		return String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}", key1, message1, key2, message2);
	}

	public static String constructSuccessMessage(String message)
	{
		return constructSimpleMessage("success", message);
	}

	public static String constructErrorMessage(String message, String type)
	{
		String msg = constructSimpleMessage(type, message);
		CTLogger.error(msg);
		return msg;
	}

	public static String constructErrorMessage(String message)
	{
		String msg = constructSimpleMessage("error", message);
		CTLogger.error(msg);
		return msg;
	}

	public static String constructErrorMessage(String message, Exception ex)
	{
		CTLogger.error(ex);
		if (ex instanceof AuthenticationException)
			return constructSimpleMessage("authError", ex.getMessage());
		else if (ex instanceof IllegalArgumentException)
			return constructSimpleMessage("error", ex.getMessage());
		else
			return constructSimpleMessage("error", message);
	}

}
