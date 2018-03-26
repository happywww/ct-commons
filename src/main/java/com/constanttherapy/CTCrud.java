/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
*/

package com.constanttherapy;

import com.constanttherapy.db.DbConnection;
import com.constanttherapy.db.ReadWriteDbConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * CRUD API interface supported by CT entity classes.
 */
public interface CTCrud
{
	String tableName = null;
	// Create
	void create(ReadWriteDbConnection sql) throws SQLException;
	// Read
	void read(DbConnection sql) throws SQLException, IllegalArgumentException;
	void read(DbConnection sql, Integer id) throws SQLException, IllegalArgumentException;
	void read(ResultSet rs) throws SQLException;
	// Update
	void update(ReadWriteDbConnection sql) throws SQLException;
	// Delete
	void delete(ReadWriteDbConnection sql) throws SQLException;
}
