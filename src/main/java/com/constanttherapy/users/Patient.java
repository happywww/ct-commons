/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */
package com.constanttherapy.users;

import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.db.*;
import com.constanttherapy.enums.UserType;
import com.constanttherapy.tasks.ScheduleLite;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.KeyValuePair;
import com.constanttherapy.util.MathUtil;
import com.constanttherapy.util.StringUtil;
import org.apache.commons.configuration.Configuration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Patients differ from normal CTUsers in that they can perform tasks and save their responses.
 * A patient can have many clinicians, who are able to monitor the patient's results
 *
 * @author ehsan
 */
public class Patient extends CTUser
{
    private static final int OTHER_DEFICIT = 9999;
    public Timestamp acceptedDischargeEULATimestamp;
    private static final Configuration config = CTConfiguration.getConfig("conf/ct-common-config.xml");

    public Patient()
    {
        super();
        this.type = UserType.patient;
    }

    public Patient(ResultSet rs) throws SQLException
    {
        super();
        this.type = UserType.patient; // [mahendra, 7/27/16 10:13 AM]: is this necessary?
        this.read(rs);
    }

    @Override
    public void read(ResultSet rs) throws SQLException
    {
        super.read(rs);
        this.acceptedDischargeEULATimestamp = rs.getTimestamp("accepted_discharge_eula_timestamp");
    }

    /**
     * Returns true if the ID is a valid Patient Id
     *
     * @param sql
     * @param patientId
     * @return
     */
    public static boolean isValidId(ReadWriteDbConnection sql, Integer patientId)
    {
        try
        {
            CTUser user = CTUser.getById(sql, patientId);
            return user != null && user instanceof Patient;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    private static Patient createAccount(ReadWriteDbConnection sql, String username, PasswordTuple password,
                                         String firstName, String lastName, String email) throws Exception
    {
        return createAccount(sql, username, password, firstName, lastName, email, false);
    }

    private static Patient createAccount(ReadWriteDbConnection sql, String username, PasswordTuple password,
                                         String firstName, String lastName, String email, boolean isDemo) throws Exception
    {
        if (username.contains("@"))
            throw new IllegalArgumentException("Username cannot contain '@' symbol.");

        CTLogger.infoStart("Patient::createAccount() - "
                + String.format("username=%s, email=%s", username, email));

        Patient newPatient = new Patient();
        newPatient.setUsername(username.trim());
        newPatient.password = password;
        newPatient.firstName = firstName;
        newPatient.setLastName(lastName);
        newPatient.setEmail(email);
        newPatient.isDemo = isDemo;
        newPatient.setPromptChangePassword(false);
        newPatient.setAccountActive(true);
        newPatient.needsAssessment = true;
        newPatient.create(sql);

        return newPatient;
    }

    public static Patient createAccount(ReadWriteDbConnection sql, String username, String password,
                                        String firstName, String lastName, String email) throws Exception
    {

        return Patient.createAccount(sql, username, PasswordTuple.generateFromPassword(password), firstName, lastName, email);
    }

    public List<Clinician> getClinicians(ReadWriteDbConnection sql)
    {
        List<Clinician> clinicians = new ArrayList<>();

        SqlPreparedStatement statement;
        try
        {
            statement = sql.prepareStatement("SELECT clinician_id FROM constant_therapy.clinicians_to_patients "
                    + "WHERE patient_id = ? AND clinician_id NOT IN "
                    + "(SELECT clinician_id FROM groups_to_clinicians a "
                    + "JOIN clinician_groups b ON b.id = a.group_id WHERE b.is_admin = 1) AND "
                    + "date_removed IS NULL");

            statement.setInt(1, this.id);

            ResultSet rs = statement.executeQuery();

            while (rs.next())
            {
                clinicians.add((Clinician) Clinician.getById(sql, rs.getInt("clinician_id")));
            }
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }

        return clinicians;
    }

    @Deprecated
    public void generateDummyData(ReadWriteDbConnection sql)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("CALL GenerateDummyData(?,?,?)");
            statement.setInt(1, this.id);
            statement.setString(2, config.getString("ct-server-version"));
            statement.setString(3, config.getString("ct-client-version"));

            statement.executeQuery();
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
    }

    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        super.update(sql);

        SqlPreparedStatement statement = sql.prepareStatement("UPDATE users SET accepted_discharge_eula_timestamp = ? WHERE id = ?");

        if (this.acceptedDischargeEULATimestamp != null)
            statement.setTimestamp(1, this.acceptedDischargeEULATimestamp);
        else
            statement.setNull(1, Types.TIMESTAMP);

        statement.setInt(2, this.id);

        statement.execute();
    }

    /**
     * get the list of possible reasons that a patient could discontinue
     * use of the software when discharged by a clinician ... for now these
     * are hardcoded but we'll probably put this into a database table later
     * ... note that reason list could be different for discontinuing when
     * clinician is bot (e.g. discontinue from a patient login) vs. discontinue
     * from non-bot clinician, and there will probably be yet another set of
     * reasons for clinician discontinuing themselves
     */
    public static List<KeyValuePair<String, String>> getDiscontinueReasonsForClinicianDischargingPatient()
    {
        List<KeyValuePair<String, String>> reasons = new ArrayList<>();

        reasons.add(new KeyValuePair<>("just_trying", "Not a real client, I am trying out the app"));
        reasons.add(new KeyValuePair<>("patient_better", "Client is better"));
        reasons.add(new KeyValuePair<>("patient_cant_use", "Client cannot do therapy independently"));
        reasons.add(new KeyValuePair<>("patient_no_device", "Client does not have a device (iPhone, iPad, Android)"));
        reasons.add(new KeyValuePair<>("too_expensive", "Too expensive"));
        reasons.add(new KeyValuePair<>("too_easy", "Tasks too easy"));
        reasons.add(new KeyValuePair<>("too_difficult", "Tasks too difficult or confusing"));
        reasons.add(new KeyValuePair<>("other", "Other reasons"));
        return reasons;
    }

    /**
     * Returns list of all tasks in currently active schedule
     * @param sql
     * @param patientId
     * @param includeTaskCount
     * @return system_name.task_level[;task_count]
     */
    public static List<String> getActiveSchedule(DbConnection sql, int patientId, boolean includeTaskCount)
    {
        CTLogger.infoStart("Patient::getActiveSchedule() - patientId=" + patientId);
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            String s = "SELECT c.system_name task_name, b.task_level, b.task_count FROM schedules a "
                    + "JOIN scheduled_task_types b ON a.id = b.schedule_id "
                    + "JOIN task_types c ON b.task_type_id = c.id "
                    + "WHERE patient_id = ? AND a.end_date IS NULL ORDER BY b.task_order";

            statement = sql.prepareStatement(s);
            statement.setInt(1, patientId);

            rs = statement.executeQuery();

            List<String> sched = new ArrayList<>();

            while (rs.next())
            {
                String task = rs.getString("task_name") + "." + rs.getString("task_level");

                if (includeTaskCount)
                    task += ";" + rs.getString("task_count");

                sched.add(task);
            }

            return sched;
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return null;
        }
        finally
        {
            CTLogger.unindent();
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public static Integer getCompletedTaskCount(DbConnection sqlr, int patientId, boolean includeAdhoc)
    {
        CTLogger.infoStart("Patient::getCompletedTaskCount() - patientId=" + patientId);
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        Integer count = null;
        try
        {
            String s = "SELECT SUM(completed_task_count) tc FROM sessions WHERE patient_id = ? AND parent_id IS NULL ";
            if (!includeAdhoc)
            {
                s += " AND type != 'ADHOC'";
            }
            statement = sqlr.prepareStatement(s);
            statement.setInt(1, patientId);

            rs = statement.executeQuery();
            if (rs.next())
            {
                count = rs.getInt("tc");
            }
        }
        catch (SQLException ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            CTLogger.unindent();
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }

        return count;
    }

    /**
     * Returns average accuracy over past n responses
     *
     * @param sqlr
     * @param patientId
     * @return
     * @throws SQLException
     */
    public static Double getAverageScore(DbConnection sqlr, int patientId, int taskCount) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            CTLogger.infoStart("Patient::getAverageScore() - patientId=" + patientId);

            //String q = "SELECT AVG(IFNULL(a.accuracy,0)) FROM (SELECT * FROM responses USE INDEX (fk_responses_users1) WHERE patient_id = ? ORDER BY id DESC LIMIT ?)a";
            String q = "SELECT avg_accuracy_600 FROM ct_stats.user_stats WHERE user_id = ?";

            statement = sqlr.prepareStatement(q);
            statement.setInt(1, patientId);
            //statement.setInt(2, taskCount);

            rs = statement.executeQuery();

            if (rs.next())
                return MathUtil.round(rs.getDouble("avg_accuracy_600"), 3);
            else
                return 0.0D;
        }
        finally
        {
            CTLogger.unindent();
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public List<String> getDeficits(DbConnection sqlr) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            CTLogger.infoStart("Patient::getDeficits() - patientId=" + this.id);

            String q = "SELECT d.id AS id, d.description AS deficit FROM ct_customer.customers a "
                    + "JOIN users b ON a.user_id = b.id "
                    + "JOIN ct_customer.customers_to_deficits c ON a.id = c.customer_id "
                    + "JOIN ct_customer.deficits d ON d.id = c.deficit_id "
                    + "WHERE b.id = ?";

            statement = sqlr.prepareStatement(q);
            statement.setInt(1, this.id);

            rs = statement.executeQuery();

            List<String> deficits = new ArrayList<>();
            while (rs.next())
            {
                int deficitId = rs.getInt("id");
                String def = rs.getString("deficit");

                if (deficitId == OTHER_DEFICIT) // "Other"
                {
                    // choose only the top "other" deficit types
                    if (def.equals("Speech")
                            || def.equals("Articulation")
                            || def.equals("Math")
                            || def.equals("Executive Function")
                            || def.equals("Communication")
                            || def.equals("Voice")
                            || def.equals("Numbers")
                            || def.equals("Fluency"))
                        deficits.add("def");
                }
                else
                    deficits.add(def);
            }

            return deficits;
        }
        finally
        {
            CTLogger.unindent();
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public List<String> getDisorders(ReadOnlyDbConnection sql) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            CTLogger.infoStart("Patient::getDisorders() - patientId=" + this.id);

            String q = "SELECT d.description AS disorder FROM ct_customer.customers a "
                    + "JOIN users b ON a.user_id = b.id "
                    + "JOIN ct_customer.customers_to_disorders c ON a.id = c.customer_id "
                    + "JOIN ct_customer.disorders d ON d.id = c.disorder_id "
                    + "WHERE b.id = ?";

            statement = sql.prepareStatement(q);
            statement.setInt(1, this.id);

            rs = statement.executeQuery();

            List<String> disorders = new ArrayList<>();
            while (rs.next())
            {
                disorders.add(rs.getString("disorder"));
            }

            return disorders;
        }
        finally
        {
            CTLogger.unindent();
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public List<Integer> getDisorderIds(ReadOnlyDbConnection sql) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            CTLogger.infoStart("Patient::getDisorderIds() - patientId=" + this.id);

            String q = "SELECT d.id FROM ct_customer.customers a "
                    + "JOIN users b ON a.user_id = b.id "
                    + "JOIN ct_customer.customers_to_disorders c ON a.id = c.customer_id "
                    + "JOIN ct_customer.disorders d ON d.id = c.disorder_id "
                    + "WHERE b.id = ?";

            statement = sql.prepareStatement(q);
            statement.setInt(1, this.id);

            rs = statement.executeQuery();

            List<Integer> disorders = new ArrayList<>();
            while (rs.next())
            {
                disorders.add(rs.getInt("id"));
            }

            return disorders;
        }
        finally
        {
            CTLogger.unindent();
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public Integer getCompletedTaskCount(ReadOnlyDbConnection sql, boolean includeAdhoc)
    {
        return Patient.getCompletedTaskCount(sql, this.id, includeAdhoc);
    }

    public boolean getRepeatInstructionsSetting(ReadOnlyDbConnection sql) throws SQLException
    {
        String repeatInstrPref = UserPreferences.getPreferenceForUser(sql, this.id, "repeat_instr");
        return (repeatInstrPref == null || repeatInstrPref.equals("true"));
    }

    private List<ScheduleLite> getSchedules(ReadWriteDbConnection sql) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String q = "SELECT * FROM schedules WHERE patient_id = ?";
            statement = sql.prepareStatement(q);
            statement.setInt(1, this.id);

            rs = statement.executeQuery();

            List<ScheduleLite> schedules = new ArrayList<>();
            while (rs.next())
            {
                schedules.add(new ScheduleLite(rs));
            }
            return schedules;
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    private static final String SKIP_TASK_VIDEO = "SkipTaskVideo";

    public void setSkipTaskVideo(ReadWriteDbConnection sql, Integer taskTypeId, boolean skip) throws SQLException
    {
        ReadOnlyDbConnection sqlr = null;

        try
        {
            // get existing
            sqlr = new ReadOnlyDbConnection();
            String taskTypeIds = UserPreferences.getPreferenceForUser(sqlr, this.id, SKIP_TASK_VIDEO);

            String tid = taskTypeId.toString();

            if (taskTypeIds != null)
            {
                List<String> ids = StringUtil.stringToList(taskTypeIds, ",");
                if (skip)
                {
                    if (!ids.contains(tid))
                        ids.add(tid);
                }
                else
                {
                    if (ids.contains(tid))
                        ids.remove(tid);
                }
                // recreate CSV string
                taskTypeIds = StringUtil.listToString(ids, ",");
            }
            else
            {
                // this is the first entry in the skip list
                if (skip)
                    taskTypeIds = tid;
            }

            // and set preference (could be null)
            UserPreferences.setPreferenceForUser(sql, this.id, SKIP_TASK_VIDEO, taskTypeIds);
        }
        finally
        {
            SQLUtil.closeQuietly(sqlr);
        }
    }

    public boolean getSkipTaskVideo(ReadOnlyDbConnection sqlr, Integer taskTypeId) throws SQLException
    {
        String taskTypeIds = UserPreferences.getPreferenceForUser(sqlr, this.id, SKIP_TASK_VIDEO);

        if (taskTypeIds != null)
        {
            String tid = taskTypeId.toString();
            List<String> ids = StringUtil.stringToList(taskTypeIds, ",");

            return ids.contains(tid);
        }
        else
            return false;  // default to not skip video
    }

    public boolean isDoingInitialAssessment(ReadWriteDbConnection sqlr) throws SQLException
    {
        List<ScheduleLite> schedules = this.getSchedules(sqlr);

        return (schedules.size() == 1 &&
                schedules.get(0).clinicianId != null &&
                ClinicianGroup.isAdmin(sqlr, schedules.get(0).clinicianId));
    }

    public boolean isBotPatient(DbConnection sqlr) throws SQLException
    {
        return Patient.isBotPatient(sqlr, this.id);
    }

    public static boolean isBotPatient(DbConnection sql, Integer patientId) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT  clinician_id " +
                "FROM constant_therapy.clinicians_to_patients " +
                "WHERE patient_id = ? AND date_removed IS NULL");

        statement.setInt(1, patientId);

        ResultSet rs = statement.executeQuery();

        while (rs.next())
        {
            int clinicianId = rs.getInt("clinician_id");
            if (clinicianId == Clinician.BOT_ID || clinicianId == Clinician.BOT_2_ID)
            {
                return true;
            }
        }

        return false;
    }

    public static int getScheduleCount(DbConnection sql, Integer patientId) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT COUNT(*) sched_count FROM schedules WHERE patient_id = ?");

        statement.setInt(1, patientId);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return rs.getInt("sched_count");
        else
            return 0;
    }

    // [godlove, 10/3/17 5:22 PM]: Function that calculates the total therapy time done including all types of schedules
    public static int getTotalTherapyMinutes(DbConnection sql, Integer patientId) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT GetPatientTotalTherapyTime(?) AS total_min");

        statement.setInt(1, patientId);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return rs.getInt("total_min");
        else
            return 0;
    }

    // [godlove, 11/15/17 5:42 PM]: Function that calculates the average active days per week for a given time period
    public static Float getPatientDaysActivePerWeek(DbConnection sql, Integer patientId, Integer daysBack) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT GetPatientDaysActivePerWeek(?,?) AS days_active_per_week");

        statement.setInt(1, patientId);
        statement.setInt(2, daysBack);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            return rs.getFloat("days_active_per_week");
        else
            return 0.0F;
    }

}

