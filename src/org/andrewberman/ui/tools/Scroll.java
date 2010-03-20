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
package org.andrewberman.ui.tools;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import org.andrewberman.ui.Point;
import org.andrewberman.ui.Shortcut;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.camera.Camera;
import org.andrewberman.ui.tween.Tween;
import org.andrewberman.ui.tween.TweenFriction;

import processing.core.PApplet;

public class Scroll extends Tool
{
	Cursor cursor;
	Cursor draggingCursor;

	float camDownX, camDownY;
	Tween xTween, yTween;

	public Scroll(PApplet p)
	{
		super(p);

		shortcut = new Shortcut("s");
		xTween = new Tween(null, TweenFriction.tween(0.8f), Tween.OUT, 0, 0, 30);
		yTween = new Tween(null, TweenFriction.tween(0.8f), Tween.OUT, 0, 0, 30);
	}

	public void draw()
	{
		xTween.update();
		yTween.update();
		if (xTween.isTweening() || yTween.isTweening())
		{
			getCamera().nudgeTo(camDownX - xTween.getPosition(),
					camDownY - yTween.getPosition());
			getCamera().fforward();
		}
		if (mouseDragging)
		{
			float dx = curPoint.x - downPoint.x;
			float dy = curPoint.y - downPoint.y;
			dx /= getCamera().getZ();
			dy /= getCamera().getZ();
			if (controlPressed)
			{
				xTween.continueTo(xTween.getFinish() - dx/10f);
				yTween.continueTo(yTween.getFinish() - dy/10f);
				p.stroke(255, 0, 0);
				p.strokeWeight(3.0f);
				p.line(downPoint.x, downPoint.y, curPoint.x, curPoint.y);
			} else
			{
				xTween.continueTo(dx);
				yTween.continueTo(dy);
			}
		}
	}

	@Override
	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
		super.mouseEvent(e, screen, model);
		switch (e.getID())
		{
			case (MouseEvent.MOUSE_PRESSED):
				UIUtils.setBaseCursor(draggingCursor);

				break;
			case (MouseEvent.MOUSE_RELEASED):
				UIUtils.setBaseCursor(cursor);
				break;
		}
	}

	@Override
	void pressReset(MouseEvent e, Point screen, Point model)
	{
		reset();
	}

	@Override
	void reset()
	{
		super.reset();
		downPoint = (Point) curPoint.clone();
		camDownX = getCamera().getX();
		camDownY = getCamera().getY();
		xTween.continueTo(0);
		yTween.continueTo(0);
		xTween.fforward();
		yTween.fforward();
	}

	@Override
	public void enter()
	{
		super.enter();
		xTween.continueTo(0);
		yTween.continueTo(0);
		xTween.fforward();
		yTween.fforward();
	}

	public Cursor getCursor()
	{
		if (cursor == null)
		{
			cursor = createCursor("cursors/grab.png", 6, 6);
			draggingCursor = createCursor("cursors/grabbing.png", 6, 6);
		}
		return cursor;
	}

	public boolean respondToOtherEvents()
	{
		return false;
	}

}
