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

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.andrewberman.ui.Point;
import org.andrewberman.ui.Shortcut;
import org.andrewberman.ui.camera.Camera;
import org.andrewberman.ui.tween.Tween;
import org.andrewberman.ui.tween.TweenFriction;

import processing.core.PApplet;

public class Zoom extends Tool
{
	Cursor zoomCursor;

	float targetX, targetY;
	double downZoom, zoomFactor;
	float downCameraX, downCameraY;

	Tween zoomTween;

	Robot r;

	public Zoom(PApplet p)
	{
		super(p);

		shortcut = new Shortcut("z");
		zoomTween = new Tween(null, TweenFriction.tween(0.3f), Tween.OUT, 1, 1,
				30);
	}

	public void draw()
	{
		zoomTween.update();
		if (mouseDragging)
		{
			float zoomDist = downPoint.y - curPoint.y;
			if (controlPressed)
			{
				zoomFactor *= Math.exp(zoomDist / 100f / 10f);
				p.stroke(255,0,0,150);
				p.strokeWeight(3.0f);
				p.line(downPoint.x, curPoint.y, downPoint.x, downPoint.y);
			} else
			{
				zoomFactor = downZoom * Math.exp(zoomDist / 100f);
				float ratio = zoomTween.getPosition()/(float)downZoom;
				p.stroke(255,0,0,150);
				p.strokeWeight(1.0f*ratio);
				p.noFill();
				p.ellipse((float)downPoint.x, (float)downPoint.y, ratio*20,ratio*20);
			}
			zoomTween.continueTo((float) zoomFactor);
		}
		if (zoomTween.isTweening())
		{
			/*
			 * Update the new center point and set it. This calculation gets a
			 * little annoying; just trust me here.
			 */
			double dx = targetX / downZoom;
			double dy = targetY / downZoom;
			// ratio of current zoom to the original zoom.
			double zoomRatio = downZoom / zoomTween.getPosition();
			// new distances from down point to center.
			float newX = (float) (downCameraX + dx - dx * zoomRatio);
			float newY = (float) (downCameraY + dy - dy * zoomRatio);
			Camera cam = getCamera();
			cam.zoomTo(zoomTween.getPosition());
			cam.nudgeTo(newX, newY);
			cam.fforward();
		}
	}

	@Override
	public void enter()
	{
		super.enter();
		reset();
	}

	void pressReset(MouseEvent e, Point screen, Point model)
	{
		reset();
	}

	void reset()
	{
		downPoint = (Point) curPoint.clone();
		targetX = downPoint.x - p.width / 2;
		targetY = downPoint.y - p.height / 2;
		downCameraX = getCamera().getX();
		downCameraY = getCamera().getY();
		downZoom = getCamera().getZ();
		zoomFactor = getCamera().getZ();
		zoomTween.continueTo((float) zoomFactor);
		zoomTween.fforward();
	}

	public Cursor getCursor()
	{
		if (zoomCursor == null)
		{
			zoomCursor = createCursor("cursors/zoom2.png", 6, 6);
		}
		return zoomCursor;
	}

	public boolean respondToOtherEvents()
	{
		return false;
	}
}
