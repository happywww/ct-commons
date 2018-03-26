package com.constanttherapy.util;

import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.db.*;
import com.constanttherapy.util.aws.S3Util;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class ResourceUtil
{
	public static List<ResourceLogEntry> getDeletedResourcesSince(DbConnection sqlr, Timestamp timestamp) throws SQLException
	{
		try
		{
			CTLogger.infoStart("ResourceUtil::getDeletedResourcesSince()");
			List<ResourceLogEntry> entries = new LinkedList<ResourceLogEntry>();

			SqlPreparedStatement statement = sqlr.prepareStatement("select * from resource_update_log where type = 'DELETE' and timestamp > ?");
			statement.setTimestamp(1, timestamp);

			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				entries.add(new ResourceLogEntry(rs));
			}
			CTLogger.info("Deleted resources found: " + entries.size());
			return entries;
		}
		finally
		{
			CTLogger.unindent();
		}
	}

	public static String getSoundPathForPhrase(String phrase)
	{
		ReadOnlyDbConnection sqlr = null;

		try
		{
			CTLogger.infoStart("ResourceUtil::getSoundPathForPhrase()");
			phrase = phrase.trim();

			// TODO: consider caching all phrases a hash?
			sqlr = new ReadOnlyDbConnection();
			SqlPreparedStatement statement = sqlr.prepareStatement("select sound_path from phrases where phrase = ?");
			statement.setString(1, phrase);

			ResultSet rs = statement.executeQuery();
			if (rs.next()) { return rs.getString("sound_path"); }
			return null;
		}
		catch (Exception e)
		{
			CTLogger.error(e);
			return null;
		}
		finally
		{
			CTLogger.unindent();
			SQLUtil.closeQuietly(sqlr);
		}
	}

	// TODO: move to config file
	private static String temporaryFolder = "/usr/share/tomcat6/temp/";

	public static void writeTemporaryFile(InputStream uploadedInputStream, String filename) throws IOException
	{

		String uploadedFileLocation = temporaryFolder + filename;

		OutputStream out = new FileOutputStream(new File(uploadedFileLocation));
		int read = 0;
		byte[] bytes = new byte[1024];

		out = new FileOutputStream(new File(uploadedFileLocation));
		while ((read = uploadedInputStream.read(bytes)) != -1)
		{
			out.write(bytes, 0, read);
		}
		out.flush();
		out.close();
	}

	private static final Configuration config = CTConfiguration.getConfig("conf/ct-common-config.xml");
	private static final String awsAccessKey = config.getString("aws-access-key");
	private static final String awsSecretKey = config.getString("aws-secret-key");

	public static final String responsesBucket = config.getString("ct-resource-response-bucket");
	public static final String commentsBucket = config.getString("ct-resource-comments-bucket");
	public static final String responsesBucketDev = config.getString("ct-resource-response-bucket-dev");
	public static final String commentsBucketDev = config.getString("ct-resource-comments-bucket-dev");

	/*
	 * Uploads to bucket and returns public url:
	 */
	public static void uploadToEC2Bucket(String bucketName, InputStream ifs, String filename) throws Exception
	{
		try
		{
			CTLogger.infoStart(String.format("ResourceUtil::uploadToEC2Bucket() - %s/%s", bucketName, filename));

            S3Util.put(bucketName, ifs, filename);
		}
		finally
		{
			CTLogger.unindent();
		}
	}

	public static String pathForBucket(String bucketName)
	{
		return "https://s3.amazonaws.com/" + bucketName;
	}

	public static String signedPathForResource(String bucketName, String resourceName) throws Exception
	{
		return S3Util.generatePresignedUrl(bucketName, resourceName);
	}

	public static void uploadToLocalStorage(String dirName, InputStream is, String responseDataFilename) throws IOException
	{
		try
		{
			String resourceDir = System.getProperty("java.io.tmpdir") + dirName;

			CTLogger.infoStart("ResourceUtil::uploadToLocalStorage() - "
			        + String.format("dirName=%s , responseDataFilename=%s", resourceDir, responseDataFilename));
			if (!dirName.endsWith("/")) dirName = dirName + "/";

			File d = new File(resourceDir);

			if (!d.exists())
			{
				boolean success = d.mkdir();
				if (!success)
					throw new IOException("Unable to create directory " + resourceDir);
			}

			String filename = resourceDir + responseDataFilename;
			OutputStream os = new FileOutputStream(new File(filename));

			IOUtils.copy(is, os);
			os.flush();
			os.close();
		}
		finally
		{
			CTLogger.unindent();
		}
	}


	public static String getPathForLocalResource(String hostUrl, String dirName, String resourceName)
	{
		return hostUrl + "/" + dirName + "/" + resourceName;
	}

	private static String _resourceUrl = null;
	public static String getResourceUrl()
	{
		if (_resourceUrl == null)
		{
			_resourceUrl = CTConfiguration.getConfig("conf/ct-common-config.xml").getString("ct-resource-url");
		}
		return _resourceUrl;
	}
}
