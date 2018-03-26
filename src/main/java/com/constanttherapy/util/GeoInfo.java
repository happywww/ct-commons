package com.constanttherapy.util;

public class GeoInfo
{
	public final String ipAddress;
	public String city;
	public String state;
	public String country;
	public String latitude;
	public String longitude;

	public GeoInfo(String ipAddr)
	{
		this.ipAddress = ipAddr;
	}

	public String getLocation()
	{
		String location = null;

		if (this.city != null && this.state != null)
			location = String.format("%s, %s", this.city, this.state);
		else if (this.city != null)
			location = this.city;
		else if (this.state != null)
			location = this.state;

		return location;
	}
}