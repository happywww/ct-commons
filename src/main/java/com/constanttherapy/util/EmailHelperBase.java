package com.constanttherapy.util;

import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.db.*;
import com.constanttherapy.service.proxies.MessagingServiceProxy;
import com.constanttherapy.service.proxies.MessagingServiceProxyArgs;
import com.constanttherapy.users.CTUser;
import com.constanttherapy.users.Patient;
import com.constanttherapy.users.payment.StripeHelper;
import com.constanttherapy.users.payment.SubscriptionHelper;
import com.constanttherapy.util.TimeUtil.TimeUnits;
import org.apache.commons.configuration.Configuration;

import javax.ws.rs.core.UriInfo;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Base EmailHelper class that is used by implementations in the service projects.
 * It provides helper functions to add replacement tokens.
 *
 * @author mahendra
 */
public class EmailHelperBase
{
    // used for testing (dry runs through email templates)
    // public static String testUsername = "patient3";
    static final String support_email = CTConfiguration.getConfig("conf/ct-common-config.xml").getString("support-mail");
    static String devSupportMailList;
    static UriInfo uriInfo       = null;
    protected static MessagingServiceProxy msgProxy      = null;

    public static void initialize(MessagingServiceProxy proxy)
    {
        CTLogger.info("EmailHelperBase::initialize() - hostUrl=" + proxy.getHostUrl(), true);
        uriInfo = proxy.getUriInfo();
        msgProxy = proxy;
        initDevSupportMailList();
    }

    public static void initialize(UriInfo info)
    {
        uriInfo = info;
        msgProxy = new MessagingServiceProxy(uriInfo);
        initDevSupportMailList();
        CTLogger.info("EmailHelperBase::initialize() - hostUrl=" + msgProxy.getHostUrl(), true);
    }

    public static boolean isInitialized()
    {
        return (uriInfo != null && msgProxy != null);
    }

	/*
    protected static UriInfo getServerUriInfo()
	{
		if (uriInfo == null)
			throw new IllegalStateException("EmailHelper has not been initialized!");

		return uriInfo;
	}
	*/

	private static void initDevSupportMailList()
    {
        Configuration config = CTConfiguration.getConfig("conf/ct-common-config.xml");
        int size = config.getList("dev-support-mail-list.email").size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++)
        {
            if (i > 0) {sb.append(",");}

            String email = config.getString("dev-support-mail-list.email(" + i + ")");
            sb.append(email);
        }

        devSupportMailList = sb.toString();
    }

    protected static MessagingServiceProxy getMessageServiceProxy()
    {
        if (msgProxy == null)
            throw new IllegalStateException("EmailHelper has not been initialized!");

        return msgProxy;
    }

    /**
     * Creates a map with standard tokens (firstname, lastname, email, username) for the specified user.
     *
     * @param user
     * @return
     */
    protected static Map<String, String> getReplacementTokens(CTUser user)
    {
        Map<String, String> tokens = new HashMap<String, String>();

        // use username if firstname is null or empty
        tokens.put("$(firstname)",
                (user.firstName == null || user.firstName.length() == 0 ? user.getUsername() : user.firstName));

        tokens.put("$(lastname)",
                (user.getLastName() == null || user.getLastName().length() == 0 ? "" : user.getLastName()));

        tokens.put("$(username)", user.getUsername());

        tokens.put("$(email)", user.getEmail());

        return tokens;
    }

    protected static Map<String, String> addReplacementTokensForPatientStats(ReadWriteDbConnection sql,
                                                                             Map<String, String> tokens,
                                                                             int userId, SubscriptionHelper stripey)
    {
        try
        {
            Patient patient = (Patient) Patient.getById(userId);
            tokens = addReplacementTokensForPatientStats(sql, tokens, patient, stripey);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }

        return tokens;
    }

    public static Map<String, String> addReplacementTokensForPatientInfo(ReadWriteDbConnection sql, Map<String, String> tokens, Patient patient)
    {
        CTLogger.infoStart("EmailHelperBase::addReplacementTokensForPatientStats() - patientId=" + patient.getId());

        try
        {
            if (tokens == null)
                tokens = new HashMap<>();

            String username = tokens.get("$(username)");

            // Only populate tokens if $(username) is not already in the map
            // or is the same as the current patient (otherwise, we'd be overriding
            // data that was intended for another user)
            if (username == null || username.equals(patient.getUsername()))
            {
                tokens.put("$(username)", patient.getUsername());

                tokens.put("$(user_id)", Integer.toString(patient.getId()));

                tokens.put("$(email_access_token)", patient.getEmailAccessToken(sql, true));

                tokens.put("$(firstname)", (patient.firstName == null || patient.firstName.length() == 0 ? patient.getUsername()
                        : patient.firstName));
            }
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            CTLogger.unindent();
        }

        return tokens;
    }

    public static Map<String, String> addReplacementTokensForPatientStats(ReadWriteDbConnection sql,
                                                                          Map<String, String> tokens,
                                                                          Patient patient,
                                                                          SubscriptionHelper stripey)
    {
        CTLogger.infoStart("EmailHelperBase::addReplacementTokensForPatientStats() - patientId=" + patient.getId());

        try
        {
            tokens = addReplacementTokensForPatientInfo(sql, tokens, patient);

            List<String> deficits = patient.getDeficits(sql);

            if (deficits.size() > 0)
                tokens.put("$(deficits)", StringUtil.listToCSV(deficits, true));
            else
                tokens.put("$(deficits)",
                        "Reading, Writing, Naming, Attention, Memory, Comprehension, Visual Processing and Problem Solving ");

            // put data into the boxes on the left
            if (stripey == null)
            {
                stripey = StripeHelper.create(sql, patient.getId());
            }

            // default days to -1
            int days = -1;

            // .. and get trial days left if user is trialing
            if (stripey.isTrialing())
                days = stripey.getDaysUntilExpiration();
            tokens.put("$(trial_days_left)", Integer.toString(days));

            tokens.put("$(expiration_date)", TimeUtil.getDateFromTimestamp(stripey.getExpirationTimestamp()));

            int taskCount = Patient.getCompletedTaskCount(sql, patient.getId(), true);
            tokens.put("$(task_count)", Integer.toString(taskCount));

            if (taskCount > 50)
                tokens.put("$(perf_table)", generateTaskResultsTable(sql, patient.getId()));
            else
                tokens.put("$(perf_table)", ""); // hide table

            double avgScore = Patient.getAverageScore(sql, patient.getId(), 600);
            String score = String.format("%s%%", (int) (avgScore * 100));
            tokens.put("$(avg_score)", score);

            if (taskCount > 10)
            {
                // show task count in first box
                tokens.put("$(td1_caption)", "TASKS DONE");
                tokens.put("$(td1_data)", Integer.toString(taskCount));
                tokens.put("$(td1_backcolor)", "#daeef3");

                if (avgScore > 0.4) // TODO: should this be configurable
                {
                    // show accuracy in second box
                    tokens.put("$(td2_caption)", "ACCURACY");
                    tokens.put("$(td2_data)", score);
                    tokens.put("$(td2_backcolor)", "#daeef3");

                    // and days left in third box
                    addTrialDaysLeftBox(tokens, 3, days);
                }
                else
                {
                    // and days left in second box
                    addTrialDaysLeftBox(tokens, 2, days);

                    // and hide the third
                    hideTableDataElement(tokens, 3);
                }
            }
            else
            {
                // only show days left in first box
                addTrialDaysLeftBox(tokens, 1, days);

                // and hide the second and third boxes
                hideTableDataElement(tokens, 2);
                hideTableDataElement(tokens, 3);
            }
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            CTLogger.unindent();
        }

        return tokens;
    }

    private static void addTrialDaysLeftBox(Map<String, String> tokens, int td, int days)
    {
        if (days > 0)
        {
            // and days left in third box
            tokens.put("$(td" + td + "_caption)", "DAYS LEFT");
            tokens.put("$(td" + td + "_data)", Integer.toString(days));
            tokens.put("$(td" + td + "_backcolor)", "#daeef3");
        }
        else
        {
            hideTableDataElement(tokens, td);
        }
    }

    private static void hideTableDataElement(Map<String, String> tokens, int td)
    {
        tokens.put("$(td" + td + "_caption)", "");
        tokens.put("$(td" + td + "_data)", "");
        tokens.put("$(td" + td + "_backcolor)", "transparent");
    }

    protected static void sendMailUsingProxy(CTUser user, String template, Map<String, String> tokens, String testUser, boolean ccSupport)
    {
        MessagingServiceProxyArgs args = new MessagingServiceProxyArgs();

        args.templateType = template;
        args.replacementTokensJson = GsonHelper.toJson(tokens);
        args.ccSupport = ccSupport;

        if (testUser != null)
        {
            args.recipients = testUser;
            args.serverType = MessagingServiceProxy.MandrillMetadataServerTypeDevelopment;
        }
        else
        {
            args.recipients = user.getEmail();
            args.serverType = MessagingServiceProxy.MandrillMetadataServerTypeProduction;
        }

        getMessageServiceProxy().sendMessageUsingTemplate(args);
    }

    private static String generateTaskResultsTable(DbConnection sqlr, int patientId) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            /*final String query = "select * from " +
					"(select b.display_name task_name, a.task_level, count(*) task_count, " +
					" round(avg(accuracy) * 100, 0) accuracy, skipped from responses a" +
					" join task_types b on a.task_type_id = b.id" +
					" where patient_id = " + patientId +
					" group by a.task_type_id, a.task_level) a" +
					" where skipped = 0 and accuracy > 50 " +
					" order by task_count desc, accuracy " +
					" limit 10";*/

            final String query = "CALL GetTopTasksForPatient(?, ?);";
            statement = sqlr.prepareStatement(query);
            statement.setInt(1, patientId);
            statement.setInt(2, 10);  // limit to 10 tasks

            rs = statement.executeQuery();

            StringBuilder sb = new StringBuilder();

            sb.append("Here are some of your performance results:<p>")
                    .append("<table style='border:1px solid gray;'><tbody>\n")
                    // .append("<tr style=\"border:1px solid gray;background-color:#0b64a2;font-family:Calibri,Verdana,sans-serif;color: white;font-size:12px\">")
                    .append("<tr style=\"border:1px solid gray;background-color:#0b64a2;font-family:inherit;color: white;font-size:inherit\">")
                    .append("<th>Task</th><th style='padding: 1px 8px;'>Level</th>")
                    .append("<th style='padding: 1px 8px;'>Count</th>")
                    .append("<th style='padding: 1px 8px;'>Accuracy</th></tr>\n");

            // table with performance
            while (rs.next())
            {
                // sb.append("<tr style=\"border:1px solid gray;font-family:Calibri,Verdana,sans-serif;color:#0B64A2;font-size:12px;\">");
                sb.append("<tr style=\"border:1px solid gray;font-family:inherit;color:#0B64A2;font-size:inherit;\">");
                sb.append("<td style=\"font-family:inherit;font-size:inherit\">").append(rs.getString("task_name")).append("</td>");
                sb.append("<td style=\"font-family:inherit;font-size:inherit\">").append(rs.getString("task_level"))
                        .append("</td>");
                sb.append("<td style=\"font-family:inherit;font-size:inherit\">").append(rs.getString("task_count"))
                        .append("</td>");
                sb.append("<td style=\"font-family:inherit;font-size:inherit\">").append(rs.getString("accuracy"))
                        .append("%</td></tr>\n");
            }

            sb.append("</tbody></table>\n");

            return sb.toString();
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public static void getClinicianPatientDashboardSummary(DbConnection sql, Map<String, String> tokens, int clinicianId) throws SQLException
    {
        CallableStatement statement = null;
        ResultSet rs = null;
        try
        {
            final String query = "CALL GetClinicianPatientDashboardSummary(?);";
            statement = sql.prepareCall(query);
            statement.setInt(1, clinicianId);

            boolean hasMoreResultSets = statement.execute();
            if (!hasMoreResultSets)
            {
                System.out.println("First result is not a ResultSet.");
                return;
            }

            rs = statement.getResultSet();
            while (rs.next())
            {
                tokens.put("$(username)", rs.getString("username"));
                int num_patients = rs.getInt("num_patients");
                int num_added_patients = rs.getInt("num_added_patients");
                int num_discharged_patients = rs.getInt("num_discharged_patients");

                tokens.put("$(num_patients)", NumberFormat.getNumberInstance(Locale.US).format(num_patients));

                StringBuilder summary  = new StringBuilder();
                if (num_added_patients == 0 && num_discharged_patients == 0)
                {
                    tokens.put("$(num_patients_summary)", "");
                }
                else
                {
                    summary.append("<img src=\"https://constanttherapy.com/shared/src/images/arrow.up.green.png\" alt=\"\" title=\"\" style=\"width: 12px; height: auto; vertical-align: 3px; margin-right: 5px;\" /> ");
                    if (num_added_patients > 0 && num_discharged_patients > 0)
                    {
                        summary.append("<b style=\"font-weight: 200; color: #14D15B;\">");
                        summary.append(NumberFormat.getNumberInstance(Locale.US).format(num_added_patients));
                        summary.append("</b> ADDED, <b style=\"font-weight: 200; color: #41A4E6;\">");
                        summary.append(NumberFormat.getNumberInstance(Locale.US).format(num_discharged_patients));
                        summary.append("</b> DISCHARGED LAST WEEK");
                    }
                    else if (num_added_patients > 0)
                    {
                        summary.append("<b style=\"font-weight: 200; color: #14D15B;\">");
                        summary.append(NumberFormat.getNumberInstance(Locale.US).format(num_added_patients));
                        summary.append("</b> ADDED LAST WEEK");
                    }
                    else if (num_discharged_patients > 0)
                    {
                        summary.append("<b style=\"font-weight: 200; color: #41A4E6;\">");
                        summary.append(NumberFormat.getNumberInstance(Locale.US).format(num_discharged_patients));
                        summary.append("</b> DISCHARGED LAST WEEK</p>");
                    }
                }

                tokens.put("$(num_patients_summary)", summary.toString());
            }

            hasMoreResultSets = statement.getMoreResults();
            if (!hasMoreResultSets)
            {
                System.out.println("Second result is not a ResultSet.");
                return;
            }

            rs = statement.getResultSet();

            while (rs.next())
            {
                int total_exercises = rs.getInt("total_exercises");
                int addedTotalExercises = rs.getInt("addedTotalExercises");
                int home_exercises = rs.getInt("home_exercises");
                int addedHomeExercises = rs.getInt("addedHomeExercises");

                tokens.put("$(total_exercises)", NumberFormat.getNumberInstance(Locale.US).format(total_exercises));
                tokens.put("$(home_exercises)", NumberFormat.getNumberInstance(Locale.US).format(home_exercises));

                StringBuilder sb = new StringBuilder();
                if (addedTotalExercises > 0)
                {
                    sb.append("<img src=\"https://constanttherapy.com/shared/src/images/arrow.up.green.png\" alt=\"\" title=\"\" style=\"width: 10px; height: auto; vertical-align: 2px; margin-right: 5px;\" /> <b style=\"font-weight: 200; color: #14D15B;\">");
                    sb.append(NumberFormat.getNumberInstance(Locale.US).format(addedTotalExercises));
                    sb.append("</b> FROM LAST WEEK");
                }
                tokens.put("$(addedTotalExercises)", sb.toString());

                sb.setLength(0);
                if (addedHomeExercises > 0)
                {
                    sb.append("<img src=\"https://constanttherapy.com/shared/src/images/arrow.up.green.png\" alt=\"\" title=\"\" style=\"width: 10px; height: auto; vertical-align: 2px; margin-right: 5px;\" /> <b style=\"font-weight: 200; color: #14D15B;\">");
                    sb.append(NumberFormat.getNumberInstance(Locale.US).format(addedHomeExercises));
                    sb.append("</b> FROM LAST WEEK");
                }
                tokens.put("$(addedHomeExercises)", sb.toString());
            }
            return;
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public static String generateClinicianEmailPatientHighlightTable(DbConnection sqlr, int clinicianId) throws SQLException
    {
        final String upArrow = "<td width=\"30%\" style=\"font-size: 16px;\"><img src=\"https://constanttherapy.com/shared/src/images/arrow.up.green.png\" alt=\"\" title=\"\" style=\"width: 16px; height: auto; vertical-align: 3px; margin-right: 5px;\" /> ";
        final String barChart = "<td width=\"30%\" style=\"font-size: 16px;\"><img src=\"https://constanttherapy.com/shared/src/images/chart.green.png\" alt=\"\" title=\"\" style=\"width: 16px; height: auto; vertical-align: 3px; margin-right: 5px;\" /> ";

        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {

            final String query = "SELECT \n" +
                    "    ue.user_id,\n" +
                    "    MASKUSERNAME(u.username) AS username,\n" +
                    "    JSON_UNQUOTE(JSON_EXTRACT(ue.event_data, '$.type')) AS email_type,\n" +
                    "    ue.event_data\n" +
                    "FROM\n" +
                    "    user_events ue\n" +
                    "        JOIN\n" +
                    "    (SELECT \n" +
                    "        MAX(ue.id) AS id\n" +
                    "    FROM\n" +
                    "        user_events ue\n" +
                    "    JOIN clinicians_to_patients cp ON cp.patient_id = ue.user_id\n" +
                    "        AND cp.clinician_id = " + clinicianId +
                    "    WHERE\n" +
                    "        ue.event_type_id = 5\n" +
                    "            AND ue.timestamp > DATE_ADD(NOW(), INTERVAL - 7 DAY)\n" +
                    "            AND ue.event_sub_type LIKE 'homework\\_progress\\_%'\n" +
                    "            AND ue.event_data IS NOT NULL\n" +
                    "    GROUP BY ue.user_id) recent ON recent.id = ue.id\n" +
                    "        JOIN\n" +
                    "    users u ON ue.user_id = u.id order by ue.timestamp desc limit 10;";
            statement = sqlr.prepareStatement(query);

            rs = statement.executeQuery();

            StringBuilder sb = new StringBuilder();

            sb.append("<table class=\"mainContentPadderTable\" role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 680px;\">\n")
                    .append("<tr>\n")
                    .append("<td bgcolor=\"#ffffff\" style=\"padding: 40px 30px;\">\n")
                    .append("<h1 style=\"margin: 0 0 15px 0; font-family: 'proxima_nova', sans-serif; font-size: 23px; line-height: 1; color: #131313; font-weight: 300; text-transform: uppercase; text-align: center;\">Weekly patient highlights</h1>\n");

            if (!rs.isBeforeFirst() ) {
                // no patient highlight
                sb.append("<p style=\"text-align: center; margin: 60px 0;\"><img src=\"https://lh3.googleusercontent.com/oAtt0QHDEkwCuKpE1kj9mFXsRk1BLtipU3uyTq8Q1oX3V-QjNvZ8eMm2Ms_wXSCxOJqiFqmwikdhR3wrHsuwdr2hD8GoE2c0OmFJ7Hk4q6B0bVO0Wfa56dC-caNGagIFX-Mt0iTSah1a2a5nPxC5xgR5ZIJ9Nh2QqHIlpCD28czBTFacuWI33sQjW7a7iqRMoBxUh-fIFZN7ZTzGvG8SjTjZCVurDEeBFPoNX9WA9jfY1OzoCb2KsP7BZviCdmzUQDmo75Hacs8Dw8eh2GIKIyOTI14Cjm-1avcZi1hUXeTxIFy04m5z9WPsVXza35z7ooQFCQXCPp-w5KZSVSpf5X8Qqo5yjF-cqmh6NRhrtGxpibRaL9F6nAGcHRc0RfRthmL4BO48LT14_d4A86VzxDUxwEsNkbuUcjB7w-lk0mEqdB2AKfUmab9O6HS9Rj5wXMjKU1dqGQOaa-6HTy2cLNYGK-jrtVKWZOWCXek5uYmL7LLrYVZ_L9IveuEUiZRxTq_3GPx_KtoQqUcSCISyONnQMOlU-zuf_9dqE4kJlGzoCsiVy46rsZs2eevI_b8uqOlTizfnpOL2a6Bl3Nu0GPtFjN6iOf1ToSmNjrLe8346a3Q6Y8OpaKgnyG4Xv_k5UXpYk7wmugYRx1Bc2WGAQaiwl51qG6i91w=w290-h230-no\" alt=\"\" title=\"\" style=\"width: 145px; height: 115px; vertical-align: bottom;\" /></p>")
                        .append("<p style=\"margin: 0; font-family: 'proxima_nova', sans-serif; font-size: 18px; line-height: 25px; color: #414141; font-weight: 300; text-align: center;\"><b style=\"font-weight: 400; letter-spacing: 0.4px;\">Looks like none of your patient’s are exercising at home.</b><br>")
                        .append("Did you know that patients who use Constant Therapy at home<br>\n").append("            get four times as much therapy?</p>");

                sb.append("<br><br>\n").append("        <p style=\"margin: 0; font-family: 'proxima_nova', sans-serif; font-size: 18px; line-height: 25px; color: #414141; font-weight: 300; text-align: center;\"><a href=\"https://drive.google.com/file/d/16DeGf8Bu-4LazXRBD7btQM6MgjzzVn0l/view\" style=\"text-decoration: none; color: #FFAA00;\">Encourage your patients to exercise at home today!</a></p>")
                        .append("</td></tr></table>");

                return sb.toString();
            }


            sb.append("<table class=\"mainContentPadderTable\" role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"font-family: 'proxima_nova', sans-serif; font-size: 14px; font-weight: 200; color: #414141;\">\n");

            // table with performance
            while (rs.next())
            {
                sb.append("<tr>\n");
                sb.append("<td width=\"12%\" style=\"line-height: 65px; text-transform: uppercase;\"><b style=\"font-weight: 400;\">").append(rs.getString("username")).append("</b></td>\n");
                sb.append("<td width=\"58%\" style=\"padding: 0 20px 0 15px;\">");

                String type = rs.getString("email_type");
                Map<String, Object> map = new HashMap<>();
                map = GsonHelper.getGson().fromJson(rs.getString("event_data"), map.getClass());

                if (type.equals("domain"))
                {
                    double improvement = (Double) map.get("domain_score_improvement");
                    improvement = Math.round(improvement * 100);
                    int score = (int) improvement;

                    sb.append("Improved in the <b style=\"font-weight: 600;\">")
                            .append(map.get("domain_name")).append("</b> skill area.</td>\n");

                    sb.append(upArrow);

                    sb.append("<b style=\"font-size: 27px; font-weight: 300;\">").append(score).append("</b> pts</td>\n");
                }
                else if (type.equals("task_accuracy"))
                {
                    Integer level = ((Double) map.get("task_level")).intValue();
                    double improvement = ((Double) map.get("accuracy_improvement"));
                    improvement = Math.round(improvement * 100);
                    int percent = (int) improvement;

                    sb.append("Improved in the <b style=\"font-weight: 600;\">")
                            .append(map.get("task_display_name")).append(" Lvl ").append(level)
                            .append("</b> task.</td>\n");

                    sb.append(upArrow);

                    sb.append("<b style=\"font-size: 27px; font-weight: 300;\">").append(percent).append("</b> percent</td>\n");
                }
                else if (type.equals("task_latency"))
                {
                    Integer level = ((Double) map.get("task_level")).intValue();
                    double improvement = ((Double) map.get("latency_improvement"));
                    int secs = (int) improvement;

                    sb.append("Is faster in the <b style=\"font-weight: 600;\">")
                            .append(map.get("task_display_name")).append(" Lvl ").append(level)
                            .append("</b> task.</td>\n");

                    sb.append(upArrow);

                    sb.append("<b style=\"font-size: 27px; font-weight: 300;\">").append(new DecimalFormat("#.#").format(improvement)).append("</b> secs</td>\n");
                }
                else if (type.equals("total_tasks_completed"))
                {
                    Integer tasks = ((Double) map.get("num_exercises")).intValue();
                    String num_exercises = NumberFormat.getNumberInstance(Locale.US).format(tasks);

                    sb.append("Has completed ").append(num_exercises).append(" exercises in total.</td>\n");

                    sb.append(barChart);

                    sb.append("<b style=\"font-size: 27px; font-weight: 300;\">").append(num_exercises).append("</b> exercises</td>\n");
                }
                else if (type.equals("avg_activity"))
                {
                    Double activeDays = (Double) map.get("days_active_per_week");

                    sb.append("Has been active an average of ").append(activeDays).append(" days / wk.</td>\n");

                    sb.append(barChart);

                    sb.append("<b style=\"font-size: 27px; font-weight: 300;\">").append(new DecimalFormat("#.#").format(activeDays)).append("</b> days</td>\n");
                }

                sb.append("</tr>\n");
            }

            sb.append("</table></td></tr></table>");

            return sb.toString();
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public static void addReplacementTokensForPatientTMinusEmail(ReadWriteDbConnection sql,
                                                                 Map<String, String> tokens,
                                                                 Patient patient,
                                                                 SubscriptionHelper stripey,
                                                                 Integer days)
    {
        String coupon = getCouponCode(sql);
        if (coupon == null)
        {
            String snippet = "$(t_minus_" + days + "_subscribe)";
            tokens.put("$(button_holder)", snippet);
        }
        else
        {
            tokens.put("$(button_holder)", "$(t_minus_coupon)");
        }
        String title = days + " DAYS LEFT IN YOUR TRIAL";
        tokens.put("$(t_minus_title)", title);

        if (stripey == null)
        {
            stripey = StripeHelper.create(sql, patient.getId());
        }
        tokens.put("$(expiration_date)", TimeUtil.getDateFromTimestamp(stripey.getExpirationTimestamp()));

        addReplacementTokensForPatient3BarStats(sql, tokens, patient.getId());
    }

    public static void addReplacementTokensForAlertNotPracticeEmail(ReadWriteDbConnection sql,
                                                                 Map<String, String> tokens,
                                                                 Patient patient,
                                                                 SubscriptionHelper stripey)
    {
        boolean needSubscibe = checkSubscriptionStatus(sql, patient.getId());

        if (needSubscibe) {
            tokens.put("$(alert_message)", "$(alert_trial_message)");

            String coupon = getCouponCode(sql);
            if (coupon == null)
                tokens.put("$(alert_trial_button)", "$(subscribe_button)");
            else
                tokens.put("$(alert_trial_button)", "$(t_minus_coupon)");

        } else {
            tokens.put("$(alert_message)", "$(alert_subscriber_message)");
        }

        if (stripey == null)
        {
            stripey = StripeHelper.create(sql, patient.getId());
        }

        addReplacementTokensForPatient3BarStats(sql, tokens, patient.getId());
    }

    public static void addReplacementTokensForPatient3BarStats(ReadWriteDbConnection sql,
                                                               Map<String, String> tokens,
                                                               Integer patientId)
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            String q = "SELECT \n" +
                    "\tGetPatientOverallAccuracy(?) as overall_accuracy,\n" +
                    "\tGetPatientTotalExercisesCompleted(?) as total_exercises_completed,\n" +
                    "\tGetPatientDaysActivePerWeek(?,15) AS days_active_per_week;";
            statement = sql.prepareStatement(q);
            statement.setInt(1, patientId);
            statement.setInt(2, patientId);
            statement.setInt(3, patientId);

            rs = statement.executeQuery();

            if (rs.next())
            {
                double avgScore =rs.getDouble("overall_accuracy");
                String accuracy = String.format("%s%%", (int) (avgScore * 100));
                double activity = rs.getDouble("days_active_per_week");

                int tasks_completed = rs.getInt("total_exercises_completed");

                String type = tokens.get("type");
                if (tasks_completed < 50) {
                    tokens.put("type", type + "_no_stats");
                    return;
                }

                tokens.put("$(total_exercises_completed)", NumberFormat.getNumberInstance(Locale.US).format(tasks_completed));
                tokens.put("$(total_exercises_completed_value)", Integer.toString(tasks_completed));
                tokens.put("$(overall_accuracy)", accuracy);
                tokens.put("$(days_active_per_week)", String.format("%.1f", activity));

                if (activity < 3.0)
                {
                    tokens.put("$(avg_activity)", "$(avg_activity_stats_recommend)");
                }
                else
                {
                    tokens.put("$(avg_activity)", "$(avg_activity_stats)");
                }
            }
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public static void generateHomeworkHighlightTable(DbConnection sql, int patientId, Map<String, String> tokens) throws SQLException
    {
        boolean needSubscibe = checkSubscriptionStatus(sql, patientId);

        if (needSubscibe) {
            tokens.put("$(subscription_status)", "_trial");
            String coupon = getCouponCode(sql);
            if (coupon == null) {
                tokens.put("$(button_holder)", "$(button_report_trial_without_coupon)");
            } else {
                tokens.put("$(button_holder)", "$(button_report_trial_with_coupon)");
            }
        } else {
            tokens.put("$(subscription_status)", "_subscriber");
            tokens.put("$(button_holder)", "$(button_report)");
        }

        CallableStatement statement = null;
        ResultSet rs = null;
        try
        {
            final String query = "CALL GetPatientHomeworkHighlights(?, ?);";
            statement = sql.prepareCall(query);
            statement.setInt(1, patientId);
            statement.setInt(2, 7);  // how many days you should use for "recent activity" when scanning for highlights

            boolean hasMoreResultSets = statement.execute();
            Map<String, Object> event_data = new HashMap<>();

            if (!hasMoreResultSets)
            {
                System.out.println("First result is not a ResultSet.");
                return ;
            }
            rs = statement.getResultSet();
            while (rs.next())
            {
                double avgScore =rs.getDouble("overall_accuracy");
                String accuracy = String.format("%s%%", (int) (avgScore * 100));
                double activity = rs.getDouble("days_active_per_week");

                int tasks_completed = rs.getInt("total_exercises_completed");
                tokens.put("$(total_exercises_completed)", NumberFormat.getNumberInstance(Locale.US).format(tasks_completed));
                tokens.put("$(total_exercises_completed_value)", Integer.toString(tasks_completed));
                tokens.put("$(overall_accuracy)", accuracy);
                tokens.put("$(days_active_per_week)", String.format("%.1f", activity));

                if (activity < 3.0)
                {
                    tokens.put("$(avg_activity)", "$(avg_activity_stats_recommend)");
                }
                else
                {
                    tokens.put("$(avg_activity)", "$(avg_activity_stats)");
                }

                if (tasks_completed < 50)
                {
                    return;
                }
            }

            // 2nd query
            hasMoreResultSets = statement.getMoreResults();
            if (!hasMoreResultSets)
            {
                System.out.println("Second result is not a ResultSet.");
                return ;
            }
            rs = statement.getResultSet();

            while (rs.next())
            {
                String type = rs.getString("type");
                if (type.equals("domain"))
                {
                    int improvement = (int) Math.round(rs.getDouble("domain_score_improvement") * 100) ;
                    String highlight = String.format("%s", improvement);
                    tokens.put("$(type)", type);
                    tokens.put("$(domain_name)", rs.getString("domain_name"));
                    tokens.put("$(domain_score_improvement)", highlight);
                    tokens.put("$(domain_score_description)", rs.getString("domain_score_description"));

                    event_data.put("type", rs.getString("type"));
                    event_data.put("domain_id", rs.getInt("domain_id"));
                    event_data.put("domain_name", rs.getString("domain_name"));
                    event_data.put("domain_score", rs.getDouble("domain_score"));
                    event_data.put("domain_score_description", rs.getString("domain_score_description"));
                    event_data.put("domain_score_improvement", rs.getDouble("domain_score_improvement"));

                    tokens.put("$(event_data)", GsonHelper.toJson(event_data));
                }
                tokens.put("$(token_text)", getTemplateText(sql, "homework_progress_" + type));
                tokens.put("$(highlight)", "$(highlight_domain)");
                return ;
            }

            // 3rd query
            hasMoreResultSets = statement.getMoreResults();
            if (!hasMoreResultSets)
            {
                System.out.println("Third result is not a ResultSet.");
                return ;
            }
            rs = statement.getResultSet();
            while (rs.next())
            {
                String type = rs.getString("type");
                tokens.put("$(type)", type);
                tokens.put("$(task_display_name)", rs.getString("task_display_name"));
                tokens.put("$(task_level)", rs.getString("task_level"));

                if (type.equals("task_accuracy"))
                {
                    int improvement = (int) Math.round(rs.getDouble("accuracy_improvement") * 100);

                    tokens.put("$(accuracy_improvement)", String.format("%s", improvement));

                    event_data.put("type", rs.getString("type"));
                    event_data.put("task_type_id", rs.getInt("task_type_id"));
                    event_data.put("task_display_name", rs.getString("task_display_name"));
                    event_data.put("task_level", rs.getInt("task_level"));
                    event_data.put("latest_accuracy", rs.getString("latest_accuracy"));
                    event_data.put("accuracy_improvement", rs.getDouble("accuracy_improvement"));
                    tokens.put("$(highlight)", "$(highlight_task_accuracy)");
                }
                else if (type.equals("task_latency"))
                {
                    DecimalFormat df = new DecimalFormat("#.#");
                    String improvement = df.format(rs.getDouble("latency_improvement"));
                    tokens.put("$(latency_improvement)", improvement);

                    event_data.put("type", rs.getString("type"));
                    event_data.put("task_type_id", rs.getInt("task_type_id"));
                    event_data.put("task_display_name", rs.getString("task_display_name"));
                    event_data.put("task_level", rs.getInt("task_level"));
                    event_data.put("latest_accuracy", rs.getDouble("latest_accuracy"));
                    event_data.put("latency_percentile", rs.getDouble("latency_percentile"));
                    event_data.put("latest_latency", rs.getDouble("latest_latency"));
                    event_data.put("latency_improvement", rs.getDouble("latency_improvement"));
                    tokens.put("$(highlight)", "$(highlight_task_latency)");
                }
                tokens.put("$(event_data)", GsonHelper.toJson(event_data));
                tokens.put("$(token_text)", getTemplateText(sql, "homework_progress_" + type));
                return ;
            }

            // 4th query
            hasMoreResultSets = statement.getMoreResults();
            if (!hasMoreResultSets)
            {
                System.out.println("Third result is not a ResultSet.");
                return ;
            }
            rs = statement.getResultSet();
            while (rs.next())
            {
                String type = rs.getString("type");
                tokens.put("$(type)", type);
                if (type.equals("total_tasks_completed"))
                {
                    int tasks_completed = rs.getInt("num_exercises");
                    tokens.put("$(num_exercises)", NumberFormat.getNumberInstance(Locale.US).format(tasks_completed));

                    event_data.put("type", rs.getString("type"));
                    event_data.put("num_exercises", rs.getInt("num_exercises"));
                    tokens.put("$(highlight)", "$(highlight_total_tasks_completed)");
                }
                else if (type.equals("avg_activity"))
                {
                    double days_active_per_week = rs.getDouble("days_active_per_week");

                    tokens.put("$(days_active_per_week)", String.format("%.1f", days_active_per_week));

                    event_data.put("type", rs.getString("type"));
                    event_data.put("days_active_per_week", rs.getDouble("days_active_per_week"));
                    tokens.put("$(highlight)", "$(highlight_avg_activity)");
                }
                tokens.put("$(event_data)", GsonHelper.toJson(event_data));
                tokens.put("$(token_text)", getTemplateText(sql, "homework_progress_" + type));
                return ;
            }
            return ;
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    private static String getTemplateText(DbConnection sql, String templateSubstring) {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        String token1 = null;
        try {
            String q = "SELECT * FROM message_text WHERE template_name = ? order by rand() limit 1";
            statement = sql.prepareStatement(q);
            statement.setString(1, templateSubstring);

            rs = statement.executeQuery();

            if (rs.next())
            {
                token1 = rs.getString("text");
            }
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }

        return token1;
    }

    private static String getCouponCode(DbConnection sql) {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        String cc = null;
        try {
            String q = "SELECT filename FROM message_templates WHERE name = '$(coupon_code)'";
            statement = sql.prepareStatement(q);
            rs = statement.executeQuery();
            if (rs.next())
            {
                cc = rs.getString("filename");
            }
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }

        return cc;
    }


    private static boolean checkSubscriptionStatus(DbConnection sql, int patientId) {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        boolean needSubscribe = false;
        try {
            String q = "select user_id, stripe_status, ios_iap_status from ct_customer.customers where user_id = ? and usertype = 'patient' and (stripe_status IN ('trialing') OR (stripe_status = 'trialEnded' AND (ios_iap_status != 'active' OR ios_iap_status IS NULL)));";
            statement = sql.prepareStatement(q);
            statement.setInt(1, patientId);

            rs = statement.executeQuery();

            if (rs.next())
            {
                needSubscribe = true;
            }
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }

        return needSubscribe;
    }

    /**
     * This function determines whether the specified amount of time has elapsed before we
     * send out an email of the specified type to the user.
     * @param sql
     * @param userId
     * @param templateSubstring
     * @param minHoursElapsed
     * @return
     */
    public static boolean canSendEmail(DbConnection sql, int userId, String templateSubstring, int minHoursElapsed)
    {
        Long minutes = getMinutesSinceLastEmail(sql, userId, templateSubstring);

        // if it has been more than specified hours since last email to user, don't send email
        if (minutes < minHoursElapsed * 60)
        {
            CTLogger.debug(
                    String.format("Last communication to user was less than %s hours ago. Not sending %s email. userId=%s",
                                  minHoursElapsed, templateSubstring, userId));
            return false;
        }

        return true;
    }

    /**
     * Gets the number of minutes since the last email was sent to the specified user.
     *
     * @param sql
     * @param userId
     * @return Number of minutes since last email. 99999999 if no email has ever been sent.
     */
    private static Long getMinutesSinceLastEmail(DbConnection sql, int userId, String templateSubstring)
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        Long minutes = null;
        try
        {
            String q = "SELECT MAX(timestamp) ts FROM user_events WHERE event_type_id = 5 AND user_id = ?" +
                    (templateSubstring == null ? "" : " AND event_sub_type LIKE ?");
            statement = sql.prepareStatement(q);
            statement.setInt(1, userId);

            if (templateSubstring != null)
                statement.setString(2, templateSubstring);

            rs = statement.executeQuery();

            if (rs.next())
            {
                Timestamp ts = rs.getTimestamp("ts");
                if (ts != null)
                    minutes = TimeUtil.timeSince(ts, TimeUnits.MINUTES);
                else
                    minutes = 99999999L;
            }
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
        return minutes;
    }
}
