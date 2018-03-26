package com.constanttherapy.users.payment;

import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.TimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ManualSubscriptionHelper extends SubscriptionHelper
{
    private Timestamp manualExpirationTimestamp;

    private ManualSubscriptionHelper()
    {
    }

    public static ManualSubscriptionHelper create(ReadWriteDbConnection sql, Integer userId)
    {
        ManualSubscriptionHelper helper = new ManualSubscriptionHelper();
        helper.setupForUser(sql, userId);
        return helper;
    }

    private void setupForUser(ReadWriteDbConnection sql, Integer userId)
    {
        initVars();
        readDataFields(sql, userId);
    }

    protected void initVars()
    {
        super.initVars();
        manualExpirationTimestamp = null;
    }

    protected void readDataFields(ReadWriteDbConnection sql, Integer userId)
    {
        super.readDataFields(sql, userId);

        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT manual_expires FROM ct_customer.customers WHERE ( user_id = ? )");
            statement.setInt(1, this.getUserId());

            ResultSet rs = statement.executeQuery();
            rs.next();
            this.manualExpirationTimestamp = rs.getTimestamp(1);
        }
        catch (Exception e)
        {
            CTLogger.error("Error getting manual subscription data", e);
        }
    }

    public void updateManualExpirationTimestamp(ReadWriteDbConnection sql, Timestamp t) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("UPDATE ct_customer.customers SET manual_expires = ? WHERE ( user_id = ? )");
        statement.setTimestamp(1, t);
        statement.setInt(2, this.getUserId());

        statement.executeUpdate();

        this.manualExpirationTimestamp = t;

        this.refreshUserSubscriptionStatusFlag(sql);
    }

    @Override
    public boolean isTrialEnded()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRecurring(DbConnection sql)
    {
        // There are no recurring payments through this method
        return false;
    }

    @Override
    public Timestamp getExpirationTimestamp()
    {
        return this.manualExpirationTimestamp;
    }

    @Override
    public boolean isTrialing()
    {
        // There is no trialing enabled through this method
        return false;
    }

    @Override
    public boolean getInternalSubStatusFlag()
    {
        if (this.manualExpirationTimestamp == null) return false;
        return TimeUtil.timeNow().before(this.manualExpirationTimestamp);
    }
}
