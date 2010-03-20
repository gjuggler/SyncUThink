/**************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of SyncUThink.
 * 
 * SyncUThink is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * SyncUThink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SyncUThink.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.unsorted;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class DelayedAction
{
	private boolean updating;
	boolean threaded;
	Timer timer;

	public void trigger(int delay)
	{
		if (timer != null)
		{
			timer.stop();
			timer.setInitialDelay(delay);
			timer.restart();
			return;
		} else
		{
			timer = new Timer(delay, new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (!updating)
					{
						timer = null;
						doUpdate();
					} else
					{
						timer.start();
					}
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
	}

	private void doUpdate()
	{
		if (threaded)
		{
			new Thread()
			{
				public void run()
				{
					updating = true;
					run();
					updating = false;
				}
			}.start();
		} else
		{
			updating = true;
			run();
			updating = false;
		}
	}

	protected void run()
	{
		// Subclasses subclass this and do stuff here.
	}

	public boolean isThreaded()
	{
		return threaded;
	}

	public void setThreaded(boolean threaded)
	{
		this.threaded = threaded;
	}
}
