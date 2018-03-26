package com.constanttherapy.db;

import java.sql.SQLException;

/**
 * Created by madvani on 7/25/16.
 */
public class ReadOnlyDbConnection extends DbConnection
{
   public ReadOnlyDbConnection() throws SQLException
    {
        this.dbInstance = "slave";
        init(null);
    }

    public ReadOnlyDbConnection(String dbName) throws SQLException
    {
        this.dbInstance = "slave";
        init(dbName);
    }

    @Override
    protected void init(String dbName) throws SQLException
    {
        if (this.sql == null)
            this.sql = dbName == null ? SQLUtil.getReadOnlyDatabaseConnection() : SQLUtil.getReadOnlyDatabaseConnection(dbName);
    }

    @Override
    public String toString()
    {
        return "ReadOnlyDbConnection{" +
                "sqlr=" + this.sql.toString() +
                '}';
    }
}
