package com.constanttherapy.db;

import com.constanttherapy.util.CTLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by madvani on 7/25/16.
 */
public class ReadWriteDbConnection extends DbConnection
{
    public ReadWriteDbConnection() throws SQLException
    {
        this.dbInstance = "master";
        init(null);
    }

    // REVIEW: [mahendra, 8/21/16 4:50 PM] - is this necessary?
    protected ReadWriteDbConnection(Connection c)
    {
        this.dbInstance = "master";
        this.sql = c;
    }

    public static ReadWriteDbConnection fromConnection(Connection c) throws SQLException
    {
        if (c.isReadOnly()) throw new IllegalArgumentException("Must be writeable connection");
        return new ReadWriteDbConnection(c);
    }

    public ReadWriteDbConnection(String dbName) throws SQLException
    {
        this.dbInstance = "master";
        init(dbName);
    }

    @Override
    public String toString()
    {
        return "ReadWriteDbConnection{" +
                "sqlr=" + this.sql.toString() +
                '}';
    }

    public void setAutoCommit(boolean b) throws SQLException
    {
        this.sql.setAutoCommit(b);
    }

    public void commit() throws SQLException
    {
        this.sql.commit();
    }

    public void rollback() throws SQLException
    {
        try
        {
            CTLogger.infoStart("ReadWriteDbConnection::rollback()");
            this.sql.rollback();
        }
        finally
        {
            CTLogger.unindent();
        }
    }
}
