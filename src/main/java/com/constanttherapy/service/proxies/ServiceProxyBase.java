package com.constanttherapy.service.proxies;

import com.constanttherapy.ServiceBase;
import com.constanttherapy.util.CTLogger;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.*;
import java.net.URLEncoder;
import java.util.Map;

public abstract class ServiceProxyBase // extends ServiceBase
{
    //@Context protected UriInfo _uriInfo;

    /**
     * http://<hostname>:<port>/<service_context>/<service_path>/<resource|method>
     */
    protected String baseUri = null;
    /**
     * Key in the config file that points to the service name
     */
    protected String serviceNameKey;

    private final UriInfo uriInfo; // host Uri
    protected String servicePath; // the service name
    protected String serviceUri; // full service name

    public ServiceProxyBase(UriInfo ui, String path)
    {
        this.uriInfo = ui;
        this.servicePath = ServiceBase.getServicePath(path);
        this.serviceUri = ServiceBase.getServiceUri(this.uriInfo, this.servicePath);
    }

    protected String doServiceGet(String method, Map<String, String> params, String accept)
    {
        try
        {
            ClientConfig config = new DefaultClientConfig();
            Client client = Client.create(config);

            WebResource service = client.resource(this.serviceUri).path(method);

            // [Mahendra, Apr 23, 2015 6:09:53 PM]: Jersey 1.19 throws
            // javax.ws.rs.core.UriBuilderException: java.net.URISyntaxException: Illegal character in query at index
            // Apparently it is less forgiving for characters that should be encoded (like spaces and double quotes)
            // than previous versions. Reverted back to Jersey 1.17 for now.
            // TODO: migrate to Jersey 2.0. This will take considerable refactoring to the server API.

            MultivaluedMap<String, String> qp = getQueryParams(params);

            ClientResponse response = service.queryParams(qp)
                    .accept(accept)
                    .get(ClientResponse.class);

            if (CTLogger.isDebugEnabled())
            {
                String s = response.toString();
                if (s.length() > 255)
                    s = s.substring(0, 255);
                CTLogger.debug("ServiceProxyBase::doServiceGet() - response=" + s);
            }
            return response.getEntity(String.class);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return null;
        }
    }

    protected String doServiceGet(String method, Map<String, String> params)
    {
        return doServiceGet(method, params, MediaType.APPLICATION_JSON);
    }

    protected String doServicePost(String method, Map<String, String> params, String postBody, String accept)
    {
        try
        {
            ClientConfig config = new DefaultClientConfig();
            Client client = Client.create(config);

            WebResource service = client.resource(this.serviceUri).path(method);

            MultivaluedMap<String, String> qp = getQueryParams(params);

            ClientResponse response = service.queryParams(qp)
                    .accept(accept)
                    .type(MediaType.TEXT_PLAIN)
                    .post(ClientResponse.class, postBody);

            if (CTLogger.isDebugEnabled())
            {
                String s = response.toString();
                if (s.length() > 255)
                    s = s.substring(0, 255);
                CTLogger.debug("ServiceProxyBase::doServicePost() - response=" + s);
            }

            return response.getEntity(String.class);

        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return null;
        }
    }

    protected String doServicePost(String method, Map<String, String> params, String postBody)
    {
        return doServicePost(method, params, postBody, MediaType.APPLICATION_JSON);
    }

    // convert Map into a MultivaluedMap to import into query params for the service call
    private MultivaluedMap<String, String> getQueryParams(Map<String, String> params)
    {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        for (String key : params.keySet())
        {
            try
            {
                map.add(key, URLEncoder.encode(params.get(key), "UTF-8"));
            }
            catch (java.io.UnsupportedEncodingException e)
            {
                CTLogger.error(e);
            }
        }
        return map;
    }

    public UriInfo getUriInfo()
    {
        return this.uriInfo;
    }

    public String getHostUrl()
    {
        return ServiceBase.getHostUrl(this.uriInfo);
    }
}
