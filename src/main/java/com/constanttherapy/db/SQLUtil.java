/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */
package com.constanttherapy.db;

import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.StringUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.dbutils.DbUtils;

import java.sql.*;
import java.util.*;

public class SQLUtil
{
    private static final String              DEFAULT_DB             = "constant_therapy";
    private static       Map<String, String> dbConnections          = null;
    private static       boolean             isInitialized          = false;
    private static       String              dbConnectionProperties = "";

    static java.sql.Connection getDatabaseConnection() throws SQLException
    {
        // If no argument is supplied, it returns the production (constant_therapy) database.
        return getDatabaseConnection(DEFAULT_DB, false);
    }

    private static java.sql.Connection getDatabaseConnection(String dbName, boolean readonly) throws SQLException
    {
        try
        {
            if (dbName == null) dbName = DEFAULT_DB;

            String connectionString = getConnectionString(dbName);
            Class.forName("com.mysql.jdbc.Driver")
                 .newInstance();
            java.sql.Connection conn = DriverManager.getConnection(connectionString);
            conn.setReadOnly(readonly);
            return conn;
        }
        catch (Exception ex)
        {
            throw new SQLException(ex.getMessage()); // TODO: throw custom exception type
        }
    }

    private static String getConnectionString(String dbName)
    {
        if (!isInitialized)
        {
            initialize(); // for backward compatibility
            CTLogger.warn("SQLUtil not initialized.");
            // throw new IllegalStateException("SQLUtil is not initialized!");
        }
        if (dbName == null) dbName = DEFAULT_DB;
        return dbConnections.get(dbName);
    }

    public static void initialize()
    {
        if (isInitialized) return;

        CTLogger.infoStart("SQLUtil::initialize()");

        try
        {
            Configuration config = CTConfiguration.getConfig("conf/ct-common-config.xml");

            // connection Url query parameters
            dbConnectionProperties = config.getString("db-connection-properties");

            // get client-connection types
            dbConnections = new TreeMap<>();

            Map<String, String> dbClients = new TreeMap<>();

            int size = config.getList("db-clients.db-client")
                             .size();
            for (int i = 0; i < size; i++)
            {
                String client = config.getString("db-clients.db-client(" + i + ").[@name]");
                String server = config.getString("db-clients.db-client(" + i + ")");

                dbClients.put(client, server);
            }

            String clientHost;

            // get client name
            clientHost = java.net.InetAddress.getLocalHost().getHostName();

            CTLogger.debug("LocalHostName: " + clientHost);

            String serverToUse = null;

            if (dbClients.containsKey(clientHost))
            {
                serverToUse = dbClients.get(clientHost);
            }
            else
            {
                // use default
                if (dbClients.containsKey("default"))
                {
                    serverToUse = dbClients.get("default");
                }
            }

            if (serverToUse == null)
                throw new ConfigurationException("Unable to find server to use for client " + clientHost);

            CTLogger.debug("SQLUtil::initialize() - using db server: " + serverToUse);

            final String slaveServerToUse = serverToUse + "_slave";

            // load databases connection strings for selected server
            size = config.getList("databases.database").size();

            List<String> dbNames = new ArrayList<>(size);
            for (int i = 0; i < size; i++)
            {
                String dbName = config.getString("databases.database(" + i + ")");
                dbNames.add(dbName);
            }

            // load databases connection strings for selected server
            size = config.getList("db-servers.db-server").size();

            for (int i = 0; i < size; i++)
            {
                String serverName = config.getString("db-servers.db-server(" + i + ").[@name]");
                if (serverToUse.equals(serverName) || slaveServerToUse.equals(serverName))
                {
                    String connection = config.getString("db-servers.db-server(" + i + ")");
                    //connection += "?" + dbConnectionProperties;
                    for (String dbName : dbNames)
                    {
                        String key = (serverName.endsWith("_slave")) ? dbName + "_slave" : dbName;
                        String value = connection + "/" + dbName + "?" + dbConnectionProperties;
                        CTLogger.debug(String.format("dbName: %s, connectionString: %s", key,
                                                     StringUtil.truncatePasswordFromString(value)));
                        dbConnections.put(key, value);
                    }
                }
            }

            isInitialized = true;
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    static java.sql.Connection getReadOnlyDatabaseConnection() throws SQLException
    {
        // If no argument is supplied, it returns the production (constant_therapy) database.
        return getReadOnlyDatabaseConnection(DEFAULT_DB);
    }

    static java.sql.Connection getReadOnlyDatabaseConnection(String dbName) throws SQLException
    {
        // If no argument is supplied, it returns the production (constant_therapy) database.
        return getDatabaseConnection(dbName + "_slave", true);
    }

    static java.sql.Connection getDatabaseConnection(String dbName) throws SQLException
    {
        return getDatabaseConnection(dbName, false);
    }

    // TODO consider a general-purpose query execution method that can
    // - set parameters
    // - handle exceptions
    // - can return specific number of records
    public static ResultSet executeQuery(ReadWriteDbConnection sql, String query, Object... args) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement(query);
        // TODO: Add SQL parameters

        return statement.executeQuery();
    }

    public static boolean execute(ReadWriteDbConnection sql, String query) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement(query);

        return statement.execute();
    }

    /**
     * Delete records based on specified filter
     *
     * @param sql
     * @param tableName
     * @param whereClause
     * @throws SQLException
     */
    public static void deleteRecords(ReadWriteDbConnection sql, String tableName, String whereClause) throws SQLException
    {
        SqlPreparedStatement statement = sql
                .prepareStatement("delete from " + tableName + " where " + whereClause);

        statement.execute();
    }

    /**
     * Delete a specific record
     *
     * @param sql
     * @param tableName
     * @param id
     * @throws SQLException
     */
    public static void deleteRecord(ReadWriteDbConnection sql, String tableName, Integer id) throws SQLException
    {
        SqlPreparedStatement statement = sql
                .prepareStatement("delete from " + tableName + " where id = ?");
        statement.setInt(1, id);

        statement.execute();
    }

    static public Integer getInteger(ResultSet rs, String colName)
    {
        int val;
        try
        {
            val = rs.getInt(colName);
            return rs.wasNull() ? null : val;
        }
        catch (SQLException e)
        {
            return null;
        }
    }

    static public Float getFloat(ResultSet rs, String colName)
    {
        float val;
        try
        {
            val = rs.getFloat(colName);
            return rs.wasNull() ? null : val;
        }
        catch (SQLException e)
        {
            return null;
        }
    }
    static public List<Integer> getIntegerList(SqlPreparedStatement statement, String colName) throws SQLException
    {
        List<Integer> returnValue = new ArrayList<>();

        ResultSet rs = statement.executeQuery();
        while (rs.next())
        {
            Integer val = rs.getInt(colName);
            returnValue.add(val);
        }

        return returnValue;
    }


    /**
     * close a connection, handling null, and snuffing exceptions
     * ... note that in most implementations this will also close any result
     * sets and prepared statements associated with the given connection
     */
    public static void closeQuietly(Connection sql)
    {
        DbUtils.closeQuietly(sql);
    }

    /**
     * close a result set, handling null, and snuffing exceptions
     */
    public static void closeQuietly(ResultSet rs)
    {
        DbUtils.closeQuietly(rs);
    }

    /**
     * close a prepared statement, handling null, and snuffing exceptions
     */
    public static void closeQuietly(Statement st)
    {
        DbUtils.closeQuietly(st);
    }

    public static String dump(String separator)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Db Connections:")
          .append(separator);

        for (String key : dbConnections.keySet())
        {
            //String con = dbConnections.get(key).split("\\?")[0];

            sb.append(key)
              .append(": ")
              .append(dbConnections.get(key))
              .append(separator);
        }
        return sb.toString();
    }

    public static void closeQuietly(DbConnection sql)
    {
        if (sql != null) sql.close();
    }

    public static void closeQuietly(SqlPreparedStatement statement)
    {
        if (statement != null) statement.close();
    }
}
