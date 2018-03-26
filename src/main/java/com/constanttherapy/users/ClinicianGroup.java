package com.constanttherapy.users;

import com.constanttherapy.CTCrud;
import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SQLUtil;
import com.constanttherapy.db.SqlPreparedStatement;
import com.constanttherapy.util.CTLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClinicianGroup implements CTCrud
{
    private Integer id;
    private String groupName;
    private boolean isAdminFlag;
    private boolean isSupportFlag;
    private boolean isBotFlag;

    public ClinicianGroup(ResultSet rs) throws SQLException
    {
        read(rs);
    }

    @Override
    public void create(ReadWriteDbConnection sql) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void read(DbConnection sql) throws SQLException
    {
        if (this.id == null || this.id == 0)
            throw new IllegalArgumentException("No id specified!");
        this.read(sql, this.id);
    }

    @Override
    public void read(DbConnection sql, Integer id) throws SQLException
    {
        SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM clinician_groups WHERE id = ?");
        statement.setInt(1, id);

        ResultSet rs = statement.executeQuery();

        if (rs.next())
            this.read(rs);
        else
            throw new IllegalArgumentException("Clinician Group with Id '" + id + "' does not exist.");
    }

    @Override
    public void read(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("id");
        this.groupName = rs.getString("group_name");
        this.isAdminFlag = rs.getBoolean("is_admin");
        this.isSupportFlag = rs.getBoolean("is_support");
        this.isBotFlag = rs.getBoolean("is_bot");
    }

    /**
     * returns true if the given clinician is a member of any admin group
     */
    public static boolean isAdmin(DbConnection sql, Integer clinicianId)
    {
        List<ClinicianGroup> groups = groupsForClinician(sql, clinicianId);

        for (ClinicianGroup group : groups)
        {
            if (group.isAdminFlag) return true;
        }
        return false;
    }

    static boolean isSupport(DbConnection sql, Integer clinicianId)
    {
        List<ClinicianGroup> groups = groupsForClinician(sql, clinicianId);

        for (ClinicianGroup group : groups)
        {
            if (group.isSupportFlag) return true;
        }
        return false;
    }

    static boolean isBot(DbConnection sql, Integer clinicianId)
    {
        List<ClinicianGroup> groups = groupsForClinician(sql, clinicianId);

        for (ClinicianGroup group : groups)
        {
            if (group.isBotFlag) return true;
        }
        return false;
    }

    static List<ClinicianGroup> groupsForClinician(DbConnection sql, Integer clinicianId)
    {
        try
        {
            List<ClinicianGroup> list = new LinkedList<ClinicianGroup>();

            SqlPreparedStatement statement = sql.prepareStatement(
                    "SELECT g.* FROM clinician_groups g INNER JOIN groups_to_clinicians g2c ON g.id = g2c.group_id "
                            + " WHERE g2c.clinician_id = ?");
            statement.setInt(1, clinicianId);

            ResultSet rs = statement.executeQuery();
            while (rs.next())
            {
                list.add(new ClinicianGroup(rs));
            }
            return list;
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return Collections.emptyList();
        }
    }

    private static List<ClinicianGroup> adminGroups(DbConnection sql)
    {
        boolean closeConnection = false;
        try
        {
            if (sql == null)
            {
                closeConnection = true;
                sql = new ReadWriteDbConnection();
            }
            List<ClinicianGroup> list = new LinkedList<ClinicianGroup>();

            SqlPreparedStatement statement = sql.prepareStatement("SELECT * FROM clinician_groups WHERE is_admin = 1");

            ResultSet rs = statement.executeQuery();
            while (rs.next())
            {
                list.add(new ClinicianGroup(rs));
            }
            return list;
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return Collections.emptyList();
        }
        finally
        {
            if (closeConnection) SQLUtil.closeQuietly(sql);
        }
    }

    public boolean hasClinician(DbConnection sql, Integer clinicianId)
    {
        try
        {
            SqlPreparedStatement statement =
                    sql.prepareStatement("SELECT g2c.* FROM clinician_groups g INNER JOIN groups_to_clinicians g2c ON g.id = g2c.group_id "
                            + " WHERE g.id = ? AND g2c.clinician_id = ?");
            statement.setInt(1, this.id);
            statement.setInt(2, clinicianId);

            ResultSet rs = statement.executeQuery();
            return rs.next();
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
    }

    public static boolean assignPatientToAllAdminGroups(ReadWriteDbConnection sql, Integer patientId)
    {
        boolean success = true;

        List<ClinicianGroup> adminGroups = ClinicianGroup.adminGroups(null);
        for (ClinicianGroup g : adminGroups)
        {
            success &= g.assignPatient(sql, patientId);
        }

        return success;
    }

    private boolean assignPatient(ReadWriteDbConnection sql, Integer patientId)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("CALL AssignPatientToGroup(?, ?)");
            statement.setInt(1, patientId);
            statement.setInt(2, this.id);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
    }

    public boolean removePatient(ReadWriteDbConnection sql, Integer patientId)
    {
        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("CALL RemovePatientFromGroup(?, ?)");
            statement.setInt(1, patientId);
            statement.setInt(2, this.id);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
            return false;
        }
    }

    @Override
    public void update(ReadWriteDbConnection sql) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(ReadWriteDbConnection sql) throws SQLException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
}
