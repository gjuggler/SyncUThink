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

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import org.andrewberman.ui.ifaces.Malleable;
import org.andrewberman.ui.menu.MenuStyle;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphicsJava2D;

/**
 * The <code>TextField</code> class is a Processing-based implementation of a
 * simple one-line text field.
 * <p>
 * The x- and y- coordinates of this text field represent the upper-left hand
 * corner of the bounding box; if you wish to position the object using some
 * other reference point, use one of the convenience methods with the name
 * <code>layoutByXXXX</code>.
 * <p>
 * As with most of the other UI objects in this package, the
 * <code>TextField</code> relies heavily on Java2D calls for all of its
 * drawing. As such, it switches drawing strategies depending on the type of
 * <code>PGraphics</code> object being used by the current
 * <code>PApplet</code>:
 * <ul>
 * <li> If the canvas is a <code>PGraphicsJava2D</code> instance, then:
 * <ul>
 * <li>Draw directly onto the canvas' internal Graphics2D object.</li>
 * </ul>
 * </li>
 * <li> If the canvas is anything else (P3D or OpenGL), then:
 * <ul>
 * <li>Draw onto an off-screen PGraphicsJava2D buffer, and use the
 * <code>image()</code> function to copy the off-screen buff onto the canvas.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * If you want your TextField to maintain all the current matrix transformations
 * associated with the "canvas" <code>PGraphics</code> object, then set the
 * <code>useCameraCoordinates</code> value to <code>true</code>. Otherwise,
 * the TextField will be positioned and drawn in screen coordinates (i.e. the
 * top-left corner equals [0,0]).
 * 
 * @author Greg
 */
public class TextField extends AbstractUIObject implements Malleable
{
	static final int LEFT = -1;
	static final int RIGHT = 1;
	static final int SELECT = 0;
	static final int MOVE = 1;
	static final int CARET_JUMP = 1; // Default the CARET_JUMP to 5
	// characters.
	static final int DRAG_BORDER_FACTOR = 10;
	static final int MOUSE_DRAG_DELAY = 1;
	static final float TEXT_BORDER_MULT = .1f;
	static final int metaMask = UIUtils.getMetaMask();

	static final int OFFSET = 10;

	PApplet canvas;
	PGraphicsJava2D pg;
	TextFieldStyle style;
	PFont pFont;
	Font font;
	float fontSize;
	Blinker blinker;
	StringClipboard clip;
	Rectangle2D.Float clipRect, buffRect;
	RenderingHints oldRH;

	float offsetX, offsetY, ascent, descent;
	float x, y, width, height, pad;
	float mouseDragPos;
	int mouseDragCounter;
	int caret, anchorPos, selAnchor;
	int viewLo, viewHi, selLo, selHi;
	boolean anchorRight;
	boolean mouseDragging, shiftPressed;
	protected boolean hidden;

	public StringBuffer text = new StringBuffer();

	/**
	 * If set to true, then this textfield will draw itself in "camera"
	 * coordinates, i.e. it won't reset the camera before it draws itself.
	 */
	public boolean useCameraCoordinates = true;
	/**
	 * If set to true, then this text field will always have its text anchored
	 * to the left. If you're constantly resizing the text field to fit the text
	 * and don't want it looking funky, set this to true.
	 */
	public boolean alwaysAnchorLeft = false;

	public TextField(PApplet p)
	{
		UIUtils.loadUISinglets(p);
		StringClipboard.lazyLoad();
		Blinker.lazyLoad();

		canvas = p;
		style = new TextFieldStyle();
		style.set("f.fontSize",12);
		pFont = style.getFont("font");
		font = pFont.font;
		blinker = Blinker.instance;
		clipRect = new Rectangle2D.Float(0, 0, 0, 0);
		buffRect = new Rectangle2D.Float(0, 0, 0, 0);
		clip = StringClipboard.instance;

		width = 50;
		x = 0;
		y = 0;

		if (UIUtils.isJava2D(canvas))
		{
			/*
			 * If the canvas is Java2D, we can draw directly to it.
			 */
			pg = (PGraphicsJava2D) canvas.g;
			layout();
		} else
		{
			// We need to create our own graphics here.
			pg = createBuffer(OFFSET, OFFSET);
			layout();
		}
		EventManager.instance.add(this);
	}

	PGraphicsJava2D createBuffer(int w, int h)
	{
		PGraphicsJava2D asdf = (PGraphicsJava2D) canvas.createGraphics(w, h,
				PApplet.JAVA2D);
		return asdf;
	}

	protected void reset()
	{
		anchorRight = false;
		selLo = selHi = 0;
		anchorPos = 0;
		caret = 0;
		mouseDragging = false;
		shiftPressed = false;
	}

	protected void layout()
	{
		/*
		 * Size me up.
		 */
		fontSize = style.getF("f.fontSize");
		
		ascent = UIUtils.getTextAscent(pg, pFont, fontSize, true);
		descent = UIUtils.getTextDescent(pg, pFont, fontSize, true);
		float textHeight = ascent + descent;
		pad = textHeight * TEXT_BORDER_MULT;
		height = textHeight;
		offsetY = ascent;

		if (!UIUtils.isJava2D(canvas))
		{
			float fullHeight = height + pad * 2 + OFFSET * 2;
			float fullWidth = width + pad * 2 + OFFSET * 2;
			if (pg.width < fullWidth || pg.height < fullHeight)
				pg = createBuffer((int) fullWidth, (int) fullHeight);
			hint();
		}
	}

	public void draw()
	{
		if (hidden)
			return;
		calculateViewport();
		// hint();
		canvas.pushMatrix();
		resetMatrix();
		if (!UIUtils.isJava2D(canvas))
		{
			pg.beginDraw();
			pg.background(255, 0);
			pg.translate(-x, -y);
			pg.translate(OFFSET, OFFSET);
			doTheDrawing();
			pg.modified = true;
			pg.endDraw();
			drawToCanvas();
		} else
		{
			// resetMatrix();
			doTheDrawing();
		}
		canvas.popMatrix();
	}

	public void hide()
	{
		hidden = true;
		UIUtils.releaseCursor(this, canvas);
	}

	public void show()
	{
		hidden = false;
	}

	protected void drawToCanvas()
	{
		int w = (int) (width + OFFSET * 2);
		int h = (int) (height + OFFSET * 2);
		// canvas.pushMatrix();
		// resetMatrix();
		canvas.image(pg, (int) (x - OFFSET), (int) (y - OFFSET), w, h, 0, 0, w,
				h);
		// canvas.popMatrix();
	}

	protected void resetMatrix()
	{
		if (useCameraCoordinates)
			return;
		if (UIUtils.isJava2D(canvas))
			canvas.resetMatrix();
		else
		{
			canvas.camera();
			// canvas.translate(-canvas.width/2,-canvas.height/2);
		}
	}

	protected void doTheDrawing()
	{
		Color bgFill = style.getC("c.backgroundFill");
		Color stroke = style.getC("c.foreground");
		
		/*
		 * Draw the padded outer region of the textarea.
		 */
		clipRect.setRect(x, y, width + 2 * pad, height + 2 * pad);
		pg.g2.setPaint(Color.white);
		pg.g2.fill(clipRect);
		pg.g2.setPaint(stroke);
		pg.g2.setStroke(new BasicStroke(style.getF("f.strokeWeight")));
		pg.g2.draw(clipRect);
		/*
		 * Draw the actual text.
		 */
		clipRect.setRect(x + pad, y + pad, width, height);
		pg.g2.setClip(clipRect);
		pg.g2.setFont(pFont.font.deriveFont(fontSize));
		pg.g2.setPaint(stroke);
		synchronized (this)
		{
			pg.g2.drawString(text.substring(viewLo, viewHi), x + pad + offsetX,
					y + pad + offsetY);
		}
		/*
		 * Draw the selection box.
		 */
		if (selHi - selLo > 0)
		{
			int lo = Math.max(viewLo, selLo);
			int hi = Math.min(viewHi, selHi);
			float loX = getPosForIndex(lo);
			float selWidth = getWidth(lo, hi);
			clipRect.setRect(x + loX + pad, y + pad, selWidth, height);
			pg.g2.setPaint(style.getC("c.highlight"));
			pg.g2.fill(clipRect);
			pg.g2.setClip(clipRect);
			pg.g2.setPaint(stroke.inverse());
			synchronized (this)
			{
				pg.g2.drawString(text.substring(lo, hi), x + pad + loX, y + pad
						+ offsetY);
			}
		}
		/*
		 * Draw the caret.
		 */
		if (blinker.isOn && FocusManager.instance.isFocused(this)
				&& selHi - selLo == 0)
		{
			// System.out.println("Heyoo");
			pg.g2.setStroke(new BasicStroke(1));
			pg.g2.setPaint(stroke);
			int caretX = (int) (x + pad + getPosForIndex(caret));
			if (caret == text.length() && caret != 0)
				caretX--;
			pg.g2.drawLine(caretX, (int) (y + pad + height / 10), caretX,
					(int) (y + pad + height - height / 10));
		}
		pg.g2.setClip(null);

		/*
		 * Handle the edge-of-box mouse dragging gesture.
		 */
		handleDragScroll();
	}

	protected void handleDragScroll()
	{
		if (!mouseDragging)
			return;
		mouseDragCounter--;
		if (mouseDragCounter <= 0)
		{
			mouseDragCounter = MOUSE_DRAG_DELAY;
			if (mouseDragPos < x + (width / DRAG_BORDER_FACTOR / 2.0))
			{
				this.selectChar(-1);
				mouseDragCounter += MOUSE_DRAG_DELAY;
			}
			if (mouseDragPos > x + width - (width / DRAG_BORDER_FACTOR / 2.0))
			{
				this.selectChar(1);
				mouseDragCounter += MOUSE_DRAG_DELAY;
			}
		}
	}

	protected void hint()
	{
		oldRH = pg.g2.getRenderingHints();
		pg.g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		pg.g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		pg.g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
	}

	protected void unhint()
	{
		pg.g2.setRenderingHints(oldRH);
	}

	protected void calculateViewport()
	{
		if (anchorRight)
		{
			viewHi = anchorPos;
			int i = viewHi;
			float textWidth = 0;
			while (textWidth <= width && i > 0)
			{
				textWidth = getWidth(i - 1, viewHi);
				i--;
			}
			viewLo = i;
			offsetX = width - textWidth;
		} else
		{
			viewLo = anchorPos;
			int i = viewLo;
			float textWidth = 0;
			while (textWidth <= width && i < text.length())
			{
				textWidth = getWidth(viewLo, i + 1);
				i++;
			}
			viewHi = i;
			offsetX = 0;
		}
	}

	protected float getWidth(int lo, int hi)
	{
		if (lo > hi) // Switch lo and hi if they're reversed.
		{
			lo = Math.min(lo, hi);
			hi = Math.max(lo, hi);
		}
		synchronized (text)
		{
			FontMetrics fm = UIUtils.getMetrics(pg, pFont.font, fontSize);
			float f = (float) fm.getStringBounds(text.substring(lo, hi), pg.g2)
					.getWidth();
			// System.out.println(Math.round(f)+" "+text.substring(lo,hi));
			return f;
		}
	}

	protected float getPosForIndex(int index)
	{
		return offsetX + getWidth(viewLo, index);
	}

	public float getX()
	{
		return x;
	}

	public float getY()
	{
		return y;
	}

	public void setPosition(float x, float y)
	{
		 setPositionByCorner(x,y);
	}

	public void setPositionByCorner(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	public void setPositionByBaseline(float x, float y)
	{
		this.x = x - pad;
		this.y = y - ascent - pad;
	}

	public void setTextSize(float textSize)
	{
		getStyle().set("f.fontSize",textSize);
		layout();
	}

	public TextFieldStyle getStyle()
	{
		return style;
	}
	
	public void setWidth(float width)
	{
		this.width = width;
		layout();
	}

	protected void selectAll()
	{
		selAnchor = 0;
		selectChar(text.length());
	}

	protected void selectWord(int n)
	{
		nextWord(SELECT, n);
	}

	protected void selectToEnd(int dir)
	{
		if (dir == LEFT)
			selectChar(0 - caret);
		else if (dir == RIGHT)
			selectChar(text.length() - caret);
	}

	protected void moveToEnd(int dir)
	{
		if (dir == LEFT)
			moveChar(0 - caret);
		else if (dir == RIGHT)
			moveChar(text.length() - caret);
	}

	protected void moveWord(int n)
	{
		nextWord(MOVE, n);
	}

	protected void nextWord(int type, int n)
	{
		nextWord(type, n, true);
	}

	protected void nextWord(int type, int dir, boolean skipSpace)
	{
		int pad = 0;
		if (skipSpace)
			pad = 1;

		String s = "";
		if (dir == RIGHT)
		{
			s = text.substring(caret, text.length());
			String[] words = s.split("\\w\\W", 2);
			String firstWord = words[0];
			if (type == SELECT)
				selectChar(firstWord.length() + pad);
			else
				moveChar(firstWord.length() + pad);
		} else if (dir == LEFT)
		{
			s = text.reverse().substring(text.length() - caret, text.length());
			text.reverse(); // Reverse the stringbuffer back to normal!
			String[] words = s.split("\\W\\w", 2);
			String firstWord = words[0];
			if (type == SELECT)
				selectChar(-1 * (firstWord.length() + 1));
			else
				moveChar(-1 * (firstWord.length() + 1));
		}
	}

	// Lowest-level method for selection moving.
	protected void selectChar(int dist)
	{
		selLo = Math.min(caret + dist, selAnchor);
		selHi = Math.max(caret + dist, selAnchor);
		if (selLo < 0)
			selLo = 0;
		if (selHi > text.length())
			selHi = text.length();
		// System.out.println("Lo: "+selLo+" Hi:"+selHi);
		moveCaretTo(caret + dist);
	}

	// Lowest-level method for caret moving.
	protected void moveChar(int dist)
	{
		if (caret + dist > text.length())
			dist = text.length() - caret;
		if (caret + dist < 0)
			dist = 0 - caret;
		moveCaretTo(caret + dist);
		clearSelection();
	}

	protected void moveCaretTo(int index)
	{
		caret = index;

		if (caret > text.length())
			caret = text.length();
		else if (caret < 0)
			caret = 0;

		if (getWidth(0, text.length()) <= width)
		{
			anchorRight = false;
			anchorPos = 0;
		} else if (viewLo + 1 > caret)
		{
			anchorPos = caret - CARET_JUMP;
			anchorRight = false;
		} else if (viewHi - 1 < caret)
		{
			anchorPos = caret + CARET_JUMP;
			anchorRight = true;
		}

		/*
		 * A quick hack to allow the alwaysAnchorLeft override to work.
		 */
		if (alwaysAnchorLeft)
		{
			anchorRight = false;
			anchorPos = 0;
		}

		if (anchorPos > text.length())
			anchorPos = text.length();
		else if (anchorPos < 0)
			anchorPos = 0;

		blinker.reset();
		fireEvent(UIEvent.TEXT_CARET);
	}


	protected void insertCharAt(char c, int pos)
	{
		insert(String.valueOf(c),pos);
		fireEvent(UIEvent.TEXT_VALUE);
	}
	
	
	protected void insert(String s, int pos)
	{
		text.insert(pos, s);
		moveChar(s.length());
		// textChanged();
		fireEvent(UIEvent.TEXT_VALUE);
	}

	protected void backspaceAt(int pos)
	{
		// Remove the char that exists before index pos.
		if (pos <= 0)
			return;
//		text.deleteCharAt(pos - 1);
		deleteAt(pos-1);
		moveCaretTo(pos-1);
		fireEvent(UIEvent.TEXT_VALUE);
	}

	protected void deleteAt(int pos)
	{
		// Remove the char that exists at index pos.
		if (pos < 0 || pos >= text.length())
			return;
		text.deleteCharAt(pos);
		moveChar(0);
		fireEvent(UIEvent.TEXT_VALUE);
	}

	protected void deleteRange(int lo, int hi)
	{
		for(int i=hi; i >= lo; i--)
		{
			deleteAt(i);
		}
	}
	
	protected void deleteSelection()
	{
//		text.delete(selLo, selHi);
		deleteRange(selLo,selHi);
		// selHi = selAnchor = caret = selLo; // Need to resolve selection
		// // deletion before calling
		// // moveCaretTo.
		moveCaretTo(selLo);
		clearSelection();
		fireEvent(UIEvent.TEXT_VALUE);
	}

	protected void clearSelection()
	{
		selHi = selLo = caret;
		selAnchor = caret;
		fireEvent(UIEvent.TEXT_SELECTION);
	}

	protected void cut()
	{
//		String s = text.substring(selLo, selHi);
		String s = getText(selLo,selHi);
		clip.toClipboard(s);
		deleteSelection();
	}

	protected void copy()
	{
		clip.toClipboard(text.substring(selLo, selHi));
	}

	protected void paste()
	{
		if (selHi - selLo > 0)
		{
			deleteSelection();
		}
		String s = clip.fromClipboard();
		insert(s, caret);
	}

	public void replaceText(String replacement)
	{
	selectAll();
	insert(replacement, 0);
	}
	
	public String getText(int lo,int hi)
	{
		return text.substring(lo, hi);
	}
	
	public String getText()
	{
//		return text.toString();
		return getText(0,text.length());
	}

	synchronized protected void printState()
	{
		System.err.println("Text: " + text.toString());
		System.err.println("Text length: " + text.length());
		System.err.println("Anchor: " + (anchorRight ? "Right" : "Left")
				+ "   Position: " + anchorPos);
		System.err.println("View Low: " + viewLo + "   View High: " + viewHi);
		System.err.println("Caret Position: " + caret);
		System.err.println("Selection: " + text.substring(selLo, selHi));
		System.err.println("");
	}

	public void keyEvent(KeyEvent e)
	{
		if (hidden)
			return;
		if (!FocusManager.instance.isFocused(this))
		{
			return;
		}
		
//		System.out.println(getText());
		// System.out.println(e);
		int code = e.getKeyCode();
		boolean meta = ((e.getModifiersEx() & metaMask) != 0);
		boolean alt = ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK);
		boolean shift = ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == KeyEvent.SHIFT_DOWN_MASK);
		shiftPressed = shift;

		/*
		 * Do some stuff to appease the OSX junkies.
		 */
		boolean wordJump = meta;
		if (PApplet.platform == PApplet.MACOSX)
			wordJump = alt;
		boolean endJump = false;
		if (PApplet.platform == PApplet.MACOSX)
			endJump = meta;

		if (e.getID() == KeyEvent.KEY_PRESSED)
		{
			int dir = 0;
			switch (code)
			{
				case (37): // Left
					dir = LEFT;
					break;
				case (39): // Right
					dir = RIGHT;
					break;
				case (8): // Backspace
					if (selHi - selLo > 0)
						deleteSelection();
					else
						backspaceAt(caret);
					break;
				case (127): // Delete
					if (selHi - selLo > 0)
						deleteSelection();
					else
						deleteAt(caret);
					break;
				case (36): // Home
					if (shift)
						selectChar(-caret);
					else
						moveChar(-caret);
					break;
				case (35): // End
					if (shift)
						selectChar(text.length() - caret);
					else
						moveChar(text.length() - caret);
					break;
				case (16): // Shift
				case (17): // Control
				case (18): // Alt
				case (9): // Tab
					// Do nothing.
					break;
				case (88): // X
					if (meta)
						cut();
					break;
				case (67): // C
					if (meta)
						copy();
					break;
				case (86): // V
					if (meta)
						paste();
					break;
				case (65): // A
					if (meta)
						selectAll();
					break;
			}

			if (dir != 0)
			{
				if (shift && wordJump)
					selectWord(dir);
				else if (wordJump)
					moveWord(dir);
				else if (shift && endJump)
					selectToEnd(dir);
				else if (endJump)
					moveToEnd(dir);
				else if (shift)
					selectChar(dir);
				else
					moveChar(dir);
			}
			if (!Character.isISOControl(e.getKeyChar()))
			{
				e.consume();
			}
		} else if (e.getID() == KeyEvent.KEY_TYPED)
		{
			char c = e.getKeyChar();
			/*
			 * Java tries to let us insert all sorts of garbage characters, such
			 * as backspace, delete, and Control-F (wtf?). Use this method to
			 * check that the current character is something actually worth
			 * displaying.
			 */
			if (!Character.isISOControl(c))
			{
				if (selHi - selLo != 0)
					deleteSelection();
				insertCharAt(c, caret);
				/*
				 * Consume the event so the ToolManager doesn't dispatch the
				 * event to any tools.
				 */
				e.consume();
			}
		}
	}

	protected boolean withinOuterRect(Point pt)
	{
		buffRect.setRect(x, y, width + 2 * pad, height + 2 * pad);
		return buffRect.contains(pt);
	}

	protected boolean withinInnerRect(Point pt)
	{
		buffRect.setRect(x + pad, y + pad, width, height);
		return buffRect.contains(pt);
	}

	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
		if (hidden)
			return;

		Point p1;
		if (useCameraCoordinates)
			p1 = model;
		else
			p1 = screen;

		Point pt = new Point(p1.x, p1.y);

		if (e.getID() != MouseEvent.MOUSE_DRAGGED)
		{
			mouseDragging = false;

			if (FocusManager.instance.isFocused(this)
					&& FocusManager.instance.isModal())
			{
				FocusManager.instance.removeFromFocus(this);
				FocusManager.instance.setFocus(this);
			}
		}
		if (e.getID() == MouseEvent.MOUSE_MOVED
				|| e.getID() == MouseEvent.MOUSE_RELEASED
				|| e.getID() == MouseEvent.MOUSE_ENTERED
				|| e.getID() == MouseEvent.MOUSE_EXITED)
		{
			if (withinInnerRect(pt) || mouseDragging)
			{
				UIUtils.setCursor(this, canvas, Cursor.TEXT_CURSOR);
			} else
			{
				UIUtils.releaseCursor(this, canvas);
			}
			return;
		}
		if (e.isPopupTrigger())
			return;

		// printState();

		if (withinOuterRect(pt) || mouseDragging) // contained within
		// text+padding area.
		{
			if (withinInnerRect(pt) || mouseDragging) // contained within text
			// area.
			{
				FocusManager.instance.setFocus(this);
				// Find the correct insertion point.
				int insertionIndex = viewLo;
				float ult = x + getPosForIndex(viewLo);
				float penult = ult;
				for (int i = viewLo; i <= viewHi; i++)
				{
					float pos = x + getPosForIndex(i); // get the left edge of
					// the "i"th character.
					insertionIndex = i;
					penult = ult;
					ult = pos;
					if (pos > pt.x)
					{
						break; // If the left edge is past our point, bail out.
					}
				}
				float middle = (ult + penult) / 2;
				if (pt.x < middle)
					insertionIndex--;
				int diff = insertionIndex - caret;
				if (e.getID() == MouseEvent.MOUSE_DRAGGED)
				{
					mouseDragging = true;
					FocusManager.instance.setModalFocus(this);
					mouseDragPos = pt.x;
					// if (insertionIndex <= viewLo || insertionIndex >= viewHi)
					if (insertionIndex > 1
							&& insertionIndex < text.length() - 1)
						handleDragScroll();
					selectChar(diff);
				} else if (e.getID() == MouseEvent.MOUSE_PRESSED)
				{
					switch (e.getClickCount())
					{
						case (1): // Single click: move to insertion point.
							if (shiftPressed)
								selectChar(diff);
							else
								moveChar(diff);
							break;
						case (2): // Double click: select surrounding word.
							moveChar(diff);
							nextWord(MOVE, RIGHT, false);
							nextWord(SELECT, LEFT, false);
							break;
						case (3): // Triple click: select all.
							selectAll();
							break;
					}
				}
			}
		} else if (e.getID() == MouseEvent.MOUSE_PRESSED)
		{
			// Point is not contained within this box, so unset focus.
			if (FocusManager.instance.removeFromFocus(this))
				clearSelection();
		}
	}

	public void focusEvent(FocusEvent e)
	{
		if (e.getID() == FocusEvent.FOCUS_LOST)
		{

		}
	}

	public void setX(float f)
	{
		x = f;
	}

	public void setY(float f)
	{
		y = f;
	}

	public float getBaselineY()
	{
		return y + pad + ascent;
	}
	
	public float getHeight()
	{
		return height + 2*pad;
	}

	public float getWidth()
	{
		return width + 2*pad;
	}

	public float getFontSize()
	{
		return fontSize;
	}
	
	public void setHeight(float h)
	{
		// TODO
	}

	public void setSize(float w, float h)
	{
		// TODO
	}
	
	public StringBuffer getTextModel()
	{
		return text;
	}
	
	
	class TextFieldStyle extends MenuStyle
	{

		public TextFieldStyle()
		{
			super();
			set("c.highlight",new Color(40, 40, 255));
			set("c.backgroundFill",new Color(255,255,255));
		}
		
	}
}
