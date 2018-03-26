package com.constanttherapy.db;

import com.constanttherapy.util.CTLogger;

import java.sql.*;
import java.util.Calendar;

/**
 * Created by madvani on 7/26/16.
 */
public class SqlPreparedStatement
{
    private final PreparedStatement statement;
    private final String dbInstance;

    SqlPreparedStatement(PreparedStatement statement, String dbInstance)
    {
        this.statement = statement;
        this.dbInstance = dbInstance;
    }

    public boolean execute() throws SQLException
    {
        try
        {
            long startTime = System.currentTimeMillis();
            boolean retval = this.statement.execute();
            logStatement(getExecutionTime(startTime));
            return retval;
        }
        catch (SQLException e)
        {
            logStatement("EXECUTE FAIL");
            throw e;
        }
    }

    public void setInt(int i, Integer val) throws SQLException
    {
        if (val != null)
            this.statement.setInt(i, val);
        else
            this.statement.setNull(i, Types.INTEGER);
    }

    public void setString(int i, String str) throws SQLException
    {
        if (str != null)
            this.statement.setString(i, str);
        else
            this.statement.setNull(i, Types.VARCHAR);
    }

    public void setTimestamp(int i, Timestamp timestamp) throws SQLException
    {
        if (timestamp != null)
            this.statement.setTimestamp(i, timestamp);
        else
            this.statement.setNull(i, Types.TIMESTAMP);
    }

    public int executeUpdate() throws SQLException
    {
        try
        {
            long startTime = System.currentTimeMillis();
            int retval = this.statement.executeUpdate();
            logStatement(getExecutionTime(startTime));
            return retval;
        }
        catch (SQLException e)
        {
            logStatement("UPDATE FAIL");
            throw e;
        }
    }

    public ResultSet executeQuery() throws SQLException
    {
        try
        {
            long startTime = System.currentTimeMillis();
            ResultSet rs = this.statement.executeQuery();
            logStatement(getExecutionTime(startTime));
            return rs;
        }
        catch (SQLException e)
        {
            logStatement("QUERY FAIL");
            throw e;
        }
    }

    private void logStatement(String msg)
    {
        String log = this.statement.toString();
        int i = log.indexOf(':');
        if (i > 0)
            log = log.substring(i + 1);
        CTLogger.debug(String.format("[%s]: (%s) %s", this.dbInstance, msg, log));
    }

    private String getExecutionTime(long startTime)
    {
        long d = (System.currentTimeMillis() - startTime);
        return String.format("%dms", d);
    }

    public void setBoolean(int i, Boolean val) throws SQLException
    {
        if (val != null)
            this.statement.setBoolean(i, val);
        else
            this.statement.setNull(i, Types.BOOLEAN);
    }

    public void setBytes(int i, byte[] bytes) throws SQLException
    {
        this.statement.setBytes(i, bytes);
    }

    public ResultSet getGeneratedKeys() throws SQLException
    {
        return this.statement.getGeneratedKeys();
    }

    public void setDouble(int i, Double val) throws SQLException
    {
        if (val != null)
            this.statement.setDouble(i, val);
        else
            this.statement.setNull(i, Types.DOUBLE);
    }

    public void close()
    {
        SQLUtil.closeQuietly(this.statement);
    }

    public void setNull(int i, int type) throws SQLException
    {
        this.statement.setNull(i, type);
    }

    public void setTimestamp(int i, Timestamp timestamp, Calendar cal) throws SQLException
    {
        if (timestamp != null)
            this.statement.setTimestamp(i, timestamp, cal);
        else
            this.statement.setNull(i, Types.TIMESTAMP);
    }

    public void setLong(int i, Long val) throws SQLException
    {
        if (val != null)
            this.statement.setLong(i, val);
        else
            this.statement.setNull(i, Types.BIGINT);
    }

    public void setDate(int i, Date date) throws SQLException
    {
        if (date != null)
            this.statement.setDate(i, date);
        else
            this.statement.setNull(i, Types.DATE);
    }

    public int getUpdateCount() throws SQLException
    {
        return this.statement.getUpdateCount();
    }
}
