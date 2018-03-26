package com.constanttherapy.util;

import com.constanttherapy.ui.Color;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageMap
{
	// RGBA list indices for parsing from comma separated string.
	private transient static final int R = 0;
	private transient static final int G = 1;
	private transient static final int B = 2;
	// private transient static final int A = 3; 	// Alpha value is for future versions.

	public String imagePath;
	//TODO maybe hotspots should be a map
	// public Map<String, Hotspot> hotspots;
	public ArrayList<Hotspot> hotspots;

	public static class Hotspot
	{
		public String name;
		public double x;
		public double y;
		public double width;
		public double height;
		String fontName;
		Color textColor;
		Color backgroundColor;
		public Object value = null;
		public ValueType valueType;
		public String text = null;
		//public boolean interactive;
		//public boolean autofill;
		public String align;
		public boolean hidden;

		transient public String constraint;

		public Hotspot(ResultSet rs) throws SQLException
		{
			this.name = rs.getString("name");
			this.x = rs.getDouble("x");
			this.y = rs.getDouble("y");
			this.width = rs.getDouble("w");
			this.height = rs.getDouble("h");
			this.fontName = rs.getString("font_name");
			this.valueType = (rs.getString("value_type").equals("list") ? Hotspot.ValueType.ListItem : Hotspot.ValueType.Expression);
			this.align = rs.getString("align");

			// Font and background colors are stored as comma separated strings.
			List<String> fontRGBA = Arrays.asList(rs.getString("font_rgba").split(","));
			List<String> backgroundRGBA = Arrays.asList(rs.getString("background_rgba").split(","));

			// Parse string lists into integer values for ColorUtil.RGB.
			this.textColor = new Color(	MathUtil.tryParseInt(fontRGBA.get(R)),
					MathUtil.tryParseInt(fontRGBA.get(G)),
					MathUtil.tryParseInt(fontRGBA.get(B)));
			this.backgroundColor = new Color(	MathUtil.tryParseInt(backgroundRGBA.get(R)),
					MathUtil.tryParseInt(backgroundRGBA.get(G)),
					MathUtil.tryParseInt(backgroundRGBA.get(B)));

			// Hotspots can be set to hidden in a *_task_vars table.
			this.hidden = rs.getBoolean("hidden");

			String val = rs.getString("value").trim();

			if (this.valueType == ValueType.ListItem)
			{
				List<String> values = GsonHelper.listFromJson(val);
				this.value = values.get((int) (Math.random() * values.size()));
				this.constraint = null;
			}
			else
			{
				this.value = null;
				this.constraint = val + (val.endsWith(";") ? "" : ";");
			}
		}

		public enum ValueType
		{
			/**
			 * Value is evaluated from an expression
			 */
			Expression,

			/**
			 * Value is an item from a list of predefined values
			 */
			ListItem
		}

		@Override
		public String toString()
		{
			return "Hotspot{" +
					"name='" + this.name + '\'' +
					", valueType=" + this.valueType +
					", constraint='" + this.constraint + '\'' +
					", value=" + this.value +
					'}';
		}
	}
}
