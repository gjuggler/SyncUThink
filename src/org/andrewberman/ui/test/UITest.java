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
package org.andrewberman.ui.test;

import org.andrewberman.ui.menu.CheckBox;
import org.andrewberman.ui.menu.NumberScroller;
import org.andrewberman.ui.menu.Toolbar;

import processing.core.PApplet;

public class UITest extends PApplet
{

	public boolean isEnabled = true;
	
	@Override
	public void setup()
	{
		super.setup();
		Toolbar t = new Toolbar(this);
		t.setOrientation(Toolbar.VERTICAL);
		CheckBox b = new CheckBox();
		b.setName("Hello");
		b.setProperty(this, "isEnabled");
		t.add(b);
		NumberScroller s = new NumberScroller();
		s.setName("ScrollMe");
		s.setDefault(50);
		t.add(s);
		t.add("Whatever");
		t.get("Whatever").add("Hey");
		t.get("Whatever").add("How");
		t.get("Whatever").add("Hoo");
	}
	
	public void draw()
	{
		background(255);
	}
	
}
