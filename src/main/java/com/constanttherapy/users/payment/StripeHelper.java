package com.constanttherapy.users.payment;

import com.constanttherapy.ServiceBase;
import com.constanttherapy.db.*;
import com.constanttherapy.enums.UserEventType;
import com.constanttherapy.service.proxies.MessagingServiceProxy;
import com.constanttherapy.service.proxies.MessagingServiceProxyArgs;
import com.constanttherapy.users.Patient;
import com.constanttherapy.users.UserEventLogger;
import com.constanttherapy.util.*;
import com.google.gson.reflect.TypeToken;
import com.stripe.exception.*;
import com.stripe.model.*;

import javax.ws.rs.core.UriInfo;
import java.sql.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Date;

/**
 * helper class for working with Stripe cloud API for payments ... note that this is
 * not a static helper class and not a singleton because instantiating makes it
 * unnecessary to synchronize the various methods - at least, from the perspective
 * of protecting local method variables anyhow.
 */
public class StripeHelper extends SubscriptionHelper
{
    // holders for current vals
    private String             mStripeCid                 = null;
    private String             mStripeStatus              = null;
    private java.sql.Timestamp mStripeExpirationTimestamp = null;
    private String             mStripePlanId              = null;

    private class StripeEventType
    {
        /**
         * Stripe event type for a customer deletion event
         */
        static final String CustomerDeleted     = "customer.deleted";
        /**
         * Stripe event type for a customer update event
         */
        static final String CustomerUpdated     = "customer.updated";
        /**
         * Stripe event type for a customer creation event
         */
        static final String CustomerCreated     = "customer.created";
        /**
         * Stripe event type for a customer subscription update event
         */
        static final String SubscriptionUpdated = "customer.subscription.updated";
        /**
         * Stripe event type for a customer subscription deletion event
         */
        static final String SubscriptionDeleted = "customer.subscription.deleted";
        /**
         * Stripe event type for a customer subscription creation event
         */
        static final String SubscriptionCreated = "customer.subscription.created";
        /**
         * Stripe event type for a card being declined
         */
        static final String ChargeFailed        = "charge.failed";
    }

    // subscription statuses we know & care about
    private class SubscriptionStatus
    {
        /**
         * Stripe's status label for subscription is active
         */
        public static final String Active     = "active";
        /**
         * Stripe's status label for subscription is in a trial period
         */
        static final String Trialing   = "trialing";
        /**
         * Our status label for subscription is expired e.g. Stripe status of cancelled, or
         * Stripe status of unpaid (which leads to cancelled) ... Stripe does not have
         * "expired", it has cancelled, unpaid, past_due, active, and trialing
         */
        static final String Expired    = "expired";
        /**
         * our status label for subscription trial period ended without a
         * real subscription being submitted ... we put this into our db
         * when we get a subscription change where the previous status was
         * "trialing" but now there are no active subscriptions
         */
        static final String TrialEnded = "trialEnded";
        /**
         * Stripe's status label for subscription has failed payment
         */
        static final String PastDue    = "past_due";
    }

    // db fields
    /**
     * Stripe customer ID
     */
    private static final String KEY_STRIPE_CID       = "stripe_id";
    /**
     * status from Stripe subscription record, e.g. active, trialing, expired
     */
    private static final String KEY_STRIPE_STATUS    = "stripe_status";
    /**
     * expiration date of the latest-expiring active Stripe subscription
     */
    private static final String KEY_STRIPE_EXPIRES        = "stripe_expires";
    /**
     * CT user ID for reference - this is provided to StripeHelper from outside, not read from db
     */
    private static final String KEY_CT_USER_ID            = "ct_user_id";
    /**
     * stripe subscription plan id
     */
    private static final String KEY_STRIPE_PLAN_ID        = "stripe_plan_id";
    private static final String STRIPE_PLAN_ID_NONE       = "";
    /**
     * non-db key for blurb describing subscription status
     */
    private static final String KEY_BLURB                 = "subscriptionBlurb";
    /**
     * non-db key for subscribe button flag
     */
    private static final String KEY_SUBSCRIBE_BUTTON      = "subscribeButton";
    /**
     * plan ID for 30 day trial - and only 30 day trial e.g. we won't renew it
     */
    //public static final  String THIRTY_DAY_TRIAL_PLAN_ID = "patient_thirty_day_trial";

    /**
     * plan ID for 30 day trial - and only 30 day trial e.g. we won't renew it
     */
    private static final String FIFTEEN_DAY_TRIAL_PLAN_ID = "patient_fifteen_day_trial";
    /**
     * API key for Stripe - test version
     */
    private static final String STRIPE_TEST_API_KEY       = "sk_test_q29MGX87AT9jDhDKTMD1Vrsu";

    // keys for the map of CT fields returned by our big helper function
    /**
     * Constant Therapy user ID
     */
    private static final String KEY_CT_ID = "ctId";

    // public StripeHelper() {
    // initVars();
    // }

    private StripeHelper()
    {
    }

    /**
     * preferred way to instantiate StripeHelper - once you do this its internal
     * fields will be correctly initialized to the latest info about the given
     * patient, so that subsequent calls to informative methods will have the
     * needed data for the correct user
     */
    public static StripeHelper create(ReadWriteDbConnection sql, Integer userId)
    {
        StripeHelper stripey = new StripeHelper();
        stripey.setupForUser(sql, userId);
        return stripey;
    }

    @Override
    protected void initVars()
    {
        super.initVars();

        this.mStripeCid = null;
        this.mStripeStatus = null;
        this.mStripeExpirationTimestamp = null;
        this.mStripePlanId = null;
    }

    /**
     * check the link between CT id and Stripe customer ID, in the ct_customer.customers table
     * ... if there is already a Stripe customer ID for this CT id, make sure it's the same one
     * we got ... if it's not, and the expire date is later and the status is active or trial,
     * then adopt the new one as our official Stripe customer ID and do the rest of the updates
     * ... returns true if we do actually update the record ... this method is only expecting
     * to be called if the user is supposed to have its subscription status auto-maintained
     */
    private boolean checkAndUpdateStripeCustomerData(ReadWriteDbConnection sql, String stripeCidFromWebhook, String statusFromWebhook, long
            endEpochSecFromWebhook, String planIdFromWebhook)
    {
        boolean recordActuallyUpdated = false;

        int ctIdFromWebhook = getUserId();

        try
        {
            CTLogger.infoStart("StripeHelper::checkAndUpdateStripeCustomerData() - user_id=" + ctIdFromWebhook);

            // fetch the customer record info ... the "stripe_id" in the database is the stripe customer ID (CID)
            SqlPreparedStatement statement = sql
                    .prepareStatement("SELECT stripe_id, stripe_status, stripe_expires, stripe_plan_id FROM ct_customer.customers WHERE user_id = ?");

            // java.sql.Timestamp endDateTimestamp = new java.sql.Timestamp(endEpochSec * 1000l);
            statement.setInt(1, ctIdFromWebhook);

            ResultSet rs = statement.executeQuery();
            boolean exists = rs.next();
            if (exists)
            {

                // they have a ct_customer.customers table record
                String dbStripeCid = rs.getString(1);
                String dbStripeStatus = rs.getString(2);
                java.sql.Timestamp dbEndTimestamp = rs.getTimestamp(3);
                java.sql.Timestamp endTimestampFromWebhook = new java.sql.Timestamp(endEpochSecFromWebhook * 1000L);

                // ... should only be one but that table does not insist
                if (rs.next())
                {
                    // WHOA, should not be multiples here
                    CTLogger.error("Found multiple ct_customer.customers rows for user ID, updating subscription for first one only "
                            + ctIdFromWebhook);
                }

                rs.close();
                statement.close();

                boolean shouldUpdate;

                if (!isActive(statusFromWebhook) &&
                        (isTrialing(dbStripeStatus) || isTrialEnded(dbStripeStatus)))
                {
                    // special case: if a trial is not renewed we want to remember it differently
                    // ... the statusFromWebhook will be "expired" (we set it to that when there are
                    // no active subs), but we only want to store "expired" if
                    // we are going from non-trial to expired ... if we are going from trialing
                    // to expired, we want to store a different status so we know we never subscribed
                    // and so our comms to the user can say "your trial expired" instead of "your
                    // subscription expired" ... note that we have to not only store this status correctly when
                    // the record initially goes from trialing to expired, but we have to protect it later if
                    // other updates come in that reaffirm an existing expired-after-trial state
                    statusFromWebhook = SubscriptionStatus.TrialEnded;
                }

                // is the Stripe ID from the table the same one we have? or is there even a Stripe ID in the db?
                if ((null == dbStripeCid) || (stripeCidFromWebhook.compareTo(dbStripeCid) == 0))
                {

                    // new record, or record with matching Stripe customer ID
                    shouldUpdate = true;
                }
                else
                {

                    // there's a Stripe customer ID in the db and it's not the same as the one from the webhook

                    if (!isActive(dbStripeStatus) && isActive(statusFromWebhook))
                    {
                        // new customer and subscription re-activates ... adopt the new customer ID and store
                        // the new status and date ... and log a warning
                        CTLogger.info("New Stripe record reactivates subscription so storing new Stripe ID " + stripeCidFromWebhook + " for user ID "
                                + ctIdFromWebhook);
                        shouldUpdate = true;
                    }
                    else if (isActive(dbStripeStatus) && isActive(statusFromWebhook)
                            && (endTimestampFromWebhook.after(dbEndTimestamp)))
                    {

                        // status from the table is active/trialing, and the status from the Stripe server is also active/trialing,
                        // and the end date from the server is later than the end date from the datbase, so adopt the new customer
                        // ID
                        // and store the new status and date ... and log a warning
                        CTLogger.info("New Stripe record has later end date so storing new Stripe ID " + stripeCidFromWebhook + " for user ID "
                                + ctIdFromWebhook);
                        shouldUpdate = true;
                    }
                    else
                    {

                        // else ignore ... and log a warning
                        shouldUpdate = false;
                        CTLogger.warn("New Stripe customer record would expire or shorten active subscription so ignoring new Stripe ID "
                                + stripeCidFromWebhook + " for user ID " + ctIdFromWebhook);
                    }
                }

                if (shouldUpdate)
                {

                    // update all fields ... the Stripe customer ID might not actually change but that's OK
                    // ... note that in the not-good case where there is more than one ct_customer.customers row per user ID,
                    // all of them will be updated
                    statement = sql
                            .prepareStatement(
                                    "UPDATE ct_customer.customers SET stripe_id = ?, stripe_status = ?, stripe_expires = ?, stripe_plan_id = ? WHERE (user_id = ? )");

                    statement.setString(1, stripeCidFromWebhook);
                    statement.setString(2, statusFromWebhook);

                    // if status goes to expired, that means we don't even actually
                    // have a valid date - Stripe does not send back expired subscriptions
                    // with customer records ... so we should keep the old date
                    // ... unless the old date is in the future in which case this
                    // was not an expiration but a cancellation and we need to store
                    // the cancelled date, otherwise when they look at their status they
                    // will see "your subscription ended in the future"
                    Timestamp timestampToWrite;
                    if (isActive(statusFromWebhook))
                    {
                        // to active, adopt new end date
                        timestampToWrite = endTimestampFromWebhook;
                    }
                    else
                    {
                        // to inactive, keep old end date unless it's in the future
                        java.sql.Timestamp today = new Timestamp((new Date()).getTime());
                        if (dbEndTimestamp.after(today))
                        {
                            timestampToWrite = today;
                        }
                        else
                        {
                            timestampToWrite = dbEndTimestamp;
                        }
                    }
                    statement.setTimestamp(3, timestampToWrite);

                    if (planIdFromWebhook != null)
                    {
                        statement.setString(4, planIdFromWebhook);
                    }
                    else
                    {
                        statement.setNull(4, Types.VARCHAR);
                    }

                    statement.setInt(5, ctIdFromWebhook);

                    statement.executeUpdate();
                    recordActuallyUpdated = true;

                    // Update the local instance variables also:
                    this.mStripeCid = stripeCidFromWebhook;
                    this.mStripeStatus = statusFromWebhook;
                    this.mStripeExpirationTimestamp = timestampToWrite;
                    this.mStripePlanId = planIdFromWebhook;
                }
            }
            else
            {

                // they don't have a ct_customer.customers table record, so we just ignore them ... this is odd, though,
                // since we don't expect to be called in this situation, we expect someone to check the auto-maintain
                // status before calling us
                CTLogger.warn("No ct_customer.customers record so subscription will not be auto-maintained for user " + ctIdFromWebhook);
                recordActuallyUpdated = false;
            }
        }
        catch (SQLException e)
        {

            recordActuallyUpdated = false;
            CTLogger.error("Failed to update Stripe ct_customer data due to SQLException for CT user : " + ctIdFromWebhook);
            e.printStackTrace();
        }
        finally
        {
            CTLogger.unindent();
        }

        // done
        return recordActuallyUpdated;
    }

    /**
     * update our info based on a Stripe customer record, which includes its subscriptions ...
     * returns true if successfully updates the db, false otherwise
     */
    private boolean updateDbFromStripeCustomer(ReadWriteDbConnection sql, com.stripe.model.Customer stripeCustomer)
    {
        boolean success = false;
        Map<String, String> fields = getCtFields(stripeCustomer);

        // transaction! we are setting multiple tables
        try
        {
            sql.setAutoCommit(false);

            // update ct_customer.customers ... returns false if we didn't update it e.g. if the Stripe customer
            // ID didn't match the one we had in our table and the new record would cause the customer to
            // suddenly expire or shorten the end date of the subscription
            assert fields != null;
            boolean needToUpdateUsersTable = checkAndUpdateStripeCustomerData(sql, fields.get(KEY_STRIPE_CID),
                    fields.get(KEY_STRIPE_STATUS), Long.parseLong(fields.get(KEY_STRIPE_EXPIRES)), fields.get(KEY_STRIPE_PLAN_ID));

            // update constant_therapy.users, but only if everything in the ct_customer.customers checks out
            if (needToUpdateUsersTable)
            {
                // updateUserSubscriptionStatusFlag(sql, fields.get(KEY_STRIPE_STATUS), fields.get(KEY_CT_ID));
                refreshUserSubscriptionStatusFlag(sql);
            }

            // done with transaction ... returning to auto commit will commit
            sql.setAutoCommit(true);

            success = true;
        }
        catch (SQLException e)
        {
            try
            {
                sql.rollback();
                sql.setAutoCommit(true);
            }
            catch (SQLException e1)
            {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

        // done
        return success;
    }

    public boolean handleCancel(ReadWriteDbConnection sql, String reason) throws SQLException
    {
        return handleCancel(sql, reason, true);
    }

    /**
     * cancel all Stripe subscriptions for a CT user - asssumes that this CT user
     * exists in our db
     */
    boolean handleCancel(ReadWriteDbConnection sql, String reasonString, boolean log) throws SQLException
    {
        int ctUserId = getUserId();
        CTLogger.info("StripeHelper::handleCancel() - userId=" + ctUserId + ", reason=" + reasonString);

        boolean success = false;

        Map<String, String> stripeInfoFromOurDb = getStripeDataFieldMap(sql);
        String stripeCustomerId = stripeInfoFromOurDb.get(KEY_STRIPE_CID);
        String stripeStatus = stripeInfoFromOurDb.get(KEY_STRIPE_STATUS);

        // should there be anything to unsubscribe from?
        if (null == stripeCustomerId)
        {
            // nothing to unsubscribe from, since no stripe customer ID in
            // db ... they are not subscribed ... they should
            // have known better but this is not a big deal
            CTLogger.warn("Attempted to cancel user " + ctUserId + " that does not have a Stripe customer ID");
        }
        else
        {
            // have Stripe ID
            if (!isActive(stripeStatus))
            {
                // not active subscription, no need to cancel
                CTLogger.warn("Attempted to cancel user " + ctUserId + " that is already expired for Stripe customer ID " + stripeCustomerId);
            }

            // we go ahead and try to cancel even if they are not active in our db
            // when we do have a Stripe customer ID ... that way we can cancel our way
            // out of some data consistency issues

            com.stripe.model.Customer stripeCustomer = getStripeCustomer(null, ctUserId);

            // fetched correctly?
            if (null == stripeCustomer)
            {
                // erp ... we have a Stripe customer ID but we couldn't get the record?!?!?
                CTLogger.error("Could not cancel subscription because could not get Stripe customer record for CT user ID " + ctUserId
                        + " and Stripe customer ID " + stripeCustomerId);
            }
            else
            {
                // have the Stripe customer ... now cancel

                // save for later logging purposes - will be gone after cancel
                com.stripe.model.Subscription theSubscr = getLatestExpiringActiveSubscription(stripeCustomer);
                String subscrId = (theSubscr == null) ? ("NA") : (theSubscr.getId());

                try
                {

                    // com.stripe.model.Subscription cancelledSub = stripeCustomer.cancelSubscription();

                    CustomerSubscriptionCollection subsWrapper = stripeCustomer.getSubscriptions();
                    List<Subscription> subs = subsWrapper.getData();

                    // any subs? succeed even if nothing needed to be canceled
                    boolean cancelSuccess = true;
                    if (subs.size() > 0)
                    {
                        for (Subscription current : subs)
                        {
                            if (isActive(current.getStatus()))
                            {
                                Subscription gotCanceled = current.cancel(null);
                                cancelSuccess &= (gotCanceled != null);
                            }
                        }
                    }

                    if (!cancelSuccess)
                    {
                        // failed to cancel ... this means we actually had subs and failed to cancel them,
                        // we won't log this if there were no subs
                        CTLogger.error("Failed to cancel subscription " + subscrId +
                                " for CT user ID " + ctUserId + " and Stripe customer ID " + stripeCustomerId);
                    }
                    else
                    {
                        // happy
                        success = true;

                        // calling cancel doesn't update the Stripe customer record we already have
                        // so refetch it
                        stripeCustomer = com.stripe.model.Customer.retrieve(stripeCustomer.getId());

                        boolean dbSaveSuccess = updateDbFromStripeCustomer(sql, stripeCustomer);
                        if (!dbSaveSuccess)
                        {
                            // failed to save the cancel to the database, but don't worry, the webhook will try again
                            CTLogger.error("Failed to update db for Stripe subscription we canceled in the Stripe API");
                        }

                        // user event log needs to know
                        if (log)
                        {
                            UserEventLogger.EventBuilder
                                    .create(UserEventType.SubscriptionCanceled)
                                    .userId(ctUserId)
                                    .eventData("Stripe CID " + stripeCustomer.getId() + " Stripe subscr ID " + subscrId + " with reason: " + reasonString)
                                    .log(sql);
                        }
                    }
                }
                catch (AuthenticationException | InvalidRequestException | CardException | APIException | APIConnectionException e)
                {
                    CTLogger.error("Failed to cancel subscription for CT user ID " + ctUserId + " and Stripe customer ID " + stripeCustomerId, e);
                    e.printStackTrace();
                }
            }
        }

        // done
        return success;
    }

    /**
     * handle a subscription request, either creating a new Stripe customer and
     * updating it with the subscription, or by cancelling the existing subscription
     * of an existing Stripe customer then adding a new one to that customer, and
     * then in either case updating our database fields that cache relevant
     * subscription info ... returns false if we are unable to create the
     * customer or subscription in Stripe, or if we are unable to update our own db
     */
    public boolean handleSubscription(ReadWriteDbConnection sql, String aCard, String aCoupon, String aBillingEmail, String aPlan) throws SQLException
    {
        boolean success = false;
        int ctUserId = getUserId();

        CTLogger.infoStart("StripeHelper::handleSubscription() - userId=" + ctUserId);
        // do we have a Stripe customer ID in our db? If so, fetch
        com.stripe.model.Customer stripeCustomer = getStripeCustomer(sql, ctUserId);
        // if customer is there, but deleted, ignore it and make a new one
        // ... unfortunately, getDeleted() can return null
        if (null != stripeCustomer)
        {
            Boolean isDeleted = stripeCustomer.getDeleted();
            if ((null != isDeleted) && (isDeleted))
                stripeCustomer = null;
        }

        // if no current Stripe customer (or existing but deleted in
        // Stripe), create new Stripe customer rec
        if (null == stripeCustomer)
        {

            // create it via Stripe API (and update the db), including creating the subscription
            stripeCustomer = createStripeCustomerWithSubscription(sql,
                    ctUserId, aCard, aCoupon, aBillingEmail, aPlan);

            if (null != stripeCustomer) success = true;
        }
        else
        {
            // cancel old subscription and create a new one
            success = updateStripeCustomerWithSubscription(sql, stripeCustomer, aCard, aCoupon, aBillingEmail, aPlan);
        }

        // done
        return success;
    }

    /**
     * update a new customer from card data, plan, etc. via Stripe API, and save the
     * resulting info to our db ... will return false if we fail to create in Stripe or to
     * save in our db ... note that this method will overwrite any existing Stripe data
     * and user subscription status without checking the existing status
     */
    private boolean updateStripeCustomerWithSubscription(ReadWriteDbConnection sql, com.stripe.model.Customer stripeCustomer,
                                                         String aCard, String aCoupon, String aBillingEmail, String aPlan)
    {

        int ctUserId = getUserId();

        boolean returnValue = false;

        Map<String, Object> customerParams = new HashMap<>();

        if (aCard != null)
            customerParams.put("card", aCard); // token representing card, obtained with Stripe.js

        if (null != aCoupon)
            customerParams.put("coupon", aCoupon);

        if (aPlan == null)
            throw new IllegalArgumentException("No plan specified");

        customerParams.put("plan", aPlan);

        try
        {

            // always kill the existing subscription ... but if there isn't one, Stripe will throw an exception
            // when you call cancelSubscription
            if (getLatestExpiringActiveSubscription(stripeCustomer) != null)
                stripeCustomer.cancelSubscription();

            // we don't put email into trial subscription they get by default upon login,
            // so we have to put it in here ... we have to do this BEFORE we subscribe
            // for payment because the email needs to be in there to get a receipt
            if (null != aBillingEmail)
            {
                Map<String, Object> updateParams = new HashMap<>();
                updateParams.put("email", aBillingEmail);
                stripeCustomer.update(updateParams);
            }

            // create the new subscription
            stripeCustomer.createSubscription(customerParams);

            // calling createSubscription doesn't update the Stripe customer record
            stripeCustomer = com.stripe.model.Customer.retrieve(stripeCustomer.getId());

            // user event log needs to know
            com.stripe.model.Subscription theSubscr = getLatestExpiringActiveSubscription(stripeCustomer);

            UserEventLogger.EventBuilder
                    .create(UserEventType.SubscriptionCreated)
                    .userId(ctUserId)
                    .eventData("Stripe CID " + stripeCustomer.getId() + " Stripe subscr ID " + ((theSubscr == null) ? ("NA") : (theSubscr.getId())))
                    .log(sql);

            // calling udpate doesn't update the Stripe customer record
            stripeCustomer = com.stripe.model.Customer.retrieve(stripeCustomer.getId());

            // don't wait for the webhook to update our db
            returnValue = updateDbFromStripeCustomer(sql, stripeCustomer);
            if (!returnValue)
            {
                // failed to save this customer, so big problem ... db
                // does not match subscription ... but we can't just nuke everything
                // because their old subscription may still be valid
                // ... note that we killed the subscription in Stripe but our db
                // still represents the old subscription
                CTLogger.error("Failed to update db for Stripe subscription we created in the Stripe API");
            }
        }
        catch (AuthenticationException | InvalidRequestException | CardException | APIException | APIConnectionException e)
        {
            CTLogger.error("Failed to update subscription for Stripe customer");
            e.printStackTrace();
        }

        // done
        return returnValue;
    }

    /**
     * create a default 30-day trial customer and plan for a new patient who just logged in
     */
    public boolean createDefaultTrialSubscription(ReadWriteDbConnection sql) throws SQLException
    {
        int ctUserId = getUserId();

        CTLogger.info("StripeHelper::createDefaultTrialSubscription() - userId=" + ctUserId);
        boolean success = false;

        // do we have a Stripe customer ID in our db? If so, fetch
        com.stripe.model.Customer stripeCustomer = getStripeCustomer(sql, ctUserId);

        // if customer is there, but deleted, ignore it and make a new one
        // ... unfortunately, getDeleted() can return null
        if (null != stripeCustomer)
        {
            Boolean isDeleted = stripeCustomer.getDeleted();
            if ((null != isDeleted) && (isDeleted))
                stripeCustomer = null;
        }

        // if no current Stripe customer (or existing but deleted in
        // Stripe), create new Stripe customer rec
        if (null == stripeCustomer)
        {
            // make a new customer in Stripe, with the trial subscription
            stripeCustomer = createStripeCustomerWithSubscription(
                    sql, ctUserId, null, null, null, FIFTEEN_DAY_TRIAL_PLAN_ID);

            if (null != stripeCustomer) success = true;
        }
        else
        {
            // cancel old subscription and create a new one
            success = updateStripeCustomerWithSubscription(sql, stripeCustomer, null, null, null, FIFTEEN_DAY_TRIAL_PLAN_ID);
        }

        // cancel it at the end of the first (trial) period ... otherwise it goes into
        // "active" state for another month, until it realizes it can't bill them because
        // it has no contact info and no credit card, and puts them into expired ... so we
        // set this up so that the trial is already canceled and is just waiting until the end
        // of the trial period to actually cut off the subscription
        if (!success)
            throw new IllegalStateException("tried and failed to create trial subscription for user with id " + ctUserId);

        // refresh customer object
        stripeCustomer = getStripeCustomer(sql, ctUserId);

        com.stripe.model.Subscription theSubscr = getLatestExpiringActiveSubscription(stripeCustomer);
        Map<String, Object> cancelArgs = new HashMap<>();
        cancelArgs.put("at_period_end", true);
        try
        {
            theSubscr.cancel(cancelArgs);
        }
        catch (Exception e)
        {
            CTLogger.error("Failed to set up trial subscription end cancellation for user with id " + ctUserId, e);
        }

        // done
        return true;
    }

    /**
     * create a new customer from card data, plan, etc. via Stripe API, and save the
     * resulting info to our db ... will return null if we fail to create in Stripe or to
     * save in our db ... note that this method will overwrite any existing Stripe data
     * and user subscription status without checking the existing status
     */
    private com.stripe.model.Customer createStripeCustomerWithSubscription(ReadWriteDbConnection sql,
                                                                           int ctUserId, String aCard, String aCoupon, String aBillingEmail, String aPlan)
    {

        CTLogger.info("StripeHelper::createStripeCustomerWithSubscription() - ctUserId=" + ctUserId);

        com.stripe.model.Customer stripeCustomer = null;

        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("description", Integer.toString(ctUserId));

        if (null != aCard) customerParams.put("card", aCard); // token representing card, obtained with Stripe.js
        if (null != aCoupon) customerParams.put("coupon", aCoupon);
        if (null != aBillingEmail) customerParams.put("email", aBillingEmail);

        Map<String, String> theMetadata = new HashMap<>();
        theMetadata.put("userId", Integer.toString(ctUserId));
        customerParams.put("metadata", theMetadata);
        customerParams.put("plan", aPlan);

        try
        {
            stripeCustomer = com.stripe.model.Customer.create(customerParams);

            boolean success;
            if (null != stripeCustomer)
            {
                success = updateDbFromStripeCustomer(sql, stripeCustomer);
                if (!success)
                {
                    // failed to save this customer, so big problem ... db
                    // does not match subscription ... must delete customer
                    // (which cancels subscription
                    CTLogger.error("Failed to update db for Stripe customer we created in the Stripe API");
                    stripeCustomer.delete();
                    stripeCustomer = null;
                }
                else
                {
                    // user event log needs to know
                    com.stripe.model.Subscription theSubscr = getLatestExpiringActiveSubscription(stripeCustomer);

                    UserEventLogger.EventBuilder
                            .create(UserEventType.SubscriptionCreated)
                            .userId(ctUserId)
                            .eventData("Stripe CID " + stripeCustomer.getId() + " Stripe subscr ID " + ((theSubscr == null) ? ("NA") : (theSubscr.getId())))
                            .log(sql);
                }
            }
            else
            {
                CTLogger.error("Failed to create Stripe customer");
            }
        }
        catch (AuthenticationException | InvalidRequestException | CardException | APIException | APIConnectionException e)
        {
            CTLogger.error("Failed to create Stripe customer");
            e.printStackTrace();
        }

        // done
        return stripeCustomer;
    }

    /*
    public boolean updateCreditCard(String cardToken)
	{
		try
		{
			com.stripe.model.Customer theCustomer = com.stripe.model.Customer.retrieve(this.mStripeCid);
			com.stripe.model.Card newCard = theCustomer.createCard(cardToken);
			theCustomer.setDefaultCard(newCard.getId());
			return true;
		}
		catch (Exception e)
		{
			CTLogger.error("Failed to update credit card", e);
			return false;
		}
	}
    */

    private static com.stripe.model.Customer createAnonymousCustomer(String aCard, String email, String description)
            throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException
    {
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("description", description);
        customerParams.put("card", aCard);
        customerParams.put("email", email);

        return com.stripe.model.Customer.create(customerParams);
    }

    /**
     * creates a separate charge not associated with any ct user
     */
    public static String createAndChargeAnonymousCustomer(String aCard, int amount, String email, String description)
    {
        try
        {
            CTLogger.info("StripeHelper::createAndChargeAnonymousCustomer() - email=" + email);
            com.stripe.model.Customer theCustomer = createAnonymousCustomer(aCard, email, description);

            String customerId = theCustomer.getId();

            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", amount);
            chargeParams.put("currency", "usd");
            chargeParams.put("customer", customerId);
            com.stripe.model.Charge.create(chargeParams);

            return customerId;
        }
        catch (AuthenticationException | APIConnectionException | InvalidRequestException | APIException | CardException e)
        {
            CTLogger.error("Failed to create Stripe charge");
            e.printStackTrace();
        }
        return null;
    }

    public static String createAndSubscribeAnonymousCustomer(String aCard, String planId, String email, String description)
    {
        try
        {
            CTLogger.info("StripeHelper::createAndSubscribeAnonymousCustomer() - email=" + email);
            com.stripe.model.Customer theCustomer = createAnonymousCustomer(aCard, email, description);

            Map<String, Object> params = new HashMap<>();
            params.put("plan", planId);
            theCustomer.createSubscription(params);

            return theCustomer.getId();
        }
        catch (AuthenticationException
                | InvalidRequestException
                | APIConnectionException
                | CardException
                | APIException e)
        {
            CTLogger.error("Failed to create Stripe charge");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * retrieve the Stripe customer record for the given CT user ID ... returns null
     * if there is no Stripe customer record for this user
     */
    private com.stripe.model.Customer getStripeCustomer(ReadWriteDbConnection sql, int ctUserId) throws SQLException
    {

        boolean closeConnection = false;
        try
        {
            if (sql == null)
            {
                closeConnection = true;
                sql = new ReadWriteDbConnection();
            }

            com.stripe.model.Customer returnValue = null;

            Map<String, String> existingStripeData = getStripeDataFieldMap(sql);
            // only have null here if the user itself does not exist
            if (null == existingStripeData) return null;
            // have Stripe customer ID?
            String existingStripeID = existingStripeData.get(StripeHelper.KEY_STRIPE_CID);
            if ((null != existingStripeID) && (existingStripeID.length() > 0))
            {
                try
                {

                    returnValue = com.stripe.model.Customer.retrieve(existingStripeID);
                }
                catch (AuthenticationException | InvalidRequestException | CardException | APIException | APIConnectionException e)
                {
                    e.printStackTrace();
                }
            }

            // done
            return returnValue;
        }
        finally
        {
            if (closeConnection) SQLUtil.closeQuietly(sql);
        }
    }

    public boolean isActive()
    {
        return isActive(this.mStripeStatus);
    }

    /**
     * handy checker for Stripe status that for our purposes is active ...
     * we consider Stripe status of "trialing" as active ... null status
     * is inactive
     */
    private static boolean isActive(String status)
    {
        return (status != null)
                && (
                (status.equals(SubscriptionStatus.Active))
                        || (status.equals(SubscriptionStatus.Trialing))
                        || (status.equals(SubscriptionStatus.PastDue))
        );
    }

    /**
     * checks to see if a user account is expired
     */
    public boolean isExpired() { return isExpired(this.mStripeStatus); }

    private static boolean isExpired(String status)
    {
        return status != null && status.equals(SubscriptionStatus.Expired);
    }

    /**
     * checks to see if a user account is active and not trialing
     */
    private static boolean isPaying(String status)
    {
        return status != null
                && (status.equals(SubscriptionStatus.Active) || status.equals(SubscriptionStatus.PastDue));
    }

    /**
     * need a public way to see if the stripe status is just active
     * (aka a paying or special customer)
     */
    public boolean isPaying()
    {
        return isPaying(this.mStripeStatus);
    }

    /**
     * handy checker for whether our status flag says the user was
     * trialing and then expired without paying ... this status is
     * not correlated with any Stripe status, since Stripe only
     * has canceled or unpaid ... we track this so we can send
     * different messages to folks who ended their active period
     * from a trial versus from pay-then-cancel
     */
    @Override
    public boolean isTrialEnded()
    {
        return isTrialEnded(this.mStripeStatus);
    }

    /**
     * handy checker for whether our status flag says the user was
     * trialing and then expired without paying ... this status is
     * not correlated with any Stripe status, since Stripe only
     * has canceled or unpaid ... we track this so we can send
     * different messages to folks who ended their active period
     * from a trial versus from pay-then-cancel
     */
    private static boolean isTrialEnded(String status)
    {
        return status != null && status.equals(SubscriptionStatus.TrialEnded);
    }

    /**
     * handy checker for whether our stat
     * <p>
     * /** handy checker for Stripe status that for our purposes is trialing ...
     * ... null status is inactive
     */
    private static boolean isTrialing(String status)
    {
        return (status != null) && status.equals(SubscriptionStatus.Trialing);
    }

    /**
     * public trialing status checker - true iff trialing - returns false if expired, active, etc.
     * ... EXPECTS CALLER TO HAVE ALREADY RUN setupForUser() WITH CORRECT USER ID
     */
    @Override
    public boolean isTrialing()
    {
        return isTrialing(this.mStripeStatus);
    }

    /**
     * gets the expiration date timestamp (expiration of trial or active, whichever
     * is current ... can return a date in theh past if already expired
     * ... EXPECTS TO HAVE ALREADY HAD
     * THE DATA FIELDS INITIALIZED VIA static create() OR BY setupForUser()
     */
    @Override
    public java.sql.Timestamp getExpirationTimestamp()
    {
        return this.mStripeExpirationTimestamp;
    }

    /**
     * determine whether the given day count is one of the days we use to send
     * messages and emails about expiration of the Stripe account
     */
    public boolean isExpirationWarningDayCount(int theCount)
    {
        switch (theCount)
        {
            case 20:
            case 10:
            case 5:
            case 4:
            case 3:
            case 2:
            case 1:
                return true;
            default:
                return false;
        }
    }

    /**
     * update the constant_therapy.users table's is_subscribed flag based on the Stripe
     * subscription status of a user
     */
    private void updateUserSubscriptionStatusFlag(ReadWriteDbConnection sql, String stripeStatusString)
    {
        Integer ctUserId = getUserId();

        if ((null == stripeStatusString) || (null == ctUserId) || (stripeStatusString.length() == 0))
            return;

        // int ctSubscriptionFlag = CT_USER_FLAG_NOT_SUBSCRIBED;
        // if (isActive(stripeStatusString)) ctSubscriptionFlag = CT_USER_FLAG_SUBSCRIBED;

        try
        {
            // this.updateUserSubscriptionStatusFlag(sql, ctSubscriptionFlag, ctUserId);
            refreshUserSubscriptionStatusFlag(sql);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
    }

    /**
     * Given a Stripe event ID for either a customer event (e.g. customer.created) or a
     * subscription event (e.g. subscription.created), gets the relevant customer and
     * subscription info from Stripe and returns it in a map contining the Stripe
     * customer ID in key "stripeId", our CT ID corresponding to that Stripe customer ID in
     * key "ctId", the expiration date for the latest-expiring subscription of that customer
     * in key "endDate", and the status of the latest-expiring subscription for that customer
     * in key "status" ... note that the end date will be in UNIX epoch time (sec) as a string
     * ... note that if any of these fields are not available this method will return null
     * ... if no subscription is active or trialing, the end date will be zero and
     * the status will be null
     */
    private static Map<String, String> getStripeUpdateFields(String eventId)
    {
        Map<String, String> returnMe = null;

        try
        {
            com.stripe.model.Event theEvent = Event.retrieve(eventId);

            if (isInterestingStripeSubscriptionEvent(theEvent.getType()))
            {
                // subscription event

                // if I get "customer.subscription.*" then the "data.object" is a Subscription object,
                // so i will have to fetch the customer from the customer ID in the "customer" field of the
                // subscription ...
                com.stripe.model.Subscription theSub = (Subscription) theEvent.getData().getObject();

                // Stripe customer ID
                String stripeId = theSub.getCustomer();

                // Stripe customer record
                com.stripe.model.Customer theCustomer = Customer.retrieve(stripeId);

                // it's possible that the customer record is null ... this happens for
                // example if someone deletes the customer in Stripe and we get this
                // subscription webhook event later
                if (null != theCustomer)
                {
                    returnMe = getCtFields(theCustomer);
                }
            }
            else if (isInterestingStripeCustomerEvent(theEvent.getType()))
            {
                // customer event, so "data" is customer - the data.object is actually a real
                // Customer object, the cast is there because getObject()'s method signature
                // says it returns a StripeObject, Customer inherits from StripeObject
                com.stripe.model.Customer theCustomer = (Customer) theEvent.getData().getObject();

                // unfortunately this information may be stale - we may be getting an
                // old record via a webhook, and so we can't trust the info
                // that is in the webhook that describes the customer ... so like
                // the interesting subscription event above, fetch the latest customer
                // info from Stripe via the customer ID that was in the webhook's
                // stale customer record
                theCustomer = Customer.retrieve(theCustomer.getId());

                if (null != theCustomer)
                    returnMe = getCtFields(theCustomer);
            }
            else if (isInterestingStripeChargeEvent(theEvent.getType()))
            {
                com.stripe.model.Charge theCharge = (Charge) theEvent.getData().getObject();

                com.stripe.model.Customer theCustomer = Customer.retrieve(theCharge.getCustomer());

                Map<String, String> data = getCtFields(theCustomer);
                assert data != null;
                String ctUserId = data.get(KEY_CT_ID);

                // Send email to support if this is a charge failure
                if (StripeEventType.ChargeFailed.equals(theEvent.getType()))
                {
                    MessagingServiceProxy msg = new MessagingServiceProxy(ServiceBase.getUriInfo());
                    MessagingServiceProxyArgs args = new MessagingServiceProxyArgs();
                    args.recipients = SubscriptionHelper.informationalEmailRecipients;
                    args.subject = "Stripe Charge Failed For Customer: Action Required";
                    args.body = "Charge failed for User Id: " + ctUserId;
                    msg.sendMessageUsingBody(args);
                }

                // We don't need to update anything in our database for charge events, so return null
                returnMe = null;
            }
            else
            {
                // not supposed to be dealing with anything else
                throw new IllegalArgumentException("Tried to update from Stripe event of unsupported type");
            }
        }
        catch (AuthenticationException e)
        {
            // API key fail
            CTLogger.error("API key failure when fetching Stripe event with id: " + eventId);
            e.printStackTrace();
        }
        catch (InvalidRequestException e)
        {
            // e.g. the event ID does not exist, in which case
            // the e.detailMessage is "No such event"
            CTLogger.error("Failed to retrieve Stripe event for id: " + eventId);
            e.printStackTrace();
        }
        catch (APIConnectionException | CardException | APIException e)
        {
            e.printStackTrace();
        }

        // done
        return returnMe;
    }

    /**
     * given a Stripe customer record, returns a map of CT-relevant fields
     */
    private static Map<String, String> getCtFields(com.stripe.model.Customer theCustomer)
    {
        Map<String, String> returnMe = null;

        if (null == theCustomer) return null;

        Map<String, String> theMetadata = theCustomer.getMetadata();
        if (null == theMetadata)
        {
            CTLogger.error("Stripe customer record without metadata, Stripe ID = " + theCustomer.getId());
            return null;
        }

        // CT ID
        String ctId = theCustomer.getMetadata().get("userId");

        if (null == ctId)
        {
            // can't go on - no way to hook this up with CT records
            CTLogger.error("Stripe customer record without CT user ID metadata - Stripe customer ID = " + theCustomer.getId());
        }
        else
        {
            // active subscription with latest end date ... though we have a subscription record,
            // which is the one that was changed, it may not be the best one
            Subscription bestSub = getSubscriptionForCT(theCustomer);
            // our "best sub" may be non-active - if so it's the latest-expiring non-active one
            // which we track anyhow because its expiration date will be a part of various
            // notices and emails ... or we may have no subs
            long latestEndDate = EPOCH_1999;
            String latestStatus = SubscriptionStatus.Expired;
            String latestPlanId = STRIPE_PLAN_ID_NONE;

            if (null != bestSub)
            {
                latestEndDate = bestSub.getCurrentPeriodEnd();
                latestStatus = bestSub.getStatus();
                latestPlanId = bestSub.getPlan().getId();
            }

            // have everything we need, so return it in a map ...
            returnMe = new HashMap<>();
            returnMe.put(KEY_STRIPE_CID, theCustomer.getId());
            returnMe.put(KEY_CT_ID, ctId);
            returnMe.put(KEY_STRIPE_EXPIRES, Long.toString(latestEndDate));
            returnMe.put(KEY_STRIPE_STATUS, latestStatus);
            returnMe.put(KEY_STRIPE_PLAN_ID, latestPlanId);
        }

        // done
        return returnMe;
    }

    /**
     * given a Stripe customer record, get the active subscription with the latest end date
     * ... returns the latest-expiring non-active sub if no subs are active
     */
    private static com.stripe.model.Subscription getSubscriptionForCT(com.stripe.model.Customer theCustomer)
    {

        Subscription bestSub = getLatestExpiringActiveSubscription(theCustomer);
        if (null == bestSub) bestSub = getLatestExpiringSubscription(theCustomer);

        // done
        return bestSub;
    }

    /**
     * given a Stripe customer record, get the subscription with the latest end date
     * regardless of whether it's active or not
     */
    private static com.stripe.model.Subscription getLatestExpiringSubscription(com.stripe.model.Customer theCustomer)
    {

        // Subscriptions
        CustomerSubscriptionCollection subsWrapper = theCustomer.getSubscriptions();
        List<Subscription> subs = subsWrapper.getData();
        Subscription bestSub = null;

        // any subs?
        if (subs.size() > 0)
        {
            long latestEndDate = 0;
            for (Subscription current : subs)
            {
                long endDate = current.getCurrentPeriodEnd();
                if (endDate > latestEndDate)
                {
                    // "live" subscription with most recent end date so far
                    latestEndDate = endDate;
                    bestSub = current;
                }
            }
        }

        // done
        return bestSub;
    }

    /**
     * given a Stripe customer record, get the active subscription with the latest end date
     */
    private static com.stripe.model.Subscription getLatestExpiringActiveSubscription(com.stripe.model.Customer theCustomer)
    {

        // Subscriptions
        CustomerSubscriptionCollection subsWrapper = theCustomer.getSubscriptions();
        List<Subscription> subs = subsWrapper.getData();
        Subscription bestSub = null;

        // any subs?
        if (subs.size() > 0)
        {
            long latestEndDate = 0;
            for (Subscription current : subs)
            {
                long endDate = current.getCurrentPeriodEnd();
                String status = current.getStatus();
                if ((isActive(status)) && (endDate > latestEndDate))
                {
                    // "live" subscription with most recent end date so far
                    latestEndDate = endDate;
                    bestSub = current;
                }
            }
        }

        // done
        return bestSub;
    }

    /**
     * given the POST body from a Stripe webhook event, checks to see if it is an event type that we
     * care about, and if it is, fetches the data elements we care about from Stripe, so that the
     * caller can then do relevant updates with that data ... note that these are sent even when we
     * made the original API call that changed the record
     */
    private static Map<String, String> getStripeDataForEvent(ReadWriteDbConnection sql, String stripeWebhookPostBody) throws SQLException
    {
        Map<String, String> returnMe = null;

        Map<String, String> map = getStripeEventJsonFromHttpPostBody(stripeWebhookPostBody);

        String id = map.get("id");
        String type = map.get("type");

        if ((null != id) && (null != type))
        {

            // [Mahendra, Dec 18, 2014 9:31:31 AM]: store this in the stripe_webhooks table
            // to be post-processed for reports and such
            storeEventData(sql, map, stripeWebhookPostBody);

            if ((isInterestingStripeCustomerEvent(type)) ||
                    (isInterestingStripeSubscriptionEvent(type)) ||
                    (isInterestingStripeChargeEvent(type)))
            {

                // SUBSCRIPTION OR CUSTOMER EVENT ... DO SOMETHING ABOUT IT
                CTLogger.info("Stripe Event ID, type: " + id + ", " + type);
                returnMe = getStripeUpdateFields(id);
            }
        }

        // done
        return returnMe;
    }

    /**
     * detects Stripe customer-related event types we care about
     */
    private static boolean isInterestingStripeCustomerEvent(String stripeEventType)
    {
        return (0 == stripeEventType.compareTo(StripeEventType.CustomerCreated)) ||
                (0 == stripeEventType.compareTo(StripeEventType.CustomerDeleted)) ||
                (0 == stripeEventType.compareTo(StripeEventType.CustomerUpdated));
    }

    /**
     * detects Stripe subscription-related event types we care about
     */
    private static boolean isInterestingStripeSubscriptionEvent(String stripeEventType)
    {
        return (0 == stripeEventType.compareTo(StripeEventType.SubscriptionCreated)) ||
                (0 == stripeEventType.compareTo(StripeEventType.SubscriptionDeleted)) ||
                (0 == stripeEventType.compareTo(StripeEventType.SubscriptionUpdated));
    }

    /**
     * detects Stripe charge-related event types we care about
     */
    private static boolean isInterestingStripeChargeEvent(String stripeEventType)
    {
        return 0 == stripeEventType.compareTo(StripeEventType.ChargeFailed);
    }

    /**
     * given an HTTP post body element from a Stripe webhook endpoing call, extract
     * the Stripe event ID and the Stripe event type, returning them in a String map
     */
    @SuppressWarnings("unchecked")
    static Map<String, String> getStripeEventJsonFromHttpPostBody(String postBody)
    {
        Map<String, String> returnMe = new HashMap<>();
        String bodyWithoutReturns = postBody.replace('\n', ' ');

        // the Stripe.com APIs have an "Event" class that we could use to
        // parse the request from the server, but that is risky since the
        // event object is very complex and someone could try to break the
        // parser by sending bogus events that would allow who knows what hacks.
        // So we just use a simplified POJO to get the event ID. From there
        // we request that event securely from the Stripe server, so we know
        // it is authentic, and process it
        Map<String, Object> map = GsonHelper.getGson().fromJson(bodyWithoutReturns,
                new TypeToken<Map<String, Object>>()
                {
                }.getType());

		/* Process map for each stripe event type.  The structure is typically like this:
           Example: customer.subscription.created
			{
			  "id": "evt_15BHtb2Oy29pqGwqsJlrB1Eb",
			  "created": 1418961463,
			  "type": "customer.subscription.created",
			  "data": {
			    "object": {
			      "id": "sub_5LztKq4cTuBJAJ",
			      "plan": {
			        "id": "patient_thirty_day_trial",
			        "object": "plan",
			      },
			      "object": "subscription",
			      "status": "trialing",
			      "customer": "cus_5Lzt1wpHuIdFeY",
			    }
			  },
			  "object": "event"
			}
		 */

        // grab id, type and timestamp from top level
        String eventId = (String) map.get("id");
        String type = (String) map.get("type");
        Long timestamp = ((Double) map.get("created")).longValue();

        if ((null != eventId) && (null != type))
        {

            returnMe.put("id", eventId);
            returnMe.put("type", type);
            returnMe.put("timestamp", timestamp.toString());

            Map<String, Object> dataMap = (Map<String, Object>) map.get("data");

            if (dataMap != null)
            {
                // data map typically has an object child node
                // String object = (String) map.get("object");
                Map<String, Object> objectMap = (Map<String, Object>) dataMap.get("object");

                if (objectMap != null)
                {
                    String objectId = (String) objectMap.get("id");
                    if (objectId != null)
                        returnMe.put("objectId", objectId);

                    returnMe.put("customerId", (String) objectMap.get("customer"));

                    if (type.startsWith("charge."))
                    {
                        if (objectId == null)
                            returnMe.put("objectId", (String) objectMap.get("charge"));

                        Double amount = (Double) objectMap.get("amount");
                        if (amount != null)
                            amount = amount / 100;
                        else
                            amount = 0.0d;

                        returnMe.put("data", amount.toString());
                    }
                    else if (type.startsWith("invoice."))
                    {
                        // get the total amount
                        Double amount = (Double) objectMap.get("amount_due");
                        if (amount != null)
                            amount = amount / 100;
                        else
                            amount = 0.0d;

                        returnMe.put("data", amount.toString());
                    }
                    else if (type.equals("customer.created") || type.equals("customer.updated"))
                    {
                        returnMe.put("customerId", (String) objectMap.get("id"));
                        returnMe.put("data", (String) objectMap.get("description")); // user_id
                    }
                    else if (type.startsWith("customer.subscription."))
                    {
                        Map<String, Object> planMap = (Map<String, Object>) objectMap.get("plan");

                        if (planMap != null)
                            returnMe.put("data", (String) planMap.get("id"));
                    }
                    else if (type.startsWith("customer.discount"))
                    {
                        Map<String, Object> couponMap = (Map<String, Object>) objectMap.get("coupon");
                        if (couponMap != null)
                        {
                            if (!returnMe.containsKey("objectId") || (returnMe.containsKey("objectId") && returnMe.get("objectId") == null))
                                returnMe.put("objectId", (String) couponMap.get("id"));
                            else
                                returnMe.put("data", (String) couponMap.get("id"));
                        }
                    }
                }
            }
        }
        else
        {
            // bogus
            CTLogger.error("Stripe event received but could not be parsed");
        }
        // done
        return returnMe;
    }

    /**
     * given a user ID, gets the Stripe fields in a Map<String, String> suitable for
     * returning as JSON ... returns null if failure to read i.e. if bad user ID
     * ... note that if the user does not have any Stripe-originated data, the
     * fields we get from Stripe will be null in the map, but there is always
     * a value for KEY_IS_SUBSCRIBED
     */
    public Map<String, String> getStripeDataFieldMap(ReadWriteDbConnection sql)
    {
        Map<String, String> returnValue;

        readDataFields(sql, getUserId());

        returnValue = new HashMap<>();
        returnValue.put(KEY_CT_USER_ID, "" + getUserId());
        returnValue.put(KEY_IS_SUBSCRIBED, "" + (isSubscribed() ? 1 : 0));
        returnValue.put(KEY_STRIPE_CID, this.mStripeCid);
        returnValue.put(KEY_STRIPE_STATUS, this.mStripeStatus);
        DateFormat myDateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        returnValue.put(KEY_STRIPE_EXPIRES, (
                (this.mStripeExpirationTimestamp == null) ? null : (myDateFormat.format(this.mStripeExpirationTimestamp))));
        returnValue.put(KEY_IS_AUTO_MAINTAIN_SUBSCRIPTION, "" + (isNotGrandfathered() ? 1 : 0));
        returnValue.put(KEY_LAST_SUBSCRIPTION_ALERT, (
                (getUserLastSubscriptionAlert() == null) ? null : (myDateFormat.format(getUserLastSubscriptionAlert()))));

        // done
        return returnValue;
    }

    /* get the API key from system params - will only run once on class load */
    static
    {

        // default to test
        com.stripe.Stripe.apiKey = STRIPE_TEST_API_KEY;

        ReadOnlyDbConnection sqlr = null;
        try
        {

            sqlr = new ReadOnlyDbConnection();

            SqlPreparedStatement statement = sqlr.prepareStatement(
                    "SELECT sys_param_value FROM ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "stripePrivateAPIKey");
            ResultSet rs = statement.executeQuery();

            // unique so should be only one row
            if (rs.next())
            {
                com.stripe.Stripe.apiKey = rs.getString(1);
                CTLogger.debug("Stripe API key set from system parameters table");
            }
            else
            {
                CTLogger.warn("No Stripe API key found in system parameters table, using test key");
            }
        }
        catch (Exception e)
        {
            CTLogger.error("Error getting Stripe API key from system parameters table, setting to test value", e);
        }
        finally
        {
            SQLUtil.closeQuietly(sqlr);
        }
    }

    /**
     * set up this instance with data for a particular user
     */
    private void setupForUser(ReadWriteDbConnection sql, Integer userId)
    {
        initVars();
        readDataFields(sql, userId);
    }

    /**
     * update my local data fields
     */
    @Override
    protected void readDataFields(ReadWriteDbConnection sql, Integer userId)
    {
        super.readDataFields(sql, userId);

        try
        {
            SqlPreparedStatement statement = sql
                    .prepareStatement("SELECT ccc.stripe_id, ccc.stripe_status, ccc.stripe_expires, ccc.stripe_plan_id FROM users uuu "
                            + "LEFT JOIN ct_customer.customers ccc ON (uuu.id = ccc.user_id) WHERE uuu.id = ?");
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();

            // can only be one row since we went into users table by ID
            if (rs.next())
            {
                this.mStripeCid = rs.getString(KEY_STRIPE_CID);
                this.mStripeStatus = rs.getString(KEY_STRIPE_STATUS);
                this.mStripeExpirationTimestamp = rs.getTimestamp(KEY_STRIPE_EXPIRES);
                this.mStripePlanId = rs.getString(KEY_STRIPE_PLAN_ID);
            }
        }
        catch (Exception e)
        {
            CTLogger.error("Error getting Stripe data", e);
        }
    }

    private float applyDiscount(float startValue, com.stripe.model.Discount theDiscount)
    {
        float returnValue = startValue;
        if (null != theDiscount)
        {
            com.stripe.model.Coupon theCoupon = theDiscount.getCoupon();
            if (null != theCoupon)
            {
                // currently we apply both types of discounts, assuming that only one will actually
                // be non-null
                Integer theAmountOff = theCoupon.getAmountOff();
                if (null != theAmountOff) returnValue -= (((float) theAmountOff) / 100.0f);
                Integer thePercentOff = theCoupon.getPercentOff();
                if (null != thePercentOff) returnValue /= ((float) thePercentOff);
            }
        }
        // done
        return returnValue;
    }

    public Map<String, String> getSubscriptionInfo(ReadWriteDbConnection sql, UriInfo theUriInfo)
    {
        CTLogger.debug("StripeHelper::getSubscriptionInfo() - userId=" + getUserId());

        // also updates our local vars when we call getStripeDataFieldMap
        Map<String, String> returnValue = getStripeDataFieldMap(sql);

        // OK now the interesting part: the blurb & subscribe button
        DateFormat myDateFormat = DateFormat.getDateInstance(DateFormat.LONG);

        // patient changing own schedule, then sched.patientId == sched.clinicianId
        MessagingServiceProxy msgProxy = new MessagingServiceProxy(theUriInfo);
        Map<String, String> replaceMap = new HashMap<>();

        if (!isNotGrandfathered())
        {
            if (!isSubscribed())
            {
                // is not auto maintain, is not subscribed: "not enabled"
                // replaceMap.put("$(days_to_expire)", "" + stripey.getDaysUntilExpiration());
                returnValue.put(KEY_BLURB, msgProxy.getMessageBodyFromTemplate("msg_your_account_is_not_enabled", null));
                returnValue.put(KEY_SUBSCRIBE_BUTTON, "hide");
            }
            else
            {
                // is not auto maintain, is subscribed: "does not currently require a subscription"
                returnValue.put(KEY_BLURB, msgProxy.getMessageBodyFromTemplate("msg_account_subscription_not_required", null));
                returnValue.put(KEY_SUBSCRIBE_BUTTON, "hide");
            }
        }
        else
        {
            if (isSubscribed())
            {

                if (isTrialing(this.mStripeStatus))
                {

                    // is auto maintain, is subscribed, trial: days left in trial
                    replaceMap.put("$(days_to_expire)", "" + getDaysUntilExpiration());

                    returnValue.put(KEY_BLURB,
                            msgProxy.getMessageBodyFromTemplate("msg_you_have_days_left_in_trial", replaceMap));
                    returnValue.put(KEY_SUBSCRIBE_BUTTON, "subscribe");
                }
                else if (isActive(this.mStripeStatus))
                {

                    // is auto maintain, is subscribed, active: tell them monthly cost
                    // your current subscription cost: $19.99/month
                    try
                    {
                        com.stripe.model.Customer stripeCustomer = com.stripe.model.Customer.retrieve(this.mStripeCid);
                        com.stripe.model.Discount theDiscount = stripeCustomer.getDiscount();
                        com.stripe.model.Subscription theSub = getLatestExpiringActiveSubscription(stripeCustomer);
                        if (null == theSub) theSub = getLatestExpiringSubscription(stripeCustomer);
                        com.stripe.model.Plan thePlan = theSub.getPlan();
                        NumberFormat theFormatter = NumberFormat.getCurrencyInstance();
                        Float theAmount = (float) thePlan.getAmount() / 100.0f; // still need to apply discount
                        com.stripe.model.Discount subDiscount = theSub.getDiscount();
                        // subscription discount wins
                        if (null != subDiscount) theDiscount = subDiscount;
                        theAmount = applyDiscount(theAmount, theDiscount);

                        replaceMap.put("$(cost)", theFormatter.format(theAmount) + " /" + thePlan.getInterval());
                        returnValue.put(KEY_BLURB,
                                msgProxy.getMessageBodyFromTemplate("msg_your_current_subscription_cost", replaceMap));
                        returnValue.put(KEY_SUBSCRIBE_BUTTON, "cancel");
                    }
                    catch (Exception e)
                    {

                        returnValue.put(KEY_BLURB,
                                msgProxy.getMessageBodyFromTemplate("msg_could_not_retrieve_subscription", null));
                        CTLogger.error("Could not retrieve subscription info for stripe CID " + this.mStripeCid + " user ID "
                                + getUserId(), e);
                    }
                }
                else
                {
                    if (null == this.mStripeStatus)
                    {

                        // is auto maintain, is subscribed, null expiration: have not logged in yet
                        returnValue.put(KEY_BLURB,
                                msgProxy.getMessageBodyFromTemplate("msg_trial_starts_when_you_use_app", null));
                        returnValue.put(KEY_SUBSCRIBE_BUTTON, "subscribe");
                    }
                    else
                    {
                        // ehsan (11-11-2014): this is no longer an illegal state because there are alternative payment methods to
                        // Stripe
                        /*
                        // is auto maintain, is subscribed, expired: ERROR
						throw new IllegalStateException("ERROR: Subscribed user with null subscription status");
						 */
                    }
                }
            }
            else
            { // user.is_subscribed = 0
                if (null == this.mStripeStatus)  // haven't started trial yet
                {
                    // TODO: [mahendra, 6/1/16 9:16 AM] - this logic should be re-thought
                    // we got into this piece of code when is_subscribed was defaulted to 0 by the CTUser class
                    // (overriding the default in the user table).  The CTUser now defaults isSubscribed to 1,
                    // so this scenario should not occur unless someone manually changes the field in the users table

                    // is auto maintain, is not subscribed, null: "disabled by administrator"
                    returnValue.put(KEY_BLURB,
                            msgProxy.getMessageBodyFromTemplate("msg_account_disabled", null));
                    returnValue.put(KEY_SUBSCRIBE_BUTTON, "hide");
                }
                else
                {
                    // is auto maintain, is not subscribed, expired: "expired on"
                    replaceMap.put("$(expiration_date)", myDateFormat.format(this.mStripeExpirationTimestamp));
                    returnValue.put(KEY_BLURB,
                            msgProxy.getMessageBodyFromTemplate("msg_your_subscription_expired_on", replaceMap));
                    returnValue.put(KEY_SUBSCRIBE_BUTTON, "subscribe");
                }
            }
        }

        // done
        return returnValue;
    }

    /**
     * given a Stripe event, update our database
     */
    public static void updateDbFromStripeEvent(ReadWriteDbConnection sql, String httpPostBody, UriInfo theUriInfo)
    {
        if (null == sql) throw new IllegalArgumentException();

        try
        {
            CTLogger.infoStart("StripeHelper::updateDbFromStripeEvent() - httpPostBody=" + httpPostBody.substring(0, Math.min(150,httpPostBody.length())));
            Map<String, String> customerDataMap = getStripeDataForEvent(sql, httpPostBody);

            if (null != customerDataMap)
            {
                // it was a request that we cared about and were able to successfully parse
                String ctUserIdString = customerDataMap.get(StripeHelper.KEY_CT_ID);
                String stripeCustomerId = customerDataMap.get(StripeHelper.KEY_STRIPE_CID);
                String stripeStatus = customerDataMap.get(StripeHelper.KEY_STRIPE_STATUS);
                String stripePlanId = customerDataMap.get(StripeHelper.KEY_STRIPE_PLAN_ID);
                long subscriptionEndEpochSecs = Long.parseLong(customerDataMap.get(StripeHelper.KEY_STRIPE_EXPIRES));

                CTLogger.debug("Stripe customer with Stripe ID: " + stripeCustomerId + ", CT ID = " + ctUserIdString);

                if (subscriptionEndEpochSecs == EPOCH_1999)
                {
                    // not real end date, means no subscription
                    CTLogger.debug("Stripe subscription end Date: No subscription");
                }
                else
                {
                    CTLogger.debug("Stripe subscription end Date: " +
                            new java.util.Date(subscriptionEndEpochSecs * 1000L) + ", status = " + stripeStatus);
                }

                int ctUserId = Integer.parseInt(ctUserIdString);
                StripeHelper stripey = StripeHelper.create(sql, ctUserId);

                if (!stripey.isNotGrandfathered())
                {
                    // should not make changes initiated by to Stripe
                    CTLogger.error("Cloud API change to subscription status refused for non-auto-maintain user id: " + ctUserId);
                }
                else
                {
                    // save state from before we do this stuff so we can compare later
                    String beforeUpdateStripeStatus = stripey.mStripeStatus;

                    // transaction! we are setting multiple tables
                    sql.setAutoCommit(false);

                    // update ct_customer.customers ... returns false if we didn't update it e.g. if the Stripe customer
                    // ID didn't match the one we had in our table and the new record would cause the customer to
                    // suddenly expire or shorten the end date of the subscription
                    boolean needToUpdateUsersTable = stripey.checkAndUpdateStripeCustomerData(sql, stripeCustomerId, stripeStatus,
                            subscriptionEndEpochSecs, stripePlanId);

                    // update constant_therapy.users, but only if everything in the ct_customer.customers checks out
                    if (needToUpdateUsersTable) stripey.updateUserSubscriptionStatusFlag(sql, stripeStatus);

                    // done with transaction
                    sql.commit();
                    sql.setAutoCommit(true);

                    String afterUpdateStripeStatus = stripeStatus;

                    // send email if went from active/trialing to expired/trialEnded
                    sendSubscriptionStateTransitionedEmailIfNeeded(sql, theUriInfo, beforeUpdateStripeStatus, afterUpdateStripeStatus, stripey);

                    // If the sub status changed, then log this as a user event
                    if (!Objects.equals(afterUpdateStripeStatus, beforeUpdateStripeStatus))
                    {
                        UserEventLogger.logEvent(sql, UserEventType.UserSubscriptionSubStatusChanged, "stripe_status",
                                stripey.getUserId(), String.format("%s|%s", beforeUpdateStripeStatus, afterUpdateStripeStatus), TimeUtil.timeNow(),
                                null);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            try
            {
                sql.rollback();
                sql.setAutoCommit(true);
            }
            catch (SQLException e1)
            {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    /**
     * sends the appropriate email, if needed, based on the transition from the previous to the current stripe status
     * (note that this method is designed to be used when ct-services receive an event directly from the Stripe server)
     */
    private static void sendSubscriptionStateTransitionedEmailIfNeeded(ReadWriteDbConnection sql,
                                                                       UriInfo theUriInfo,
                                                                       String beforeUpdateStripeStatus,
                                                                       String afterUpdateStripeStatus,
                                                                       StripeHelper stripey) throws SQLException
    {
        MessagingServiceProxy msg = new MessagingServiceProxy(theUriInfo);

        int ctUserId = stripey.getUserId();

        // If we're going from 'active' state to 'past_due' state, then there is a special email to send to support
        if (beforeUpdateStripeStatus.equals(SubscriptionStatus.Active) &&
                afterUpdateStripeStatus.equals(SubscriptionStatus.PastDue))
        {
            // REVIEW: should we log this in the user events??

            MessagingServiceProxyArgs args = new MessagingServiceProxyArgs();
            args.recipients = SubscriptionHelper.informationalEmailRecipients;
            args.subject = "Stripe Customer Status changed to 'Past Due' - Action Required";
            args.body = "This is due to a failed charge for User Id: " + ctUserId;
            msg.sendMessageUsingBody(args);
        }

        // If we are going from a non-expired to expired state, then send an email to both support and patient
        if (isActive(beforeUpdateStripeStatus))
        {
            if (!isActive(afterUpdateStripeStatus))
            {
                // send an email containing the subscription link
                Patient thePatient = (Patient) Patient.getById(ctUserId);
                Map<String, String> tokens = new HashMap<>();
                tokens.put("$(firstname)",
                        (thePatient.firstName == null || thePatient.firstName.length() == 0 ? thePatient.getUsername() : thePatient.firstName));

                EmailHelperBase.initialize(theUriInfo);

                tokens = EmailHelperBase.addReplacementTokensForPatientStats(sql, tokens, thePatient, stripey);

                String recipients = thePatient.getEmail();
                String replacement_json = GsonHelper.toJson(tokens);

                MessagingServiceProxyArgs args = new MessagingServiceProxyArgs();
                args.recipients = recipients;
                args.replacementTokensJson = replacement_json;
                args.showStats = true;

                if (isTrialing(beforeUpdateStripeStatus))
                {
                    // TO PATIENT WHOSE TRIAL EXPIRED: email_to_patient_your_trial_period_has_expired.html
                    args.templateType = "email_to_patient_your_trial_period_has_expired";
                    msg.sendMessageUsingTemplate(args);
                }
                else
                {
                    // NON-TRIAL PATIENT WHO PAID AT SOME POINT: email_to_patient_terminating_subscription.html
                    args.templateType = "email_to_patient_terminating_subscription";
                    msg.sendMessageUsingTemplate(args);

                    // Also notify support that this subscription ended:
                    // REVIEW: should we log this in the user events??

                    args = new MessagingServiceProxyArgs();
                    args.recipients = SubscriptionHelper.informationalEmailRecipients;
                    args.subject = "Subscription Terminated for Customer";
                    args.body = "Subscription Terminated for User Id: " + ctUserId;
                    msg.sendMessageUsingBody(args);
                }
            }
            else
            {
                CTLogger.debug("Stripe status is still active so no expiration email sent");
            }
        }
        else
        {
            CTLogger.debug("Stripe status was not active so no expiration email sent");
        }
    }

    /**
     * if this is the first login for this user ever, make a 30 day trial for them
     */
    public void checkForFirstLogin(ReadWriteDbConnection sql) throws SQLException
    {
        // only care about auto-maintain folks
        if (isNotGrandfathered())
        {
            // if they have logged in before they will have a non-null value in the
            // ct_customer.customers table for the Stripe customer ID

            Map<String, String> stripeMap = getStripeDataFieldMap(sql);
            String stripeCid = stripeMap.get(KEY_STRIPE_CID);
            if ((null == stripeCid) || (stripeCid.length() == 0))
            {
                // new guy ... sign them up for 30 day trial right away
                createDefaultTrialSubscription(sql);
            }
        }
    }

    public static List<Map<String, String>> getAvailableSubscriptionPlans()
    {
        List<Map<String, String>> listOfPlans = new ArrayList<>();

        try
        {
            PlanCollection fromStripe = com.stripe.model.Plan.all(null);
            if (null == fromStripe) throw new IllegalStateException("No subscription plans available from Stripe");

            // add each one to my list, unless they have a trial period
            List<Plan> thePlans = fromStripe.getData();

            Map<String, String> planStuff;
            for (Plan currentPlan : thePlans)
            {
                // trial?
                if (null == currentPlan.getTrialPeriodDays())
                {
                    // not trial
                    planStuff = new HashMap<>();
                    planStuff.put("id", currentPlan.getId());
                    planStuff.put("rawCost", "" + currentPlan.getAmount());
                    NumberFormat theFormatter = NumberFormat.getCurrencyInstance();
                    String theCostString = theFormatter.format(((float) currentPlan.getAmount()) / 100.0f);
                    planStuff.put("cost", theCostString);
                    planStuff.put("interval", currentPlan.getInterval());
                    planStuff.put("name", currentPlan.getName());
                    planStuff.put("statementDescription", currentPlan.getStatementDescription());
                    listOfPlans.add(planStuff);
                }
            }
        }
        catch (AuthenticationException | InvalidRequestException | CardException | APIException | APIConnectionException e)
        {
            e.printStackTrace();
        }
        // done
        return listOfPlans;
    }

    @Override
    public boolean isRecurring(DbConnection sql)
    {
        // Stripe is always a recurring plan (for now..)
        return true;
    }

    static void storeEventData(ReadWriteDbConnection sql, Map<String, String> map, String rawData) throws SQLException
    {
        String q = "INSERT INTO ct_customer.stripe_events " +
                "(event_id, type, json, customer_id, object_id, timestamp, data) VALUES (?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE type=?, customer_id=?, object_id=?, timestamp=?, data=?, json=?";

        SqlPreparedStatement statement = sql.prepareStatement(q);

        statement.setString(1, map.get("id"));
        // statement.setString(1, "test1");

        statement.setString(2, map.get("type"));
        statement.setString(8, map.get("type"));

        statement.setString(3, rawData);
        statement.setString(13, rawData);

        String customerId = map.get("customerId");
        if (customerId == null)
        {
            statement.setNull(4, Types.VARCHAR);
            statement.setNull(9, Types.VARCHAR);
        }
        else
        {
            statement.setString(4, customerId);
            statement.setString(9, customerId);
        }

        String objectId = map.get("objectId");
        if (objectId == null)
        {
            statement.setNull(5, Types.VARCHAR);
            statement.setNull(10, Types.VARCHAR);
        }
        else
        {
            statement.setString(5, objectId);
            statement.setString(10, objectId);
        }

        String ts = map.get("timestamp");

        if (ts == null)
        {
            statement.setTimestamp(6, TimeUtil.timeNow());
            statement.setTimestamp(11, TimeUtil.timeNow());
        }
        else
        {
            Timestamp timestamp = TimeUtil.getTimestampFromLong(MathUtil.tryParseLong(ts));

            statement.setTimestamp(6, timestamp);
            statement.setTimestamp(11, timestamp);
        }

        String data = map.get("data");
        if (data == null)
        {
            statement.setNull(7, Types.VARCHAR);
            statement.setNull(12, Types.VARCHAR);
        }
        else
        {
            statement.setString(7, data);
            statement.setString(12, data);
        }

        statement.execute();
    }

    public String getStripeStatus()
    {
        return this.mStripeStatus;
    }

    @Override
    public boolean getInternalSubStatusFlag()
    {
        return isActive();
    }
}
