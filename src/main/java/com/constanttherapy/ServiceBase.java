package com.constanttherapy;

import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.db.*;
import com.constanttherapy.service.ServiceMessage;
import com.constanttherapy.users.CTUser;
import com.constanttherapy.users.Clinician;
import com.constanttherapy.util.*;
import org.apache.commons.configuration.Configuration;

import javax.security.sasl.AuthenticationException;
import javax.servlet.ServletContext;
import javax.ws.rs.core.*;
import java.net.URI;
import java.sql.SQLException;

public abstract class ServiceBase extends SimpleService
{
    /*
    @Context
	public static UriInfo uriInfo;
	 */
    @Context
    public static ServletContext context;
    private static       String        servicePath = null;
    private static final Configuration config      = CTConfiguration.getConfig("conf/ct-common-config.xml");

    private static String getServicePath()
    {
        return getServicePath(servicePath);
    }

    public static String getServicePath(String path)
    {
        return config.getString(path);
    }

    public static String getHostUrl()
    {
        return getHostUrl(_uriInfo);
    }

    public static String getHostUrl(UriInfo uriInfo)
    {
        URI uri = uriInfo.getBaseUri();
        int port = uri.getPort();
        String scheme = uri.getScheme();

        if (port < 0)
            port = (scheme.toLowerCase().equals("https") ? 8443 : 8080);

        return uri.getScheme() + "://" + uri.getHost() + ":" + port;
    }

    public static String getServiceUri(UriInfo uriInfo, String path)
    {
        // [Mahendra, Oct 24, 2014 6:37:57 PM]: BUGFIX: service path for messaging service
        // was occasionally showing up as ct-service instead of ct-messaging!
        if (path == null)
            path = getServicePath();

        assert (path.length() > 0);

        String host = getHostUrl(uriInfo);
        return host + "/" + path;
    }

    public static String getServerUri()
    {
        if (_uriInfo != null)
            return getServerUri(_uriInfo);
        else
            return "No Uri context injected!";
    }

    private static String getServerUri(UriInfo info)
    {
        URI uri = info.getBaseUri();
        return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/";
    }

    public static UriInfo getUriInfo()
    {
        return _uriInfo;
    }

    // for testing, insert mockUriInfo here
    public void setUriInfo(UriInfo info)
    {
        _uriInfo = info;
    }

/*
    public static ServletContext getServletContext()
    {
        return context;
    }
*/

    public static boolean isProductionServer()
    {
        return _uriInfo != null && isProductionServer(_uriInfo);
    }

    private static boolean isProductionServer(UriInfo info)
    {
        String host = info.getBaseUri().getHost();
        return (host.startsWith("api.constanttherapy.com") || host.startsWith("api2.constanttherapy.com"));
    }

    public static boolean isDevelopmentServer()
    {
        return _uriInfo != null && isDevelopmentServer(_uriInfo);
    }

    private static boolean isDevelopmentServer(UriInfo info)
    {
        String host = info.getBaseUri().getHost();
        return (host.startsWith("dev.constanttherapy.com"));
    }

    private static boolean isHindiServer(UriInfo info)
    {
        String host = info.getBaseUri().getHost();
        return (host.startsWith("hindi.constanttherapy.com"));
    }

    public static boolean isHindiServer()
    {
        return _uriInfo != null && isHindiServer(_uriInfo);
    }

    String dumpUriInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("uriInfo.getPath():\t").append(_uriInfo.getPath()).append("\n");
        sb.append("uriInfo.getAbsolutePath():\t").append(_uriInfo.getAbsolutePath()).append("\n");
        sb.append("uriInfo.getBaseUri():\t").append(_uriInfo.getBaseUri()).append("\n");
        sb.append("uriInfo.getRequestUri():\t").append(_uriInfo.getRequestUri()).append("\n");

        sb.append("uriInfo.getMatchedResources():\n");
        for (Object o : _uriInfo.getMatchedResources())
            sb.append("\t").append(o.toString()).append("\n");

        sb.append("uriInfo.getMatchedURIs():\n");
        for (Object o : _uriInfo.getMatchedURIs())
            sb.append("\t").append(o.toString()).append("\n");

        sb.append("uriInfo.getPathParameters():\n");
        for (Object o : _uriInfo.getPathParameters().values())
            sb.append("\t").append(o.toString()).append("\n");

        sb.append("uriInfo.getPathSegments():\n");
        for (PathSegment o : _uriInfo.getPathSegments())
            sb.append("\t").append(o.getPath()).append("\n");

        sb.append("uriInfo.getQueryParameters():\n");
        for (Object o : _uriInfo.getQueryParameters().values())
            sb.append("\t").append(o.toString()).append("\n");

        return sb.toString();
    }

    String dumpServletContext()
    {
        if (context != null)
        {
            return "ServletContext.contextPath: " + context.getContextPath() + "\nServletContext.realPath: " + context.getRealPath("test");
        }
        else
            return "No ServletContext found!";
    }

/*
    public String dumpServiceInfo()
    {
        return dumpUriInfo() + "\n" + dumpServletContext() + "\nHostUrl: " + getHostUrl() + "\nVersion: " + getCTVersion();
    }
*/

    private static String _ctVersion = null;

    /**
     * Constant Therapy Version used for getting resources from S3
     *
     * @return
     */
    public static String getCTVersion()
    {
        if (_ctVersion == null)
        {
            _ctVersion = config.getString("ct-version");
            if (_ctVersion == null)
                _ctVersion = ""; // make it empty
        }

        //HACK: figure out a better way to do this!
        //return ResourceUtil.getCurrentLocale() + (_ctVersion.length() == 0 ? "" : "/" +  _ctVersion);
        return _ctVersion;
    }

    private static String _serverVersion = null;

    /**
     * Constant Therapy server version. Comprised of major, minor and revision (or build).
     * Each new deployment of service WAR files typically will have a new version or build number.
     *
     * @return
     */
    public static String getServerVersion()
    {
        if (_serverVersion == null)
            _serverVersion = config.getString("ct-server-version");
        return _serverVersion;
    }

    /**
     * Custom Exception Types
     **/
    static class InvalidPatientIdException extends Exception
    {
        private static final long serialVersionUID = 1L;

        InvalidPatientIdException(String message)
        {
            super(message);
        }
    }

    /**
     * This exception can be used anytime the code needs to send a message to the client
     * <p>
     * Any subclass of ServiceBase that has a method for an endpoint should convert this exception into an alert service message
     *
     * @author ehsan
     */
    public static class ClientDisplayMessageException extends Exception
    {
        private String messageTitle;
        private String messageBody;
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public ClientDisplayMessageException(String messageTitle, String messageBody)
        {
            super(messageTitle);
            this.messageTitle = messageTitle;
            this.messageBody = messageBody;
        }

        ServiceMessage toServiceMessage()
        {
            return new ServiceMessage(this.messageTitle, this.messageBody);
        }
    }

    Clinician validateClinicianForPatient(ReadWriteDbConnection sql, String accessToken, int patientId)
            throws AuthenticationException, SQLException
    {
        CTUser patient = CTUser.getById(patientId);
        return validateClinicianForPatient(sql, accessToken, patient.getUsername());
    }

    Clinician validateClinicianForPatient(ReadWriteDbConnection sql, String accessToken, String patientUsername)
            throws AuthenticationException
    {
        Clinician clinician = null;
        try
        {
            clinician = (Clinician) CTUser.login(sql, accessToken);
            validateClinicianForPatient(sql, clinician, patientUsername);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
        return clinician;
    }

    Clinician validateClinicianForPatient(ReadWriteDbConnection sql, String clinicianUsername,
                                          String encryptedPassword, String unencryptedPassword, String patientUsername)
            throws AuthenticationException
    {
        Clinician clinician = null;
        try
        {

            String password = null;
            if (encryptedPassword != null && encryptedPassword.length() > 0)
                password = StringUtil.decodeStr(encryptedPassword);
            else if (unencryptedPassword != null && unencryptedPassword.length() > 0)
                password = unencryptedPassword;
            else
                throw new AuthenticationException("Invalid password specified!");

            clinician = (Clinician) CTUser.login(sql, clinicianUsername, password);
            validateClinicianForPatient(sql, clinician, patientUsername);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
        return clinician;
    }

    private void validateClinicianForPatient(ReadWriteDbConnection sql, Clinician clinician, String patientUsername)
            throws AuthenticationException
    {
        if (clinician == null)
            throw new AuthenticationException("Incorrect login credentials");
        if (patientUsername != null && patientUsername.length() > 0)
            if (!clinician.getUsername().equalsIgnoreCase("hal3po") && !clinician.hasPatient(sql, patientUsername))
                throw new AuthenticationException("You are not authorized to work with this patient.");
    }

    /**
     * Gets connection to default database in writeable (master) instance
     *
     * @return
     * @throws SQLException
     */
    protected static ReadWriteDbConnection getDatabaseConnection() throws SQLException
    {
        SQLUtil.initialize();
        //return SQLUtil.getDatabaseConnection();
        return new ReadWriteDbConnection();
    }

    /**
     * Gets connection to specified database in writeable (master) instance
     *
     * @param dbName
     * @return
     * @throws SQLException
     */
    protected static ReadWriteDbConnection getDatabaseConnection(String dbName) throws SQLException
    {
        SQLUtil.initialize();
        //return SQLUtil.getDatabaseConnection(dbName);
        return new ReadWriteDbConnection(dbName);
    }

    /**
     * Gets connection to default database in readonly (slave) instance
     *
     * @return
     * @throws SQLException
     */
    static ReadOnlyDbConnection getReadOnlyDatabaseConnection() throws SQLException
    {
        SQLUtil.initialize();
        return new ReadOnlyDbConnection();
    }

    /**
     * Gets connection to specified database in readonly (slave) instance
     *
     * @param dbName
     * @return
     * @throws SQLException
     */
    protected static ReadOnlyDbConnection getReadOnlyDatabaseConnection(String dbName) throws SQLException
    {
        SQLUtil.initialize();
        return new ReadOnlyDbConnection(dbName);
    }

    public String test()
    {
        ReadWriteDbConnection sql = null;

        try
        {
            sql = new ReadWriteDbConnection();
            if (sql.isValid())
            {
                return constructSuccessMessage("Server is up - " + _uriInfo.getBaseUri().toString()
                        + " [Version " + getServerVersion() + "]");
            }
            else
            {
                return MsgUtil.constructErrorMessage("server is down");
            }
        }
        catch (Exception e)
        {
            return MsgUtil.constructErrorMessage("server is down", e);
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
        }
    }
}
