/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */

package com.constanttherapy.util;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.URL;
import java.util.Arrays;

public class CTLogger
{
	private static Logger logger      = null;

	// TODO: make this ThreadLocal
	private static int    indentLevel = 0;

	private CTLogger() {}

	public static Logger getLogger()
	{
		if (logger == null)
		{
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			URL url = loader.getResource("log4j.properties");
			PropertyConfigurator.configure(url);
			// PropertyConfigurator.configure("log4j.properties");
			// BasicConfigurator.configure();

			logger = Logger.getLogger(CTLogger.class);
		}
		return logger;
	}

	@Deprecated
	public static void log(Throwable e)
	{
		e.printStackTrace();
	}

	public static void error(Throwable e)
	{
		error(null, e);
	}
	/* these should be used from now on */
	public static void error(String message, Throwable e)
	{
		String trace = ExceptionUtils.getStackTrace(e);

		// parse out the root of the stack only show from our code up
		int i = trace.lastIndexOf("com.constanttherapy");

		if (i > 0)
		{
			// truncate at the next new line char
			int j = trace.indexOf('\n', i);
			trace = trace.substring(0, j);
		}

		if (message == null)
			message =  trace;
		else
			message =  message + "\n" + trace;

		getLogger().error(message);
	}

	public static void error(String message)
	{
		getLogger().error(message);
	}

	public static void debugStart(String message)
	{
        indent();
        getLogger().debug(getIndentFill() + message);
	}

	public static void debug(String message)
	{
		getLogger().debug(getIndentFill() + message);
	}

	public static void debug(String message, int indentLevel)
	{
		getLogger().debug(getIndentFill(indentLevel) + message);
	}

	public static void indent()
	{
        indentLevel++;
	}

	public static void unindent()
	{
        indentLevel--;
        if (indentLevel < 0) indentLevel = 0;
	}

	public static void infoStart(String message)
	{
        indent();
        getLogger().info(getIndentFill() + message);
	}

	public static void infoEnd(String message)
	{
        getLogger().info(getIndentFill() + message);
        unindent();
	}

	public static void info(String message, boolean indent)
	{
		if (indent) unindent();
		getLogger().info(getIndentFill() + message);
		if (indent) unindent();
	}
	public static void info(String message)
	{
		CTLogger.info(message, false);
		// getLogger().info(getIndentFill(CTLogger.indentLevel) + message);
	}

	public static void info(String message, int indentLevel)
	{
		// reset tab indents if indentLevel specified is 0 or less.
		if (indentLevel < 1) CTLogger.indentLevel = 0;
		getLogger().info(getIndentFill(indentLevel) + message);
	}

	public static void warn(String message)
	{
		getLogger().warn(message);
	}

	public static boolean isInfoEnabled()
	{
		//return getLogger().isDebugEnabled();
		return getLogger().isInfoEnabled();
	}

	public static boolean isDebugEnabled()
	{
		return getLogger().isDebugEnabled();
	}

	private static String getIndentFill()
	{
		return getIndentFill(indentLevel);
	}
	private static String getIndentFill(int level)
	{
		if (level > 0)
		{
			char[] array = new char[level];
			Arrays.fill(array, '\t');
			return new String(array);
		}
		return "";
	}
}
