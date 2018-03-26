package com.constanttherapy.users.payment;

import com.constanttherapy.db.*;
import com.constanttherapy.users.CTUser;
import com.constanttherapy.users.payment.ios.IOSInAppPurchaseHelper;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.TimeUtil;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * @author ehsan
 */
public abstract class SubscriptionHelper
{
    public static final  boolean AUTO_MAINTAIN_STATUS_NON_GRANDFATHERED = true;
    public static final  boolean AUTO_MAINTAIN_STATUS_GRANDFATHERED     = false;
    // our users table values for active/trialing or not active/trialing
    private static final int     CT_USER_FLAG_SUBSCRIBED                = 1;
    private static final int     CT_USER_FLAG_NOT_SUBSCRIBED            = 0;
    // our ct_customer.customers table values for auto maintaining of the subscription via Stripe
    public static final  int     CT_CUSTOMER_FLAG_AUTO_MAINTAIN         = 1;
    public static final  int     CT_USER_FLAG_NOT_NOT_AUTO_MAINTAIN     = 0;
    /**
     * epoch for 1999, which is what we set when no subs, as a "filler" value
     */
    static final         long    EPOCH_1999                             = 925077044;
    /**
     * value for an alert or other even that hasn't happened in like forever
     */
    private static final int     A_WHOLE_LOT_OF_DAYS                    = 9999999;
    /**
     * whether or not the user currently has an active valid subscription and can use the product,
     * (or does not need one e.g. is grandfathered in) - if this value is 1 they can use CT
     */
    static final         String  KEY_IS_SUBSCRIBED                      = "is_subscribed";
    /**
     * whether or not we allow Stripe to determine is_subscribed field - set to no if grandfathered in
     */
    static final         String  KEY_IS_AUTO_MAINTAIN_SUBSCRIPTION      = "is_auto_maintain_subscription";
    /**
     * last time we showed client message or sent email about subscription stuff
     */
    static final         String  KEY_LAST_SUBSCRIPTION_ALERT            = "last_subscription_alert";
    public static String developerEmailRecipients;
    public static String informationalEmailRecipients;
    private static DateFormat dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");

    static
    {
        ReadWriteDbConnection sql = null;
        try
        {
            sql = new ReadWriteDbConnection();

            SqlPreparedStatement statement =
                    sql.prepareStatement("SELECT sys_param_notes FROM  ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "subscriptionEmailRecipients");
            ResultSet rs = statement.executeQuery();
            if (rs.next())
            {
                CTLogger.debug("Subscription email recipients set from system parameters table");
                informationalEmailRecipients = rs.getString(1);
            }
            else
            {
                // Reasonable defaults.
                CTLogger.error("Email recipients not found, set to default values");
                informationalEmailRecipients = "support@constanttherapy.com," +
                        "veera.anantha@constanttherapy.com," +
                        "david.poskanzer@constanttherapy.com";
            }

            statement =
                    sql.prepareStatement("SELECT sys_param_value FROM  ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "subscriptionDeveloperEmailRecipients");
            rs = statement.executeQuery();
            if (rs.next())
            {
                CTLogger.debug("Developer subscription email recipients set from system parameters table");
                developerEmailRecipients = rs.getString(1);
            }
            else
            {
                CTLogger.error("Developer email recipients not found, set to default values");
                developerEmailRecipients = "ehsan.dadgar@constanttherapy.com," +
                        "support@constanttherapy.com";
            }
        }
        catch (SQLException e)
        {
            // Reasonable defaults.
            CTLogger.error("Error getting email recipients, set to default values", e);
            informationalEmailRecipients =
                    "support@constanttherapy.com," +
                            "veera.anantha@constanttherapy.com," +
                            "david.poskanzer@constanttherapy.com";
            developerEmailRecipients = "ehsan.dadgar@constanttherapy.com," +
                    "support@constanttherapy.com";
        }
        finally
        {
            if (sql != null)
            {
                sql.close();
            }
        }
    }

    // These are the properties that are independent of the payment method.
    // Any additional properties are added by subclasses
    private boolean            mIsSubscribed          = false;
    /**
     * the flag that indicates whether the given user has their subscription flag
     * maintained automatically or not. If true, the subscription flag for the given
     * user should be automatically maintained via Stripe. If false, the subscription
     * flag for the given user is manually maintained.
     * <p/>
     * We don't auto-maintain users who are grandfathered into not needing a subscription, or who have a problem
     * with their subscription process and have been granted temporary or permanent subscription by customer support, etc. ...
     * <p/>
     * Note that if a user has no record at all in ct_customer.customers, then the value
     * will be false and they will be treated as "subscription NOT auto maintained".
     * Many of our older user records don't have a ct_customer.customers entry
     */
    private boolean            mIsNotGrandfathered    = false;
    // db fields
    private java.sql.Timestamp mLastSubscriptionAlert = null;
    private Integer            mCtUserId              = null;

    /**
     * Don't use the default constructor; use one of the static methods
     */
    protected SubscriptionHelper()
    {
    }

    public static SubscriptionHelper create(ReadWriteDbConnection sql, String username)
    {
        // we default to Stripe because that is the core of our payment system
        int userId = CTUser.getIdFromUsername(sql, username);
        if (userId < 1)
        {
            throw new IllegalArgumentException("Username " + username + " does not exist");
        }
        return create(sql, userId);
    }

    /**
     * preferred way to instantiate SubscriptionHelper - once you do this its internal
     * fields will be correctly initialized to the latest info about the given
     * patient, so that subsequent calls to informative methods will have the
     * needed data for the correct user
     */
    public static SubscriptionHelper create(ReadWriteDbConnection sql, Integer userId)
    {
        // we default to Stripe because that is the core of our payment system
        return StripeHelper.create(sql, userId);
    }

    /**
     * @return null if no valid customer record or payment info
     */
    public static SubscriptionHelper read(ReadWriteDbConnection sql, int userId) throws SQLException
    {
        if (sql == null)
        {
            throw new IllegalArgumentException("customerSql==null");
        }

        if (sql.isClosed())
        {
            throw new IllegalArgumentException("customerSql==closed");
        }

        assert (userId > 0);

        String customerSubQuery = "SELECT * FROM ct_customer.customers WHERE user_id = ?";
        SqlPreparedStatement statement = sql.prepareStatement(customerSubQuery);
        statement.setInt(1, userId);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
        {
            String paymentMethod = rs.getString("payment_method");
            if (paymentMethod == null)
            {
                paymentMethod = "Stripe";
            }
            switch (PaymentMethod.valueOf(paymentMethod))
            {
                case Stripe:
                    return StripeHelper.create(sql, userId);
                case iOSIAP:
                    return IOSInAppPurchaseHelper.create(sql, userId);
                case Manual:
                    return ManualSubscriptionHelper.create(sql, userId);
                default:
                    throw new IllegalStateException("Customer payment method record does not match possible enum values");
            }
        }

        return null;
    }

    protected void initVars()
    {
        this.mIsSubscribed = false;
        this.mIsNotGrandfathered = false;
        this.mLastSubscriptionAlert = null;
        this.mCtUserId = null;
    }

    protected void readDataFields(ReadWriteDbConnection sql, Integer userId)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT uuu.id, uuu.is_subscribed, ccc.is_auto_maintain_subscription, "
                    + "ccc.last_subscription_alert FROM constant_therapy.users uuu LEFT JOIN ct_customer.customers ccc "
                    + "ON (uuu.id = ccc.user_id) WHERE uuu.id = ?");
            statement.setInt(1, userId);

            ResultSet rs = statement.executeQuery();

            // can only be one row since we went into users table by ID
            if (rs.next())
            {
                this.mCtUserId = userId;
                this.mIsSubscribed = (rs.getInt(KEY_IS_SUBSCRIBED) == 1);

                Integer theInt = rs.getInt(KEY_IS_AUTO_MAINTAIN_SUBSCRIPTION);
                if (rs.wasNull())
                {
                    // if null, or they don't have a ct_customer.customers table record, then we assume that we are NOT auto
                    // maintain
                    this.mIsNotGrandfathered = false;
                    this.mLastSubscriptionAlert = null;
                }
                else
                {
                    this.mIsNotGrandfathered = (theInt == 1);
                    this.mLastSubscriptionAlert = rs.getTimestamp(KEY_LAST_SUBSCRIPTION_ALERT);
                }
            }
        }
        catch (Exception e)
        {
            CTLogger.error("Error getting user data", e);
        }
    }

    public abstract boolean isTrialEnded();

    public abstract boolean isRecurring(DbConnection sql);

    public String getExpirationTimestampString()
    {
        return dateFormat.format(getExpirationTimestamp());
    }

    public abstract Timestamp getExpirationTimestamp();

    java.sql.Timestamp getUserLastSubscriptionAlert()
    {
        return this.mLastSubscriptionAlert;
    }

	/*
    public boolean createDefaultTrialSubscription(Connection sql, CTUser theUser) {
		// should default to Stripe's implementation
	}

	public boolean handleCancel(Connection sql, int ctUserId, String reasonString) {
		// should default to Stripe's implementation
	}
	 */

    /**
     * returns true if trialing and if less than 30 days from trial expiration
     * ... EXPECTS TO HAVE ALREADY HAD
     * THE DATA FIELDS INITIALIZED VIA static create() OR BY setupForUser()
     */
    public boolean isTrialingAndLessThanThirtyDaysFromTrialExpiration()
    {
        if (!isTrialing()) return false;
        long endTime = getExpirationTimestamp().getTime();
        long startTime = new Date().getTime();
        long diffTime = endTime - startTime;
        long daysLeft = diffTime / (1000 * 60 * 60 * 24); // ms, minute, hour, day
        return (daysLeft < 30);
    }

    public abstract boolean isTrialing();

    /**
     * gets the number of days since our last subscription-related alert/message
     * ... returns 0 if last message was today ... EXPECTS TO HAVE ALREADY HAD
     * THE DATA FIELDS INITIALIZED VIA static create() OR BY setupForUser()
     */
    public int getDaysSinceLastSubscriptionAlert()
    {
        if (null == this.mLastSubscriptionAlert) return A_WHOLE_LOT_OF_DAYS;
        long startTime = this.mLastSubscriptionAlert.getTime();
        long endTime = new Date().getTime();
        long diffTime = endTime - startTime;
        long daysLeft = diffTime / (1000 * 60 * 60 * 24); // ms, minute, hour, day
        return (int) daysLeft;
    }

    // [Mahendra, Nov 5, 2014 9:42:56 AM]: needs refactoring
    public int getHoursSinceLastSubscriptionAlert()
    {
        if (null == this.mLastSubscriptionAlert) return A_WHOLE_LOT_OF_DAYS;
        long startTime = this.mLastSubscriptionAlert.getTime();
        long endTime = new Date().getTime();
        long diffTime = endTime - startTime;
        long hoursLeft = diffTime / (1000 * 60 * 60); // ms, minute, hour, day
        return (int) hoursLeft;
    }

    /**
     * gets the number of days before the subscription expires (if trialing) or
     * renews (if active)
     */
    public int getDaysUntilExpiration()
    {

        Timestamp expiration = getExpirationTimestamp();
        if (expiration == null) return -1;

        Long diff = TimeUtil.timeDiff(TimeUtil.timeNow(), expiration, TimeUtil.TimeUnits.DAYS);
        return diff.intValue();
    }

    public void refreshUserSubscriptionStatusFlag(ReadWriteDbConnection sql) throws SQLException
    {
        long startTime = System.nanoTime();
        try
        {
            CTLogger.infoStart("SubscriptionHelper::refreshUserSubscriptionStatusFlag() - START - userId = " + getUserId());

            int ctUserId = getUserId();
            int ctSubscriptionFlag = CT_USER_FLAG_NOT_SUBSCRIBED;

            List<PaymentMethod> otherPaymentMethods = new ArrayList<>(Arrays.asList(PaymentMethod.values()));
            PaymentMethod thisPaymentMethod = PaymentMethod.getPaymentMethodFromInstance(this);
            otherPaymentMethods.remove(thisPaymentMethod);

            // Keep track of which payment method ended up being valid
            SubscriptionHelper currentOrNull = null;

            // if the user is grandfathered, then they are automatically subscribed
            if (!isNotGrandfathered())
            {
                ctSubscriptionFlag = CT_USER_FLAG_SUBSCRIBED;
                CTLogger.debug("this user is grandfathered");
            }

            // even if the user is grandfathered, we still check to see if they are paying us for some reason
            // so we can update the db accordingly

            if (getInternalSubStatusFlag()) // first check this current payment method's flag
            {
                ctSubscriptionFlag = CT_USER_FLAG_SUBSCRIBED;
                currentOrNull = this;

                CTLogger.debug(thisPaymentMethod.toString() + " IS the valid payment method", 1);
            }
            else
            // If its false, then we still need to check the rest of the flags
            {
                CTLogger.debug(thisPaymentMethod.toString() + " is NOT the valid payment method", 1);

                for (PaymentMethod pm : otherPaymentMethods)
                {
                    SubscriptionHelper helper = pm.createInstance(sql, ctUserId);
                    assert helper != null;

                    if (helper.getInternalSubStatusFlag())
                    {
                        CTLogger.debug(pm.toString() + " IS the valid payment method", 1);

                        ctSubscriptionFlag = CT_USER_FLAG_SUBSCRIBED;
                        currentOrNull = helper;
                        break;
                    }
                    else
                    {
                        CTLogger.debug(pm.toString() + " is NOT the valid payment method", 1);
                    }
                }
            }

            if (currentOrNull == null)
            {
                CTLogger.debug("No valid payment method found", 1);
            }

            // update is_subscribed field in users table
            updateUserSubscriptionStatusFlag(sql, ctSubscriptionFlag);

            // Also update the status and payment_method fields in the customers table
            updateSubscriptionStatusAndPmtMethod(sql, currentOrNull);
        }
        finally
        {
            CTLogger.infoEnd(String.format("SubscriptionHelper::refreshUserSubscriptionStatusFlag() - END - executionTime=%.2fms",
                    (System.nanoTime() - startTime) / 1000000.0f));
        }

    }

    public Integer getUserId()
    {
        return this.mCtUserId;
    }

    /**
     * if this value is FALSE, then the user is grandfathered and does not require a subscription.
     * otherwise if the value is TRUE, then the user will need to either be trialing or subscribed in
     * order to access certain features
     */
    public boolean isNotGrandfathered()
    {
        return this.mIsNotGrandfathered;
    }

    /**
     * each implementation of this class will probably have its own flag of whether a user is subscribed through that payment method
     */
    public abstract boolean getInternalSubStatusFlag();

    /**
     * update the constant_therapy.users table's is_subscribed flag
     *
     * @param ctSubscriptionFlag
     */
    private void updateUserSubscriptionStatusFlag(ReadWriteDbConnection sql, int ctSubscriptionFlag) throws SQLException
    {
        int ctUserId = getUserId();

        SqlPreparedStatement statement = sql.prepareStatement("UPDATE users SET is_subscribed = ? WHERE ( id = ? )");

        statement.setInt(1, ctSubscriptionFlag);
        statement.setInt(2, ctUserId);

        statement.executeUpdate();

        CTLogger.info("Subscription status set to " + ctSubscriptionFlag + " for user " + ctUserId);

        this.mIsSubscribed = (ctSubscriptionFlag == CT_USER_FLAG_SUBSCRIBED);
    }

    private void updateSubscriptionStatusAndPmtMethod(ReadWriteDbConnection sql, SubscriptionHelper currentMethodOrNull) throws SQLException
    {
        PaymentStatus status;
        if (isNotGrandfathered())
        {
            if (currentMethodOrNull == null)
            {
                status = PaymentStatus.expired;
            }
            else if (currentMethodOrNull.isTrialing())
            {
                status = PaymentStatus.trialing;
            }
            else
            {
                status = PaymentStatus.paying;
            }
        }
        else
        {
            status = PaymentStatus.grandfathered;
        }

        String q = "UPDATE ct_customer.customers " +
                "SET subscription_status = ?, payment_method = ? " +
                "WHERE ( user_id = ? )";

        SqlPreparedStatement statement = sql.prepareStatement(q);
        statement.setString(1, status.toString());

        PaymentMethod pm = PaymentMethod.getPaymentMethodFromInstance(currentMethodOrNull);
        if (pm != null)
        {
            statement.setString(2, pm.toString());
        }
        else
        {
            statement.setNull(2, Types.VARCHAR);
        }

        statement.setInt(3, this.mCtUserId);

        statement.executeUpdate();
    }

    public void setUserId(int userId)
    {
        this.mCtUserId = userId;
    }

    /**
     * sets the number of days since our last subscription-related alert/message
     * ... EXPECTS TO HAVE ALREADY HAD
     * THE DATA FIELDS INITIALIZED VIA static create() OR BY setupForUser()
     */
    public void updateLastSubscriptionAlert(ReadWriteDbConnection sql)
    {
        // set last_subscription_alert to NOW()
        try
        {
            SqlPreparedStatement statement = sql
                    .prepareStatement("UPDATE ct_customer.customers SET last_subscription_alert = NOW() WHERE ( user_id = ? )");

            statement.setInt(1, this.mCtUserId);

            statement.executeUpdate();

            CTLogger.debug("Last subscription alert set to now for user " + this.mCtUserId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void grandfather(ReadWriteDbConnection customerSql, PaymentStatus type) throws SQLException
    {
        if (customerSql == null)
        {
            throw new IllegalArgumentException("customerSql==null");
        }

        if (customerSql.isClosed())
        {
            throw new IllegalArgumentException("customerSql==closed");
        }

        if (this.mCtUserId == null)
        {
            throw new IllegalStateException("userId==null");
        }

        assert (!(this instanceof IOSInAppPurchaseHelper));

        if (this instanceof StripeHelper && this.isSubscribed())
        {
            // Cancel Stripe subscription.
            StripeHelper uglyCast = (StripeHelper) this;
            ReadWriteDbConnection throwaway = null;
            try
            {
                throwaway = new ReadWriteDbConnection();
                uglyCast.handleCancel(throwaway, type.toString(), false);
            }
            catch (SQLException e)
            {
                throw e;
            }
            finally
            {
                if (throwaway != null)
                {
                    SQLUtil.closeQuietly(throwaway);
                }
            }
        }

        String grandfatherQuery = "UPDATE ct_customer.customers " +
                "SET is_auto_maintain_subscription = 0, subscription_status = ?, stripe_status = ?" +
                " WHERE user_id = ?";
        SqlPreparedStatement statement = customerSql.prepareStatement(grandfatherQuery);
        statement.setString(1, type.toString()); // Subscription status.
        statement.setString(2, type.toString());
        statement.setInt(3, this.mCtUserId);

        statement.executeUpdate();
    }

    /**
     * gets the flag that indicates whether the user has an up-to-date subscription or has
     * been set up to not actually need a subscription to use the software - if this method
     * returns true, we should allow them to use the software
     * ... EXPECTS setupForUser() TO HAVE BEEN CALLED FOR CORRECT USER ID
     */
    public boolean isSubscribed()
    {
        return this.mIsSubscribed;
    }

    public enum PaymentStatus
    {
        grandfathered,
        trialing,
        paying,
        expired,
        trialEnded,
        active,  // deprecate (use 'paying' instead)
        veteran,
        scholarship,
        past_due,
        research,
        flex
    }

    private enum PaymentMethod
    {
        Stripe,
        iOSIAP,
        Manual;

        public static PaymentMethod getPaymentMethodFromInstance(SubscriptionHelper helper)
        {
            if (helper instanceof StripeHelper)
                return Stripe;
            else if (helper instanceof IOSInAppPurchaseHelper)
                return iOSIAP;
            else if (helper instanceof ManualSubscriptionHelper)
                return Manual;
            else
                return null;
        }

        public SubscriptionHelper createInstance(ReadWriteDbConnection sql, Integer userId)
        {
            switch (this)
            {
                case Stripe:
                    return StripeHelper.create(sql, userId);
                case iOSIAP:
                    return IOSInAppPurchaseHelper.create(sql, userId);
                case Manual:
                    return ManualSubscriptionHelper.create(sql, userId);
                default:
                    return null;
            }
        }
    }

    // TODO:
    // public abstract String getUserFriendlyStatusString();
}
