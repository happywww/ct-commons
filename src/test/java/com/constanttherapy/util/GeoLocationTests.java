package com.constanttherapy.util;

import com.constanttherapy.util.CTTestBase;
import com.constanttherapy.util.GeoLocation;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GeoLocationTests extends CTTestBase
{
	@Test
	public void test_getCityStateFromIpAddress() throws Exception
	{
		String city = GeoLocation.getCityStateFromIpAddress(_sqlr, "98.110.225.56");
		assertTrue(city != null);
		System.out.println(city);
	}

	@Test
	public void test_getCityStateFromIpAddress2() throws Exception
	{
		String city = GeoLocation.getCityStateFromIpAddress(_sqlr, "invalid_ip!");
		assertTrue(city != null);
		assertTrue(city.equals("invalid_ip!"));
		System.out.println(city);
	}
}
