package org.andrewberman.ui.menu;

import org.andrewberman.ui.Point;

public class Spacer extends MenuItem
{

	float wSpace;
	float hSpace;
	
	public void setSpaceWidth(float width)
	{
		wSpace = width;
	}
	
	public void setSpaceHeight(float height)
	{
		hSpace = height;
	}
	
	@Override
	public void setWidth(float width)
	{
		super.setWidth(width);
	}
	
	@Override
	public void setHeight(float height)
	{
		super.setHeight(height);
	}
	
	@Override
	protected void calcPreferredSize()
	{
		super.calcPreferredSize();
		setWidth(wSpace);
		setHeight(hSpace);
	}
	
	@Override
	protected boolean containsPoint(Point p)
	{
		return false;
	}

}
