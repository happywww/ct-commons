package com.constanttherapy.tasks;

import com.constanttherapy.db.*;
import com.constanttherapy.enums.ClientPlatform;
import com.constanttherapy.share.tasks.types.TaskTypeData;
import com.constanttherapy.util.CTLogger;
import com.constanttherapy.util.MathUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TaskTypeInfo extends TaskTypeData
{
    public Integer redirectTo;
    public Integer redirectFrom;
    public Integer taskTypeId; // TODO: move to TaskTypeData to replace typeId

    // default constructor
    public TaskTypeInfo()
    {
        super();
    }

    public TaskTypeInfo(ResultSet rs) throws SQLException
    {
        this();
        this.systemName = rs.getString("system_name");
        this.displayName = rs.getString("display_name");
        this.defaultTaskClassName = rs.getString("task_class_name");
        this.defaultFactoryClassName = rs.getString("factory_class_name");
        this.taskTypeId = rs.getInt("id");
        this.typeId = this.taskTypeId;  //TODO: refactor typeId

        this.availableInVersion = MathUtil.tryParseFloat(rs.getString("available_in_version"));
        this.availableInVersion_iOS = MathUtil.tryParseFloat(rs.getString("available_in_version_ios"));
        this.availableInVersion_Android = MathUtil.tryParseFloat(rs.getString("available_in_version_android"));

        this.maxLevel = rs.getInt("max_level");
        //REVIEW: [mahendra, 6/22/16 6:47 PM]: why are we setting maxLevel to 1 for assessment tasks?
        if (this.maxLevel == 0) this.maxLevel = 1; // for assessment tasks

        this.maxSet = rs.getInt("max_set");
        if (this.maxSet == 0) this.maxSet = null;  // this is a therapy task

        this.redirectFrom = MathUtil.tryParseInt(rs.getString("redirect_from"));
        this.redirectTo = MathUtil.tryParseInt(rs.getString("redirect_to"));

        this.canAssignAsHomework = (rs.getInt("hw_assignable") != 0);
        this.usesMicrophone = (rs.getInt("uses_microphone") != 0);
    }

    // --------------- STATIC HELPER FUNCTIONS ----------------------
    private static Integer maxTaskTypeId;
    private static Map<String, TaskTypeInfo>  mapBySystemName = null;
    private static Map<Integer, TaskTypeInfo> mapById         = null;

    public static boolean taskExists(String systemName, int level)
    {
        return getMapBySystemName().containsKey(systemName) && (level <= getMapBySystemName().get(systemName).maxLevel);
    }

    static Integer getMaxLevelForTaskType(String taskSystemName)
    {
        TaskTypeInfo tti = getMapBySystemName().get(taskSystemName);
        if (tti == null) throw new IllegalArgumentException("Invalid task specified: " + taskSystemName);

        return tti.maxLevel;
    }

    public static void init()
    {
        ReadOnlyDbConnection sqlr = null;
        try
        {
            sqlr = new ReadOnlyDbConnection();
            init(sqlr);
        }
        catch (SQLException e)
        {
            CTLogger.error(e);
        }
        finally
        {
            SQLUtil.closeQuietly(sqlr);
        }
    }

    public static void init(ReadOnlyDbConnection sql)
    {
        mapBySystemName = new TreeMap<>();
        mapById = new TreeMap<>();

        maxTaskTypeId = 0;

        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            String q = "SELECT redirect_from, " +
                    "       a.id, " +
                    "       redirect_id redirect_to, " +
                    "       system_name, " +
                    "       display_name, " +
                    "       max_level, " +
                    "       max_set, " +
                    "       task_class_name, " +
                    "       factory_class_name, " +
                    "       available_in_version, " +
                    "       available_in_version_ios, " +
                    "       available_in_version_android, " +
                    "       hw_assignable, " +
                    "       uses_microphone " +
                    "FROM task_types a " +
                    "LEFT JOIN (SELECT redirect_id id, id redirect_from " +
                    "FROM task_types " +
                    "WHERE redirect_id IS NOT NULL) b ON a.id = b.id " +
                    "ORDER BY id";

            statement = sql.prepareStatement(q);

            rs = statement.executeQuery();
            while (rs.next())
            {
                TaskTypeInfo tti = new TaskTypeInfo(rs);

                if (tti.taskTypeId > maxTaskTypeId)
                    maxTaskTypeId = tti.taskTypeId;

                getMapBySystemName().put(tti.systemName, tti);
                getMapById().put(tti.taskTypeId, tti);
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
    }

    public static int getTaskTypeId(String taskSystemName)
    {
        return getMapBySystemName().get(taskSystemName).taskTypeId;
    }

    public static String getTaskNameFromId(int taskId)
    {
        return getMapById().get(taskId).systemName;
    }

    public static int getMaxTaskTypeId()
    {
        return maxTaskTypeId;
    }

    static Map<String, TaskTypeInfo> getMapBySystemName()
    {
        if (mapBySystemName == null) init();
        return mapBySystemName;
    }

    private static Map<Integer, TaskTypeInfo> getMapById()
    {
        if (mapById == null) init();
        return mapById;
    }

    public static String getDisplayNameForTask(String systemName)
    {
        TaskTypeInfo tti = getMapBySystemName().get(systemName);
        return tti != null ? tti.displayName : null;
    }

    public static boolean isTaskAvailableInVersion(Integer taskTypeId, float clientVersionNumber, ClientPlatform clientOS)
    {
        TaskTypeInfo tti = getMapById().get(taskTypeId);
        return isTaskAvailableInVersion(tti, clientVersionNumber, clientOS);
    }

    public static boolean isTaskAvailableInVersion(String taskSystemName, float clientVersionNumber, ClientPlatform clientOS)
    {
        TaskTypeInfo tti = getMapBySystemName().get(taskSystemName);
        return isTaskAvailableInVersion(tti, clientVersionNumber, clientOS);
    }

    static boolean isTaskAvailableInVersion(TaskTypeInfo tti, float clientVersionNumber, ClientPlatform clientOS)
    {
        if (clientVersionNumber <= 0.0f) return true; // if version not set, assume true
        if (clientOS == null) clientOS = ClientPlatform.iOS;

        if (tti != null)
        {
            Float availableInVersion = clientOS.equals(ClientPlatform.iOS) ? tti.availableInVersion_iOS : tti.availableInVersion_Android;
            return availableInVersion != null && availableInVersion <= clientVersionNumber;
        }
        else
            return false;
    }

    public static TaskTypeInfo getAvailableTaskType(Integer taskId, Float clientVersionNumber, ClientPlatform clientPlatform)
    {
        // first look for highest redirect for the specified task Id
        TaskTypeInfo tti = findHighestVersionTaskType(taskId);

        assert (tti != null);

        // then work our way backwards to find the task that is available for the specified client OS and app version
        // [mahendra, 7/7/15 11:00 AM]: only do this if clientVersion and platform are specified

        if (clientVersionNumber != null && clientPlatform != null)
            tti = getHighestAvailableTaskType(tti.taskTypeId, clientVersionNumber, clientPlatform);

        return tti;
    }

    private static TaskTypeInfo getHighestAvailableTaskType(Integer taskId, float clientVersionNumber, ClientPlatform clientOS)
    {
        TaskTypeInfo tti = getMapById().get(taskId);

        if (!isTaskAvailableInVersion(tti, clientVersionNumber, clientOS))
        {
            if (tti.redirectFrom != null)
                tti = getHighestAvailableTaskType(tti.redirectFrom, clientVersionNumber, clientOS); // recursion
            else
                tti = null;  // no available task type found
        }

        return tti;
    }

    /**
     * For specified task type, traverses the hierarchy to find the
     * most recent version
     *
     * @param systemName of the original task
     * @return TaskTypeInfo of the highest level redirection
     */
    public static TaskTypeInfo findHighestVersionTaskType(String systemName)
    {
        TaskTypeInfo tti = getMapBySystemName().get(systemName);
        return findHighestVersionTaskType(tti.taskTypeId);
    }

    private static TaskTypeInfo findHighestVersionTaskType(Integer taskId)
    {
        TaskTypeInfo tti = getMapById().get(taskId);

        if (tti == null)
            throw new IllegalArgumentException("Invalid task Id specified: " + taskId);

        if (tti.redirectTo != null)
            tti = findHighestVersionTaskType(tti.redirectTo); // recursion

        return tti;
    }

    /**
     * Returns a list of all task types in descending order (most recent or highest task version first)
     *
     * @param taskSystemName
     * @return
     */
    public static List<String> getTaskRedirects(String taskSystemName)
    {
        List<String> tasks = new ArrayList<>();

        int id = getTaskTypeId(taskSystemName);
        TaskTypeInfo tti = findHighestVersionTaskType(id);

        while (tti != null)
        {
            tasks.add(tti.getSystemName());
            if (tti.redirectFrom != null)
                tti = TaskTypeInfo.getMapById().get(tti.redirectFrom);
            else
                tti = null;
        }

        return tasks;
    }

    @Override
    public String toString()
    {
        // return this.taskTypeId + "." + this.taskLevel;
        return this.systemName + "; maxLevel=" + this.maxLevel + "; availableInVersion=" + this.availableInVersion;
    }

    public String getSystemName()
    {
        if (this.systemName == null)
        {
            assert (this.taskTypeId != null);
            this.systemName = getSystemName(this.taskTypeId);
        }

        return this.systemName;
    }

    public static String getSystemName(Integer taskTypeId)
    {
        if (taskTypeId == null) return null;

        if (getMapById().containsKey(taskTypeId))
            return getMapById().get(taskTypeId).systemName;
        else
            return null;
    }

    public static List<TaskTypeInfo> getRedirectAncestors(int taskTypeId)
    {
        TaskTypeInfo tti = TaskTypeInfo.mapById.get(taskTypeId);
        return tti.getRedirectAncestors();
    }

    private List<TaskTypeInfo> redirectAncestors = null;

    private List<TaskTypeInfo> getRedirectAncestors()
    {
        if (this.redirectAncestors == null)
        {
            this.redirectAncestors = new ArrayList<>();
            this.redirectAncestors.add(this);
            if (this.redirectFrom != null)
            {
                // get ancestors from the previous redirector
                TaskTypeInfo tti = TaskTypeInfo.mapById.get(this.redirectFrom);
                assert (tti != null);
                this.redirectAncestors.addAll(tti.getRedirectAncestors());
            }
        }
        return this.redirectAncestors;
    }

    private List<TaskTypeInfo> redirectDescendents = null;

    private List<TaskTypeInfo> getRedirectDescendents()
    {
        if (this.redirectDescendents == null)
        {
            this.redirectDescendents = new ArrayList<>();
            this.redirectDescendents.add(this);
            if (this.redirectTo != null)
            {
                // get descendents from the previous redirector
                TaskTypeInfo tti = TaskTypeInfo.mapById.get(this.redirectTo);
                assert (tti != null);
                this.redirectDescendents.addAll(tti.getRedirectDescendents());
            }
        }
        return this.redirectDescendents;
    }

    /**
     * Gets the system name of the original task from which this one was redirected to.
     * Used for task progressions
     *
     * @param systemName
     * @return
     */
    static List<String> getRedirectAncestorTaskSystemNames(String systemName)
    {
        TaskTypeInfo tti = TaskTypeInfo.mapBySystemName.get(systemName);
        if (tti != null)
        {
            List<TaskTypeInfo> tasks = tti.getRedirectAncestors();
            return tasks.stream()
                    .filter(task -> !task.systemName.equals(systemName))
                    .map(task -> task.systemName)
                    .collect(Collectors.toList());
        }
        else
            return null;
    }

    /**
     * Gets the system name of the original task from which this one was redirected from.
     * Used for task progressions
     *
     * @param systemName
     * @return
     */
    static List<String> getRedirectDescendentTaskSystemNames(String systemName)
    {
        TaskTypeInfo tti = TaskTypeInfo.mapBySystemName.get(systemName);

        if (tti != null)
        {
            List<TaskTypeInfo> tasks = tti.getRedirectDescendents();
            return tasks.stream()
                    .filter(task -> !task.systemName.equals(systemName))
                    .map(task -> task.systemName)
                    .collect(Collectors.toList());
        }
        else
            return null;
    }

    public static TaskTypeInfo getById(int taskTypeId)
    {
        return TaskTypeInfo.getMapById().get(taskTypeId);
    }

    public static TaskTypeInfo getBySystemName(String systemName)
    {
        return TaskTypeInfo.getMapBySystemName().get(systemName);
    }

    /**
     * Returns all the versions of the specified task, by traversing
     * up and down the redirect hierarchy.
     *
     * @param systemName systemName for current task
     * @return String collection with all ancestors and descendants of the specified task (does not include the task itself)
     */
    public static Collection<String> getAllTaskVersions(String systemName)
    {
        TaskTypeInfo tti = TaskTypeInfo.mapBySystemName.get(systemName);

        List<TaskTypeInfo> ancestors = tti.getRedirectAncestors();
        Map<Integer, String> taskMap = new TreeMap<>();
        for (TaskTypeInfo task : ancestors)
        {
            taskMap.put(task.taskTypeId, task.systemName);
        }

        List<TaskTypeInfo> descendents = tti.getRedirectDescendents();

        for (TaskTypeInfo task : descendents)
        {
            taskMap.put(task.taskTypeId, task.systemName);
        }

        return taskMap.values();
    }

    public Integer getMaxLevel()
    {
        return (this.maxSet != null ? this.maxSet : this.maxLevel);
    }
}
