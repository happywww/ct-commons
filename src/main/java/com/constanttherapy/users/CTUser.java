/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */
package com.constanttherapy.users;

import com.constanttherapy.CTCrud;
import com.constanttherapy.ServiceBase;
import com.constanttherapy.db.*;
import com.constanttherapy.enums.*;
import com.constanttherapy.service.proxies.MessagingServiceProxy;
import com.constanttherapy.util.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.security.sasl.AuthenticationException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * A single user with a name, email-address, and username/password combination
 *
 * @author ehsan
 */
public abstract class CTUser extends UserData implements CTCrud
{
    protected static SecureRandom random = new SecureRandom();
    private transient String emailAccessToken;

    public CTUser(ResultSet rs) throws SQLException
    {
        read(rs);
    }

    public CTUser()
    {
    }

    // Valid username: [a-z], [A-Z], [0-9], ., _ and - (less than 32 characters)
    public static boolean checkUsernameFormat(String username)
    {
        String regex = "^(\\w|\\-|\\.){1,32}$";
        return username != null && username.matches(regex);
    }

    public static void updateLastResponseTime(ReadWriteDbConnection sql, int userId, Timestamp ts) throws SQLException
    {
        if (ts == null)
        {
            ts = TimeUtil.timeNow();
        }

        SqlPreparedStatement statement = sql.prepareStatement("UPDATE ct_stats.user_stats SET last_response_time = ? WHERE user_id = ?");
        statement.setTimestamp(1, ts);
        statement.setInt(2, userId);

        statement.execute();
    }

    public static CTUser getByToken(ReadWriteDbConnection sql, String token) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE access_token = ? " +
                "OR email_access_token = ? OR website_access_token = ?");
        statement.setString(1, token);
        statement.setString(2, token);
        statement.setString(3, token);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return CTUser.fromResultSet(rs);
        else
            return null;
    }

    static CTUser fromResultSet(ResultSet rs) throws SQLException
    {
        CTUser user = null;
        String userType = rs.getString("type");
        UserType type = UserType.valueOf(userType);

        if (type == UserType.patient)
        {
            user = new Patient(rs);
        }
        else if (type == UserType.clinician)
        {
            user = new Clinician(rs);
        }
        else if (type == UserType.admin)
        {
            user = new Admin(rs);
        }

        return user;
    }

    public static List<CTUser> search(ReadWriteDbConnection sql, String searchString) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement(
                "SELECT * FROM users WHERE id = ? OR username LIKE ? OR email LIKE ? OR first_name LIKE ? OR last_name LIKE ? LIMIT 30");
        Integer id;
        try
        {
            id = Integer.parseInt(searchString);
        }
        catch (NumberFormatException e)
        {
            id = -1;
        }
        statement.setInt(1, id);
        statement.setString(2, "%" + searchString + "%");
        statement.setString(3, "%" + searchString + "%");
        statement.setString(4, "%" + searchString + "%");
        statement.setString(5, "%" + searchString + "%");

        ResultSet rs = statement.executeQuery();

        List<CTUser> users = new ArrayList<>();
        while (rs.next())
        {
            users.add(CTUser.fromResultSet(rs));
        }
        return users;
    }

    public static CTUser getByIdOrName(ReadWriteDbConnection sql, String userIdOrName) throws SQLException
    {
        CTUser user = null;

        // first see if it's a numeric id
        Integer id = MathUtil.tryParseInt(userIdOrName);

        if (id != null) // numeric - try to get by id
        {
            user = CTUser.getById(sql, id);
        }

        if (user == null)
        {
            user = CTUser.getByUsername(sql, userIdOrName);
        }
        return user;
    }

    /*
     * TODO:
     * These few methods: CTUser::getById(), CTUser::login(), CTUser::getByUsername() should eventually
     * be moved to a separate factory class
     */
    public static CTUser getById(DbConnection sql, Integer id) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE id = ?");
        statement.setInt(1, id);

        ResultSet rs = statement.executeQuery();
        if (rs.next())
            return CTUser.fromResultSet(rs);
        else
            return null;
    }

    /**
     * Gets CTUser from the specified username
     *
     * @param sql
     * @param username
     * @return CTUser object if found, null otherwise.
     * @throws SQLException
     */
    public static CTUser getByUsername(DbConnection sql, String username) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE username = ?");
        statement.setString(1, username);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return CTUser.fromResultSet(rs);
        else
            return null;
    }

    public static CTUser getById(Integer id) throws SQLException
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return CTUser.getById(sql, id);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }
    }

    /**
     * User provided email and username, so verify both match.
     *
     * @param sql
     * @param email
     * @param username
     * @return List of users.  Contains 1 element if a match is found, 0 otherwise.
     * @throws SQLException
     */
    public static List<CTUser> getByEmailAndUsername(ReadWriteDbConnection sql, String email, String username) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE email = ? AND username = ?");
        statement.setString(1, email);
        statement.setString(2, username);

        ResultSet rs = statement.executeQuery();
        ArrayList<CTUser> users = new ArrayList<>();

        while (rs.next())
        {
            users.add(CTUser.fromResultSet(rs));
        }

        return users;
    }

    public static List<CTUser> getByEmail(ReadWriteDbConnection sql, String email) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE email = ?");
        statement.setString(1, email);

        ResultSet rs = statement.executeQuery();
        ArrayList<CTUser> users = new ArrayList<>();

        while (rs.next())
        {
            users.add(CTUser.fromResultSet(rs));
        }

        return users;
    }

    /**
     * Gets CTUser from the specified username
     *
     * @param username
     * @return CTUser object if found, null otherwise.
     * @throws SQLException
     */
    public static CTUser getByUsername(String username) throws SQLException
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return getByUsername(sql, username);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }
    }

    public static CTUser login(DbConnection sql, String token) throws SQLException
    {
        SqlPreparedStatement statement =
                sql.prepareStatement("SELECT * FROM users WHERE access_token = ? OR website_access_token = ? OR email_access_token = ?");
        statement.setString(1, token);
        statement.setString(2, token);
        statement.setString(3, token);

        ResultSet rs = statement.executeQuery();
        if (rs.next())
        {
            CTUser user = CTUser.fromResultSet(rs);
            if (user != null)
                if (user.isAccountActive())
                    return user;
                else
                {
                    CTLogger.warn("**** CTUser::login() - Account is inactive; username: " + user.getUsername());
                }
        }
        else
        {
            CTLogger.warn("*** CTUser::login() - Invalid (or stale) access token: " + token);
        }

        return null;
    }

    public static CTUser login(ReadWriteDbConnection sql, String username, String password) throws SQLException, AuthenticationException
    {
        CTLogger.infoStart(String.format("CTUser::login() - username=%s", username));

        username = username.trim();  // remove leading & trailing spaces

        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE username = ?");

        statement.setString(1, username);

        ResultSet rs = statement.executeQuery();
        if (rs.next())
        {
            CTUser user = CTUser.fromResultSet(rs);

            if (user != null)
                if (user.isAccountActive())
                {
                    if (user.checkPassword(password))
                        return user;
                    else
                    {
                        CTLogger.warn("**** CTUser::login() - Invalid password specified; username: " + username);
                    }
                }
                else
                {
                    CTLogger.warn("**** CTUser::login() - Account is inactive; username: " + username);
                }
        }
        else
        {
            CTLogger.warn("*** CTUser::login() - Invalid username specified: " + username);
        }

        return null;
    }

    public boolean checkPassword(String attemptedPassword) throws AuthenticationException
    {
        return this.password.checkPassword(attemptedPassword);
    }

    public static boolean usernameExists(ReadWriteDbConnection sql, String username) throws SQLException
    {
        CTUser user = CTUser.getByUsername(sql, username);
        return user != null;
    }

    public static String createRandomSimplePassword(ReadWriteDbConnection sql)
    {
        try
        {
            // Just pick a name from the items table:
            //return ItemFilter.filter().byNameLength(6).getRandomItem(sql).name;
            SqlPreparedStatement statement = sql.prepareStatement("SELECT name FROM items WHERE length(name) = 6 ORDER BY rand() LIMIT 1;");

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getString("name");
            else
                return createRandomPassword();
        }
        catch (Exception e)
        {
            return createRandomPassword();
        }
    }

    public static String createRandomPassword()
    {
        return new BigInteger(30, random).toString(32);
    }

    public static int getIdFromUsername(String uname)
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return CTUser.getIdFromUsername(sql, uname);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }

        return -1;
    }

    public static int getIdFromUsername(DbConnection sql, String uname)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT id FROM users WHERE username = ?");
            statement.setString(1, uname);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getInt("id");
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }

        return -1;
    }

    public static int getIdFromAccessToken(String token)
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return CTUser.getIdFromAccessToken(sql, token);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }

        return -1;
    }

    private static int getIdFromAccessToken(ReadWriteDbConnection sql, String token)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT id FROM users WHERE access_token = ? OR email_access_token = ? OR website_access_token = ?");
            statement.setString(1, token);
            statement.setString(2, token);
            statement.setString(3, token);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getInt("id");
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }

        return -1;
    }

    public static String getEmailFromUserId(int userId)
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return getEmailFromUserId(sql, userId);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }

        return null;
    }

    private static String getEmailFromUserId(ReadWriteDbConnection sql, int userId)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT email FROM users WHERE id = ?");
            statement.setInt(1, userId);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getString("email");
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }

        return null;
    }

    private static String getEmailFromUsername(ReadWriteDbConnection sql, String uname)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT email FROM users WHERE username = ?");
            statement.setString(1, uname);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getString("email");
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }

        return null;
    }

    public static float getClientVersionNumber(int patientId)
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return CTUser.getClientVersionNumber(sql, patientId);
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
            return 0.0f;
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }
    }

    private static float getClientVersionNumber(ReadWriteDbConnection sql, int patientId) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT client_version_number FROM users WHERE id = ?");
        statement.setInt(1, patientId);

        ResultSet rs = statement.executeQuery();
        if (rs.next())
        {
            String ver = rs.getString("client_version_number");
            return MathUtil.versionStringToFloat(ver);
        }

        return -1.0f;
    }

    private static int getDaysSinceLastUse(ReadWriteDbConnection sql, int userId) throws SQLException
    {
        SqlPreparedStatement statement =
                sql.prepareStatement("SELECT TIMESTAMPDIFF(DAY, MAX(timestamp), NOW()) days FROM responses WHERE patient_id=?");
        statement.setInt(1, userId);

        ResultSet rs = statement.executeQuery();

        Integer days = null;
        if (rs.next())
        {
            days = rs.getInt("days");
        }

        return (days == null ? -1 : days);
    }

    public static String getUsernameFromId(int userId)
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            return CTUser.getUsernameFromId(sql, userId);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }

        return null;
    }

    private static String getUsernameFromId(ReadWriteDbConnection sql, int userId)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT username FROM users WHERE id = ?");
            statement.setInt(1, userId);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
                return rs.getString("username");
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }

        return null;
    }

	/*
    public static CTUser loginToPortal(Connection sql, String websiteToken) throws SQLException
	{
		SqlPreparedStatement statement = sql.prepareStatement("select * from users where website_access_token = ?");
		statement.setString(1, websiteToken);

		ResultSet rs = statement.executeQuery();
		if (rs.next())
		{
			CTUser user = CTUser.fromResultSet(rs);
			if (user != null)
			{
				if (user.isAccountActive)
					return user;
				else
					CTLogger.warn("**** CTUser::login() - Account is inactive; username: " + user.username);
			}
		}
		else
			CTLogger.warn("*** CTUser::login() - Invalid (or stale) website access token: " + websiteToken);

		return null;
	}
	 */

    /**
     * Gets CTUser from the user's email access token
     *
     * @param sql
     * @param token the email access token
     * @return CTUser object if found, null otherwise.
     * @throws SQLException
     */
    public static CTUser getByEmailAccessToken(ReadWriteDbConnection sql, String token) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE email_access_token = ?");
        statement.setString(1, token);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return CTUser.fromResultSet(rs);
        else
            return null;
    }

    public static void updateLatLong(ReadWriteDbConnection sql, int userId, Double latitude, Double longitude)
    {
        SqlPreparedStatement statement = null;

        try
        {
            statement = sql.prepareStatement("CALL AddUserLatLong(?,?,?)");
            statement.setInt(1, userId);
            statement.setDouble(2, latitude);
            statement.setDouble(3, longitude);

            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
        }
    }

    public void create(ReadWriteDbConnection sql, String pass) throws AuthenticationException, SQLException
    {
        this.password = PasswordTuple.generateFromPassword(pass);
        create(sql);
    }

    // CRUD Methods:
    @Override
    public void create(ReadWriteDbConnection sql) throws SQLException
    {

        CTLogger.infoStart("CTUser::create()");

        try
        {
            /*
             * We are ignoring the accessToken and acceptedEULA upon user creation because they should
			 * be null
			 */

            SqlPreparedStatement statement = sql.prepareStatement("INSERT INTO users " +
                    "(username, password_hash, password_salt, first_name, last_name, email, type, is_demo, prompt_change_password, create_date, needs_assessment, is_subscribed) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, this.getUsername());

            statement.setBytes(2, this.password.getHash());
            statement.setBytes(3, this.password.getSalt());

            // First name and last name aren't required
            statement.setString(4, this.firstName);

            statement.setString(5, this.getLastName());

            statement.setString(6, this.getEmail());
            statement.setString(7, this.type.toString());
            statement.setBoolean(8, this.isDemo);
            statement.setBoolean(9, this.isPromptChangePassword());
            this.createDate = TimeUtil.timeNow();
            statement.setTimestamp(10, this.createDate);

            statement.setBoolean(11, this.needsAssessment);

            statement.setBoolean(12, this.isSubscribed());

            boolean success = statement.executeUpdate() > 0;
            if (success)
            {
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next())
                {
                    this.id = rs.getInt(1);
                    UserEventLogger.EventBuilder
                            .create(UserEventType.AccountCreated)
                            .userId(this.id)
                            .log(sql);
                }
            }
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    // Read
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
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE id = ?");
        statement.setInt(1, id);

        ResultSet rs = statement.executeQuery();
        read(rs);
    }

    @Override
    public void read(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("id");
        this.setUsername(rs.getString("username"));
        this.password = new PasswordTuple(rs.getBytes("password_hash"), rs.getBytes("password_salt"));
        this.firstName = rs.getString("first_name");
        this.setLastName(rs.getString("last_name"));
        this.setEmail(rs.getString("email"));
        this.phoneNumber = rs.getString("phone_number");
        this.accessToken = rs.getString("access_token");
        this.websiteAccessToken = rs.getString("website_access_token");
        this.websiteAccessTokenTimeout = rs.getTimestamp("website_access_token_timestamp");
        this.emailAccessToken = rs.getString("email_access_token");
        this.type = UserType.valueOf(rs.getString("type"));
        this.setAcceptedEULA(rs.getBoolean("accepted_eula"));
        this.isDemo = rs.getBoolean("is_demo");
        this.setPromptChangePassword(rs.getBoolean("prompt_change_password"));
        this.setLeftHanded(rs.getBoolean("is_left_handed"));
        this.deviceToken = rs.getString("device_token");
        this.setAccountActive(rs.getBoolean("is_account_active"));
        this.lastLogin = rs.getTimestamp("last_login");
        this.createDate = rs.getTimestamp("create_date");
        this.lastResponseTime = rs.getTimestamp("last_response_time");
        this.setDoNotContact(rs.getInt("do_not_contact"));
        this.setPushNotificationToken(rs.getString("push_notif_token"));

        this.clientVersion = rs.getString("client_version_number");
        this.clientPlatform = ClientPlatform.fromInteger(rs.getInt("client_platform"));
        this.clientTimeZone = rs.getString("client_timezone");
        this.clientOSVersion = rs.getString("client_os_version");
        this.clientHardwareType = rs.getString("client_hardware_type");
        this.clientIPAddress = rs.getString("client_ip_address");
        this.clientDeviceIdentifier = rs.getString("client_device_identifier");
        this.needsAssessment = rs.getBoolean("needs_assessment");
        this.setSubscribed(rs.getBoolean("is_subscribed"));
    }

    // Update
    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        assert (this.id != null && this.id > 0);
        CTLogger.infoStart("CTUser::update()");
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("UPDATE users SET username = ?, "
                    + "password_hash = ?, password_salt = ?, first_name = ?, last_name = ?, "
                    + "email = ?, access_token = ?, type = ?, accepted_eula = ?, is_demo = ?, "
                    + "is_left_handed = ?, prompt_change_password = ?, device_token = ?, "
                    + "last_login = ?, is_account_active = ?, website_access_token = ?, "
                    + "email_access_token = ?, needs_assessment = ?, is_subscribed = ?, "
                    + "push_notif_token = ? "
                    + "WHERE id = ?");

            statement.setString(1, this.getUsername());
            statement.setBytes(2, this.password.getHash());
            statement.setBytes(3, this.password.getSalt());
            statement.setString(4, this.firstName);
            statement.setString(5, this.getLastName());
            statement.setString(6, this.getEmail());
            statement.setString(7, this.accessToken);
            statement.setString(8, this.type.toString());
            statement.setBoolean(9, this.isAcceptedEULA());
            statement.setBoolean(10, this.isDemo);
            statement.setBoolean(11, this.isLeftHanded());
            statement.setBoolean(12, this.isPromptChangePassword());
            statement.setString(13, this.deviceToken);
            statement.setTimestamp(14, this.lastLogin);
            statement.setBoolean(15, this.isAccountActive());
            statement.setString(16, this.websiteAccessToken);
            statement.setString(17, getEmailAccessToken(sql, false));
            statement.setBoolean(18, this.needsAssessment);
            statement.setBoolean(19, this.isSubscribed());
            statement.setString(20, this.getPushNotificationToken());

            // do not update do_not_contact. this is done directly in the SQL for now

            statement.setInt(21, this.id);

            statement.execute();
        }
        finally
        {
            CTLogger.unindent();
        }
        // TODO: add debug statements
    }

    // Delete
    @Override
    public void delete(ReadWriteDbConnection sql) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isClinician()
    {
        return this.type.equals(UserType.clinician);
    }

    public boolean isBot()
    {
        return (this.id == Clinician.BOT_ID || this.id == Clinician.BOT_2_ID);
    }

    public boolean isPatient()
    {
        return this.type.equals(UserType.patient);
    }

    public UpdateUserInfoResult updateIdentifiableInfo(ReadWriteDbConnection sql, IdentifiableUserInfo userInfo)
    {
        try
        {
            sql.setAutoCommit(false);

            UpdateUserInfoResult updateResult = new UpdateUserInfoResult();

            if (userInfo.email != null && !this.getEmail().equals(userInfo.email))
            {
                EmailValidationResult result = CTUser.validateEmail(sql, userInfo.email);
                if (!result.isValid())
                {
                    updateResult.success = false;
                    updateResult.addMessage("email", result.toString());
                    return updateResult;
                }

                logUserInfoUpdatedEvent(sql, "email", this.getEmail(), userInfo.email);

                // Send email to support when this happens
                MessagingServiceProxy msgProxy = new MessagingServiceProxy(ServiceBase.getUriInfo());
                msgProxy.sendMessageUsingBody("support@constanttherapy.com",
                        "Email changed for Username: " + this.getUsername(),
                        ("User Id: " + this.id + "<br/>" + "Old email: " + this.getEmail() + "<br/>" + "New email: " + userInfo.email));

                updateResult.addMessage("email", String.format("%s|%s", this.getEmail(), userInfo.email));
                this.setEmail(userInfo.email);
            }

            if (userInfo.phoneNumber != null && !this.phoneNumber.equals(userInfo.phoneNumber))
            {
                logUserInfoUpdatedEvent(sql, "phoneNumber", this.phoneNumber, userInfo.phoneNumber);
                updateResult.addMessage("phoneNumber", String.format("%s|%s", this.phoneNumber, userInfo.phoneNumber));

                // TODO: Send email to support when this happens
                this.phoneNumber = userInfo.phoneNumber;
            }

            if (userInfo.firstName != null && !this.getFirstName().equals(userInfo.firstName))
            {
                logUserInfoUpdatedEvent(sql, "firstName", this.getFirstName(), userInfo.firstName);
                updateResult.addMessage("firstName", String.format("%s|%s", this.getFirstName(), userInfo.firstName));
                this.firstName = userInfo.firstName;
            }

            if (userInfo.lastName != null && !this.getLastName().equals(userInfo.lastName))
            {
                logUserInfoUpdatedEvent(sql, "lastName", this.getLastName(), userInfo.lastName);
                updateResult.addMessage("lastName", String.format("%s|%s", this.getLastName(), userInfo.lastName));
                this.setLastName(userInfo.lastName);
            }

            this.update(sql);
            sql.commit();

            // now perform the update on the Customer object as well
            Customer cust = Customer.getCustomerByUserId(sql, this.id);

            if (cust == null)
                CTLogger.error(String.format("No customer record found for user %s (id %s)", this.getUsername(), this.getId()));
            else
                cust.updateIdentifiableInfo(sql, userInfo);

            updateResult.success = true;

            return updateResult;
        }
        catch (Exception e)
        {
            CTLogger.error(e);

            UpdateUserInfoResult result = new UpdateUserInfoResult();
            result.success = false;
            return result;
        }
        finally
        {
            try
            {
                sql.setAutoCommit(true);
            }
            catch (SQLException e)
            {
                CTLogger.error(e);
            }
        }
    }

    public static EmailValidationResult validateEmail(ReadWriteDbConnection sql, String email) throws SQLException
    {
        if (!StringUtil.isEmailAddressValid(email))
            return EmailValidationResult.EmailInvalidFormat;

        if (new EmailValidator(email).checkValidity())
        {
            // is it in the database? only consider active users' email addresses
            if (CTUser.emailExists(sql, email))
                return EmailValidationResult.EmailExists;
            else
                return EmailValidationResult.EmailValid;
        }
        else
            return EmailValidationResult.EmailFake;
    }

    private void logUserInfoUpdatedEvent(ReadWriteDbConnection sql, String type, Object before, Object after)
    {
        UserEventLogger.logEvent(sql,
                UserEventType.UserInfoUpdated,
                type,
                this.id,
                String.format("%s|%s", before, after),
                TimeUtil.timeNow(),
                null);
    }

    /**
     * email exists if a user has it as their email address AND if that user is active
     */
    public static boolean emailExists(ReadWriteDbConnection sql, String email) throws SQLException
    {
        List<CTUser> users = CTUser.getActiveByEmail(sql, email);
        return !users.isEmpty();
    }

    public String getEmailAccessToken(ReadWriteDbConnection sql, boolean update) throws SQLException
    {
        boolean closeConnection = false;

        try
        {
            if (this.emailAccessToken == null)
            {
                this.emailAccessToken = newAccessToken();

                if (update)
                {
                    if (sql == null)
                    {
                        closeConnection = true;
                        sql = new ReadWriteDbConnection();
                    }

                    SqlPreparedStatement statement = sql.prepareStatement("UPDATE users SET email_access_token = ? WHERE id = ?");

                    statement.setString(1, this.emailAccessToken);
                    statement.setInt(2, this.id);

                    statement.execute();
                }
            }
        }
        finally
        {
            if (closeConnection) SQLUtil.closeQuietly(sql);
        }

        return this.emailAccessToken;
    }

    /**
     * this version of getByEmail only returns active users, since it's OK to duplicate an email
     * address if the user is not active ... note that the database does not enforce non-dup emails
     */
    public static List<CTUser> getActiveByEmail(ReadWriteDbConnection sql, String email) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE (is_account_active = 1) AND (email = ? )");
        statement.setString(1, email);

        ResultSet rs = statement.executeQuery();
        ArrayList<CTUser> users = new ArrayList<>();

        while (rs.next())
        {
            users.add(CTUser.fromResultSet(rs));
        }

        return users;
    }

    private String newAccessToken()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void updateClientInfo(ReadWriteDbConnection sql,
                                 String clientVersion,
                                 Integer clientPlatform,
                                 String clientTimeZone,
                                 String clientHardwareType,
                                 String clientOSVersion,
                                 String clientIPAddress,
                                 String clientDeviceIdentifier) throws IllegalArgumentException
    {
        int params = 0;
        StringBuilder sb = new StringBuilder("update users set ");

        if (shouldUpdateClientInfo(this.clientVersion, clientVersion))
        {
            sb.append("client_version_number = ?");
            params++;
        }
        else
        {
            clientVersion = null; // no need to update
        }

        ClientPlatform cp = clientPlatform != null ? ClientPlatform.fromInteger(clientPlatform) : null;
        if (shouldUpdateClientInfo(this.clientPlatform, cp))
        {
            if (params > 0) sb.append(", ");
            sb.append("client_platform = ?");
            params++;
        }
        else
        {
            clientPlatform = null; // no need to update
        }

        if (shouldUpdateClientInfo(this.clientTimeZone, clientTimeZone))
        {
            if (params > 0) sb.append(", ");
            sb.append("client_timezone = ?");
            params++;
        }
        else
        {
            clientTimeZone = null; // no need to update
        }

        if (shouldUpdateClientInfo(this.clientHardwareType, clientHardwareType))
        {
            if (params > 0) sb.append(", ");
            sb.append("client_hardware_type = ?");
            params++;
        }
        else
        {
            clientHardwareType = null; // no need to update
        }

        if (shouldUpdateClientInfo(this.clientOSVersion, clientOSVersion))
        {
            if (params > 0) sb.append(", ");
            sb.append("client_os_version = ?");
            params++;
        }
        else
        {
            clientOSVersion = null; // no need to update
        }

        if (shouldUpdateClientInfo(this.clientIPAddress, clientIPAddress))
        {
            if (params > 0) sb.append(", ");
            sb.append("client_ip_address = ?");
            params++;
        }
        else
        {
            clientIPAddress = null; // no need to update
        }

        // REVIEW: we always want to update the device identifier. Some platforms
        // don't provide a device identifier so this may not be sent, and in those cases
        // we will put 'null'
        if (clientDeviceIdentifier == null)
            clientDeviceIdentifier = "null";

        if (shouldUpdateClientInfo(this.clientDeviceIdentifier, clientDeviceIdentifier))
        {
            if (params > 0) sb.append(", ");
            sb.append("client_device_identifier = ?");
            params++;
        }
        else
        {
            clientDeviceIdentifier = null; // no need to update
        }

        if (params == 0) return;  // no update needed

        sb.append(" where id = ?");

        // reset counter
        params = 0;

        try
        {
            SqlPreparedStatement statement = sql.prepareStatement(sb.toString());

            // transaction! we are setting multiple tables
            sql.setAutoCommit(false);

            if (clientVersion != null)
            {
                params++;
                statement.setString(params, clientVersion);
                logClientInfoUpdatedEvent(sql, "clientVersion", this.clientVersion, clientVersion);
            }

            if (clientPlatform != null)
            {
                params++;
                statement.setInt(params, clientPlatform);
                logClientInfoUpdatedEvent(sql, "clientPlatform", this.clientPlatform, clientPlatform);
            }

            if (clientTimeZone != null)
            {
                params++;
                statement.setString(params, clientTimeZone);
                logClientInfoUpdatedEvent(sql, "clientTimeZone", this.clientTimeZone, clientTimeZone);
            }

            if (clientHardwareType != null)
            {
                params++;
                statement.setString(params, clientHardwareType);
                logClientInfoUpdatedEvent(sql, "clientHardwareType", this.clientHardwareType, clientHardwareType);
            }

            if (clientOSVersion != null)
            {
                params++;
                statement.setString(params, clientOSVersion);
                logClientInfoUpdatedEvent(sql, "clientOSVersion", this.clientOSVersion, clientOSVersion);
            }

            if (clientIPAddress != null)
            {
                params++;
                statement.setString(params, clientIPAddress);
                logClientInfoUpdatedEvent(sql, "clientIPAddress", this.clientIPAddress, clientIPAddress);

                // also insert into user_ip_addr table
                this.updateIpAddress(sql, clientIPAddress);
            }

            if (clientDeviceIdentifier != null)
            {
                params++;
                statement.setString(params, clientDeviceIdentifier);
                logClientInfoUpdatedEvent(sql, "clientDeviceIdentifier", this.clientDeviceIdentifier, clientDeviceIdentifier);
            }

            params++;
            statement.setInt(params, this.id);

            statement.execute();

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
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private static <T> boolean shouldUpdateClientInfo(T before, T after)
    {
        return after != null && !after.equals(before);
    }

    private void logClientInfoUpdatedEvent(ReadWriteDbConnection sql, String type, Object before, Object after)
    {
        UserEventLogger.logEvent(sql,
                UserEventType.ClientInfoUpdated,
                type,
                this.id,
                String.format("%s|%s", before, after),
                TimeUtil.timeNow(),
                null);
    }

/*
    public static String getEmailFromUsername(String usrname)
    {
        ReadWriteDbConnection sql = null;
        try
        {
            sql = SQLUtil.getDatabaseConnection();
            return getEmailFromUsername(sql, usrname);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }

        return null;
    }
*/

    private void updateIpAddress(ReadWriteDbConnection sql, String ipAddr)
    {
        SqlPreparedStatement statement = null;

        try
        {
            statement = sql.prepareStatement("INSERT IGNORE INTO user_ip_addr (user_id, ip_addr) VALUES (?,?)");
            statement.setInt(1, this.id);
            statement.setString(2, ipAddr);

            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
        }
    }

    /**
     * added individual do not contact
     */
    public void updateDoNotContact(ReadWriteDbConnection sql) throws SQLException
    {
        assert (this.id != null && this.id > 0);
        CTLogger.infoStart("CTUser::updateDoNotContact()");
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("UPDATE users SET do_not_contact = ? WHERE id = ?");
            statement.setInt(1, this.getDoNotContact());
            statement.setInt(2, this.id);

            statement.execute();
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    public boolean checkAccessToken(String token)
    {
        return this.accessToken.equals(token);
    }

/*
    public static int getDaysSinceLastUse(int userId) throws SQLException
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = SQLUtil.getDatabaseConnection();
            return CTUser.getDaysSinceLastUse(sql, userId);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }
    }
*/

    public void generateAccessToken(ReadWriteDbConnection sql) throws SQLException
    {
        this.accessToken = newAccessToken();
        this.lastLogin = TimeUtil.timeNow();
        update(sql);
    }

/*    public static int getDoNotContact(int userId) throws SQLException
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = SQLUtil.getDatabaseConnection();
            return CTUser.getDoNotContact(sql, userId);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }
    }
*/

/*
    public static int getDoNotContact(Connection sql, int userId) throws SQLException
    {
        SqlPreparedStatement statement =
                sql.prepareStatement("SELECT do_not_contact FROM users WHERE id=?");
        statement.setInt(1, userId);

        ResultSet rs = statement.executeQuery();

        Integer dnc = 0; // defaults to contact
        if (rs.next())
        {
            dnc = rs.getInt("do_not_contact");
        }

        return dnc;
    }
*/

    public String generateWebsiteAccessToken(ReadWriteDbConnection sql) throws SQLException
    {
        this.websiteAccessToken = null;
        return getWebsiteAccessToken(sql, true);
    }

    public String getWebsiteAccessToken(ReadWriteDbConnection sql, boolean update) throws SQLException
    {
        boolean closeConnection = false;
        try
        {
            if (this.websiteAccessToken == null)
            {
                this.websiteAccessToken = newAccessToken();

                if (update)
                {
                    if (sql == null)
                    {
                        sql = new ReadWriteDbConnection();
                        closeConnection = true;
                    }

                    SqlPreparedStatement statement = sql.prepareStatement(
                            "UPDATE users SET website_access_token = ?, website_access_token_timestamp = ? WHERE id = ?");

                    statement.setString(1, this.websiteAccessToken);
                    statement.setTimestamp(2, new Timestamp((new Date()).getTime()));
                    statement.setInt(3, this.id);

                    statement.execute();
                }
            }
            return this.websiteAccessToken;
        }
        finally
        {
            if (closeConnection) SQLUtil.closeQuietly(sql);
        }
    }

    public void changePassword(ReadWriteDbConnection sql, String oldPassword, String newPassword) throws AuthenticationException, SQLException
    {
        if (this.password.checkPassword(oldPassword))
        {
            changePassword(sql, newPassword);
        }
        else
            throw new AuthenticationException();
    }

    public void changePassword(ReadWriteDbConnection sql, String newPassword) throws AuthenticationException, SQLException
    {
        CTLogger.infoStart("CTUser::changePassword() - username=" + this.getUsername());
        CTLogger.unindent();

        this.password = PasswordTuple.generateFromPassword(newPassword);
        update(sql);
        UserEventLogger.EventBuilder
                .create(UserEventType.PasswordChanged)
                .userId(this.id)
                .log(sql);
    }

    public void adminChangePassword(ReadWriteDbConnection sql, String newPassword, Integer adminId) throws AuthenticationException, SQLException
    {
        CTLogger.infoStart("CTUser::changePassword() - username=" + this.getUsername());
        CTLogger.unindent();

        this.password = PasswordTuple.generateFromPassword(newPassword);
        update(sql);
        UserEventLogger.EventBuilder.create(UserEventType.PasswordChanged).userId(this.id).notes("Admin: " + String.valueOf(adminId)).log(sql);
    }

    public String getFullName()
    {
        String name = this.firstName;

        if (this.getLastName() != null)
            if (name != null)
            {
                name += " " + this.getLastName();
            }
            else
            {
                name = this.getLastName();
            }

        return name;
    }

    /**
     * Gets first_name field for user. If first_name is null, returns username instead
     * If first_name contains a space, it returns the first word, assume the rest of it
     * is the middle or last name
     *
     * @return
     */
    public String getFirstName()
    {
        if (this.firstName == null)
            return this.getUsername();

        String[] name = this.firstName.split(" ");

        return name[0];
    }

    public String getEmailAccessToken()
    {
        try
        {
            return this.getEmailAccessToken(null, true);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return "";
        }
    }

    public void setDoNotContact(ReadWriteDbConnection sql, int val) throws SQLException
    {
        SqlPreparedStatement statement = null;

        try
        {
            statement = sql.prepareStatement("UPDATE users SET do_not_contact = ? WHERE id = ?");
            statement.setInt(1, val);
            statement.setInt(2, this.id);

            statement.execute();
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
        }
    }

    public boolean hasWebsiteTokenTimedOut()
    {
        // Subtract timeout period (8 hours) from the current time and make sure it is before the token creation date.
        Timestamp timeout = new Timestamp(new Date().getTime() - 1000 * 60 * 60 * 8);
        return !timeout.before(this.websiteAccessTokenTimeout);
    }

    public void changeEmail(ReadWriteDbConnection sql, String newEmail) throws SQLException
    {
        this.setEmail(newEmail);
        update(sql);
    }

/*
    public float getClientVersionNumber()
    {
        return MathUtil.versionStringToFloat(this.clientVersion);
    }
*/

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof CTUser))
        {
            return false;
        }
        CTUser user = (CTUser) obj;
        return this.id.equals(user.id);
    }

    @Override
    public String toString()
    {
        return String.format("%s: %s (%s %s), %s", this.id, this.getUsername(), this.firstName, this.getLastName(), this.getEmail());
    }

    /**
     * Puts all security code for passwords in one place (in case we decide to switch to a different
     * algorithm later on). A password is converted from text to two byte-arrays using a one-way hashing
     * algorithm
     *
     * @author ehsan
     */
    public static class PasswordTuple
    {
        private final transient byte[] hash;
        private final transient byte[] salt;

        PasswordTuple(byte[] hash, byte[] salt)
        {
            this.hash = hash;
            this.salt = salt;
        }

        public static PasswordTuple generateFromPassword(String password) throws AuthenticationException
        {
            try
            {
                byte[] hash;
                byte[] salt;

                salt = new byte[16];
                Random random = new Random();
                random.nextBytes(salt);

                KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 2048, 160);
                SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                hash = f.generateSecret(spec).getEncoded();

                return new PasswordTuple(hash, salt);
            }
            catch (Exception ex)
            {
                throw new AuthenticationException(ex.getMessage());
            }
        }

        public boolean checkPassword(String attemptedPassword) throws AuthenticationException
        {
            try
            {
                KeySpec spec = new PBEKeySpec(attemptedPassword.toCharArray(), this.salt, 2048, 160);
                SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                byte[] attemptedHash = f.generateSecret(spec).getEncoded();
                return Arrays.equals(attemptedHash, this.hash);
            }
            catch (Exception ex)
            {
                throw new AuthenticationException(ex.getMessage());
            }
        }

        public byte[] getHash()
        {
            return this.hash;
        }

        public byte[] getSalt()
        {
            return this.salt;
        }
    }

    private class UpdateUserInfoResult
    {
        boolean             success;
        Map<String, String> msgs = null;

        public boolean getSuccess()
        {
            return this.success;
        }

        public Map<String, String> getMessages()
        {
            return this.msgs;
        }

        void addMessage(String attribute, String details)
        {
            if (this.msgs == null)
                this.msgs = new HashMap<>();

            this.msgs.put(attribute, details);
        }
    }

    public Float getClientVersionNumber()
    {
        return MathUtil.versionStringToFloat(this.clientVersion);
    }
}
