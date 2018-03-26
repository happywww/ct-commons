/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */
package com.constanttherapy.users;

import com.constanttherapy.db.*;
import com.constanttherapy.enums.UserEventType;
import com.constanttherapy.enums.UserType;
import com.constanttherapy.util.CTLogger;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Clinicians differ from normal users in that they can have many patients, whose data they can monitor
 *
 * @author ehsan
 */
public class Clinician extends CTUser
{
    /**
     * the predefined ID owned by bot
     */
    public static final int BOT_ID   = 78;
    /**
     * the "new bot" ID
     */
    public static final int BOT_2_ID = 4836;
    public String referralCode;
    public  boolean canAddPatients;

    public Clinician()
    {
        super();
        this.type = UserType.clinician;
    }

    public Clinician(ResultSet rs) throws SQLException
    {
        super(rs);
        this.referralCode = rs.getString("clinician_referral_code");
        this.canAddPatients = rs.getBoolean("can_add_patients");
    }

    /**
     * Returns true if the ID is a valid clinician Id
     *
     * @param sql
     * @param clinicianId
     * @return
     */
    public static boolean isValidId(ReadWriteDbConnection sql, Integer clinicianId)
    {
        try
        {
            CTUser user = CTUser.getById(sql, clinicianId);
            return user != null && user instanceof Clinician;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    public static Clinician createAccount(ReadWriteDbConnection sql, String username, String password,
                                          String firstName, String lastName, String email) throws Exception
    {
        if (username.contains("@"))
            throw new IllegalArgumentException("Username cannot contain '@' symbol.");

        CTLogger.infoStart("Clinician::createAccount() - "
                + String.format("username=%s, email=%s", username, email));

        Clinician newClinician = new Clinician();
        newClinician.setUsername(username.trim());
        newClinician.firstName = firstName;
        newClinician.setLastName(lastName);
        newClinician.setEmail(email);
        newClinician.setPromptChangePassword(false);
        newClinician.setAccountActive(true);
        newClinician.create(sql, password);

        newClinician.generateReferralCode(sql);

        return newClinician;
    }

    private void generateReferralCode(ReadWriteDbConnection sql) throws SQLException
    {
        if (this.id == null || this.id == 0)
        {
            this.create(sql);
        }

        this.referralCode = new BigInteger(30, random).toString(32);

        // We can't call update because referralCode does not exist in the base CTUser class so it won't get written
        update(sql);
    }

    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        super.update(sql);

        SqlPreparedStatement statement = sql.prepareStatement("UPDATE users " +
                "SET clinician_referral_code = ?, " +
                "can_add_patients = ? WHERE id = ?");

        if (this.referralCode != null)
            statement.setString(1, this.referralCode);
        else
            statement.setNull(1, Types.VARCHAR);

        statement.setBoolean(2, this.canAddPatients);
        statement.setInt(3, this.id);

        statement.execute();
    }

    public static Clinician clinicianForReferralCode(DbConnection sql, String referralCode)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM users WHERE clinician_referral_code = ?");
            statement.setString(1, referralCode);

            ResultSet rs = statement.executeQuery();
            if (rs.next())
            {
                return new Clinician(rs);
            }
            else
            {
                return null;
            }
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return null;
        }
    }

    /**
     * Gets the count of patients for specified clinician that have had some activity in the past 90 days.
     *
     * @param sql
     * @param clinicianId
     * @return
     * @throws SQLException
     */
    public static int getActivePatientCount(ReadOnlyDbConnection sql, int clinicianId) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT COUNT(*) count FROM clinicians_to_patients a "
                + "JOIN users b ON a.patient_id = b.id "
                + "JOIN ct_stats.user_stats c ON a.patient_id = c.user_id "
                + "WHERE b.is_account_active = 1 AND is_demo = 0 AND id > 22 "
                + "AND c.last_response_time > TIMESTAMPADD(DAY, -90, NOW()) AND a.clinician_id = ?");

        statement.setInt(1, clinicianId);

        ResultSet rs = statement.executeQuery();
        int count = 0;
        if (rs.next())
        {
            count = rs.getInt("count");
        }
        return count;
    }

    public static Integer getPatientCount(ReadOnlyDbConnection sqlr, int clinicianId) throws SQLException
    {
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String q = "SELECT count(*) patient_count FROM clinicians_to_patients WHERE clinician_id = ?";
            statement = sqlr.prepareStatement(q);
            statement.setInt(1, clinicianId);

            int count = 0;
            rs = statement.executeQuery();
            if (rs.next())
            {
                count = rs.getInt("patient_count");
            }
            return count;
        }
        finally
        {
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    public static boolean isBot(ReadWriteDbConnection sqlr, int clinicianId)
    {
        try
        {
            Clinician clinician = (Clinician) CTUser.getById(clinicianId);
            return clinician.isBot(sqlr);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
    }

    public boolean isBot(DbConnection sql) { return ClinicianGroup.isBot(sql, this.id); }

    public boolean isSupport(DbConnection sql)
    {
        return ClinicianGroup.isSupport(sql, this.id);
    }

    /**
     * getter for the referral code
     */
    public String getReferralCode() { return this.referralCode; }

    public List<Patient> getPatients(DbConnection sql)
    {
        return Clinician.getPatients(sql, this.id);
    }

    public static List<Patient> getPatients(DbConnection sql, int clinicianId)
    {
        // make two separate lists sorted by username
        Map<String, Patient> patients = new TreeMap<>();
        Map<String, Patient> demos = new TreeMap<>();

        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            statement = sql.prepareStatement("CALL GetPatientsForClinician(?)");

            statement.setInt(1, clinicianId);

            rs = statement.executeQuery();

            while (rs.next())
            {
                int patientId = rs.getInt("id");
                try
                {
                    Patient patient = (Patient) CTUser.fromResultSet(rs);

                    // note that as of 2.3 we are no longer creating demo patients
                    if (patient.getUsername().toLowerCase().endsWith("_demo"))
                        demos.put(patient.getUsername().toLowerCase(), patient);
                    else
                        patients.put(patient.getUsername().toLowerCase(), patient);
                }
                catch (SQLException e)
                {
                    CTLogger.error("Error loading user, patientId=" + patientId, e);
                }
            }
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(rs);
            SQLUtil.closeQuietly(statement);
        }

        // combine the two lists, demos first
        List<Patient> combined = new ArrayList<>();
        combined.addAll(demos.values());
        combined.addAll(patients.values());

        return combined;
    }

    public boolean addPatient(ReadWriteDbConnection sql, int patientId)
    {
        return addPatient(sql, this.id, patientId);
    }
    /**
     * Add patient for clinician.
     *
     * @param sql
     * @param patientId
     * @return
     */
    private static boolean addPatient(ReadWriteDbConnection sql, int clinicianId, int patientId)
    {
        try
        {
            CTLogger.infoStart(String.format("Clinician::addPatient() - clinicianId=%s, patientId=%s", clinicianId, patientId));
            SqlPreparedStatement statement =
                    sql.prepareStatement("INSERT INTO clinicians_to_patients " +
                            "(patient_id, clinician_id, date_added) VALUES(?, ?, NOW()) " +
                            "ON DUPLICATE KEY UPDATE clinician_id = values(clinician_id), " +
                            "patient_id = values(patient_id), date_added = NOW(), date_removed = NULL");

            statement.setInt(1, patientId);
            statement.setInt(2, clinicianId);

            boolean success = statement.executeUpdate() > 0;
            if (success)
            {
                UserEventLogger.EventBuilder
                        .create(UserEventType.ClinicianAddedToPatient)
                        .userId(patientId)
                        .eventData(String.valueOf(clinicianId))
                        .log(sql);
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    public boolean removePatient(ReadWriteDbConnection sql, Integer patientId)
    {
        return removePatient(sql, this.id, patientId, false);
    }

    private static boolean removePatient(ReadWriteDbConnection sql, Integer clinicianId, Integer patientId, boolean removeFromGroup)
    {
        try
        {
            CTLogger.infoStart(String.format("Clinician::removePatient() - clinicianId=%s, patientId=%s", clinicianId, patientId));

            if (removeFromGroup)
            {
                // Remove the patient from all the groups this clinician is a part of:
                List<ClinicianGroup> groups = ClinicianGroup.groupsForClinician(sql, clinicianId);

                boolean dischargeSuccess = true;
                if (groups.size() > 0)
                {
                    for (ClinicianGroup g : groups)
                    {
                        dischargeSuccess &= g.removePatient(sql, patientId);
                    }

                    // happy if all expected removals were successful
                    return dischargeSuccess;
                }
            }

            SqlPreparedStatement statement =
                    sql.prepareStatement("DELETE FROM clinicians_to_patients WHERE " +
                            "patient_id = ? AND clinician_id = ?");

            statement.setInt(1, patientId);
            statement.setInt(2, clinicianId);

            boolean success = statement.executeUpdate() > 0;
            if (success)
            {
                // UserEventLogger.logEvent(sql, UserEventType.ClinicianRemovedFromPatient, patientId, this.id + "");
                UserEventLogger.EventBuilder
                        .create(UserEventType.ClinicianRemovedFromPatient)
                        .userId(patientId)
                        .eventData(String.valueOf(clinicianId))
                        .log(sql);

                return true;
            }
            else
            {
                return false;
            }
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
        finally
        {
        	CTLogger.unindent();
        }
    }

    public boolean hasPatient(ReadWriteDbConnection sqlr, String patientUsername)
    {
        try
        {
            // allow admin users to access all patients
            if (isAdmin(sqlr)) return true;

            SqlPreparedStatement statement =
                    sqlr.prepareStatement("SELECT * FROM clinicians_to_patients a " +
                            "INNER JOIN users b ON a.patient_id = b.id " +
                            "WHERE date_removed IS NULL AND " +
                            "a.clinician_id = ? AND b.username = ?");

            statement.setInt(1, this.id);
            statement.setString(2, patientUsername);

            ResultSet rs = statement.executeQuery();
            return rs.next();
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
    }

    /**
     * returns true if I am admin
     */
    public boolean isAdmin(DbConnection sql) { return ClinicianGroup.isAdmin(sql, this.id); }

    public boolean hasPatient(ReadWriteDbConnection sql, Integer patientId)
    {
        try
        {

            // allow admin users to access all patients
            if (isAdmin(sql)) return true;

            SqlPreparedStatement statement =
                    sql.prepareStatement("SELECT * FROM clinicians_to_patients " +
                            "WHERE date_removed IS NULL AND " +
                            "clinician_id = ? AND patient_id = ?");
            statement.setInt(1, this.id);
            statement.setInt(2, patientId);

            ResultSet rs = statement.executeQuery();
            return rs.next();
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
    }

    public void setFilterInactivePatients(ReadWriteDbConnection sql, String days) throws SQLException
    {
        UserPreferences.setPreferenceForUser(sql, this.id, "FilterInactivePatients", days);
    }

    public static void addPatientToBot(ReadWriteDbConnection sql, Integer patientId)
    {
        addPatient(sql, Clinician.BOT_ID, patientId);
        addPatient(sql, Clinician.BOT_2_ID, patientId);
    }

    public static void removePatientFromBot(ReadWriteDbConnection sql, Integer patientId)
    {
        removePatient(sql, Clinician.BOT_ID, patientId, false);
        removePatient(sql, Clinician.BOT_2_ID, patientId, false);
    }
}
