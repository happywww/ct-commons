package com.constanttherapy.users.payment.ios;

import com.constanttherapy.CTCrud;
import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.TimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * @author ehsan
 */
public class IOSProductTransaction implements CTCrud
{
    /**
     * The index of this transaction in the db
     */
    public  int       id;
    /**
     * The customer this transaction is associated with
     */
    public  int       customerId;
    /**
     * The identifier for the apple in-app product this transaction is associated with
     * <p/>
     * (this should be sent from the client)
     */
    public String productIdentifier;
    /**
     * Data that is used to retrieve the the receipt json from Apple's servers
     * <p/>
     * (this should be sent from the client)
     */
    public String receiptData; // base64 encoded data
    /**
     * Apple's identifier for this transaction
     * <p/>
     * Each renewal of a subscription will have a different value
     * <p/>
     * (this should be sent from the client)
     */
    public String transactionIdentifier;
    /**
     * The timestamp of this transaction
     * <p/>
     * (this should be sent from the client)
     */
    public Timestamp transactionTimestamp;
    /**
     * The json of the receipt retrieved from Apple's servers
     */
    public String receiptJson;
    /**
     * The expiration of this subscription/product
     * <p/>
     * (this is calculated from the receipt)
     */
    public Timestamp expirationTimestamp;
    /**
     * A flag indicating whether or not we should reload the receipt
     * <p/>
     * REVIEW: this was made private so that it could not be changed anywhere else in the code
     * if you really need to change the value (for testing purposes), go directly to database
     */
    private boolean   reloadReceipt;

    public boolean getReloadReceipt()
    {
        return this.reloadReceipt;
    }

    // This is an encrypted version of the username that we send from the client to Apple's servers
    // to help prevent fraudulent transactions (currently not used on the server)
    //public String applicationUsername;

    @Override
    public void create(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.infoStart("IOSProductTransaction::create()");

        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("INSERT INTO ct_customer.ios_product_transactions " +
                    "(customer_id, product_identifier, receipt_data, receipt_json, transaction_timestamp," +
                    "expiration_timestamp, row_created_timestamp, transaction_identifier) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            statement.setInt(1, this.customerId);
            statement.setString(2, this.productIdentifier);
            statement.setString(3, this.receiptData);
            statement.setString(4, this.receiptJson);
            statement.setTimestamp(5, this.transactionTimestamp);
            statement.setTimestamp(6, this.expirationTimestamp);
            statement.setTimestamp(7, TimeUtil.timeNow());
            statement.setString(8, this.transactionIdentifier);

            boolean success = statement.executeUpdate() > 0;
            if (success)
            {
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next())
                {
                    this.id = rs.getInt(1);
                }
            }
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    public IOSInAppProduct getProduct(DbConnection sql) throws SQLException
    {
        IOSInAppProduct ios = new IOSInAppProduct();
        ios.read(sql, this.productIdentifier);
        return ios;
    }

    static IOSProductTransaction getByCustomerId(DbConnection sql, int customerId) throws SQLException
    {
        CTLogger.infoStart("IOSProductTransaction::getByCustomerId(customerId=" + customerId + ")");

        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM ct_customer.ios_product_transactions WHERE customer_id = ?");
            statement.setInt(1, customerId);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
            {
                IOSProductTransaction trans = new IOSProductTransaction();
                trans.read(rs);
                return trans;
            }
            else
            {
                return null;
            }
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    static IOSProductTransaction getByTransactionIdentifier(DbConnection sql, String transactionIdentifier) throws SQLException
    {
        CTLogger.infoStart("IOSProductTransaction::getByTransactionIdentifier(transactionIdentifier=" + transactionIdentifier + ")");

        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM ct_customer.ios_product_transactions WHERE transaction_identifier = ?");
        statement.setString(1, transactionIdentifier);

        ResultSet rs = statement.executeQuery();
        if (rs.next())
        {
            IOSProductTransaction trans = new IOSProductTransaction();
            trans.read(rs);
            return trans;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void read(DbConnection sql) throws SQLException, IllegalArgumentException
    {
        if (this.id == 0)
            throw new IllegalArgumentException("No id specified");
        read(sql, this.id);
    }

    @Override
    public void read(DbConnection sql, Integer id) throws SQLException, IllegalArgumentException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM ct_customer.ios_product_transactions WHERE id = ?");
        statement.setInt(1, id);

        ResultSet rs = statement.executeQuery();
        rs.next();
        read(rs);
    }

    @Override
    public void read(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("id");
        this.customerId = rs.getInt("customer_id");
        this.productIdentifier = rs.getString("product_identifier");
        this.receiptData = rs.getString("receipt_data");
        this.receiptJson = rs.getString("receipt_json");
        this.transactionTimestamp = rs.getTimestamp("transaction_timestamp");
        this.expirationTimestamp = rs.getTimestamp("expiration_timestamp");
        this.transactionIdentifier = rs.getString("transaction_identifier");
        this.reloadReceipt = rs.getBoolean("reload_receipt");
    }

    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        assert (this.id > 0);

        CTLogger.info("IOSProductTransaction::update()");

        SqlPreparedStatement statement = sql.prepareStatement("UPDATE ct_customer.ios_product_transactions SET " +
                "customer_id = ?, product_identifier = ?, receipt_data = ?, receipt_json = ?, transaction_timestamp = ?," +
                "expiration_timestamp = ?, " +
                "transaction_identifier = ? WHERE id = ?");

        statement.setInt(1, this.customerId);
        statement.setString(2, this.productIdentifier);
        statement.setString(3, this.receiptData);
        statement.setString(4, this.receiptJson);
        statement.setTimestamp(5, this.transactionTimestamp);
        statement.setTimestamp(6, this.expirationTimestamp);
        statement.setString(7, this.transactionIdentifier);
        statement.setInt(8, this.id);

        statement.execute();
    }

    @Override
    public void delete(ReadWriteDbConnection sql) throws SQLException
    {
        throw new UnsupportedOperationException("IOSProductTransaction::delete() is not yet implemented");
    }
}
