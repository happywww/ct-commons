/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
*/
package com.constanttherapy.users;

import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadOnlyDbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.util.CTLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows key->value saving of preferences associated with a user Id
 *
 * @author ehsan
 */
public class UserPreferences
{
    /**
     * Returns a map of preferences for a specific user Id
     *
     * @param sql
     * @param userId
     * @return
     * @throws SQLException
     */
    public static Map<String, String> getAllPreferencesForUser(ReadOnlyDbConnection sql, Integer userId) throws SQLException
    {
        CTLogger.infoStart("UserPreferences::getAllPreferencesForUser() - userId=" + userId);
        CTLogger.unindent();
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM user_preferences WHERE user_id = ?");
        statement.setInt(1, userId);

        ResultSet rs = statement.executeQuery();
        return preferencesFromResultSet(rs);
    }

    /**
     * Creates a map of preferences from a Result Set
     *
     * @param rs
     * @return
     * @throws SQLException
     */
    private static Map<String, String> preferencesFromResultSet(ResultSet rs) throws SQLException
    {
        Map<String, String> preferences = new HashMap<String, String>();
        while (rs.next())
        {
            String key = rs.getString("pref_key");
            String value = rs.getString("pref_value");
            preferences.put(key, value);
        }
        return preferences;
    }

    /**
     * Inserts/Updates a preference in the database
     *
     * @param sql
     * @param userId
     * @param key
     * @throws SQLException
     */
    public static void setPreferenceForUser(ReadWriteDbConnection sql, Integer userId, String key, String value) throws SQLException
    {
        CTLogger.infoStart("UserPreferences::setPreferenceForUser() - " + String.format("userId=%s, key=%s, value=%s", userId, key, value));
        CTLogger.unindent();
        // Check to see if the preference already exists in the database:
        if (!preferenceExistsForUser(sql, userId, key))
        {
            // The preference doesn't exist - create it
            insertPreferenceForUser(sql, userId, key, value);
        }
        else
        {
            // The preference exists - update it
            updatePreferenceForUser(sql, userId, key, value);
        }
    }

    /**
     * This is a better way to check if a user preference exists
     */
    private static boolean preferenceExistsForUser(DbConnection sql, Integer userId, String key) throws SQLException
    {
        CTLogger.infoStart("UserPreferences::preferenceExistsForUser() - " + String.format("userId=%s, key=%s", userId, key));
        CTLogger.unindent();
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM user_preferences WHERE user_id = ? AND pref_key = ?");
        statement.setInt(1, userId);
        statement.setString(2, key);

        ResultSet rs = statement.executeQuery();
        return rs.next();
    }

    private static boolean insertPreferenceForUser(ReadWriteDbConnection sql, Integer userId, String key, String value) throws SQLException
    {
        CTLogger.infoStart("UserPreferences::insertPreferenceForUser() - " + String.format("userId=%s, key=%s, value=%s", userId, key, value));
        CTLogger.unindent();
        SqlPreparedStatement statement = sql.prepareStatement("INSERT INTO user_preferences (user_id, pref_key, pref_value) VALUES(?,?,?)");
        statement.setInt(1, userId);
        statement.setString(2, key);
        statement.setString(3, value);

        return statement.executeUpdate() > 0;
    }

    private static boolean updatePreferenceForUser(ReadWriteDbConnection sql, Integer userId, String key, String value) throws SQLException
    {
        CTLogger.infoStart("UserPreferences::updatePreferenceForUser() - " + String.format("userId=%s, key=%s, value=%s", userId, key, value));
        CTLogger.unindent();
        SqlPreparedStatement statement = sql.prepareStatement("UPDATE user_preferences SET pref_value = ? WHERE pref_key = ? AND user_id = ?");
        statement.setString(1, value);
        statement.setString(2, key);
        statement.setInt(3, userId);

        return statement.executeUpdate() > 0;
    }

    /**
     * Returns a preference from the database
     *
     * @param sql
     * @param userId
     * @param key
     * @return
     * @throws SQLException
     */
    public static String getPreferenceForUser(DbConnection sql, Integer userId, String key) throws SQLException
    {
        CTLogger.infoStart("UserPreferences::getPreferenceForUser() - " + String.format("userId=%s, key=%s", userId, key));
        CTLogger.unindent();
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM user_preferences WHERE user_id = ? AND pref_key = ?");
        statement.setInt(1, userId);
        statement.setString(2, key);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return rs.getString("pref_value");
        else
            return null;
    }
}
