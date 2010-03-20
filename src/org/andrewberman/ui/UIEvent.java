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

import java.awt.AWTEvent;
import java.awt.Event;

public class UIEvent extends AWTEvent
{
	private static final long serialVersionUID = 1L;

	public static final int TEXT_VALUE = 0;
	public static final int TEXT_SELECTION = 1;
	public static final int TEXT_CARET = 2;
	
	public static final int MENU_OPENED = 3;
	public static final int MENU_CLOSED = 4;
	public static final int MENU_ACTIONPERFORMED = 7;
	
	public static final int DOCK_ITEM_SELECTED = 8;
	public static final int DOCK_ACTIVATED = 9;
	public static final int DOCK_DEACTIVATED = 10;
	
	public UIEvent(Event event)
	{
		super(event);
	}
	
	public UIEvent(Object o, int id)
	{
		super(o,id);
	}
}
