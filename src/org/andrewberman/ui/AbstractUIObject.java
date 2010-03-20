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

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import org.andrewberman.ui.ifaces.UIListener;
import org.andrewberman.ui.ifaces.UIObject;

public abstract class AbstractUIObject implements UIObject
{
	protected ArrayList listeners = new ArrayList(1);
	
	public void addListener(UIListener o)
	{
		listeners.add(o);
	}
	
	public void removeListener(UIListener o)
	{
		listeners.remove(o);
	}
	
	public void fireEvent(int id)
	{
		UIEvent e = new UIEvent(this,id);
		for (int i=0; i < listeners.size(); i++)
		{
			((UIListener)listeners.get(i)).uiEvent(e);
		}
	}
	
	public void draw()
	{
	}

	public void focusEvent(FocusEvent e)
	{
	}

	public void keyEvent(KeyEvent e)
	{
	}

	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
	}
	
	
}
