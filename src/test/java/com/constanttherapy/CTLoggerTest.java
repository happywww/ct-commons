package com.constanttherapy;

import com.constanttherapy.util.CTLogger;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.NullEnumeration;
import org.junit.Test;

import java.util.Enumeration;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

@SuppressWarnings("unused")
public class CTLoggerTest
{
    @Test
	public void test_Logger()
	{
		Logger logger = CTLogger.getLogger();
		Logger root = Logger.getRootLogger();
		Appender a = root.getAppender("myConsoleAppender");
		@SuppressWarnings("unchecked")
        Enumeration<Appender> appenders = root.getAllAppenders();
		assertFalse(appenders instanceof NullEnumeration);
		
		while (appenders.hasMoreElements())
		{
			Appender appender = appenders.nextElement();
			System.out.println("Appender.name=" + appender.getName());
			System.out.println("Appender.class=" + appender.getClass());
		}
	}
	
	@Test
	public void test_logInfoMethod()
	{
		CTLogger.info("This is a test");
		CTLogger.error(new Exception("This is an exception"));
	}
	
	@Test
	public void test_logInfoMethod2()
	{
		CTLogger.info(null);
	}
}
