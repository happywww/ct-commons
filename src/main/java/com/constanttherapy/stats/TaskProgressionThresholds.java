package com.constanttherapy.stats;

import com.constanttherapy.db.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by madvani on 11/4/16 6:25 PM 6:26 PM.
 */
public class TaskProgressionThresholds
{
    private static Map<String, TaskProgressionThresholds> map = null;
    public final int taskTypeId;
    public final int taskLevel;
    public final ThresholdValues t1 = new ThresholdValues();
    public final ThresholdValues t2 = new ThresholdValues();
    public final ThresholdValues t3 = new ThresholdValues();
    public final ThresholdValues t4 = new ThresholdValues();

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskProgressionThresholds tv = (TaskProgressionThresholds) o;

        return this.taskTypeId == tv.taskTypeId
            && this.taskLevel == tv.taskLevel
            && this.t1.equals(tv.t1)
            && this.t2.equals(tv.t2)
            && this.t3.equals(tv.t3)
            && this.t4.equals(tv.t4);
    }

    @Override
    public int hashCode()
    {
        int result = this.taskTypeId;
        result = 31 * result + this.taskLevel;
        result = 31 * result + this.t1.hashCode();
        result = 31 * result + this.t2.hashCode();
        result = 31 * result + this.t3.hashCode();
        result = 31 * result + this.t4.hashCode();
        return result;
    }

    public class ThresholdValues
    {
        public Integer sessionCount = null;
        public Integer taskCount    = null;
        public Float   accuracyHigh = null;
        public Float   accuracyLow  = null;
        public Float   latencyHigh  = null;
        Float   latencyLow   = null;

        @Override
        public String toString()
        {
            return "ThresholdValues{" +
                    "sessionCount=" + this.sessionCount +
                    ", taskCount=" + this.taskCount +
                    ", accuracyHigh=" + this.accuracyHigh +
                    ", accuracyLow=" + this.accuracyLow +
                    ", latencyHigh=" + this.latencyHigh +
                    ", latencyLow=" + this.latencyLow +
                    '}';
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ThresholdValues tv = (ThresholdValues) o;

            return (this.sessionCount != null ? this.sessionCount.equals(tv.sessionCount) : tv.sessionCount == null)
                && (this.taskCount != null ? this.taskCount.equals(tv.taskCount) : tv.taskCount == null)
                && (this.accuracyHigh != null ? this.accuracyHigh.equals(tv.accuracyHigh) : tv.accuracyHigh == null)
                && (this.accuracyLow != null ? this.accuracyLow.equals(tv.accuracyLow) : tv.accuracyLow == null)
                && (this.latencyHigh != null ? this.latencyHigh.equals(tv.latencyHigh) : tv.latencyHigh == null)
                && (this.latencyLow != null ? this.latencyLow.equals(tv.latencyLow) : tv.latencyLow == null);
        }

        @Override
        public int hashCode()
        {
            int result = this.sessionCount != null ? this.sessionCount.hashCode() : 0;
            result = 31 * result + (this.taskCount != null ? this.taskCount.hashCode() : 0);
            result = 31 * result + (this.accuracyHigh != null ? this.accuracyHigh.hashCode() : 0);
            result = 31 * result + (this.accuracyLow != null ? this.accuracyLow.hashCode() : 0);
            result = 31 * result + (this.latencyHigh != null ? this.latencyHigh.hashCode() : 0);
            result = 31 * result + (this.latencyLow != null ? this.latencyLow.hashCode() : 0);
            return result;
        }
    }

    private TaskProgressionThresholds(ResultSet rs) throws SQLException
    {
        this.taskLevel = rs.getInt("task_level");
        this.taskTypeId = rs.getInt("task_type_id");
        this.t1.accuracyHigh = SQLUtil.getFloat(rs, "t1_acc_high");
        this.t1.accuracyLow = SQLUtil.getFloat(rs, "t1_acc_low");
        this.t1.latencyHigh = SQLUtil.getFloat(rs, "t1_lat_high");
        this.t1.latencyLow = SQLUtil.getFloat(rs, "t1_lat_low");
        this.t2.accuracyHigh = SQLUtil.getFloat(rs, "t2_acc_high");
        this.t2.accuracyLow = SQLUtil.getFloat(rs, "t2_acc_low");
        this.t2.latencyHigh = SQLUtil.getFloat(rs, "t2_lat_high");
        this.t2.latencyLow = SQLUtil.getFloat(rs, "t2_lat_low");
        this.t3.accuracyHigh = SQLUtil.getFloat(rs, "t3_acc_high");
        this.t3.accuracyLow = SQLUtil.getFloat(rs, "t3_acc_low");
        this.t1.sessionCount = SQLUtil.getInteger(rs, "t1_session_count");
        this.t2.sessionCount = SQLUtil.getInteger(rs, "t2_session_count");
        this.t3.sessionCount = SQLUtil.getInteger(rs, "t3_session_count");
        this.t4.sessionCount = SQLUtil.getInteger(rs, "t4_session_count");
        this.t3.taskCount = SQLUtil.getInteger(rs, "t3_task_count");
        this.t4.taskCount = SQLUtil.getInteger(rs, "t4_task_count");
    }

    public static void load() throws SQLException
    {
        ReadWriteDbConnection sql = null;
        SqlPreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            sql = new ReadWriteDbConnection();
            //String query = "SELECT * FROM ct_stats.task_progression_thresholds;";
            // [godlove, 3/1/18 4:35 PM]: modified query to auto include any new versions of the task that comes after the thresholds are set

            String query = "SELECT  "
                + "    c.tt_id AS task_type_id, "
                + "    c.task_level, "
                + "    tpt.`t1_session_count`, "
                + "    tpt.`t1_acc_low`, "
                + "    tpt.`t1_acc_high`, "
                + "    tpt.`t1_lat_low`, "
                + "    tpt.`t1_lat_high`, "
                + "    tpt.`t2_session_count`, "
                + "    tpt.`t2_acc_low`, "
                + "    tpt.`t2_acc_high`, "
                + "    tpt.`t2_lat_low`, "
                + "    tpt.`t2_lat_high`, "
                + "    tpt.`t3_session_count`, "
                + "    tpt.`t3_acc_low`, "
                + "    tpt.`t3_acc_high`, "
                + "    tpt.`t3_task_count`, "
                + "    tpt.`t4_session_count`, "
                + "    tpt.`t4_task_count` "
                + "FROM "
                + "    (SELECT  "
                + "        b.tt_id, "
                + "            b.root_id, "
                + "            MAX(task_type_id) AS thres_id, "
                + "            b.task_level, "
                + "            b.t1_acc_low "
                + "    FROM "
                + "        (SELECT  "
                + "        a.*, IFNULL(tt.id, a.task_type_id) AS tt_Id "
                + "    FROM "
                + "        (SELECT  "
                + "        thr.task_type_id, thr.task_level, thr.t1_acc_low, tt.root_id "
                + "    FROM "
                + "        ct_stats.task_progression_thresholds thr "
                + "    LEFT JOIN task_types tt ON tt.id = thr.task_type_id) a "
                + "    LEFT JOIN task_types tt ON tt.root_id = a.root_id "
                + "        AND tt.id >= a.task_type_id "
                + "    ORDER BY root_id ASC , tt_id DESC) b "
                + "    GROUP BY tt_id , task_level) c "
                + "        JOIN "
                + "    ct_stats.task_progression_thresholds tpt ON tpt.task_type_id = c.thres_id "
                + "        AND tpt.task_level = c.task_level";
            statement = sql.prepareStatement(query);

            rs = statement.executeQuery();

            map = new HashMap<>();

            while (rs.next())
            {
                TaskProgressionThresholds tpt = new TaskProgressionThresholds(rs);
                map.put(getKey(tpt), tpt);
            }
        }
        finally
        {
            SQLUtil.closeQuietly(sql);
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }
    }

    private static String getKey(TaskProgressionThresholds tpt)
    {
        return getKey(tpt.taskTypeId, tpt.taskLevel);
    }

    private static String getKey(int taskTypeId, int taskLevel)
    {
        return taskTypeId + "." + taskLevel;
    }

    public static TaskProgressionThresholds get(int taskTypeId, int taskLevel) throws SQLException
    {
        if (map == null)
        {
            load();
        }

        String key = getKey(taskTypeId, taskLevel);
        if (map.containsKey(key))
            return map.get(key);
        else
        {
            // see if we have a common config for all levels of the task
            key = getKey(taskTypeId, 0);
            if (map.containsKey(key))
                return map.get(key);
            else
                // just use defaults
                return map.get(getKey(0, 0));
        }
    }

    @Override
    public String toString()
    {
        return "TaskProgressionThresholds{" +
                "taskTypeId=" + this.taskTypeId +
                ", taskLevel=" + this.taskLevel +
                ", t1=" + this.t1 +
                ", t2=" + this.t2 +
                ", t3=" + this.t3 +
                ", t4=" + this.t4 +
                '}';
    }

    public static String dump()
    {
        StringBuilder sb = new StringBuilder();
        map.values()
                .forEach(t -> sb.append(t.toString()).append("\n"));

        return sb.toString();
    }
}
