package com.constanttherapy.util;

import com.constanttherapy.ui.Color;

public class ColorUtil
{
	private ColorUtil() {} // private constructor
	
	public static String getRGBString(float value, float mean)
	{	
		Color col = getRGB(value, mean);
		
		return col.R + "," + col.G + "," + col.B;
	}
	
	public static Color getRGB(float value, float mean)
	{
		int max = 233;
		
		Color col = new Color();
		col.B = 0;
		
		if (value <= mean)
		{
			col.R = max;
			col.G = (int) Math.floor(max * value / mean);
		}
		else
		{
			col.G = max;
			col.R = (int) Math.floor(max * (1 - value)/ (1 - mean));
		}
		
		return col;
	}

}
