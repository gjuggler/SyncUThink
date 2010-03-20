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

import java.awt.geom.Rectangle2D;

import org.andrewberman.ui.ifaces.Malleable;

public class UIRectangle extends Rectangle2D.Float
{

	public UIRectangle(float x, float y, float w, float h)
	{
		super(x,y,w,h);
	}
	
	public UIRectangle()
	{
		super();
	}

	public UIRectangle(Malleable m)
	{
		super(m.getX(),m.getY(),m.getWidth(),m.getHeight());
	}
	
	public float distToPoint(Point pt)
	{
		float px = pt.x;
		float py = pt.y;
		/*
		 * First, look for the "easy" case where we can do a straight subtraction.
		 */
		if (px >= x && px <= x+width && py <= y)
			return y - py;
		if (px >= x && px <= x+width && py >= y+height)
			return py - (y+height);
		if (py >= y && py <= y+height && px <= x)
			return x-px;
		if (py >= y && py <= y+height && px >= x+width)
			return px - (x+width);
		/*
		 * If we contain this rectangle, return 0
		 */
		if (this.contains(pt)) return 0;
		/*
		 * Now, handle the "hard" case, where we have to calculate from the corners.
		 */
		float mx = 0, my = 0;
		if (px < x)
			mx = x;
		else if (px > x+width)
			mx = x+width;
		if (py < y)
			my = y;
		else if (py > y+height)
			my = y+height;
		return (float) Math.sqrt( (px-mx)*(px-mx) + (py-my)*(py-my));
	}
	
	public void translate(float x, float y)
	{
		setRect(this.x+x, this.y+y, width, height);
	}
}
