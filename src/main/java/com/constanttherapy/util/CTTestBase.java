package com.constanttherapy.util;

import com.constanttherapy.db.ReadOnlyDbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;
import org.junit.After;
import org.junit.Before;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CTTestBase
{
	protected static ReadWriteDbConnection _sql;
	protected static ReadOnlyDbConnection _sqlr;
	protected Map<String, String> parameters = new TreeMap<>();

	@Before
	public void initialize() throws Exception
	{
		SQLUtil.initialize();
		_sql = new ReadWriteDbConnection();
		_sqlr = new ReadOnlyDbConnection();
	}

	@After
	public void teardown() throws Exception
	{
		_sql.close();
		_sql = null;
		_sqlr.close();
		_sqlr = null;
	}

	protected UriInfo getMockUriInfo(String serviceName)
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

	protected UriInfo getMockUriInfo(String protocol, String serverName, String serviceName)
	{
		try
		{
			URI mock_uri;
			mock_uri = new URI(protocol + "://" + serverName + "/" + serviceName + "/api");
			UriInfo mockUriInfo = mock(UriInfo.class);
			when(mockUriInfo.getBaseUri()).thenReturn(mock_uri);
			return mockUriInfo;
		}
		catch (URISyntaxException e)
		{
			return null;
		}
	}

	public String getToken(int userId)
	{
		// for testing, just grab token from db for test clinician or patient
		SqlPreparedStatement statement;
		try
		{
			statement = _sql.prepareStatement("select access_token from users where id = ?");
			statement.setInt(1, userId);
			ResultSet rs = statement.executeQuery();
			if (rs.next())
			{
				return rs.getString("access_token");
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
