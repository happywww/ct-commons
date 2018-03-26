package com.constanttherapy.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Phrase
{
	public int id;
	public String text;
	public String soundPath;
	public String imagePath;
	
	public Phrase(ResultSet rs) throws SQLException
	{
		id = rs.getInt("id");
		text = rs.getString("phrase");
		soundPath = rs.getString("sound_path");
		imagePath = rs.getString("image_path");
	}
}
