/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
*/
package com.constanttherapy.tasks;

import com.constanttherapy.db.*;
import com.constanttherapy.util.*;

import java.sql.*;
import java.util.*;

public class ScheduledTaskType extends ScheduledTaskTypeLite // extends TaskTypeBase
{
    private String displayName;

    // default constructor for testing
    protected ScheduledTaskType()
    {
    }

    public ScheduledTaskType(ResultSet rs) throws SQLException
    {
        read(rs);
        this.displayName = rs.getString("display_name");
    }

    /**
     * Reads/refreshes ScheduledTask object from specified database record
     *
     * @param rs ResultSet with record from scheduled_task_types table
     * @throws SQLException
     */
    public void read(ResultSet rs) throws SQLException
    {
        this.id = rs.getInt("id");
        this.scheduleId = rs.getInt("schedule_id");
        this.taskTypeId = rs.getInt("task_type_id");
        this.taskCount = rs.getInt("task_count");
        this.taskOrder = rs.getInt("task_order");
        this.taskLevel = rs.getInt("task_level");

        this.setParameters(rs.getString("parameters_json"));

        // this.displayName = rs.getString("display_name");  // from join to task_types table, if made
    }

    public ScheduledTaskType(int taskTypeId, int taskCount, int taskLevel)
    {
        this.taskTypeId = taskTypeId;
        this.taskCount = taskCount;
        this.taskLevel = taskLevel;
    }

    // copy constructor
    public ScheduledTaskType(ScheduledTaskTypeLite stl)
    {
        this(stl.scheduleId, stl.taskTypeId, stl.taskCount, stl.taskOrder, stl.taskLevel, stl.getParameters());
    }

    public ScheduledTaskType(Integer scheduleId, Integer taskTypeId, Integer taskCount, Integer taskOrder, int taskLevel, Map<String, String>
            parameters)
    {
        this.scheduleId = scheduleId;
        this.taskTypeId = taskTypeId;
        this.taskCount = taskCount;
        this.taskOrder = taskOrder;
        this.taskLevel = taskLevel;

        if (parameters != null)
        {
            this.setParameters(parameters);
            this.getParameters().put("level", Integer.toString(this.taskLevel));
        }
    }

    public static void deleteTasksBySchedule(ReadWriteDbConnection sql, Integer scheduleId) throws SQLException
    {
        CTLogger.infoStart("ScheduledTaskType::deleteTasksBySchedule() - scheduleId=" + scheduleId);
        CTLogger.unindent();

        SqlPreparedStatement statement = sql
                .prepareStatement("DELETE FROM scheduled_task_types WHERE schedule_id = ?");
        statement.setInt(1, scheduleId);

        statement.execute();
    }

    // TODO move this to a factory class
    public static List<ScheduledTaskType> getTaskTypesForSchedule(ReadOnlyDbConnection sql, Integer scheduleId) throws SQLException
    {
        CTLogger.infoStart("ScheduledTaskType::getTaskTypesForSchedule() - scheduleId=" + scheduleId);
        try
        {
            List<ScheduledTaskType> tasks = new ArrayList<>();

            SqlPreparedStatement statement = sql.prepareStatement("SELECT a.*, b.display_name FROM scheduled_task_types a, " +
                    "task_types b WHERE a.task_type_id = b.id AND schedule_id = ? ORDER BY task_order;");
            statement.setInt(1, scheduleId);

            ResultSet rs = statement.executeQuery();
            while (rs.next())
            {
                tasks.add(new ScheduledTaskType(rs));
            }

            return tasks;
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    public static void deleteTaskTypesForSchedule(ReadWriteDbConnection sql, Integer scheduleId) throws SQLException
    {
        CTLogger.infoStart("ScheduledTaskType::deleteTaskTypesForSchedule() - scheduleId=" + scheduleId);

        try
        {
            SqlPreparedStatement statement = sql.prepareStatement("DELETE FROM scheduled_task_types WHERE schedule_id = ?");
            statement.setInt(1, scheduleId);

            statement.execute();
        }
        finally
        {
            CTLogger.unindent();
        }
    }

    public void validate()
    {
        this.taskCount = MathUtil.clamp(this.taskCount, 0, maxTaskCount);
    }

    /**
     * Updates the database record for this object.
     *
     * @param sql SQL Connection
     * @throws SQLException
     */
    public synchronized void update(ReadWriteDbConnection sql) throws SQLException
    {
        // Do a create if this doesn't already exist in the database:

        System.out.println("scheduled task type id: " + this.id);

        if (this.id == null || this.id == 0)
        {
            System.out.println("Creating");
            create(sql);
            return;
        }

        SqlPreparedStatement statement = sql.prepareStatement("UPDATE scheduled_task_types SET schedule_id = ?, task_type_id = ?, "
                + "task_count = ?, " + "task_order = ?, " + "task_level = ?, " + "parameters_json = ? " + "WHERE id = ?");
        statement.setInt(1, this.scheduleId);
        statement.setInt(2, this.taskTypeId);
        statement.setInt(3, this.taskCount);
        statement.setInt(4, this.taskOrder);
        statement.setInt(5, this.taskLevel);

        // REVIEW: this is mainly here for backwards-compatibility
        this.getParameters().put("level", Integer.toString(this.taskLevel));

        String parametersJson = GsonHelper.toJson(this.getParameters());
        statement.setString(6, parametersJson);

        statement.setInt(7, this.id);
        statement.executeUpdate();
        CTLogger.indent();
        CTLogger.infoEnd("ScheduledTaskType::update() - id=" + this.id);
    }

    /**
     * Persists the ScheduledTask object into the database.
     *
     * @param sql SQL Connection
     * @throws SQLException
     */
    public synchronized void create(ReadWriteDbConnection sql) throws SQLException
    {
        String q = "INSERT INTO scheduled_task_types "
                + "(schedule_id, task_type_id, task_count, task_order, task_level, parameters_json) "
                + "VALUES (?,?,?,?,?,?)";

        SqlPreparedStatement statement = sql.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);

        statement.setInt(1, this.scheduleId);
        statement.setInt(2, this.taskTypeId);
        statement.setInt(3, this.taskCount);
        statement.setInt(4, this.taskOrder);
        statement.setInt(5, this.taskLevel);

        this.getParameters().put("level", Integer.toString(this.taskLevel));

        String parametersJson = GsonHelper.toJson(this.getParameters());
        statement.setString(6, parametersJson);

        boolean success = statement.executeUpdate() > 0;
        if (success)
        {
            // get stt id
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next())
                this.id = rs.getInt(1);
        }

        CTLogger.indent();
        CTLogger.infoEnd("ScheduledTaskType::create() - id=" + this.id);
    }

    /**
     * Deletes scheduled task from database.
     *
     * @param sql SQL Connection
     */
    public void delete(ReadWriteDbConnection sql) throws SQLException
    {
        CTLogger.infoStart("ScheduledTaskType::delete() - id=" + this.id);
        CTLogger.unindent();
        SQLUtil.deleteRecord(sql, "scheduled_task_types", this.id);
    }

    @Override
    public String toString()
    {
        return String.format("TaskType id:%s, name:%s, level:%s, count:%s, order:%s",
                this.taskTypeId,
                TaskTypeInfo.getSystemName(this.taskTypeId),
                this.taskLevel,
                this.taskCount,
                this.taskOrder);
    }
}
