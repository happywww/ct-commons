package com.constanttherapy.ui;

import com.constanttherapy.share.util.ColorData;

public class Color extends ColorData
{
	public final static Color RED = new Color(255, 0, 0);
	public final static Color GREEN = new Color(0, 255, 0);
	public final static Color BLUE = new Color(0, 0, 255);

	public final static Color CORRECT = new Color(129,199,132);
	public final static Color WRONG = new Color(255,138,101);
	public final static Color SKIPPED = new Color(221,221,221);

	public Color() {
		this.R = this.G = this.B = 0;
	}

	public Color(int R, int G, int B) {
		this.R = R;
		this.G = G;
		this.B = B;
	}

	public String toHex()
	{
        String sr = Integer.toHexString(this.R);
        if (sr.length() == 1) sr = "0" + sr;

        String sg = Integer.toHexString(this.G);
        if (sg.length() == 1) sg = "0" + sg;

        String sb = Integer.toHexString(this.B);
        if (sb.length() == 1) sb = "0" + sb;

        String hex = "#" + sr + sg + sb;

        return hex.toUpperCase();
	}
}
