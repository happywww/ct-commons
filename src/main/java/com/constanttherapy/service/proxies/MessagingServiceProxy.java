package com.constanttherapy.service.proxies;

import com.constanttherapy.service.ServiceMessage;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.GsonHelper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.TreeMap;

public class MessagingServiceProxy extends ServiceProxyBase
{
    /**
     * token used to mark message body in JSON/map returned by client message endpoint
     */
    public static final String KEY_MSG_BODY = "msgBody";
    /**
     * token used to mark message subject in JSON/map returned by client message endpoint
     */
    public static final String KEY_MSG_SUBJECT = "msgSubject";

    public final static String MandrillMetadataServerTypeProduction = "prod";
    public final static String MandrillMetadataServerTypeDevelopment = "dev";

    public MessagingServiceProxy(UriInfo uriInfo)
    {
        super(uriInfo, "ct-messaging-service-path");

        CTLogger.debug("MessageServiceProxy constructor - serviceUri = " + this.serviceUri);
    }

    public Response sendMessageUsingBody(MessagingServiceProxyArgs args)
    {
        return sendMessageUsingBody(args.recipients, args.subject, args.body, args.ccSupport, args.fromName, args.fromEmail);
    }

    public Response sendMessageUsingBody(String recipients, String subject, String body)
    {
        return sendMessageUsingBody(recipients, subject, body, false, null, null);
    }

    /**
     * Sends email using the specified subject and email body.
     *
     * @param recipients Comma-separated list of usernames or userIds
     * @param subject    Email subject
     * @param body       Email body
     * @param ccSupport  If true, bcc support on the email
     * @return
     */
    private Response sendMessageUsingBody(String recipients, String subject, String body, boolean ccSupport, String fromName, String fromEmail)
    {
        try
        {
            CTLogger.infoStart("MessagingServiceProxy::sendMessageUsingBody() - recipients=" + recipients);
            Map<String, String> queryParams = new TreeMap<>();
            queryParams.put("recipients", recipients);
            queryParams.put("subject", subject);

            if (fromName != null)
                queryParams.put("fromname", fromName);

            if (fromEmail != null)
                queryParams.put("fromemail", fromEmail);

            if (ccSupport)
                queryParams.put("ccsupport", "T");

            String resp = doServicePost("api/sendmail", queryParams, body);
            return Response.ok(resp).build();
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return Response.serverError().build();
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    public Response sendMessageUsingTemplate(String recipients, String templateType, String replacementTokensJson)
    {
        // defaults: ccSupport = true; fromEmail = null; toEmail = null; showStats = false
        // serverType = MandrillMetadataServerTypeProduction
        return sendMessageUsingTemplate(recipients, templateType, replacementTokensJson, true, null, null, false,
                MandrillMetadataServerTypeProduction);
    }

    public Response sendMessageUsingTemplate(MessagingServiceProxyArgs args)
    {
        return sendMessageUsingTemplate(args.recipients, args.templateType, args.replacementTokensJson,
                args.ccSupport, args.fromName, args.fromEmail, args.showStats, args.serverType);
    }

    /**
     * Sends a message using the specified template
     *
     * @param recipients            Comma-separated list of usernames or userIds
     * @param templateType          Name of template to be used
     * @param replacementTokensJson Replacement tokens in a JSON formatted map
     * @param ccSupport             If true, bcc support on the email
     * @return
     */
    private Response sendMessageUsingTemplate(String recipients, String templateType, String replacementTokensJson,
                                              boolean ccSupport, String fromName, String fromEmail, boolean showStats, String serverType)
    {
        try
        {
            CTLogger.infoStart("MessagingServiceProxy::sendMessageUsingTemplate() - templateType=" + templateType);

            Map<String, String> queryParams = new TreeMap<String, String>();

            if (recipients != null)
                queryParams.put("recipients", recipients);

			/*
            if (replacementTokensJson != null)
				queryParams.put("tokens", replacementTokensJson);
			*/

            if (ccSupport)
                queryParams.put("ccsupport", "true");

            if (fromName != null)
                queryParams.put("fromname", fromName);

            if (fromEmail != null)
                queryParams.put("fromemail", fromEmail);

            queryParams.put("showstats", showStats ? "1" : "0");

            String method = "api/sendmail/usingtemplate/" + templateType;

            CTLogger.debug(String.format("Invoking service method: %s/%s", this.serviceUri, method), 2);

            String resp;
            if (replacementTokensJson == null)
                resp = doServiceGet(method, queryParams);
            else
                resp = doServicePost(method, queryParams, replacementTokensJson);

            return Response.ok(resp).build();
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return Response.serverError().build();
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    /**
     * for folks who want to ignore the subject line and only get the message body as a string
     * ... note that this only gets a variable-substituted message body, it doesn't send it to anyone
     */
    public String getMessageBodyFromTemplate(String templateType, Map<String, String> replacementTokensMap)
    {
        ServiceMessage theMsg = getMessageFromTemplate(templateType, replacementTokensMap);
        return theMsg.getMessage();
    }

    /**
     * applies a template to a set of replacement tokens and returns the result as a ServiceMessage
     * ... does not send this to anyone!  used either as a utility function by other Mailer
     * functions or used to help server send a message to the client for immediate display in
     * the client UI as a popup
     */
    public ServiceMessage getMessageFromTemplate(String templateType, Map<String, String> replacementTokensMap)
    {
        return getMessageFromTemplateUsingJson(templateType, GsonHelper.toJson(replacementTokensMap));
    }

    /**
     * applies a template to a set of replacement tokens and returns the result as a ServiceMessage
     * ... does not send this to anyone!  used either as a utility function by other Mailer
     * functions or used to help server send a message to the client for immediate display in
     * the client UI as a popup
     */
    public ServiceMessage getMessageFromTemplateUsingJson(String templateType, String replacementTokensJson)
    {
        try
        {
            Map<String, String> queryParams = new TreeMap<String, String>();

            if (replacementTokensJson != null)
                queryParams.put("tokens", replacementTokensJson);

            String resp = doServiceGet("api/message/apply/" + templateType, queryParams, MediaType.APPLICATION_JSON);

            // resp is JSON map of body, subject
            Map<String, String> respMap = GsonHelper.mapFromJson(resp);
            String msgBody = respMap.get(KEY_MSG_BODY);
            String msgSubject = respMap.get(KEY_MSG_SUBJECT);
            ServiceMessage theMsg = null;
            if ((null == msgBody) || (null == msgSubject))
            {
                // oof
                CTLogger.error("Failed to retrieve message from template " + templateType);
            }
            else
            {
                theMsg = new ServiceMessage(msgSubject, msgBody);
            }

            return theMsg;
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return null;
        }
    }

    public Response botSendScheduleUpdateEmailToPatient(String clinicianId, String patientUsername, String accessToken)
    {
        try
        {
            CTLogger.infoStart("MessagingServiceProxy::botSendScheduleUpdateEmailToPatient() - patientUsername=" + patientUsername);

            Map<String, String> queryParams = new TreeMap<>();

            queryParams.put("c", clinicianId);
            queryParams.put("token", accessToken);
            queryParams.put("u", patientUsername);

            String method = "api/sendscheduleupdateemail";

            CTLogger.debug(String.format("Invoking service method: %s/%s", this.serviceUri, method), 2);

            String resp;
            resp = doServiceGet(method, queryParams);

            return Response.ok(resp).build();
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return Response.serverError().build();
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    @Override
    protected String doServiceGet(String method, Map<String, String> params)
    {
        return doServiceGet(method, params, MediaType.TEXT_PLAIN);
    }

    @Override
    protected String doServicePost(String method, Map<String, String> params, String postBody)
    {
        return doServicePost(method, params, postBody, MediaType.TEXT_PLAIN);
    }
}
