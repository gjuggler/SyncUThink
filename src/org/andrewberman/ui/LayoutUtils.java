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

import org.andrewberman.ui.ifaces.Malleable;
import org.andrewberman.ui.ifaces.Positionable;

public class LayoutUtils
{

	public static void alignRight (Malleable moveMe, Malleable alignToMe)
	{
		alignRight(moveMe, new UIRectangle(alignToMe));
	}
	
	public static void alignRight(Malleable moveMe, UIRectangle r)
	{
		float hiX = (float) (r.getX()+r.getWidth());
		moveMe.setX(hiX-moveMe.getWidth());
	}

	public static void centerHorizontal(Malleable m, UIRectangle r)
	{
		centerHorizontal(m, r.x, r.x+r.width);
	}
	
	public static void centerHorizontal(Malleable m, float lo, float hi)
	{
		float offsetX = ((hi-lo)-(m.getWidth()))/2;
		m.setPosition(lo+offsetX, m.getY());
	}
	
	public static void centerVertical(Malleable m, float lo, float hi)
	{
		float offsetY = ((hi-lo)-(m.getHeight()))/2;
		m.setPosition(m.getX(), lo+offsetY);
	}
	
	public static void center2D(Malleable m, float x, float y, float w, float h)
	{
		
	}
	
}
