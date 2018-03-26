package com.constanttherapy.util;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.*;
import java.util.AbstractMap;
import java.util.List;

public class HttpHelper
{

    /**
     * prevent instantiation
     */
    private HttpHelper()
    {
    }

    /**
     * tells whether the HTTP request is from one of our own CT servers
     * as opposed to being from a client
     * ... note that if the requestor is capable of messing with the HTTP
     * headers to spoof its origin, it could fool this function
     */
    public static boolean isFromCTServer(HttpServletRequest request)
    {
        boolean result = false;

        String remoteHost = request.getRemoteHost();
        String remoteAddr = request.getRemoteAddr();
        if (remoteHost.contains("constanttherapy.com") ||
                (remoteHost.equals("107.20.180.207")) ||
                (remoteAddr.equals("107.20.180.207")) ||
                (remoteHost.equals("107.20.182.46")) ||
                (remoteAddr.equals("107.20.182.46")))
        {
            result = true;
        }

        // done
        return result;
    }


    /**
     * gets a string response from an HTTP GET ... assumes that encoding is UTF-8
     */
    public static String doSimpleHttpGetForJsonResponse(String theUrl, List<AbstractMap.SimpleEntry<String, String>> queries)
    {
        assert (theUrl != null);
        StringBuilder returnJson = null;

        try
        {
            // build query if any
            StringBuilder queryString = new StringBuilder();
            if (null != queries)
            {
                for (int w = 0; w < queries.size(); w++)
                {
                    if (w > 0) queryString.append("&");
                    queryString.append(queries.get(w).getKey() + "="
                            + URLEncoder.encode(queries.get(w).getValue(), "UTF-8"));
                }
            }

            // assemble URL
            String fullUrl = null;
            if (0 == queryString.length()) fullUrl = theUrl;
            else fullUrl = theUrl + "?" + queryString.toString();

            // open connection for HTTP GET (that's the response ... POST would require more settings)
            URLConnection connection = null;
            connection = new URL(fullUrl).openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            // receive response (triggers send then waits for response)
            InputStream response = connection.getInputStream();

            int status = ((HttpURLConnection) connection).getResponseCode();
            if (status == HttpURLConnection.HTTP_OK)
            {

                String contentType = connection.getHeaderField("Content-Type");
                String charset = null;
                for (String param : contentType.replace(" ", "").split(";"))
                {
                    if (param.startsWith("charset="))
                    {
                        charset = param.split("=", 2)[1];
                        break;
                    }
                }

                // OK, parse response
                // if we get the expected text response, the charset will not be null
                if (charset != null)
                {
                    returnJson = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));
                    try
                    {
                        for (String line; (line = reader.readLine()) != null; )
                        {
                            returnJson.append(line);
                        }
                    }
                    finally
                    {
                        try
                        {
                            reader.close();
                        }
                        catch (IOException logOrIgnore)
                        {
                        }
                    }
                }
                else
                {
                    // We only expect text, which will have a charset, so this is a fail
                    returnJson = null;
                }

                // null is our "fail"
                if (0 == returnJson.length()) returnJson = null;

            }
            else
            {

                CTLogger.error("HTTP GET failed for URL: " + theUrl);
                // null is our "fail"
                returnJson = null;
            }

        }
        catch (UnsupportedEncodingException e)
        {
            // won't happen, of course UTF-8 is OK
            returnJson = null;
            e.printStackTrace();
        }
        catch (MalformedURLException e)
        {
            returnJson = null;
            e.printStackTrace();
        }
        catch (IOException e)
        {
            returnJson = null;
            e.printStackTrace();
        }

        // done
        return (returnJson == null) ? null : returnJson.toString();
    }

}
