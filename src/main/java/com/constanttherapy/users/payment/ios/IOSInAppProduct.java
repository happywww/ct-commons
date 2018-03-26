package com.constanttherapy.users.payment.ios;

import com.constanttherapy.CTCrud;
import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.util.CTLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ehsan
 */
public class IOSInAppProduct implements CTCrud
{
    public enum Duration
    {
        MONTH,
        YEAR;

        public String toString()
        {
            switch (this)
            {
                case MONTH:
                    return "month";
                case YEAR:
                    return "year";
                default:
                    return null;
            }
        }
    }

    // instance variables
    int      id;
    String productIdentifier;
    Duration duration;
    String price;
    boolean  autoRenewing;
    String displayName;
    String description;

    public int getId()
    {
        return this.id;
    }

    public String getProductIdentifier()
    {
        return this.productIdentifier;
    }

    public Duration getDuration()
    {
        return this.duration;
    }

    public String getPrice()
    {
        return this.price;
    }

    public boolean isAutoRenewing()
    {
        return this.autoRenewing;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public String getDescription()
    {
        return this.description;
    }

    /**
     * Returns all in app products from the database
     *
     * @param sql
     * @return
     * @throws SQLException
     */
    public static List<IOSInAppProduct> allInAppProducts(DbConnection sql) throws SQLException
    {

        CTLogger.infoStart("IOSInAppProduct::allInAppProducts()");

        SqlPreparedStatement statement = sql.prepareStatement("select * from ct_customer.ios_in_app_products where visible = 1");

        ResultSet rs = statement.executeQuery();

        List<IOSInAppProduct> products = new ArrayList<IOSInAppProduct>();

        while (rs.next())
        {
            IOSInAppProduct p = new IOSInAppProduct();
            p.read(rs);
            products.add(p);
        }

        return products;
    }

    static IOSInAppProduct getByProductIdentifier(ReadWriteDbConnection sql, String productIdentifier)
    {
        try
        {
            IOSInAppProduct p = new IOSInAppProduct();
            p.read(sql, productIdentifier);
            return p;
        }
        catch (SQLException e)
        {
            return null;
        }
    }

    @Override
    public void create(ReadWriteDbConnection sql) throws SQLException
    {
        throw new UnsupportedOperationException("IOSInAppProduct::update() is not yet implemented");
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
        SqlPreparedStatement statement = sql.prepareStatement("select * from ct_customer.ios_in_app_products where id = ?");
        statement.setInt(1, id);
        ResultSet rs = statement.executeQuery();
        rs.next();
        read(rs);
    }

    public void read(DbConnection sql, String productIdentifier) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("select * from ct_customer.ios_in_app_products where product_identifier = ?");
        statement.setString(1, productIdentifier);
        ResultSet rs = statement.executeQuery();
        rs.next();
        read(rs);
    }

    @Override
    public void read(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("id");
        this.productIdentifier = rs.getString("product_identifier");
        this.duration = Duration.valueOf(rs.getString("duration"));
        this.price = rs.getString("price");
        this.autoRenewing = rs.getBoolean("auto_renewing");
        this.displayName = rs.getString("display_name");
        this.description = rs.getString("description");
    }

    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        throw new UnsupportedOperationException("IOSInAppProduct::update() is not yet implemented");
    }

    @Override
    public void delete(ReadWriteDbConnection sql) throws SQLException
    {
        throw new UnsupportedOperationException("IOSInAppProduct::delete() is not yet implemented");
    }
}
