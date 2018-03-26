/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */

package com.constanttherapy.configuration;

import com.constanttherapy.util.CTLogger;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import java.net.URL;
import java.util.TreeMap;

public class CTConfiguration
{

	private static TreeMap<String, XMLConfiguration> configFiles = new TreeMap<String, XMLConfiguration>();

	private CTConfiguration()
	{
	}

	public static Configuration getConfig(String filename)
	{
		XMLConfiguration config = null;

		if (configFiles.containsKey(filename))
			config = configFiles.get(filename);
		else
		{
			try
			{
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				URL configURL = loader.getResource(filename);
				if (configURL == null)
					throw new ConfigurationException("Unable to load configuration file: " + filename);

				config = new XMLConfiguration(configURL);
				configFiles.put(filename, config);
			}
			catch (ConfigurationException cex)
			{
				CTLogger.error(cex);
			}
		}
		return config;
	}

}
