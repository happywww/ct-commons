package com.constanttherapy.users.payment;

import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.users.payment.StripeHelper;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.CTTestBase;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class StripeHelperTest extends CTTestBase
{
    @Test
    public void test_storeEventData()
    {
        /*
		StripeHelper sh = new StripeHelper();
		sh.storeEventData(_sql, "test123", "payment",
		        "1fupwoirupwoeiruqwep roquwerpqioweur pqioewrupqwe qewpriouq epqoewiruqpweoriuqpweoriuq pqwoieru qpweoiruqw erqweporiuqw");
		 */
    }

    @Test
    public void processStripeJson()
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            statement = _sql.prepareStatement("select * from ct_customer.stripe_events where data is null");
            rs = statement.executeQuery();

            while (rs.next())
            {
                String id = rs.getString("event_id");
                System.out.println("Updating " + id);
                String json = rs.getString("json");

                Map<String, String> map = StripeHelper.getStripeEventJsonFromHttpPostBody(json);
                StripeHelper.storeEventData(_sql, map, json);
            }
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    @Test
    public void test_createDefaultTrialSubscription() throws SQLException
    {
        // use this to create a 30-day trial subscription in Stripe
        StripeHelper sh = StripeHelper.create(_sql, 48726);
        sh.checkForFirstLogin(_sql);
    }
}
