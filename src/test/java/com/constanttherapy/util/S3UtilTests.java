package com.constanttherapy.util;

import com.constanttherapy.util.aws.S3Util;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class S3UtilTests
{
	@Test
	public void test_get() throws Exception
	{
		String test = S3Util.get("CT-templates", "alert_never_used_app.html");
		assertTrue(test != null);
		System.out.println(test);
	}

	@Test
	public void test_put()
	{
		// assertTrue(sql != null);
	}
}
