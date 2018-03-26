package com.constanttherapy.util.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.util.CTLogger;
import org.apache.commons.configuration.Configuration;

import java.io.*;
// Examples:
// http://www.programcreek.com/java-api-examples/index.php?api=org.jets3t.service.S3Service

public class S3Util
{
	private static final Configuration config = CTConfiguration.getConfig("conf/ct-common-config.xml");
	private static final String awsAccessKey = config.getString("aws-access-key");
	private static final String awsSecretKey = config.getString("aws-secret-key");
	public static final String responsesBucket = config.getString("ct-resource-response-bucket");
	public static final String commentsBucket = config.getString("ct-resource-comments-bucket");

/*
    private static AWSCredentials awsCredentials;
    private static S3Service s3service;
*/
    private static final BasicAWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
	public static final AmazonS3            s3Client    = AmazonS3ClientBuilder
															.standard()
															.withCredentials(new AWSStaticCredentialsProvider(credentials))
															.withRegion(Regions.US_EAST_1)
															.build();

	private S3Util()
	{
	}


	/**
	 * Uploads to bucket
	 *
	 * @param bucketName
	 * @param inputStream
	 * @param filename
	 * @throws Exception
	 */
	public static void put(String bucketName, InputStream inputStream, String filename) throws Exception
	{
		try
		{
			CTLogger.info("S3Util::put() - " + String.format("bucketName=%s , filename=%s", bucketName, filename));
			s3Client.putObject(bucketName, filename, inputStream, null);
		}
		finally
		{
			CTLogger.unindent();
		}
	}

	/**
	 * Downloads file from bucket to temp folder
	 *
	 * @param bucketName
	 * @param filename
	 * @return path of downloaded file
	 * @throws Exception
	 */
	public static String download(String bucketName, String filename) throws IOException
	{

		try
		{
			CTLogger.info("S3Util::s3get() - " + String.format("bucketName=%s, filename=%s", bucketName, filename));

			// Retrieve the object
			S3Object obj = s3Client.getObject(bucketName, filename);
			S3ObjectInputStream stream = obj.getObjectContent();
			filename = System.getProperty("java.io.tmpdir") + filename;
			FileOutputStream fos = new FileOutputStream(new File(filename));

			byte[] read_buf = new byte[1024];
			int read_len = 0;

			while ((read_len = stream.read(read_buf)) > 0)
			{
				fos.write(read_buf, 0, read_len);
			}

			stream.close();
			fos.close();
			return filename;
		}
		finally
		{
			CTLogger.unindent();
		}
	}

	public static String get(String bucketName, String filename)
	{
		StringBuilder sb = null;
		try
		{
			CTLogger.info("S3Util::s3get() - " + String.format("bucketName=%s, filename=%s", bucketName, filename));

			// Retrieve the object
			S3Object obj = s3Client.getObject(bucketName, filename);

			// Read the data from the object's DataInputStream using a loop, and print it out.
			InputStream inputStream = obj.getObjectContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String data;
			sb = new StringBuilder();
			while ((data = reader.readLine()) != null)
			{
				sb.append(data).append("\n");
			}
		}
		catch (Exception ex)
		{
			CTLogger.error(ex);
		}
		finally
		{
			CTLogger.unindent();
		}
		return sb != null ? sb.toString() : null;
	}

	/*
	 * public static String pathForBucket(String bucketName)
	 * {
	 * return "https://s3.amazonaws.com/" + bucketName;
	 * }
	 */

	/**
	 * Returns a Url to specified resource that will expire in a minute.
	 *
	 * @param bucketName
	 * @param resourceName
	 * @return
	 * @throws Exception
	 */
	public static String generatePresignedUrl(String bucketName, String resourceName) throws Exception
	{
		CTLogger.info("S3Util::getSignedPath() - " + String.format("bucketName=%s, resourceName=%s", bucketName, resourceName));

        java.util.Date expiration = new java.util.Date();
        long msec = expiration.getTime();
        msec += 1000 * 60 * 60; // 1 hour.
        expiration.setTime(msec);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, resourceName);
        generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
        generatePresignedUrlRequest.setExpiration(expiration);

        return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
	}

	public static boolean exists(String bucketName, String objectName)
	{
		CTLogger.debug(String.format("S3Util::exists() - bucket=%s, object=%s", bucketName, objectName));
		boolean exists;
		try
		{
			exists = s3Client.doesObjectExist(bucketName, objectName);
		}
		catch (AmazonS3Exception ex)
		{
			if (ex.getStatusCode() == 403) // Forbidden. Object key not found.
			{
				exists = false;
			}
			else
				throw ex;
		}
		return exists;
	}
}
