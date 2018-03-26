package com.constanttherapy.users.payment.ios;

import com.constanttherapy.ServiceBase;
import com.constanttherapy.db.*;
import com.constanttherapy.enums.UserEventType;
import com.constanttherapy.service.proxies.MessagingServiceProxy;
import com.constanttherapy.service.proxies.MessagingServiceProxyArgs;
import com.constanttherapy.users.CTUser;
import com.constanttherapy.users.Customer;
import com.constanttherapy.users.UserEventLogger;
import com.constanttherapy.users.UserPreferences;
import com.constanttherapy.users.payment.StripeHelper;
import com.constanttherapy.users.payment.SubscriptionHelper;
import com.constanttherapy.users.payment.ios.ReceiptWrapper.Receipt;
import com.constanttherapy.users.payment.ios.ReceiptWrapper.Receipt.InAppPurchaseReceipt;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.GsonHelper;
import com.constanttherapy.util.MathUtil;
import com.constanttherapy.util.TimeUtil;

import org.apache.commons.lang.exception.ExceptionUtils;

import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ehsan
 */
public class IOSInAppPurchaseHelper extends SubscriptionHelper
{
    /**
     * Apple's endpoint for unsubscribing from Auto-renewable subscriptions
     */
    public static final  String iTunesManageSubscriptionsURL                  =
            "https://buy.itunes.apple.com/WebObjects/MZFinance.woa/wa/manageSubscriptions";
    /**
     * our status label for an active ios subscription
     */
    public static final  String IOS_IAP_SUB_STATUS_ACTIVE                     = "active";
    /**
     * our status label for a past due ios subscription
     */
    @Deprecated
    public static final  String IOS_IAP_SUB_STATUS_PAST_DUE                   = "past_due";
    /**
     * our status label for an expired ios subscription
     */
    public static final  String IOS_IAP_SUB_STATUS_EXPIRED                    = "expired";
    /**
     * status from Apple's records
     */
    public static final  String KEY_IOS_IAP_STATUS                            = "ios_iap_status";
    // dictionary fields
    public static final  String KEY_IOS_IAP_BLURB                             = "ios_iap_blurb";
    /**
     * the user preference key for the count of expired account attemtps
     */
    @Deprecated
    private static final String consecutivePastDueAccountAttemptsCountPrefKey = "_consecutivePastDueAccountAttempts";
    /**
     * the number of extra app usage attempts a user gets after their iOS expiration timestamp (until it renews)
     */
    @Deprecated
    private static final int    gracePeriodMaxNumberOfPastDueAccountAttempts  = 3;
    /**
     * Secret key used for validating receipts
     */
    private static String iTunesConnectSecretKey;
    /**
     * Apple's endpoint for validating receipts
     */
    private static String iTunesVerifyReceiptURL;

    // subscripion statuses
    /**
     * Flag for whether the app is currently under review
     */
    private static boolean iTunesIsUnderReview;
    /**
     * Used to provide context to connect to other services (e.g., MessagingService)
     * via a proxy class.
     */
    private static UriInfo uriInfo;

    /** get the iTunes Connect Secret Key & endpoint url from system params
     * (will only run once on class load) */
    static
    {

        // set to default values
        iTunesConnectSecretKey = "40d121f0343c4085bc4fdd75df6141e7";
        iTunesIsUnderReview = false;
        if (ServiceBase.isProductionServer() && !iTunesIsUnderReview)
        {
            iTunesVerifyReceiptURL = "https://buy.itunes.apple.com/verifyReceipt";
        }
        else
        {
            iTunesVerifyReceiptURL = "https://sandbox.itunes.apple.com/verifyReceipt";
        }

        ReadOnlyDbConnection sqlr = null;
        try
        {
            sqlr = new ReadOnlyDbConnection();

            SqlPreparedStatement statement = sqlr.prepareStatement(
                    "SELECT sys_param_value FROM ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "iTunesConnectSecretKey");
            ResultSet rs = statement.executeQuery();
            if (rs.next())
            {
                iTunesConnectSecretKey = rs.getString(1);
                CTLogger.debug("iTunesConnectSecretKey set from system parameters table");
            }
            else
            {
                CTLogger.debug("No iTunesConnectSecretKey value found in system parameters table, using default value");
            }

            statement = sqlr.prepareStatement(
                    "SELECT sys_param_value FROM ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "iTunesVerifyReceiptURL");
            rs = statement.executeQuery();
            if (rs.next())
            {
                iTunesVerifyReceiptURL = rs.getString(1);
                CTLogger.debug("iTunesVerifyReceiptURL set from system parameters table");
            }
            else
            {
                CTLogger.debug("No iTunesVerifyReceiptURL value found in system parameters table, using default value");
            }

            statement = sqlr.prepareStatement(
                    "SELECT sys_param_value FROM ct_system_param.system_parameters WHERE sys_param_key = ?");
            statement.setString(1, "iTunesIsUnderReview");
            rs = statement.executeQuery();
            if (rs.next())
            {
                String theValue = rs.getString(1);
                iTunesIsUnderReview = MathUtil.tryParseBoolean(theValue);
                CTLogger.debug("iTunesIsUnderReview set from system parameters table");
            }
            else
            {
                CTLogger.debug("No iTunesIsUnderReview value found in system parameters table, using default value");
            }
        }
        catch (Exception e)
        {
            CTLogger.error("Error getting IOS IAP keys from system parameters table, setting to default values", e);
        }
        finally
        {
            SQLUtil.closeQuietly(sqlr);
        }
    }

    // db fields

    /**
     * The user object for this account
     */
    private CTUser                user;
    /**
     * The customer object for this user. (could be null)
     */
    private Customer              customer;
    /**
     * Flag for the ios subscription sub-status
     */
    private String                mIOSIAPStatus;
    /**
     * The main transaction object. (could be null if the user has never paid)
     */
    private IOSProductTransaction mainTransaction;

    public static boolean getiTunesIsUnderReview()
    {
        return iTunesIsUnderReview;
    }

    /**
     * preferred way to instantiate IOSInAppPurchaseHelper - once you do this its internal
     * fields will be correctly initialized to the latest info about the given
     * patient, so that subsequent calls to informative methods will have the
     * needed data for the correct user
     */
    public static IOSInAppPurchaseHelper create(ReadWriteDbConnection sql, UriInfo uriInfo, Integer userId)
    {
        IOSInAppPurchaseHelper.uriInfo = uriInfo;
        return create(sql, userId);
    }

    public static IOSInAppPurchaseHelper create(ReadWriteDbConnection sql, Integer userId)
    {
        IOSInAppPurchaseHelper helper = new IOSInAppPurchaseHelper();
        helper.setupForUser(sql, userId);
        return helper;
    }

    private void setupForUser(ReadWriteDbConnection sql, Integer userId)
    {
        initVars();
        readDataFields(sql, userId);
    }

    @Override
    protected void initVars()
    {
        super.initVars();

        this.user = null;
        this.customer = null;
        this.mainTransaction = null;
        this.mIOSIAPStatus = null;
    }

    @Override
    protected void readDataFields(ReadWriteDbConnection sql, Integer userId)
    {
        super.readDataFields(sql, userId);

        try
        {
            this.user = CTUser.getById(sql, userId);
            this.customer = Customer.getCustomerByUserId(sql, userId);
            if (this.customer == null)
            {
                CTLogger.info("Could not load iOS IAP data for user id " + userId + " because of missing customer entry");
                return;
            }

            this.mainTransaction = IOSProductTransaction.getByCustomerId(sql, this.customer.id);

            SqlPreparedStatement statement = sql.prepareStatement("SELECT " + KEY_IOS_IAP_STATUS + " FROM ct_customer.customers WHERE id = ?");
            statement.setInt(1, this.customer.id);

            ResultSet rs = statement.executeQuery();
            rs.next();
            this.mIOSIAPStatus = rs.getString(1);
        }
        catch (Exception e)
        {
            CTLogger.error("Error loading iOS IAP data", e);
        }
    }

    @Override
    public boolean isTrialEnded()
    {
        return this.mainTransaction == null;
    }

    @Override
    public boolean isRecurring(DbConnection sql)
    {
        if (this.mainTransaction != null && this.mainTransaction.productIdentifier != null)
        {
            try
            {
                return this.mainTransaction.getProduct(sql).autoRenewing;
            }
            catch (Exception e)
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public Timestamp getExpirationTimestamp()
    {
        if (this.mainTransaction != null)
        {
            return this.mainTransaction.expirationTimestamp;
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isTrialing()
    {
        // There is no trialing enabled through IOS In App purchasing
        return false;
    }

    @Override
    public boolean getInternalSubStatusFlag()
    {
        return isActiveIOSIAPStatus();
    }

    private boolean isActiveIOSIAPStatus()
    {
        return isActiveIOSIAPStatusString(this.mIOSIAPStatus);
    }

    public static boolean isActiveIOSIAPStatusString(String status)
    {
        return (null != status) && ((status.compareTo(IOS_IAP_SUB_STATUS_ACTIVE) == 0) || (status.compareTo(IOS_IAP_SUB_STATUS_PAST_DUE) == 0));
    }

    /**
     * Iterates through the in_app field of the receipt and picks the one with the latest expiration_date
     *
     * @param sql
     * @param receipt
     * @return
     * @throws Exception
     */
    private static InAppPurchaseReceipt getInAppPurchaseWithLatestExpirationTimestamp(
            ReadWriteDbConnection sql, Receipt receipt) throws Exception
    {

        Timestamp expirationTimestamp = null;
        InAppPurchaseReceipt latestPR = null;

        for (InAppPurchaseReceipt pr : receipt.in_app)
        {
            // Only consider this purchase if it hasn't been cancelled
            if (pr.cancellation_date != null)
            {
                continue;
            }

            // compare the expiration timestamp of the transaction to the current timestamp
            Timestamp pr_expirationTimestamp = getExpirationTimestampFromInAppPurchaseReceipt(sql, pr);

            if (expirationTimestamp == null ||
                    pr_expirationTimestamp.after(expirationTimestamp))
            {
                expirationTimestamp = pr_expirationTimestamp;
                latestPR = pr;
            }
        }

        return latestPR;
    }

    /**
     * @param sql
     * @param pr
     * @return
     * @throws Exception
     */
    private static Timestamp getExpirationTimestampFromInAppPurchaseReceipt(ReadWriteDbConnection sql, InAppPurchaseReceipt pr) throws Exception
    {

        // If this is an auto-renewable subscription, then the expiration_date will be set
        Timestamp pr_expirationTimestamp = null;
        if (pr.expires_date_ms != null)
        {
            pr_expirationTimestamp = new Timestamp(Long.parseLong(pr.expires_date_ms));
        }
        // otherwise we will look up the product using the product identifier
        // and add the product's duration to the purchase date
        else
        {
            String productIdentifier = pr.product_id;

            IOSInAppProduct product = IOSInAppProduct.getByProductIdentifier(sql, productIdentifier);
            if (product == null) return null;

            // Set the calendar's date to the purchase date on the receipt
            Calendar cal = Calendar.getInstance();
            Timestamp pr_purchaseTimestamp = getTransactionTimestampFromInAppPurchaseReceipt(pr);
            cal.setTime(pr_purchaseTimestamp);

            if (ServiceBase.isProductionServer() && !iTunesIsUnderReview)
            {
                if (product.duration == IOSInAppProduct.Duration.MONTH)
                {
                    cal.set(Calendar.MONTH, (cal.get(Calendar.MONTH) + 1));
                }
                else if (product.duration == IOSInAppProduct.Duration.YEAR)
                {
                    cal.set(Calendar.YEAR, (cal.get(Calendar.YEAR) + 1));
                }
            }
            else
            {
                // For development, we will use the same subscription durations that Apple
                // uses for auto-renewable subscriptions when testing
                // (see https://developer.apple.com/Library/ios/documentation/LanguagesUtilities/Conceptual/iTunesConnectInAppPurchase_Guide
                // /Chapters/TestingInAppPurchases.html)
                /**
                 * +----------+----------+
                 * |  Actual  |   Test   |
                 * +----------+----------+
                 * | 1 week   | 3 mins   |
                 * | 1 month  | 5 mins   |
                 * | 2 months | 10 mins  |
                 * | 3 months | 15 mins  |
                 * | 6 months | 30 mins  |
                 * | 1 year   | 1 hour   |
                 * +----------+----------+
                 *
                 */

                if (product.duration == IOSInAppProduct.Duration.MONTH)
                {
                    cal.set(Calendar.MINUTE, (cal.get(Calendar.MINUTE) + 5));
                }
                else if (product.duration == IOSInAppProduct.Duration.YEAR)
                {
                    cal.set(Calendar.HOUR, (cal.get(Calendar.HOUR) + 1));
                }
            }

            java.util.Date pr_expirationDate = cal.getTime();
            pr_expirationTimestamp = new Timestamp(pr_expirationDate.getTime());
        }

        return pr_expirationTimestamp;
    }

    /**
     * For auto-renewing subscriptions, returns the receipt's original_transaction_id
     * Otherwise it returns the receipt's transaction_id
     *
     * @param pr
     * @return
     */
    private static String getTransactionIdentifierFromInAppPurchaseReceipt(InAppPurchaseReceipt pr)
    {
        if (pr.original_transaction_id != null) return pr.original_transaction_id;
        else return pr.transaction_id;
    }

    private static Timestamp getTransactionTimestampFromInAppPurchaseReceipt(InAppPurchaseReceipt pr)
    {
        return new Timestamp(Long.parseLong(pr.purchase_date_ms));
    }

    /**
     * This method checks to see if a transaction is already in use by another customer
     * <p/>
     * We need to do this for auto-renewable subscriptions because the same Apple ID cannot subscribe twice to a single type of auto-renewable
     * subscription
     * (will only get charged once, the second time around they will receive an error alert from Apple)
     * <p/>
     * However, non-renewable subscriptions do not have this problem because the same Apple ID can purchase the same type of non-renewable
     * subscription infinite times
     *
     * @param sql
     * @param transactionIdentifier
     * @param productIdentifier
     * @return
     */
    private static boolean isTransactionAlreadyInUseByAnotherCustomer(
            ReadWriteDbConnection sql,
            String transactionIdentifier,
            String productIdentifier, int
                    customerId)
    {
        try
        {
            IOSInAppProduct product = IOSInAppProduct.getByProductIdentifier(sql, productIdentifier);
            if (product == null) return false;

            if (!product.autoRenewing) return false;

            SqlPreparedStatement statement = sql.prepareStatement(
                    "SELECT * FROM ct_customer.ios_product_transactions WHERE transaction_identifier = ? AND customer_id != ?");

            statement.setString(1, transactionIdentifier);
            statement.setInt(2, customerId);

            ResultSet rs = statement.executeQuery();
            return rs.next();
        }
        catch (SQLException e)
        {
            CTLogger.error(e);

            // If there is a problem here someone needs to take action ASAP
            sendSupportEmailForException(e);

            return false;
        }
    }

    public static boolean processTransactionPublic(ReadWriteDbConnection sql,
                                                   IOSProductTransaction newTransaction,
                                                   boolean isRestore)
            throws ServiceBase.ClientDisplayMessageException
    {

        boolean refreshSuccess = false;

        // Make sure that certain fields of this transaction are set from the client
        assert (newTransaction.productIdentifier != null);
        assert (newTransaction.transactionIdentifier != null);
        assert (newTransaction.transactionTimestamp != null);
        assert (newTransaction.receiptData != null);

        try
        {
            Receipt receipt = loadReceiptForTransaction(newTransaction);

            InAppPurchaseReceipt pr = getInAppPurchaseWithLatestExpirationTimestamp(sql, receipt);

            if (pr != null)
            {
                SqlPreparedStatement statement = sql.prepareStatement("SELECT c.user_id FROM ct_customer.ios_product_transactions ios "
                        + "INNER JOIN ct_customer.customers c ON ios.customer_id = c.id WHERE ios.transaction_identifier = ?");
                statement.setString(1, getTransactionIdentifierFromInAppPurchaseReceipt(pr));
                ResultSet rs = statement.executeQuery();
                if (rs.next())
                {
                    int ctUserId = Integer.parseInt(rs.getString(1));
                    IOSInAppPurchaseHelper helper = IOSInAppPurchaseHelper.create(sql, ctUserId);

                    // We also need to make sure that this transaction is not re-used across multiple CT user accounts
                    if (isTransactionAlreadyInUseByAnotherCustomer(sql, newTransaction.transactionIdentifier, newTransaction.productIdentifier,
                            helper.customer.id))
                    {
                        sendSupportEmailForFailedTransaction("iOS In-App Purchase failed because client tried to use it for multiple user accounts",
                                newTransaction, isRestore);

                        throw new ServiceBase.ClientDisplayMessageException("This Apple ID has already been used by another Constant Therapy account",
                                "Please try again with a different Apple account");
                    }

                    helper.refreshStatusWithReceipt(sql, receipt);
                    refreshSuccess = true;
                }
            }
        }
        catch (ServiceBase.ClientDisplayMessageException e)
        {
            throw e; // bubble up this exception
        }
        catch (Exception e)
        {
            CTLogger.error(e);

            // If there is a problem here someone needs to take action ASAP
            sendSupportEmailForFailedTransaction("iOS In-App Purchase could not finish renewing because the user was logged out", newTransaction,
                    isRestore);
        }

        return refreshSuccess;
    }

    /**
     * Uses the receiptData to load the entire receipt by connecting to Apple's server
     * and calling the verifyReceipt endpoint
     *
     * @return the receipt
     * @throws Exception
     */
    private static Receipt loadReceiptForTransaction(IOSProductTransaction trans) throws Exception
    {
        // update receiptJson
        trans.receiptJson = getReceiptForTransactionFromiTunes(trans.receiptData);

        return getReceiptFromFullJson(trans.receiptJson);
    }

    private static Receipt getReceiptFromFullJson(String receiptJson)
    {
        ReceiptWrapper wrapper = GsonHelper.getGson().fromJson(receiptJson, ReceiptWrapper.class);

        Receipt receipt = null;

        if (wrapper != null)
        {

            switch (wrapper.status)
            {
                case 0:
                    receipt = wrapper.receipt;
                    if (receipt != null)
                    {
                        int receiptLength = (receipt.in_app != null) ? receipt.in_app.size() : 0;
                        int latestReceiptLength = wrapper.latest_receipt_info != null ? wrapper.latest_receipt_info.size() : 0;
                        if (latestReceiptLength > receiptLength)
                        {
                            receipt.in_app = wrapper.latest_receipt_info;
                        }
                    }
                    break;

                case 21000:
                    CTLogger.error("The App Store could not read the JSON object you provided.");
                    break;
                case 21002:
                    CTLogger.error("The data in the receipt-data property was malformed or missing.");
                    break;
                case 21003:
                    CTLogger.error("The receipt could not be authenticated.");
                    break;
                case 21004:
                    CTLogger.error("The shared secret you provided does not match the shared secret on file for your account.");
                    break;
                case 21005:
                    CTLogger.error("The receipt server is not currently available.");
                    break;
                case 21006:
                    CTLogger.error("This receipt is valid but the subscription has expired. When this status code is returned to your server, " +
                            "the receipt data is also decoded and returned as part of the response.");
                    break;
                case 21007:
                    CTLogger.error("This receipt is from the test environment, but it was sent to the production environment for verification. " +
                            "Send it to the test environment instead.");
                    break;
                case 21008:
                    CTLogger.error("This receipt is from the production environment, but it was sent to the test environment for verification. " +
                            "Send it to the production environment instead.");
                    break;
                default:
                    break;
            }
        }

        return receipt;
    }

    private static void sendSupportEmail(String recipients, String subject, String body)
    {
        MessagingServiceProxy msg = new MessagingServiceProxy(uriInfo);
        MessagingServiceProxyArgs args = new MessagingServiceProxyArgs();
        args.recipients = recipients;
        args.subject = subject;
        args.body = body;
        msg.sendMessageUsingBody(args);
    }

    private static void sendSupportNotificationEmail(String subject, String body)
    {
        String recipients = "support@constanttherapy.com";
        sendSupportEmail(recipients, subject, body);
    }

    private static void sendSupportEmailForException(Exception e)
    {
        String recipients = SubscriptionHelper.developerEmailRecipients;
        String subject = "Error when processing iOS In App Purchase";
        String body = "Please report this problem to a developer to fix ASAP<br/><br/>";
        body += ExceptionUtils.getStackTrace(e);
        sendSupportEmail(recipients, subject, body);
    }

    private static void sendSupportEmailForFailedTransaction(String subject, IOSProductTransaction transaction, boolean isRestore)
    {
        MessagingServiceProxy msg = new MessagingServiceProxy(ServiceBase.getUriInfo());
        String recipients = SubscriptionHelper.developerEmailRecipients;
        String body = String.format("Is Restore: %s<br/><br/>Purchase Info:<br/><br/>%s", isRestore, GsonHelper.toJson(transaction));
        msg.sendMessageUsingBody(recipients, subject, body);
    }

    /**
     * Updates receipts for each ios_product_transaction that we have in the database.  This does NOT refresh
     * the receipt_json data by getting the latest receipts for the subscriber from iTunes.
     *
     * @param sql
     * @throws SQLException
     */
    public static void updateAllReceipts(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.infoStart("IOSInAppPurchaseHelper::updateAllReceipts()");
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String q = "SELECT customer_id, receipt_json FROM ct_customer.ios_product_transactions;";
            statement = sql.prepareStatement(q);

            rs = statement.executeQuery();

            while (rs.next())
            {
                int customerId = rs.getInt("customer_id");
                String json = rs.getString("receipt_json");

                updateReceiptsForCustomer(sql, customerId, json);
            }
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
            CTLogger.unindent();
        }
    }

    private static void updateReceiptsForCustomer(ReadWriteDbConnection sql, int customerId, String receiptJson) throws SQLException
    {
        long startTime = System.nanoTime();
        CTLogger.infoStart("IOSInAppPurchaseHelper::updateReceiptsForCustomer() - START - customerId=" + customerId);
        SqlPreparedStatement statement = null;

        try
        {
            ReceiptWrapper wrapper = GsonHelper.getGson().fromJson(receiptJson, ReceiptWrapper.class);

            if (wrapper.latest_receipt_info == null)
            {
                CTLogger.warn("Invalid receiptJson: " + receiptJson);
                return;
            }

            for (InAppPurchaseReceipt receipt : wrapper.latest_receipt_info)
            {
                String q = "INSERT IGNORE INTO ct_customer.ios_receipts " +
                        "(`transaction_id`, " +
                        "`customer_id`, " +
                        "`product_id`, " +
                        "`original_transaction_id`, " +
                        "`timestamp`, " +
                        "`expiration_timestamp`, " +
                        "`cancellation_timestamp`) " +
                        "VALUES (?,?,?,?,?,?,?) ";

                statement = sql.prepareStatement(q);
                statement.setString(1, receipt.transaction_id);
                statement.setInt(2, customerId);
                statement.setString(3, receipt.product_id);
                statement.setString(4, receipt.original_transaction_id);
                statement.setString(5, receipt.purchase_date);
                statement.setString(6, receipt.expires_date);
                statement.setString(7, receipt.cancellation_date);

                statement.execute();
            }
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            CTLogger.infoEnd(String.format("IOSInAppPurchaseHelper::updateReceiptsForCustomer() - END - executionTime=%.2fms",
                    (System.nanoTime() - startTime) / 1000000.0f));
        }
    }

    /**
     * Updates data from iTunes for each transaction we have recorded in our database.
     * This is used to keep the receipt data updated, and can be run from a cron job
     *
     * @param sql
     */
    public static void updateAllTransactions(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.infoStart("IOSInAppPurchaseHelper::updateAllTransactions()");

        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            // find all transactions for which the last receipt expired earlier than
            // current time (now)
            String q = "SELECT a.* FROM ct_customer.ios_product_transactions a " +
                    "JOIN " +
                    "(SELECT customer_id, original_transaction_id, MAX(expiration_timestamp) expires " +
                    "FROM ct_customer.ios_receipts " +
                    "GROUP BY customer_id " +
                    "HAVING expires < NOW()) b ON b.original_transaction_id = a.transaction_identifier";

            statement = sql.prepareStatement(q);

            rs = statement.executeQuery();

            IOSProductTransaction txn = new IOSProductTransaction();
            while (rs.next())
            {
                txn.read(rs);
                txn.receiptJson = getReceiptForTransactionFromiTunes(txn.receiptData);
                txn.update(sql);

                updateReceiptsForCustomer(sql, txn.customerId, txn.receiptJson);
            }
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
            CTLogger.unindent();
        }
    }

    private static String getReceiptForTransactionFromiTunes(String receiptData) throws IOException
    {
        Map<String, String> postData = new HashMap<>();
        postData.put("receipt-data", receiptData);
        postData.put("password", iTunesConnectSecretKey); // secret key in iTunes connect

        String jsonData = GsonHelper.toJson(postData);

        URL url = new URL(iTunesVerifyReceiptURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", String.valueOf(jsonData.length()));
        OutputStream os = conn.getOutputStream();
        os.write(jsonData.getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null)
        {
            response.append(line);
            response.append('\n');
        }
        reader.close();

        return response.toString();
    }

    public IOSProductTransaction getMainTransaction()
    {
        return this.mainTransaction;
    }

    private void updateIOSInAppDataFields(ReadWriteDbConnection sql) throws SQLException
    {
        long startTime = System.nanoTime();
        CTLogger.infoStart("IOSInAppPurchaseHelper::updateIOSInAppDataFields() - START - customerId=" + this.customer.id);

        // Update the subscription status of the user/customer in a single transaction
        try
        {
            // transaction! we are setting multiple tables
            sql.setAutoCommit(false);

            // Check to see if this user already has an existing transaction saved in the database
            if (this.mainTransaction != null)
            {
                IOSProductTransaction existingTransactionInDB = IOSProductTransaction.getByCustomerId(sql, this.customer.id);
                if (existingTransactionInDB == null)
                {
                    // Create a new entry for this transaction
                    this.mainTransaction.create(sql);
                }
                else
                {
                    // Copy the new data to the old transaction entry
                    this.mainTransaction.id = existingTransactionInDB.id;
                    this.mainTransaction.update(sql);
                }

                // [mahendra, 10/21/15 1:46 PM]: update ios_receipts from the json
                updateReceiptsForCustomer(sql, this.customer.id, this.mainTransaction.receiptJson);
            }

            // Update the customer's ios_iap_status field
            SqlPreparedStatement statement = sql.prepareStatement("UPDATE ct_customer.customers SET " + KEY_IOS_IAP_STATUS + " = ? WHERE id = ?");

            if (this.mIOSIAPStatus != null)
            {
                statement.setString(1, this.mIOSIAPStatus);
            }
            else
            {
                statement.setNull(1, Types.VARCHAR);
            }

            statement.setInt(2, this.customer.id);

            statement.executeUpdate();

            // Also refresh the main subscription status flag
            super.refreshUserSubscriptionStatusFlag(sql);

            // done with transaction
            sql.commit();
            sql.setAutoCommit(true);
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
                CTLogger.error(e1);
            }
            CTLogger.error(e);

            // If there is a problem here someone needs to take action ASAP
            sendSupportEmailForExceptionAndCurrentUser(e);

            throw e;
        }
        finally
        {
            CTLogger.infoEnd(String.format("IOSInAppPurchaseHelper::updateIOSInAppDataFields() - END executionTime=%.2fms",
                    (System.nanoTime() - startTime) / 1000000.0f));
        }
    }

    /**
     * Attempts to load the receipt and then refreshes the customer's subscription status
     *
     * @param sql
     * @return whether the status could be successfully refreshed (not whether the subscription is currently active)
     */
    public void refreshStatus(ReadWriteDbConnection sql)
    {
        long startTime = System.nanoTime();
        CTLogger.infoStart("IOSInAppPurchaseHelper::refreshStatus() - START - customerId=" + this.customer.id);

        try
        {
            // If the user has never used in-app purchasing then the mainTransaction will be null
            if (this.mainTransaction == null)
            {
                // If for some reason the ios status is active, then reset it
                if (isActiveIOSIAPStatus())
                {
                    this.mIOSIAPStatus = null;
                    try
                    {
                        updateIOSInAppDataFields(sql);
                    }
                    catch (SQLException e)
                    {
                        // If there is a problem here someone needs to take action ASAP
                        sendSupportEmailForExceptionAndCurrentUser(e);
                        return;
                    }
                }

                // everything is still fine though
                return;
            }

            Receipt receipt;

            if (this.mainTransaction.getReloadReceipt())
            {
                // load the receipt for this transaction from Apple's server
                receipt = loadReceiptForTransaction(this.mainTransaction);
            }
            else
            {
                // use the local copy of the receipt
                receipt = getReceiptFromFullJson(this.mainTransaction.receiptJson);
            }

            if (receipt != null)
                refreshStatusWithReceipt(sql, receipt);

        }
        catch (Exception e)
        {
            // If there is a problem here someone needs to take action ASAP
            sendSupportEmailForExceptionAndCurrentUser(e);
        }
        finally
        {
            CTLogger.infoEnd(String.format("IOSInAppPurchaseHelper::refreshStatus() - END - executionTime=%.2fms",
                    (System.nanoTime() - startTime) / 1000000.0f));
        }
    }

    /**
     * Refreshes the customer's subscription status using the currently set mainTransaction object
     *
     * @param sql
     * @param receipt
     * @throws Exception
     */
    private void refreshStatusWithReceipt(ReadWriteDbConnection sql, Receipt receipt) throws Exception
    {
        long startTime = System.nanoTime();
        CTLogger.infoStart("IOSInAppPurchaseHelper::refreshStatusWithReceipt() - START");

        try
        {
            if (this.mainTransaction == null || receipt == null)
            {
                throw new java.lang.IllegalStateException("Error loading receipt for this transaction");
            }

            // Need previous status to know if there has been a state change from
            // active to expired, so we can alert the support team.
            String previousIOSIAPStatus = this.mIOSIAPStatus;

        /*
        CTLogger.info(String.format("IOSInAppPurchaseHelper::refreshStatus() - customerId=%d, receipt_json=%s",
                this.customer.id, this.mainTransaction.receiptJson));
        */

            // Get the purchase receipt with the latest expiration date
            InAppPurchaseReceipt pr = getInAppPurchaseWithLatestExpirationTimestamp(sql, receipt);
            if (pr != null)
            {
                // Get the latest expiration date from the receipt
                this.mainTransaction.expirationTimestamp = getExpirationTimestampFromInAppPurchaseReceipt(sql, pr);
                this.mainTransaction.productIdentifier = pr.product_id;
                this.mainTransaction.transactionIdentifier = getTransactionIdentifierFromInAppPurchaseReceipt(pr);
                this.mainTransaction.transactionTimestamp = getTransactionTimestampFromInAppPurchaseReceipt(pr);

                // If we haven't reached the expiration timestamp yet, then the subscription is currently active
                if (this.mainTransaction.expirationTimestamp != null &&
                        TimeUtil.timeNow().before(this.mainTransaction.expirationTimestamp))
                {
                    // active subscription
                    this.mIOSIAPStatus = IOS_IAP_SUB_STATUS_ACTIVE;

                    resetPastDueAccountAttempts(sql);
                }
                else
                {
                    // inactive subscription
                    this.mIOSIAPStatus = IOS_IAP_SUB_STATUS_EXPIRED;
                }
            }
            else
            {
                this.mIOSIAPStatus = IOS_IAP_SUB_STATUS_EXPIRED;
            }

            // [ehsan] REVIEW: we no longer need to have a past due status for iOS In App Purchasing
            /*
            // If we are going from an active to expired ios status, give them an extra number of attempts
            if (isActiveIOSIAPStatusString(previousIOSIAPStatus) && !isActiveIOSIAPStatusString(this.mIOSIAPStatus))
            {
                if (isWithinGracePeriodForPastDueAccountAttempts(sql))
                {
                    this.mIOSIAPStatus = IOS_IAP_SUB_STATUS_PAST_DUE;
                }
            }
            */

            // If the sub status changed, then log this as a user event
            if (!Objects.equals(this.mIOSIAPStatus, previousIOSIAPStatus))
            {
                UserEventLogger.logEvent(sql, UserEventType.UserSubscriptionSubStatusChanged, "ios_iap_status",
                        this.getUserId(), String.format("%s|%s", previousIOSIAPStatus, this.mIOSIAPStatus), TimeUtil.timeNow(), null);
            }

            // update the ios status in the database
            updateIOSInAppDataFields(sql);

            String afterIOSIAPStatus = this.mIOSIAPStatus;

            // We want to alert the support team if there has been a cancellation
            // or expiration, and we also want to log this to the user events
            // table.
            if (isActiveIOSIAPStatusString(previousIOSIAPStatus) &&
                    !isActiveIOSIAPStatusString(afterIOSIAPStatus))
            {
                // Send email
                String recipients = SubscriptionHelper.informationalEmailRecipients;

                String subject = "iOS Cancellation";

                String body = "User " + this.user.getId() + " with email " +
                        this.user.getEmail() + " cancelled their iOS in app subscription.";

                sendSupportEmail(recipients, subject, body);

                // Log user event - with cancellation date
                UserEventLogger.EventBuilder
                        .create(UserEventType.SubscriptionCanceled)
                        .userId(getUserId())
                        .eventData("expires_date_ms:" + (pr != null ? pr.expires_date_ms : "unknown"))
                        .log(sql);
            }
        }
        finally
        {
            CTLogger.infoEnd(String.format("IOSInAppPurchaseHelper::refreshStatusWithReceipt() - END - executionTime=%.2fms",
                    (System.nanoTime() - startTime) / 1000000.0f));
        }
    }

    /**
     * Processes a transaction sent from an iOS device
     *
     * @param sql
     * @param newTransaction
     * @param isRestore
     * @return boolean indicating whether this transaction was successfully processed (and thus the iPad does not need to resend it)
     */
    public boolean processTransactionForUser(ReadWriteDbConnection sql, IOSProductTransaction newTransaction, boolean isRestore)
            throws ServiceBase.ClientDisplayMessageException, SQLException
    {
        newTransaction.customerId = this.customer.id;

        // Make sure that certain fields of this transaction are set from the client
        assert (newTransaction.productIdentifier != null);
        assert (newTransaction.transactionIdentifier != null);
        assert (newTransaction.transactionTimestamp != null);
        assert (newTransaction.receiptData != null);

        // We also need to make sure that this transaction is not re-used across multiple CT user accounts
        if (isTransactionAlreadyInUseByAnotherCustomer(sql, newTransaction.transactionIdentifier, newTransaction.productIdentifier,
                this.customer.id))
        {
            sendSupportEmailForFailedTransaction("iOS In-App Purchase failed because client tried to use it for multiple user accounts",
                    newTransaction, isRestore);

            throw new ServiceBase.ClientDisplayMessageException("This Apple ID has already been used by another Constant Therapy account",
                    "Please try again with a different Apple account");
        }

        boolean isFirstTime = (this.mainTransaction == null);

        this.mainTransaction = newTransaction;

        boolean refreshSuccess = false;
        try
        {
            // First load the receipt for this transaction
            Receipt receipt = loadReceiptForTransaction(this.mainTransaction);
            refreshStatusWithReceipt(sql, receipt);
            refreshSuccess = true;
        }
        catch (ServiceBase.ClientDisplayMessageException e)
        {
            throw e; // bubble up this exception
        }
        catch (Exception e)
        {
            CTLogger.error(e);

            // If there is a problem here someone needs to take action ASAP
            sendSupportEmailForExceptionAndCurrentUser(e);

            refreshSuccess = false;
        }

        if (refreshSuccess)
        {
            if (isSubscribed() && isActiveIOSIAPStatusString(this.mIOSIAPStatus))
            {

                CTLogger.info("IOSInAppPurchaseHelper::processTransactionForUser() – Successfully finished ; {isFirstTime=" + isFirstTime + "}");

                // Send an email to support
                sendSupportNotificationEmail(
                        "iOS In-App " + (isFirstTime ? "Purchase" : "Renewal") + " by user [" + this.user.getUsername() + "]",
                        "User Id: " + this.user.getId() + "<br/>" +
                                "Username: " + this.user.getUsername() + "<br/>" +
                                "Product Identifier: " + this.mainTransaction.productIdentifier + "<br/>" +
                                "Initial Purchase: " + (isFirstTime ? "Yes" : "No"));

                try
                {
                    // If they are subscribed through other means (Stripe), cancel their subscription:
                    StripeHelper stripey = StripeHelper.create(sql, this.user.getId());
                    if (stripey.isActive())
                    {
                        stripey.handleCancel(sql, "[Automatic Stripe Cancellation after iOS In App Purchase]");

                        // REVIEW: we don't need to call refresh because stripey.handleCancel will call that itself
                        //super.refreshUserSubscriptionStatusFlag(sql);
                    }
                }
                catch (Exception e)
                {
                    // If there is a problem here someone needs to take action ASAP
                    sendSupportEmailForExceptionAndCurrentUser(e);

                    CTLogger.error(e);
                }
            }
        }

        return refreshSuccess;
    }

    private void sendSupportEmailForExceptionAndCurrentUser(Exception e)
    {
        long startTime = System.nanoTime();
        CTLogger.infoStart("IOSInAppPurchaseHelper::sendSupportEmailForExceptionAndCurrentUser() - START");

        try
        {
            String recipients = SubscriptionHelper.developerEmailRecipients;
            String subject = "Error when processing iOS In App Purchase";
            String body = "Please report this problem to a developer to fix ASAP<br/><br/>";
            body += String.format("(uid = %d, username = %s)<br/><br/>", this.user.getId(), this.user.getUsername());
            body += ExceptionUtils.getStackTrace(e);
            sendSupportEmail(recipients, subject, body);
        }
        finally
        {
            CTLogger.infoEnd(String.format("IOSInAppPurchaseHelper::sendSupportEmailForExceptionAndCurrentUser() - END - executionTime=%.2fms",
                    (System.nanoTime() - startTime) / 1000000.0f));
        }
    }

    public void recordAccountAttempt(ReadWriteDbConnection sql) throws SQLException
    {

        if (IOS_IAP_SUB_STATUS_PAST_DUE.equals(getIOSIAPStatus()) ||
                IOS_IAP_SUB_STATUS_EXPIRED.equals(getIOSIAPStatus()))
        {

            int pastDueAccountAttempts = getPastDueAccountAttempts(sql);
            UserPreferences.setPreferenceForUser(sql, getUserId(), consecutivePastDueAccountAttemptsCountPrefKey,
                    Integer.toString(pastDueAccountAttempts + 1));
        }
    }

    /**
     * Returns the current ios in-app purchase status (active/expired)
     *
     * @return
     */
    public String getIOSIAPStatus()
    {
        return this.mIOSIAPStatus;
    }

    private int getPastDueAccountAttempts(DbConnection sql) throws SQLException
    {
        String pastDueAccountAttempts_String = UserPreferences.getPreferenceForUser(sql, getUserId(), consecutivePastDueAccountAttemptsCountPrefKey);
        return MathUtil.tryParseInt(pastDueAccountAttempts_String, 0);
    }

    @Deprecated
    private boolean isWithinGracePeriodForPastDueAccountAttempts(DbConnection sql) throws SQLException
    {
        return getPastDueAccountAttempts(sql) <= gracePeriodMaxNumberOfPastDueAccountAttempts;
    }

    @Deprecated
    private void resetPastDueAccountAttempts(ReadWriteDbConnection sql) throws SQLException
    {
        UserPreferences.setPreferenceForUser(sql, getUserId(), consecutivePastDueAccountAttemptsCountPrefKey, Integer.toString(0));
    }

    public static void updateDbFromStatusUpdateNotification(ReadWriteDbConnection sql, String httpPostBody, UriInfo theUriInfo) throws SQLException
    {
        if (null == sql) throw new IllegalArgumentException();

        //CTLogger.infoStart("IOSInAppPurchaseHelper::updateDbFromStatusUpdateNotification() - httpPostBody=" + httpPostBody.substring(0, Math.min(150, httpPostBody.length())));
        CTLogger.infoStart("IOSInAppPurchaseHelper::updateDbFromStatusUpdateNotification() - httpPostBody=" + httpPostBody);

        try
        {
            StatusUpdateNotification notification = GsonHelper.fromJson(httpPostBody, StatusUpdateNotification.class);

            boolean canContinue = false;
            if (ServiceBase.isProductionServer() && notification.environment == StatusUpdateNotification.Environment.PROD) {
                canContinue = true;
            } else if (ServiceBase.isDevelopmentServer() && notification.environment == StatusUpdateNotification.Environment.SANDBOX) {
                canContinue = true;
            }

            if (canContinue == false) return;

            // Look up which customer transaction this corresponds to
            String originalTransactionId = notification.original_transaction_id;

            IOSProductTransaction transaction = IOSProductTransaction.getByTransactionIdentifier(sql, originalTransactionId);
            if (transaction != null)
            {
                int customerId = transaction.customerId;
                Customer customer = new Customer();
                customer.read(sql, customerId);
                IOSInAppPurchaseHelper ios = IOSInAppPurchaseHelper.create(sql, customer.userId);
                // we won't rely on the data sent from the webhook, we will simply reload the receipt for that customer
                ios.refreshStatus(sql);
            }
        }
        catch (SQLException e)
        {
            sendSupportEmailForException(e);
            CTLogger.error(e);
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    static void storeEventData(ReadWriteDbConnection sql, String httpPostBody) throws SQLException
    {
        // TODO:
    }
}
