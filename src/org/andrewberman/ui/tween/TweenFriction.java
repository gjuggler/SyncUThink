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

public final class TweenFriction implements TweenFunction
{
	float easeSpeed;
	
	public static TweenFriction tween = new TweenFriction(.3f);
	
	public static TweenFriction tween(float speed)
	{
		return new TweenFriction(speed);
	}
	
	public TweenFriction(float easeSpeed)
	{
		this.easeSpeed = easeSpeed;
	}
	
	public boolean isFinished(float t, float p, float b, float c, float d)
	{
		return (p >= b + c - .005 && p <= b + c + .005);
	}
	
	public float easeIn(float t, float p, float b, float c, float d)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public float easeInOut(float t, float p, float b, float c, float d)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public float easeOut(float t, float p, float b, float c, float d)
	{
		return p*(1f-easeSpeed)+easeSpeed*(b+c);
	}

}
