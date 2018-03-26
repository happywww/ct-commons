package com.constanttherapy.users.payment;

import com.constanttherapy.db.ReadOnlyDbConnection;
import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.util.CTLogger;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;

public class SubscriptionHelperTest {
    @Test
    public void testStaticInitialization() throws Exception {
        String informationalEmailRecipients = null;
        String developerEmailRecipients = null;
        ReadOnlyDbConnection sqlr = null;
        try {
            sqlr = new ReadOnlyDbConnection();

            SqlPreparedStatement statement =
                    sqlr.prepareStatement("SELECT sys_param_notes FROM  ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "subscriptionEmailRecipients");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                CTLogger.debug("Subscription email recipients set from system parameters table");
                informationalEmailRecipients = rs.getString(1);
            } else {
                // Reasonable defaults.
                CTLogger.error("Email recipients not found, set to default values");
                informationalEmailRecipients = "support@constanttherapy.com," +
                        "veera.anantha@constanttherapy.com," +
                        "david.poskanzer@constanttherapy.com";
            }

            statement =
                    sqlr.prepareStatement("SELECT sys_param_value FROM  ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "subscriptionDeveloperEmailRecipients");
            rs = statement.executeQuery();
            if (rs.next()) {
                CTLogger.debug("Developer subscription email recipients set from system parameters table");
                developerEmailRecipients = rs.getString(1);
            } else {
                CTLogger.error("Developer email recipients not found, set to default values");
                developerEmailRecipients = "ehsan.dadgar@constanttherapy.com," +
                        "support@constanttherapy.com";
            }
        } catch (SQLException e) {
            // Reasonable defaults.
            CTLogger.error("Error getting email recipients, set to default values", e);
            informationalEmailRecipients =
                    "support@constanttherapy.com," +
                            "veera.anantha@constanttherapy.com," +
                            "david.poskanzer@constanttherapy.com";
            developerEmailRecipients = "ehsan.dadgar@constanttherapy.com," +
                    "support@constanttherapy.com";

        } finally {
            if (sqlr != null) {
                SQLUtil.closeQuietly(sqlr);
            }
        }

        assertNotNull(developerEmailRecipients);
        assertNotNull(informationalEmailRecipients);
    }
}
