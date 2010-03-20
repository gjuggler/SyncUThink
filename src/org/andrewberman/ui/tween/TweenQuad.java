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
package org.andrewberman.ui.tween;

public final class TweenQuad implements TweenFunction
{

	public static TweenQuad tween;
	
	static {
		tween = new TweenQuad();
	}
	
	public boolean isFinished(float t, float p, float b, float c, float d)
	{
		return (t >= d);
	}
	
	public float easeIn(float t, float p, float b, float c, float d)
	{
		return c*(t/=d)*t + b;
	}

	public float easeOut(float t, float p, float b, float c, float d)
	{
		return -c *(t/=d)*(t-2) + b;
	}

	public float easeInOut(float t, float p, float b, float c, float d)
	{
		if ((t/=d/2) < 1) return c/2*t*t + b;
		return -c/2 * ((--t)*(t-2) - 1) + b;
	}

}
