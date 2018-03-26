package com.constanttherapy.users;

import com.constanttherapy.ServiceBase;
import com.constanttherapy.db.*;
import com.constanttherapy.enums.UserEventType;
import com.constanttherapy.util.CTLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserEventLogger
{
    private UserEventLogger() {}

    public static class EventBuilder
    {
        final UserEventType type;
        String subType;
        String username;
        Integer userId;
        String eventData;
        Timestamp timestamp;
        String notes;

        private EventBuilder(UserEventType type)
        {
            assert (type != null);
            this.type = type;
        }

        public static EventBuilder create(UserEventType type)
        {
            if (type == null)
            {
                throw new IllegalArgumentException("type==null");
            }
            return new EventBuilder(type);
        }

        public EventBuilder subType(String subType)
        {
            this.subType = subType;
            return this;
        }

        public EventBuilder username(String username)
        {
            this.username = username;
            return this;
        }

        public EventBuilder eventData(String eventData)
        {
            this.eventData = eventData;
            return this;
        }

        public EventBuilder userId(int userId)
        {
            this.userId = userId;
            return this;
        }

        public EventBuilder timestamp(Timestamp timestamp)
        {
            this.timestamp = timestamp;
            return this;
        }

        public EventBuilder notes(String notes)
        {
            this.notes = notes;
            return this;
        }

        public Integer log()
        {
            return log(null);
        }

        public Integer log(ReadWriteDbConnection sql)
        {
            if (this.userId == null && this.username == null)
            {
                throw new IllegalStateException("userId==null&&username==null");
            }

            boolean closeConnection = false;
            try
            {
                if (sql == null)
                {
                    closeConnection = true;
                    sql = new ReadWriteDbConnection();
                }

                if (this.userId == null)
                {
                    this.userId = CTUser.getIdFromUsername(sql, this.username);
                }

                return UserEventLogger.logEvent(sql, this.type, this.subType, this.userId, this.eventData, this.timestamp, this.notes);
            }
            catch (SQLException e)
            {
                CTLogger.error(e);
                return null;
            }
            finally
            {
                if (closeConnection) SQLUtil.closeQuietly(sql);
            }
        }
    }

    /**
     * Logs event for user id
     *
     * @param sql
     * @param type
     * @param subType
     * @param userId
     * @param eventData
     * @param timestamp
     */
    public static Integer logEvent(ReadWriteDbConnection sql, UserEventType type, String subType, int userId, String eventData, Timestamp timestamp, String notes)
    {
        CTLogger.info("UserEventLogger::logEvent() - userId=" + userId + ", eventType=" + type.name() + ", eventData=" + eventData);

        Integer eventId = null;

        SqlPreparedStatement statement = null;
        ResultSet rs = null;
        boolean closeConnection = false;

        try
        {
            if (sql == null)
            {
                closeConnection = true;
                sql = new ReadWriteDbConnection();
            }

            String query;
            if (timestamp == null)
                query = "INSERT INTO user_events (user_id, event_type_id, event_data, event_sub_type, notes, system_version_number) VALUES (?,?,?,?,?,?)";
            else
                query = "INSERT INTO user_events (user_id, event_type_id, event_data, event_sub_type, notes, system_version_number, timestamp) VALUES (?,?,?,?,?,?,?)";

            statement = sql.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, userId);
            statement.setInt(2, type.getValue());

            if (eventData == null)
                statement.setNull(3, Types.VARCHAR);
            else
                statement.setString(3, eventData);

            if (subType == null)
                statement.setNull(4, Types.VARCHAR);
            else
                statement.setString(4, subType);

            if (notes == null)
                statement.setNull(5, Types.VARCHAR);
            else
                statement.setString(5, notes);

            statement.setString(6, ServiceBase.getServerVersion());

            if (timestamp != null)
                statement.setTimestamp(7, timestamp);

            boolean success = statement.executeUpdate() > 0;
            if (success)
            {
                rs = statement.getGeneratedKeys();
                if (rs.next())
                {
                    eventId = rs.getInt(1);
                }
            }
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            // we may have been passed in an sql connection, in which
            // case we are not responsible for closing it
            if (closeConnection) SQLUtil.closeQuietly(sql);
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(rs);
        }

        return eventId;
    }

    @Deprecated
    public static Integer logEvent(UserEventType type, int userId, String eventData)
    {
        return EventBuilder.create(type).userId(userId).eventData(eventData).log();
    }

    @Deprecated
    public static Integer logEvent(UserEventType type, int userId)
    {
        return EventBuilder.create(type).userId(userId).log();
    }

    /**
     * Helper function to log emails sent to clinicians and patients.  Used
     * by messaging service, where userIds are not available
     */
    private static Integer logEmailEvent(ReadWriteDbConnection sql, int userId, String emailType, String emailData)
    {
        try
        {
            return logEvent(sql, UserEventType.EmailSent, emailType, userId, emailData, null, null);
        }
        catch (Exception e)
        {
            CTLogger.error(e);
            return null;
        }
    }

    public static List<Integer> logEmailEvent(ReadWriteDbConnection sql, List<Integer> userIds, String template, String eventData)
    {
        boolean closeConnection = false;
        List<Integer> eventIds = new ArrayList<>(userIds.size());
        try
        {
            if (sql == null)
            {
                closeConnection = true;
                sql = new ReadWriteDbConnection();
            }

            for (Integer userId : userIds)
            {
                eventIds.add(logEmailEvent(sql, userId, template, eventData));
            }
        }
        catch (Exception e)
        {
            CTLogger.error(e);
        }
        finally
        {
            if (closeConnection) SQLUtil.closeQuietly(sql);
        }
        return eventIds;
    }

    /**
     * Add event_data and notes to an event. Can be used to update data/notes after the event
     * has been created by some other service (e.g., MessagingService)
     *
     * @param sql
     * @param userId
     * @param type
     * @param subType
     * @param eventData
     * @param notes
     */
    public static void setEventDataOnLatestEvent(ReadWriteDbConnection sql, int userId, UserEventType type, String subType, String eventData, String notes)
    {
        SqlPreparedStatement statement = null;
        SqlPreparedStatement stmt2 = null;
        ResultSet rs = null;

        boolean closeConnection = false;

        try
        {
            String q = "SELECT MAX(id) latest_id FROM user_events "
                    + "WHERE user_id = ? AND event_type_id = ? "
                    + "AND event_sub_type = ? "
                    + "AND event_data IS NULL";

            if (sql == null)
            {
                closeConnection = true;
                sql = new ReadWriteDbConnection();
            }

            statement = sql.prepareStatement(q);
            statement.setInt(1, userId);
            statement.setInt(2, type.getValue());
            statement.setString(3, subType);

            rs = statement.executeQuery();
            if (rs.next())
            {
                int id = rs.getInt("latest_id");

                q = "UPDATE user_events SET event_data = ?, notes = ? WHERE id = ?";
                stmt2 = sql.prepareStatement(q);

                if (eventData == null)
                    stmt2.setNull(1, Types.VARCHAR);
                else
                    stmt2.setString(1, eventData);

                if (notes == null)
                    stmt2.setNull(2, Types.VARCHAR);
                else
                    stmt2.setString(2, notes);

                stmt2.setInt(3, id);

                CTLogger.debug(stmt2.toString(), 2);
                stmt2.executeUpdate();
            }
        }
        catch (Exception ex)
        {
            CTLogger.error(ex);
        }
        finally
        {
            if (closeConnection) SQLUtil.closeQuietly(sql);
            SQLUtil.closeQuietly(statement);
            SQLUtil.closeQuietly(stmt2);
            SQLUtil.closeQuietly(rs);
        }
    }
}
