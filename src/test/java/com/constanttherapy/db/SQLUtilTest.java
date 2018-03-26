package com.constanttherapy.db;

import org.junit.Test;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

public class SQLUtilTest
{
	@Test
	public void test_initialize() throws SQLException
	{
		SQLUtil.initialize();
		System.out.println(SQLUtil.dump("\n"));
	}

	@Test
	public void test_initSQLUtil2() throws SQLException
	{
		// uninitialized
		Connection sql = SQLUtil.getDatabaseConnection();

		assertTrue(sql != null);
	}

	@Test
	public void test_getDatabaseConnection() throws SQLException
	{
		SQLUtil.initialize();
		Connection sql = SQLUtil.getDatabaseConnection();
		SQLUtil.getDatabaseConnection();
		assertTrue(sql != null);

		sql = SQLUtil.getDatabaseConnection("constant_therapy");
		assertTrue(sql != null);

		// non-existent db
		sql = SQLUtil.getDatabaseConnection("foo_124132515");
		assertTrue(sql == null);
	}
/*
	private UriInfo getMockUriInfo(String serviceName)
	{
		try
		{
			URI mock_uri;
			mock_uri = new URI("http://localhost:8080/" + serviceName + "/api");
			UriInfo mockUriInfo = mock(UriInfo.class);
			when(mockUriInfo.getBaseUri()).thenReturn(mock_uri);
			return mockUriInfo;
		}
		catch (URISyntaxException e)
		{
			return null;
		}
	}
*/

	@Test
	public void test_hostName() throws UnknownHostException
	{
		String name = java.net.InetAddress.getLocalHost().getHostName();
		System.out.println(name);
	}
}
