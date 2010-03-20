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
package org.andrewberman.ui.camera;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.andrewberman.ui.UIRectangle;
import org.andrewberman.ui.tween.Tween;
import org.andrewberman.ui.tween.TweenQuad;

import processing.core.PApplet;

/*
 * A tweaked camera, for use by the TreeRenderer class and subclasses, in order to allow a renderer to smoothly move
 * around the drawing area, scaling by width and height as necessary.
 */
public class RectMover extends MovableCamera
{
	protected Rectangle2D.Float r = new Rectangle2D.Float();

	protected Tween wTween;
	protected Tween hTween;

	/**
	 * Convenience variables, storing the current position of each of the tweens.
	 */
	float cx, cy, w, h = 0;
	
	float border = 100;
	
	public RectMover(PApplet app)
	{
		super(app);
		
		wTween = new Tween(null, TweenQuad.tween, Tween.OUT, 1f, 1f, FRAMES);
		hTween = new Tween(null, TweenQuad.tween, Tween.OUT, 1f, 1f, FRAMES);

		/**
		 * Kind of important: call update() to make sure nothing here is null
		 * in case some mouse events happen before stuff is finished loading.
		 */
		update();
	}

	public void zoomBy(float factor)
	{
		float newW = wTween.getFinish() * factor;
		float newH = hTween.getFinish() * factor;
		zoomCenterTo(cx, cy, newW, newH);
	}

	public void zoomTo(float z)
	{
		wTween.continueTo(p.width * z);
		hTween.continueTo(p.height * z);
	}
	
	/**
	 * cx and cy are the CENTER coordinates of this RectMover, in order to make
	 * it more closely resemble a camera.
	 */
	public void zoomCenterTo(float cx, float cy, float w, float h)
	{
		xTween.continueTo((float) cx);
		yTween.continueTo((float) cy);
		wTween.continueTo((float) w);
		hTween.continueTo((float) h);
	}

	public void fforward()
	{
		super.fforward();
		wTween.fforward();
		hTween.fforward();
	}
	
	public void fillScreen(float f)
	{
		zoomCenterTo(0, 0, p.getWidth()*f, p.getHeight()*f);
	}
	
	public float getZ()
	{
		return w / (float)p.width;
	}

	public void update()
	{
		/*
		 * No super.update() because we're updating all the necessary tweens on our own.
		 */
		super.scroll();

		xTween.update();
		yTween.update();
		wTween.update();
		hTween.update();
		
		updateConvenienceVariables();
		constrainToScreen();
		// Set our associated object's rectangle.
		r.setRect(-cx * getZ() - w/2.0f, -cy * getZ() - h/2.0f, w, h);
	}

	public Rectangle2D.Float getRect()
	{
		return r;
	}
	
	private void updateConvenienceVariables()
	{	
		/*
		 * Set the convenience variables.
		 */
		cx = xTween.getPosition();
		cy = yTween.getPosition();
		w = wTween.getPosition();
		h = hTween.getPosition();
	}
	
	private void constrainToScreen()
	{
		float minZoom = 0.2f;
		if (getZ() < minZoom)
		{
			nudgeTo(xTween.getBegin(),yTween.getBegin());
			zoomTo(minZoom);
			
			xTween.fforward();
			yTween.fforward();
			wTween.fforward();
			hTween.fforward();
		}
		
		updateConvenienceVariables();
	}
}
