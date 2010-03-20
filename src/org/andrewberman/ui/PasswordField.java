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

import processing.core.PApplet;

public class PasswordField extends TextField
{

	StringBuffer modelText = new StringBuffer();
	
	public PasswordField(PApplet p)
	{
		super(p);
	}

	String stars(String s)
	{
		return s.replaceAll(".", "*");
	}
	
	@Override
	protected void insert(String s, int pos)
	{
		super.insert(stars(s), pos);
		modelText.insert(pos,s);
	}
	
	@Override
	protected void deleteAt(int pos)
	{
		super.deleteAt(pos);
		if (pos < 0 || pos >= modelText.length())
			return;
		modelText.deleteCharAt(pos);
	}
	
	@Override
	public String getText(int lo, int hi)
	{
//		return super.getText(lo, hi);
		return modelText.substring(lo, hi);
	}
	
}
