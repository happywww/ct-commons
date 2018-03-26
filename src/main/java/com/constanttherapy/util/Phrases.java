package com.constanttherapy.util;

import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.SqlPreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Phrases
{
    //TODO: implement crud?
    static public Phrase getPhrase(DbConnection sql, int id) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("select * from phrases where id = ?");
        statement.setInt(1, id);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return new Phrase(rs);
        else
            return null;
    }
}
