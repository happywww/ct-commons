package com.constanttherapy.util;

import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadOnlyDbConnection;
import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;

import java.sql.ResultSet;

public class GeoLocation
{
    public static GeoInfo getLocationInfoFromIpAddress(DbConnection sql, String ipAddress)
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        GeoInfo geo = new GeoInfo(ipAddress);

        try
        {
            String q = "SELECT city, state, country, latitude, longitude " +
                    "FROM geo_ip.locations " +
                    "WHERE MBRCONTAINS(ip_poly, POINTFROMWKB(POINT(INET_ATON(?), 0)))";

            statement = sql.prepareStatement(q);
            statement.setString(1, ipAddress);

            rs = statement.executeQuery();

            if (rs.next())
            {
                geo.city = rs.getString("city");
                geo.state = rs.getString("state");
                geo.country = rs.getString("country");
                geo.latitude = rs.getString("latitude");
                geo.longitude = rs.getString("longitude");
            }
        }
        catch (Exception ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }

        return geo;
    }

    public static String getCityStateFromIpAddress(ReadOnlyDbConnection sql, String ipAddress)
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String q = "SELECT CONCAT(city, ', ', state) location " +
                    "FROM geo_ip.locations " +
                    "WHERE MBRCONTAINS(ip_poly, POINTFROMWKB(POINT(INET_ATON(?), 0)))";

            statement = sql.prepareStatement(q);
            statement.setString(1, ipAddress);

            rs = statement.executeQuery();

            if (rs.next())
            {
                return rs.getString("location");
            }
            else
                return ipAddress; // no location found. Just return ip address
        }
        catch (Exception ex)
        {
            CTLogger.error(ex);
            return ipAddress;
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }
}
