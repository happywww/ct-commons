package com.constanttherapy.users;

import com.constanttherapy.CTCrud;
import com.constanttherapy.db.*;
import com.constanttherapy.util.*;

import java.sql.*;

public class Customer implements CTCrud
{
    /* ct_customer.customers table
        +-------------------------------+-------------------------------------------------+
		| Field                         | Type                                            |
		+-------------------------------+-------------------------------------------------+
		| id                            | int(11)                                         |
		| user_id                       | int(10) unsigned                                |
		| first_name                    | varchar(45)                                     |
		| last_name                     | varchar(45)                                     |
		| email                         | varchar(255)                                    |
		| location                      | varchar(255)                                    |
		| age_group                     | enum('<6','6-12','13-21','22-50','51-70','>70') |
		| condition_since               | enum('6m','1y','2y','5y','10y','>10y')          |
		| frequency_of_therapy          | varchar(100)                                    |
		| usertype                      | varchar(255)                                    |
		| role                          | varchar(255)                                    |
		| facility_name                 | varchar(255)                                    |
		| setting                       | varchar(255)                                    |
		| whatpromptedyou               | text                                            |
		| created_date                  | timestamp                                       |
		| lead_source                   | varchar(255)                                    |
		| lead_source_details           | text                                            |
		| comments                      | text                                            |
		| phone_number                  | varchar(1000)                                   |
		| first_payment_date            | timestamp                                       |
		| device_owner                  | varchar(255)                                    |
		| city                          | varchar(45)                                     |
		| state                         | varchar(45)                                     |
		| country                       | varchar(45)                                     |
		| gender                        | varchar(45)                                     |
		| education                     | varchar(45)                                     |
		| caregiver_other               | varchar(45)                                     |
		| caregiver_phone_number        | varchar(45)                                     |
		| caregiver_email               | varchar(45)                                     |
		| caregiver_first_name          | varchar(45)                                     |
		| caregiver_relationship        | varchar(45)                                     |
		| is_auto_maintain_subscription | tinyint(1)                                      |
		| stripe_id                     | varchar(25)                                     |
		| stripe_status                 | varchar(20)                                     |
		| stripe_expires                | timestamp                                       |
		| last_subscription_alert       | timestamp                                       |
		| signup_location               | varchar(45)                                     |
		| birth_year                    | int(11)                                         |
		| signup_landing_page           | varchar(100)                                    |
		| ios_iap_status                | varchar(20)                                     |
		| ip_address                    | varchar(45)                                     |
		| latitude                      | decimal(11,7)                                   |
		| longitude                     | decimal(11,7)                                   |
		+-------------------------------+-------------------------------------------------+
	 */
    public           int       id;
    public           Integer   userId; // References users table in constant_therapy db (can be null)
    public           String    firstName;
    public           String    lastName;
    public           String    email;
    public           String    location;
    public           String    ageGroup;
    public           String    conditionSince;
    public           String    frequencyOfTherapy;
    public           Object    userType;
    public           String    facilityName;
    public           String    whatPromptedYou;
    public           Timestamp createdDate;
    public           String    leadSource;
    public           String    phoneNumber;
    public           Timestamp firstPaymentDate;
    public           String    comments;
    public           String    username;
    public           String    disorders;
    public           String    deficits;
    public           Integer   birthYear;
    public           String    gender;
    public           String    education;
    public           String    caregiverEmail;
    public           String    role;
    public           String    setting;
    public           String    leadSourceDetails;
    public           String    signupLocation;
    public           String    signupLandingPage;
    public           String    ipAddress;
    public           String    city;
    public           String    state;
    public           String    country;
    // Transient metadata passed along for signup email.
    public transient String    clinicianPlatform;
    public transient String    clinicianVersion;

    @Override
    public void create(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.infoStart("Customer::create() - start");

        // There are two parts to creating a new customer in the DB. First an
        // entry is inserted into the customers table
        // with name, email address etc. Next, the disorders, deficits,
        // therapy types, relationships, notes, sources etc need to be broken
        // down first and then inserted into their respective tables with the
        // corresponding code number, which again needs to be looked up
        // from the corresponding table. For example, if we have disorders =TBI,
        // senility for a customer,
        // We need to look up the code for TBI from the disorders table (Say
        // TBI=1). Similarly, we lookup the code for
        // Senility from the table, which returns null, because it is not a
        // predefined disorder.
        // So In this case we add (cust_id, 1, null) and (cust_id, 999,
        // "Senility") to the customers_to_disorders table.

        boolean success = addCustomerToDB(sql);
        success &= addCustomersToDisordersToDB(sql);
        success &= addCustomersToDeficitsToDB(sql);
        success &= addCustomersToTherapiesToDB(sql);
        // success &= addCustomersToSourcesToDB(sql);
        // success &= addCustomersToFacilitiesToDB(sql);
        // success &= AddCustomersToRelationshipsToDB(sql);

        if (success)
        {
            CTLogger.infoEnd("Customer::create() - end - customerId=" + this.id);
        }
        else
        {
            CTLogger.infoEnd("Customer::create() - Failed.");
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    private boolean addCustomersToFacilitiesToDB(ReadWriteDbConnection sql) throws SQLException
    {
        if (this.facilityName == null || this.facilityName.isEmpty())
        {
            return true;
        }

        CTLogger.info("Customer::AddCustomersToFacilitiesToDB()");

        boolean success = true;

        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM ct_customer.facilities WHERE name = ?");
        statement.setString(1, this.facilityName);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
        {
            int facility_id = rs.getInt("id");
            SqlPreparedStatement update_facility_id = sql
                    .prepareStatement("UPDATE ct_customer.customers  SET facility_id=? WHERE user_id=?");
            update_facility_id.setInt(1, facility_id);
            update_facility_id.setInt(2, this.userId);
            success = (update_facility_id.executeUpdate() > 0);
        }
        else
        { // facility name lookup failed. Just update the customers table with facility_id=null.
            // A human will need to create an alias for these in the facilities table through a website
            SqlPreparedStatement update_facility_id = sql
                    .prepareStatement("UPDATE ct_customer.customers  SET facility_id=? WHERE user_id=?");
            update_facility_id.setNull(1, Types.INTEGER);
            update_facility_id.setInt(2, this.userId);
            success = (update_facility_id.executeUpdate() > 0);
        }
        return success;
    }

    @SuppressWarnings("unused")
    @Deprecated
    private boolean addCustomersToSourcesToDB(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.info("Customer::AddCustomersToSourcesToDB()");

        String leadsource = this.leadSource;
        boolean success = true;

        SqlPreparedStatement query = sql
                .prepareStatement("SELECT id, code FROM ct_customer.sources WHERE code = ?");
        query.setString(1, leadsource);

        CTLogger.info(query.toString(), 2);
        ResultSet rs = query.executeQuery();

        boolean empty = true;
        while (rs.next())
        {
            empty = false;
            int source_id = rs.getInt("id");
            String leadsourcename = null;
            /*
			if (leadsource.equals("CONF"))
			{
				leadsourcename = this.tradeShowName;
			}
			else if (leadsource.equals("PUBL"))
			{
				leadsourcename = this.publicationName;
			}
			 */
            leadsourcename = this.leadSourceDetails;

            // System.out.println("Source Code" + source_id);
            SqlPreparedStatement statement = sql
                    .prepareStatement("INSERT INTO ct_customer.customers_to_sources VALUES(?,?,?,?,?)");
            statement.setInt(1, this.id);
            statement.setInt(2, source_id);
            statement.setString(3, leadsourcename);
            statement.setString(4, null);
            statement.setString(5, null);

            success &= (statement.executeUpdate() > 0);
        }
        if (empty)
        {
            query = sql.prepareStatement("SELECT id FROM ct_customer.sources WHERE code = ?");
            query.setString(1, "OTHR");

            CTLogger.info(query.toString(), 2);
            rs = query.executeQuery();

            while (rs.next())
            {
                int source_id = rs.getInt("id");

                SqlPreparedStatement stmt2 = sql
                        .prepareStatement("INSERT INTO ct_customer.customers_to_sources VALUES(?,?,?,?,?)");

                stmt2.setInt(1, this.id);
                stmt2.setInt(2, source_id);
                stmt2.setString(3, this.leadSource);
                stmt2.setString(4, null);
                stmt2.setString(5, null);
                CTLogger.debug(stmt2.toString(), 2);
                success &= (stmt2.executeUpdate() > 0);
            }
        }
        return success;
    }

    /**
     * Not used.
     */
    @Deprecated
    private boolean addCustomersToTherapiesToDB(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.info("Customer::AddCustomersToTherapiesToDB()");

        String currenttherapy = this.setting;
        boolean success = true;

        SqlPreparedStatement statement = sql.prepareStatement("SELECT id FROM ct_customer.therapy_types WHERE code = ?");
        statement.setString(1, currenttherapy);

        ResultSet rs = statement.executeQuery();

        while (rs.next())
        {
            int therapy_id = rs.getInt("id");

            SqlPreparedStatement stmt2 =
                    sql.prepareStatement("INSERT INTO ct_customer.customers_to_therapy_types(customer_id, therapy_type_id) VALUES(?,?)");

            stmt2.setInt(1, this.id);
            stmt2.setInt(2, therapy_id);

            CTLogger.debug(stmt2.toString(), 2);
            success &= (stmt2.executeUpdate() > 0);
        }
        return success;
    }

    private boolean addCustomersToDeficitsToDB(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.info("Customer::AddCustomersToDeficitsToDB()");

        if (this.deficits == null)
        {
            return true;// Nothing to enter the database.
        }
        String[] splits = this.deficits.split(",");
        boolean success = true;

        for (String deficit : splits)
        {
            String query = "INSERT INTO ct_customer.customers_to_deficits (`customer_id`, `deficit_id`, `details`) " +
                    "VALUES(?, ifnull((SELECT id FROM ct_customer.deficits WHERE code=?), 9999), ?) " +
                    "ON DUPLICATE KEY UPDATE`details` = values(`details`)";

            SqlPreparedStatement statement = sql.prepareStatement(query);
            statement.setInt(1, this.id);
            statement.setString(2, deficit);
            statement.setString(3, deficit);

            success &= (statement.executeUpdate() > 0);
        }

        return success;
    }

    public boolean addCustomerToDB(ReadWriteDbConnection sql) throws SQLException
    {
        SqlPreparedStatement statement = sql
                .prepareStatement(
                        "INSERT INTO ct_customer.customers "
                                + "(user_id, first_name, last_name, email, created_date, lead_source, lead_source_details, "
                                + "location, age_group, usertype, whatpromptedyou, frequency_of_therapy, facility_name, "
                                + "condition_since, role, setting, signup_location, birth_year, signup_landing_page, phone_number, ip_address, "
                                + "city, state, country, gender, education, caregiver_email) "
                                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, ?)",
                        Statement.RETURN_GENERATED_KEYS);

        if (this.userId == null)
            statement.setNull(1, Types.INTEGER);
        else
            statement.setInt(1, this.userId);

        if (this.firstName == null)
            statement.setNull(2, Types.VARCHAR);
        else
            statement.setString(2, this.firstName);

        if (this.lastName == null)
            statement.setNull(3, Types.VARCHAR);
        else
            statement.setString(3, this.lastName);

        if (this.email == null)
            statement.setNull(4, Types.VARCHAR);
        else
            statement.setString(4, this.email);

        statement.setTimestamp(5, TimeUtil.timeNow());

        if (this.leadSource == null)
            statement.setNull(6, Types.VARCHAR);
        else
            statement.setString(6, this.leadSource);

        if (this.leadSourceDetails == null)
            statement.setNull(7, Types.VARCHAR);
        else
            statement.setString(7, this.leadSourceDetails);

        if (this.location == null)
            statement.setNull(8, Types.VARCHAR);
        else
            statement.setString(8, this.location);

        if (this.ageGroup == null)
            statement.setNull(9, Types.VARCHAR);
        else
            statement.setString(9, this.ageGroup);

        if (this.userType == null)
            statement.setNull(10, Types.VARCHAR);
        else
            statement.setString(10, this.userType.toString());

        if (this.whatPromptedYou == null)
            statement.setNull(11, Types.VARCHAR);
        else
            statement.setString(11, this.whatPromptedYou);

        if (this.frequencyOfTherapy == null)
            statement.setNull(12, Types.VARCHAR);
        else
            statement.setString(12, this.frequencyOfTherapy);

        if (this.facilityName == null)
            statement.setNull(13, Types.VARCHAR);
        else
            statement.setString(13, this.facilityName);

        if (this.conditionSince == null)
            statement.setNull(14, Types.VARCHAR);
        else
            statement.setString(14, this.conditionSince);

        if (this.role == null)
            statement.setNull(15, Types.VARCHAR);
        else
            statement.setString(15, this.role);

        if (this.setting == null)
            statement.setNull(16, Types.VARCHAR);
        else
            statement.setString(16, this.setting);

        if (this.signupLocation == null)
            statement.setNull(17, Types.VARCHAR);
        else
            statement.setString(17, this.signupLocation);

        if (this.birthYear == null)
            statement.setNull(18, Types.INTEGER);
        else
            statement.setInt(18, this.birthYear);

        if (this.signupLandingPage == null)
            statement.setNull(19, Types.VARCHAR);
        else
            statement.setString(19, this.signupLandingPage);

        if (this.phoneNumber == null)
            statement.setNull(20, Types.VARCHAR);
        else
            statement.setString(20, this.phoneNumber);

        if (this.ipAddress == null)
            statement.setNull(21, Types.VARCHAR);
        else
            statement.setString(21, this.ipAddress);

        if (this.city == null)
            statement.setNull(22, Types.VARCHAR);
        else
            statement.setString(22, this.city);

        if (this.state == null)
            statement.setNull(23, Types.VARCHAR);
        else
            statement.setString(23, this.state);

        if (this.country == null)
            statement.setNull(24, Types.VARCHAR);
        else
            statement.setString(24, this.country);

        if (this.gender == null)
            statement.setNull(25, Types.VARCHAR);
        else
            statement.setString(25, this.gender);

        if (this.education == null)
            statement.setNull(26, Types.VARCHAR);
        else
            statement.setString(26, this.education);

        if (this.caregiverEmail == null)
            statement.setNull(27, Types.VARCHAR);
        else
            statement.setString(27, this.caregiverEmail);

        boolean success = statement.executeUpdate() > 0;
        if (success)
        {
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next())
                this.id = rs.getInt(1);
            return true;
        }
        return false;
    }

    private boolean addCustomersToDisordersToDB(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.info("Customer::AddCustomersToDisordersToDB()");

        String disorders = this.disorders;
        if (disorders == null)
            return true;// nothing to insert to db
        String[] splits = disorders.split(",");
        boolean success = true;
        for (String disorder : splits)
        {
            String query = "INSERT INTO ct_customer.customers_to_disorders (`customer_id`, `disorder_id`, `details`) " +
                    "VALUES(?, ifnull((SELECT id FROM ct_customer.disorders WHERE code=?), 9999), ?) " +
                    "ON DUPLICATE KEY UPDATE`details` = values(`details`)";

            SqlPreparedStatement statement = sql.prepareStatement(query);
            statement.setInt(1, this.id);
            statement.setString(2, disorder);
            statement.setString(3, disorder);

            success &= (statement.executeUpdate() > 0);
        }
        return success;
    }

    @Override
    public void read(DbConnection sql) throws SQLException, IllegalArgumentException
    {
        CTLogger.info("Customer::read()");
        this.read(sql, this.id);
    }

    @Override
    public void read(ResultSet rs) throws SQLException
    {
        CTLogger.info("Customer::read()");

        this.id = rs.getInt("id");
        this.userId = rs.getInt("user_id");
        this.firstName = rs.getString("first_name");
        this.lastName = rs.getString("last_name");
        this.email = rs.getString("email");
        this.phoneNumber = rs.getString("phone_number");
        this.createdDate = rs.getTimestamp("created_date");
        this.leadSource = rs.getString("lead_source");
        this.leadSourceDetails = rs.getString("lead_source_details");
        this.ageGroup = rs.getString("age_group");
        this.comments = rs.getString("comments");
        this.location = rs.getString("location");
        this.city = rs.getString("city");
        this.state = rs.getString("state");
        this.country = rs.getString("country");
        this.ipAddress = rs.getString("ip_address");
        this.conditionSince = rs.getString("condition_since");
        this.frequencyOfTherapy = rs.getString("frequency_of_therapy");
        this.userType = rs.getString("usertype");
        this.facilityName = rs.getString("facility_name");
        this.whatPromptedYou = rs.getString("whatpromptedyou");
        this.role = rs.getString("role");
        this.signupLocation = rs.getString("signup_location");
        this.setting = rs.getString("setting");
        this.birthYear = rs.getInt("birth_year");
        this.gender = rs.getString("gender");
        this.education = rs.getString("education");
        this.signupLandingPage = rs.getString("signup_landing_page");
    }

    @Override
    public void read(DbConnection sql, Integer customerId) throws SQLException, IllegalArgumentException
    {
        if (this.id == 0)
            throw new IllegalArgumentException("No id specified");

        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM ct_customer.customers WHERE id = ?");
        statement.setInt(1, customerId);

        ResultSet rs = statement.executeQuery();
        if (rs.next())
            this.read(rs);
        else
            throw new IllegalArgumentException("Customer with Id '" + this.id + "' does not exist.");
    }

    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.info("Customer::update()");

        assert (this.id > 0);

        SqlPreparedStatement statement = sql
                .prepareStatement("UPDATE ct_customer.customers "
                        + "SET user_id=?, first_name=?, last_name=?, email=?, created_date=?, lead_source=?, lead_source_details=?, "
                        + "age_group=?, comments=?, location=?, condition_since=?, frequency_of_therapy=?, "
                        + "usertype=?, facility_name=?, whatpromptedyou=?, role=?, setting=?, signup_location=?, birth_year=?, signup_landing_page=?,"
                        + "phone_number=?, ip_address=?, city=?, state=?, country=?, gender=?, education=? "
                        + "WHERE id = ? ");

        if (this.userId == null)
            statement.setNull(1, Types.INTEGER);
        else
            statement.setInt(1, this.userId);

        if (this.firstName == null)
            statement.setNull(2, Types.VARCHAR);
        else
            statement.setString(2, this.firstName);

        if (this.lastName == null)
            statement.setNull(3, Types.VARCHAR);
        else
            statement.setString(3, this.lastName);

        if (this.email == null)
            statement.setNull(4, Types.VARCHAR);
        else
            statement.setString(4, this.email);

        if (this.createdDate == null)
            statement.setNull(5, Types.TIMESTAMP);
        else
            statement.setTimestamp(5, this.createdDate);

        if (this.leadSource == null)
            statement.setNull(6, Types.VARCHAR);
        else
            statement.setString(6, this.leadSource);

        if (this.leadSourceDetails == null)
            statement.setNull(7, Types.VARCHAR);
        else
            statement.setString(7, this.leadSourceDetails);

        if (this.ageGroup == null)
            statement.setNull(8, Types.VARCHAR);
        else
            statement.setString(8, this.ageGroup);

        if (this.comments == null)
            statement.setNull(9, Types.VARCHAR);
        else
            statement.setString(9, this.comments);

        if (this.location == null)
            statement.setNull(10, Types.VARCHAR);
        else
            statement.setString(10, this.location);

        if (this.conditionSince == null)
            statement.setNull(11, Types.VARCHAR);
        else
            statement.setString(11, this.conditionSince);

        if (this.frequencyOfTherapy == null)
            statement.setNull(12, Types.VARCHAR);
        else
            statement.setString(12, this.frequencyOfTherapy);

        if (this.userType == null)
            statement.setNull(13, Types.VARCHAR);
        else
            statement.setString(13, this.userType.toString());

        if (this.facilityName == null)
            statement.setNull(14, Types.VARCHAR);
        else
            statement.setString(14, this.facilityName);

        if (this.whatPromptedYou == null)
            statement.setNull(15, Types.VARCHAR);
        else
            statement.setString(15, this.whatPromptedYou);

        if (this.role == null)
            statement.setNull(16, Types.VARCHAR);
        else
            statement.setString(16, this.role);

        if (this.setting == null)
            statement.setNull(17, Types.VARCHAR);
        else
            statement.setString(17, this.setting);

        if (this.signupLocation == null)
            statement.setNull(18, Types.VARCHAR);
        else
            statement.setString(18, this.signupLocation);

        if (this.birthYear == null)
            statement.setNull(19, Types.INTEGER);
        else
            statement.setInt(19, this.birthYear);

        if (this.signupLandingPage == null)
            statement.setNull(20, Types.VARCHAR);
        else
            statement.setString(20, this.signupLandingPage);

        if (this.phoneNumber == null)
            statement.setNull(21, Types.VARCHAR);
        else
            statement.setString(21, this.phoneNumber);

        if (this.ipAddress == null)
            statement.setNull(22, Types.VARCHAR);
        else
            statement.setString(22, this.ipAddress);

        if (this.city == null)
            statement.setNull(23, Types.VARCHAR);
        else
            statement.setString(23, this.city);

        if (this.state == null)
            statement.setNull(24, Types.VARCHAR);
        else
            statement.setString(24, this.state);

        if (this.country == null)
            statement.setNull(25, Types.VARCHAR);
        else
            statement.setString(25, this.country);

        if (this.gender == null)
            statement.setNull(26, Types.VARCHAR);
        else
            statement.setString(26, this.gender);

        if (this.education == null)
            statement.setNull(27, Types.VARCHAR);
        else
            statement.setString(27, this.education);

        statement.setInt(28, this.id);

        statement.execute();
    }

    @Override
    public void delete(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.info("Customer::delete()");
        throw new UnsupportedOperationException();
    }

    public static Customer getCustomerByEmail(ReadWriteDbConnection sql, String email) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM ct_customer.customers WHERE email=?");
        statement.setString(1, email);

        ResultSet rs = statement.executeQuery();

        Customer c = null;
        if (rs.next())
        {
            c = new Customer();
            c.read(rs);
        }

        return c;
    }

    public static Customer getCustomerByUserId(DbConnection sqlr, int userId) throws SQLException
    {
        SqlPreparedStatement statement = sqlr.prepareStatement("SELECT * FROM ct_customer.customers WHERE user_id = ?");
        statement.setInt(1, userId);

        ResultSet rs = statement.executeQuery();

        Customer c = null;
        if (rs.next())
        {
            c = new Customer();
            c.read(rs);
        }

        return c;
    }

    public void addPhoneNumber(ReadWriteDbConnection sql, String phone) throws SQLException
    {
        if (phone == null)
            throw new IllegalArgumentException("phone cannot be null");

        SqlPreparedStatement statement = null;

        try
        {
            CTLogger.infoStart("Customer::addPhoneNumber() - phone=" + phone);

            if (this.phoneNumber != null)
            {
                // check that we don't already have this phone number
                String[] arr = this.phoneNumber.split(",");

                for (String s : arr)
                {
                    if (phone.equals(s))
                    {
                        CTLogger.warn(String.format("Phone number %s already registered with customer id %s", phone, this.id));
                        return;
                    }
                }

                // append new phone number with a comma
                phone = this.phoneNumber + "," + phone;
            }

            statement = sql.prepareStatement("UPDATE ct_customer.customers SET phone_number = ? WHERE user_id = ?");
            statement.setString(1, phone);
            statement.setInt(2, this.userId);

            statement.executeUpdate();
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            CTLogger.unindent();
        }
    }

    public void setIpAddress(DbConnection sql, String ipAddress)
    {
        CTLogger.debug("Customer::setIpAddress() - " + ipAddress);

        this.ipAddress = ipAddress;

        GeoInfo geo = GeoLocation.getLocationInfoFromIpAddress(sql, ipAddress);

        this.location = geo.getLocation();
        this.city = geo.city;
        this.state = geo.state;
        this.country = geo.country;
    }

    /**
     * Simple method to update customer name, email and phone number.
     * This is for internal use from the CTUser.updateIdentifiableInfo(), so that both, the users and customers tables
     * are updated simultaneously.
     * @param sql
     * @param userInfo
     * @throws SQLException
     */
    void updateIdentifiableInfo(ReadWriteDbConnection sql, IdentifiableUserInfo userInfo) throws SQLException
    {
        CTLogger.info("Customer::updateIdentifiableInfo()");

        assert (this.id > 0);

        StringBuilder sb = new StringBuilder("UPDATE ct_customer.customers SET ");

        String comma = "";

        if (userInfo.firstName != null)
        {
            sb.append(comma).append(" first_name = ?");
            comma = ", ";
        }

        if (userInfo.lastName != null)
        {
            sb.append(comma).append(" last_name = ?");
        }

        if (userInfo.email != null)
        {
            sb.append(comma).append("email = ?");
            if (comma.isEmpty()) comma = ", ";
        }

        if (userInfo.phoneNumber != null)
        {
            sb.append(comma).append(" phone_number = ?");
        }

        sb.append(" WHERE id = ?");

        SqlPreparedStatement statement = sql.prepareStatement(sb.toString());

        int c = 0;

        if (userInfo.firstName != null)
        {
            c++;
            statement.setString(c, userInfo.firstName);
        }

        if (userInfo.lastName != null)
        {
            c++;
            statement.setString(c, userInfo.lastName);
        }

        if (userInfo.email != null)
        {
            c++;
            statement.setString(c, userInfo.email);
        }

        if (userInfo.phoneNumber != null)
        {
            c++;
            statement.setString(c, userInfo.phoneNumber);
        }

        if (c > 0)
        {
            c++;
            statement.setInt(c, this.id);
            statement.executeUpdate();
        }
    }
}
