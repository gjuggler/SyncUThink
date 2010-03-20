/**************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of PhyloWidget.
 * 
 * PhyloWidget is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PhyloWidget is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with PhyloWidget.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.ui;

/**
 * <code>Color</code> is a lame extension of <code>java.awt.Color</code>, with a few
 * extra convenience functions tacked on. There should probably be a better way of doing
 * this, but I'm too lazy right now...
 * @author Greg
 * @see		java.awt.Color
 */
public final class Color extends java.awt.Color
{
	private static final long serialVersionUID = 1L;

	public Color(java.awt.Color c)
	{
		super(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
	}
	
	public Color(int r, int g, int b)
	{
		super(r, g, b);
	}

	public Color(int r, int g, int b, int a)
	{
		super(r,g,b,a);
	}
	
	public Color brighter(double diff)
	{
		int r = getRed();
        int g = getGreen();
        int b = getBlue();
        return new Color(Math.min((int)(r+diff), 255),
                Math.min((int)(g+diff), 255),
                Math.min((int)(b+diff), 255));
	}
	
	public Color darker(double diff)
	{
		int r= getRed();
		int g = getGreen();
		int b = getBlue();
		return new Color(Math.max((int)(r-diff), 0),
                Math.max((int)(g-diff), 0),
                Math.max((int)(b-diff), 0));
	}
	
	public Color inverse()
	{
		int r = 255 - getRed();
		int g = 255 - getGreen();
		int b = 255 - getBlue();
		return new Color(r,g,b);
	}
	
	public static Color parseColor(String s)
	{
		s = s.replaceAll("[()]", "");
		String[] rgb = s.split(",");
		Color color = new Color(Integer.parseInt(rgb[0]),
				Integer.parseInt(rgb[1]), Integer
						.parseInt(rgb[2]));
		return color;
	}
	
}
