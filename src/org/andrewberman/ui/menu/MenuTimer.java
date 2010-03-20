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
package org.andrewberman.ui.menu;

/**
 * The <code>MenuTimer</code> class, similar to the <code>Blinker</code>
 * class in the <code>UI</code> package, is a simple implementation of a
 * re-settable timer, designed to allow a <code>MenuItem</code> to exhibit a
 * slight delay when opening via a hover gesture. See the <code>MenuItem</code>
 * class for more info; particularly the <code>setState()</code> method.
 * 
 * @author Greg
 * @see		org.andrewberman.ui.menu.MenuItem
 * @see		org.andrewberman.ui.Blinker
 */
public final class MenuTimer extends Thread
{
	private static MenuTimer instance;

	MenuItem item;
	MenuItem parent;
	MenuItem lastSet;
	static final int delay = 100;
	boolean unset;
	boolean startDelay;

	public static MenuTimer instance()
	{
		if (instance == null || !instance.isAlive())
		{
			instance = new MenuTimer();
			instance.start();
		}
		return instance;
	}

	public void run()
	{
		while (!Thread.currentThread().isInterrupted())
		{
			synchronized (this)
			{
				if (startDelay)
				{
					startDelay = false;
					try
					{
						wait(delay);
					} catch (InterruptedException e)
					{
						// System.out.println("Interrupted!");
						break;
					}
				} else
				{
					if (parent == null)
						try
						{
							wait();
						} catch (InterruptedException e)
						{
							// System.out.println("Interrupted!");
							break;
						}
					else if (unset)
					{
						item.menu.close(item);
						item = null;
						parent = null;
					} else
					{
//						parent.closeMyChildren();
						item.menuTriggerLogic();
						parent = null;
//						item = null;
					}
				}
			}
			yield();
		}
	}

	public void setMenuItem(MenuItem setMe)
	{
		if (setMe == lastSet)
			return;
		// if (item == setMe) return;
		// System.out.println("Set item:"+setMe);
		item = setMe;
		parent = item.parent;
		lastSet = item;
		unset = false;
		triggerDelay();
	}

	public void unsetMenuItem(MenuItem unsetMe)
	{
		if (unsetMe == item || (unsetMe == lastSet))
		{
//			 System.out.println("Unset item:"+unsetMe);
			// parent = unsetMe.nearestMenu;
			// parent = unsetMe.parent;
			item = unsetMe;
			lastSet = null;
			unset = true;
			triggerDelay();
		}
	}

	public synchronized void triggerDelay()
	{
		startDelay = true;
		notifyAll();
	}
}