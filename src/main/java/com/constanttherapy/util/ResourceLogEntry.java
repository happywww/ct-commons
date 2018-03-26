package com.constanttherapy.util;

import com.constanttherapy.CTCrud;
import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;
import com.constanttherapy.db.SqlPreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class ResourceLogEntry implements CTCrud {

	private enum ResourceLogEntryType {
		CREATE, UPDATE, DELETE
	}

	transient int                  id;
	private String resourcePath;
	private Boolean important;
	transient ResourceLogEntryType type;
	transient Timestamp timestamp;

	ResourceLogEntry(ResultSet rs) throws SQLException {
		read(rs);
	}

	@Override
	public void create(ReadWriteDbConnection sql) throws SQLException {

		CTLogger.indent();

		SqlPreparedStatement statement = sql.prepareStatement("insert into resource_update_log " +
				"(resource_path, type, timestamp) values(?,?,?)",
				Statement.RETURN_GENERATED_KEYS);

		statement.setString(1, this.resourcePath);
		statement.setString(2, this.type.toString());

		if (this.timestamp == null) {
			this.timestamp = TimeUtil.timeNow();
		}

		statement.setTimestamp(3, this.timestamp);

		boolean success = statement.executeUpdate() > 0;
		if (success)
		{
			ResultSet rs = statement.getGeneratedKeys();
			if (rs.next())
				this.id = rs.getInt(1);
		}

		CTLogger.infoEnd("ResourceLogEntry::create() - id=" + this.id);
	}

	@Override
	public void read(DbConnection sql) throws SQLException, IllegalArgumentException {

		read(sql, this.id);
	}

	@Override
	public void read(DbConnection sql, Integer id) throws SQLException, IllegalArgumentException {

		SqlPreparedStatement statement = sql.prepareStatement("select * from resource_update_log where id = " + this.id);
		ResultSet rs = statement.executeQuery();

		if (rs.next())
			read(rs);
		else
			throw new IllegalArgumentException("Resource Log Entry with id '" + this.id + "' not found.");

	}

	@Override
	public void read(ResultSet rs) throws SQLException {

		this.id = rs.getInt("id");
		this.resourcePath = rs.getString("resource_path");
		this.type = ResourceLogEntryType.valueOf(rs.getString("type"));
		this.timestamp = rs.getTimestamp("timestamp");
		this.important = rs.getBoolean("important");
	}

	@Override
	public void update(ReadWriteDbConnection sql) throws SQLException {
		// TODO Auto-generated method stub
	}

	@Override
	public void delete(ReadWriteDbConnection sql) throws SQLException {
		// TODO Auto-generated method stub
	}

}
