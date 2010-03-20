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

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.andrewberman.ui.EventManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.Shortcut;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.ifaces.UIObject;
import org.andrewberman.ui.tools.Tool;
import org.andrewberman.ui.tools.ToolManager;
import org.andrewberman.ui.tools.ToolManager.ToolShortcuts;

import processing.core.PApplet;

public class ToolDock extends Dock implements ToolManager.ToolShortcuts
{
	protected ToolManager toolManager;

	public ToolDock(PApplet app)
	{
		super(app);
		toolManager = new ToolManager(this);
		EventManager.instance.setToolManager(toolManager);
	}

	public MenuItem create(String s)
	{
		ToolDockItem tdi = new ToolDockItem();
		tdi.setName(s);
			
		return tdi;
	}

	public ToolDockItem create(String name, String toolClassName, String icon)
	{
		ToolDockItem tdi = (ToolDockItem) create(name);
		tdi.setTool(toolClassName);
		tdi.setIcon(icon);
		return tdi;
	}

	public void keyEvent(KeyEvent e)
	{
		super.keyEvent(e);
		checkToolShortcuts(e);
	}

	public void checkToolShortcuts(KeyEvent e)
	{
		if (e.getID() != KeyEvent.KEY_PRESSED)
			return;
		ToolDockItem activeItem = null;
		for (int i = 0; i < items.size(); i++)
		{
			ToolDockItem tdi = (ToolDockItem) items.get(i);
			Tool t = (Tool) tdi.getTool();
			if (t.getShortcut() != null)
			{
				Shortcut s = t.getShortcut();
				if (s.matchesKeyEvent(e))
				{
					activeItem = tdi;
				}
			}
		}
		if (activeItem != null)
		{
			selectItem(activeItem);
		}
	}

	public void selectItem(MenuItem item)
	{
		setState(item,MenuItem.DOWN);
		item.performAction();
		hovered = null;
		for (MenuItem i : items)
		{
			if (i != item)
				setState(i,MenuItem.UP);
		}
	}
	
	public void selectTool(String toolName)
	{
		MenuItem tool = get(toolName);
		selectItem(tool);
	}
	
	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
		super.mouseEvent(e, screen, model);

	}


}