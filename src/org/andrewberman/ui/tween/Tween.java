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


public class Tween
{
	TweenListener listener;
	TweenFunction function;

	public static final int STARTED = 0;
	public static final int UPDATED = 1;
	public static final int STOPPED = 2;
	public static final int FINISHED = 3;
	public static final int REWOUND = 4;
	public static final int FFORWARDED = 5;
	
	public static final int IN = 0;
	public static final int OUT = 1;
	public static final int INOUT = 2;
	
	boolean isTweening;
	int tweenType; // use the IN/OUT/INOUT static variables with this.

	float position;
	float time;
	float begin;
	float change;
	float duration;

	public Tween(TweenListener listener, TweenFunction function, int type)
	{
		this.listener = listener;
		this.function = function;
		this.tweenType = type;
	}
	
	public Tween(TweenListener listener, TweenFunction function, int tweenType,
			float begin, float end, float duration)
	{
		this.listener = listener;
		this.function = function;
		this.tweenType = tweenType;
		this.begin = begin;
		this.position = begin;
		this.change = end - begin;
		this.duration = duration;
		start();
	}

	public float getTime()
	{
		return this.time;
	}

	public float getPosition()
	{
		return this.position;
	}

	public float getDuration()
	{
		return this.duration;
	}

	public float getFinish()
	{
		return this.begin + this.change;
	}
	
	public float getChange()
	{
		return change;
	}

	public float getBegin()
	{
		return begin;
	}
	
	public void rewind()
	{
		this.time = 0;
		this.position = this.begin;
		dispatchEvent(REWOUND);
	}

	public void fforward()
	{
		this.time = this.duration;
		this.position = getFinish();
		dispatchEvent(FFORWARDED);
	}

	public void start()
	{
		this.isTweening = true;
		dispatchEvent(STARTED);
	}

	public void stop()
	{
		this.isTweening = false;
		dispatchEvent(STOPPED);
	}

	public void continueTo(float newF)
	{
		if (newF == begin + change)
			return;
		if (!isTweening)
			start();
		continueTo(newF, this.duration);
	}

	public void continueTo(float newF, float newD)
	{
		this.begin = this.position;
		this.change = newF - this.begin;
		this.time = 0;
		this.duration = newD;
		start();
	}

	public void restart(float newStart, float newFinish, float newD)
	{
		this.begin = newStart;
		this.change = newFinish - newStart;
		this.time = 0;
		this.duration = newD;
		start();
	}
	
	public void yoyo()
	{
		continueTo(this.begin, this.time);
	}

	public final void dispatchEvent(int eventType)
	{
		if (listener != null)
			listener.tweenEvent(this, eventType);
	}
	
	public float update()
	{
		if (isTweening)
		{
			if (function.isFinished(time, position, begin, change, duration))
			{
				fforward();
				stop();
				dispatchEvent(FINISHED);
			} else
			{
				this.time++;
				switch (tweenType)
				{
					case IN:
						position = function.easeIn(time, position, begin, change, duration);
						break;
					case OUT:
						position = function.easeOut(time, position, begin, change, duration);
						break;
					case INOUT:
						position = function
								.easeInOut(time, position, begin, change, duration);
						break;
				}
				dispatchEvent(UPDATED);
			}
		}
		return position;
	}

	public TweenFunction getFunction()
	{
		return function;
	}

	public void setFunction(TweenFunction function)
	{
		this.function = function;
	}

	public boolean isTweening()
	{
		return isTweening;
	}
}
