package com.constanttherapy;

import com.constanttherapy.ServiceBase.InvalidPatientIdException;
import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadOnlyDbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.users.CTUser;
import com.constanttherapy.users.Clinician;
import com.constanttherapy.users.Patient;
import com.constanttherapy.util.CTLogger;

import java.sql.ResultSet;
import javax.security.sasl.AuthenticationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;

/**
 * very basic stuff that all of our services need
 */
abstract class SimpleService
{
    @Context
    public static UriInfo _uriInfo;

    protected CTUser user = null;

    static String constructSimpleMessage(String key, String message)
    {
        return String.format("{\"%s\":\"%s\"}", key, message);
    }

    static String constructDoubleMessage(String key1, String message1,
                                         String key2, String message2)
    {
        return String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}", key1, message1, key2, message2);
    }

	public static String constructSuccessMessage(String message) {
		return constructSimpleMessage("success", message);
	}

    public static String constructErrorMessage(String message, String type)
    {
        String msg = constructSimpleMessage(type, message);
        CTLogger.error(msg);
        return msg;
    }

    public static String constructErrorMessage(String message)
    {
        return constructErrorMessage(message, true);
    }

    public static String constructErrorMessage(String message, boolean writeToLog)
    {
        String msg = constructSimpleMessage("error", message);
        if (writeToLog) CTLogger.error(msg);
        return msg;
    }

    public static String constructErrorMessage(String message, Exception ex)
    {
        CTLogger.error(ex);
        if (ex instanceof AuthenticationException)
            return constructSimpleMessage("authError", ex.getMessage());
        else if (ex instanceof InvalidPatientIdException)
            return constructSimpleMessage("invalidPatientIdError", ex.getMessage());
        else
            return constructSimpleMessage("error", message);
    }

/*    @Deprecated
    Connection getValidatedConnectionByUsername(String token, String username) throws Exception
    {
        SQLUtil.initialize();
        // get a sql connection
        Connection sql = SQLUtil.getDatabaseConnection();
        // get userId from username
        int userId = CTUser.getIdFromUsername(sql, username);
        // return validated sql connection
        return validateConnection(sql, token, userId);
    }*/

    ReadWriteDbConnection getValidatedConnection(String token, String userIdString) throws Exception
    {
        int userId = Integer.parseInt(userIdString);
        return getValidatedConnection(token, userId);
    }

    /**
     * Common method to log in and get a sql connection.  Throws exceptions when the token is invalid.
     *
     * @param token
     * @param userId
     * @return A valid sql connection.  The user instance is also stored in a field.
     * @throws AuthenticationException
     * @throws SQLException
     */
    ReadWriteDbConnection getValidatedConnection(String token, int userId) throws Exception
    {
        ReadWriteDbConnection sql = new ReadWriteDbConnection();
        if (validateUser(sql, token, userId))
            return new ReadWriteDbConnection();
        else
            return null;
    }

    protected ReadWriteDbConnection getValidatedConnection(String token) throws Exception
    {
        ReadWriteDbConnection sql = new ReadWriteDbConnection();
        if (validateUser(sql, token, null))
            return sql;
        else
            return null;
    }

    ReadOnlyDbConnection getValidatedReadConnection(String token, int userId) throws Exception
    {
        ReadOnlyDbConnection sqlr = new ReadOnlyDbConnection();
        if (validateUser(null, token, userId))
            return sqlr;
        else
            return null;
    }

    protected ReadOnlyDbConnection getValidatedReadConnection(String token) throws Exception
    {
        ReadOnlyDbConnection sqlr = new ReadOnlyDbConnection();
        if (validateUser(null, token, null))
            return sqlr;
        else
            return null;
    }


    private boolean validateUser(ReadWriteDbConnection sql, String token, Integer userId) throws Exception
    {
        if (token == null || token.length() == 0)
        {
            throw new AuthenticationException("Invalid access token, userId=" + userId);
        }

        SQLUtil.initialize();

        if (sql == null)
            sql = new ReadWriteDbConnection();

        this.user = CTUser.getByToken(sql, token);

        // the trick here is that we want to close our connection before we throw any of these exceptions...
        if (this.user == null)
        {
            SQLUtil.closeQuietly(sql);
            throw new AuthenticationException("Invalid access token, userId=" + (userId == null ? "(null)" : userId));
        }
        else if (userId != null && this.user instanceof Patient && !userId.equals(this.user.getId()))
        {
            SQLUtil.closeQuietly(sql);
            throw new AuthenticationException("Access token associated with User " + this.user.getId() + ", not User " + userId);
        }
        else if (userId != null && this.user instanceof Clinician && !userId.equals(this.user.getId()) &&
                !((Clinician) this.user).hasPatient(sql, userId))
        {
            SQLUtil.closeQuietly(sql);
            throw new InvalidPatientIdException("Patient " + userId + " not associated with Clinician " + this.user.getId());
        }
        return true;
    }
}
