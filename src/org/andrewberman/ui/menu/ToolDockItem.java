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

import java.lang.reflect.Constructor;

import org.andrewberman.ui.tools.Tool;

import processing.core.PApplet;

public class ToolDockItem extends DockItem
{
	private String toolString;
	private Tool tool;

	public void setTool(String s)
	{
		toolString = s;
		if (menu != null)
		{
			PApplet p = menu.canvas;
			try
			{
				String packageName = Tool.class.getPackage().getName();
				Class toolClass = Class.forName(packageName+"."+s);
				Constructor c = toolClass
						.getConstructor(new Class[] { PApplet.class });
				Object instance = c.newInstance(new Object[] { p });
				this.tool = (Tool) instance;
			} catch (Exception e)
			{
				e.printStackTrace();
				return;
			}
		}
	}

	public Tool getTool()
	{
		return tool;
	}
	
	public void setMenu(Menu menu)
	{
		super.setMenu(menu);
		if (tool == null && toolString != null)
		{
			setTool(toolString);
		}
	}
	
	public MenuItem setShortcut(String s)
	{
		super.setShortcut(s);
		if (tool != null)
			tool.setShortcut(s);
		return this;
	}
	
	public String getLabel()
	{
		return getName() + " (" + tool.getShortcut().label + ")";
	}
	
	public void performAction()
	{
		super.performAction();
		if (nearestMenu instanceof ToolDock)
		{
			ToolDock td = (ToolDock) nearestMenu;
			td.toolManager.switchTool(tool);
		}
	}
}
