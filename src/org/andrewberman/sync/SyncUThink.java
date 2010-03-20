/*******************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of SyncUThink.
 * 
 * SyncUThink is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * SyncUThink is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SyncUThink. If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.sync;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import org.andrewberman.cookie.CookieMonster;
import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Label;
import org.andrewberman.ui.LabelHyperlink;
import org.andrewberman.ui.LayoutUtils;
import org.andrewberman.ui.PasswordField;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.TextField;
import org.andrewberman.ui.UIEvent;
import org.andrewberman.ui.UIRectangle;
import org.andrewberman.ui.camera.Camera;
import org.andrewberman.ui.ifaces.UIListener;
import org.andrewberman.ui.menu.CheckBox;
import org.andrewberman.ui.menu.MenuItem;
import org.andrewberman.ui.menu.NumberScroller;
import org.andrewberman.ui.menu.Toolbar;
import org.andrewberman.unsorted.DelayedAction;
import org.andrewberman.unsorted.PausibleThread;

import processing.core.PApplet;

public class SyncUThink extends PApplet implements UIListener
{
	private TextField tf;
	private TextField tf2;
	private UIRectangle r1, r2, r3;
	private UIRectangle[] rects;
	private TextField localdir;

	CookieMonster monster;
	private CheckBox upload;

	public static String getVersionString() {
		return "2010.03.20";
	}
	
	public void setup()
	{
		size(300, 600, JAVA2D);
		frameRate(60);

		r1 = new UIRectangle(15, 15, 270, 100);
		float usY = r1.y + 30;
		float pwY = r1.y + 85;

		float inX = r1.x + 10;

		tf = new TextField(this);
		tf.setWidth(150);
		tf.setTextSize(20);
		tf.addListener(this);

		tf2 = new PasswordField(this);
		tf2.setWidth(150);
		tf2.setTextSize(20);
		
		tf.setPositionByBaseline(r1.x + 100, usY);
		tf2.setPositionByBaseline(r1.x + 100, pwY);

		userLink = new LabelHyperlink(this, "");
		userLink.setFontSize(10);
		// userLink.setPositionByCornerNW(0, tf.getY()+tf.getHeight()+8);
		LayoutUtils.centerVertical(userLink, tf.getY() + tf.getHeight(), tf2.getY());

		Label l = new Label(this, "Username:");
		l.setPositionByBaseline(tf.getX() - l.getWidth() - 2, usY);

		l = new Label(this, "Password:");
		l.setPositionByBaseline(tf2.getX() - l.getWidth() - 2, pwY);

		r2 = new UIRectangle(r1.x, r1.y + r1.height + 10, r1.width, 160);
		inX = r2.x + 10;

		Label l2 = new Label(this, "SyncUThink");
		l2.setFontSize(24);
		l2.setPositionByCornerNW(inX, r2.y + 5);

		l = new Label(this, "Automatic PDF retrieval");
		l.setFontSize(12);
		l.setWidth(l2.getWidth());
		l.setPositionByCornerNW(inX, r2.y + 35);

		searchStatus = new Label(this);
		searchStatus.setFontSize(10);
		searchStatus.setYBaseline(l.getY() + 35);
		searchStatus.setX(inX);
		searchStatus.setColor(255, 0, 0);
		searchStatus.setLabel("");

		articleLink = new LabelHyperlink(this);
		articleLink.setYBaseline(l.getY() + 18);
		articleLink.setFontSize(10);
		articleLink.setX(inX);

		Toolbar tb = new Toolbar(this);
		tb.setFontSize(10);
		tb.getStyle().set("f.padX", 2);
		tb.getStyle().set("f.padY", 2);
		timeout = new NumberScroller();
		timeout.setName("Timeout (seconds)");
		timeout.setDefault(30);
		timeout.setIncrement(5);
		timeout.setMin(10);
		timeout.setMax(600);
		tb.add(timeout);
		tb.setY(r2.y + r2.height - 70);
		LayoutUtils.centerHorizontal(tb, r2);

		searchMenu = new Toolbar(this);
		searchGo = searchMenu.add("Go").setAction(this, "search");
		searchMenu.add("Pause").setAction(this, "searchPause");
		searchMenu.add("Stop").setAction(this, "searchStop");
		searchMenu.getStyle().set("f.padX", 8);
		searchMenu.setY(r2.y + r2.height - searchMenu.getHeight() - 10);
		LayoutUtils.centerHorizontal(searchMenu, r2.x, r2.x + r2.width);

		r3 = new UIRectangle(r2.x, r2.y + r2.height + 10, r2.width, 270);
		inX = r3.x + 10;

		l2 = new Label(this, "SyncUThink");
		l2.setFontSize(24);
		l2.setPositionByCornerNW(inX, r3.y + 5);

		l = new Label(this, "Local PDF download");
		l.setFontSize(12);
		l.setWidth(l2.getWidth());
		l.setPositionByCornerNW(inX, r3.y + 35);

		syncStatus = new Label(this);
		syncStatus.setFontSize(10);
		syncStatus.setYBaseline(l.getY() + 25);
		syncStatus.setX(inX);
		syncStatus.setColor(255, 0, 0);
		syncStatus.setLabel("");

		localdir = new TextField(this);
		localdir.setWidth(r3.width - 120);
		localdir.setTextSize(12);
		localdir.setY(r3.y + 120);
		LayoutUtils.centerHorizontal(localdir, r3.x, r3.x + r3.width);
		// ftb.setX(ftb.getX());

		Toolbar cbs = new Toolbar(this);
		// t.setOrientation(Toolbar.VERTICAL);
		cbs.getStyle().set("f.fontSize", 10);
		cbs.getStyle().set("f.padX", 5);
		cbs.getStyle().set("f.padY", 2);
		tags = new MyCheckBox("Create sub-folders based on tags");
		tags.setName("Tag sub-folders");
		tags.setValue("True");
		years = new MyCheckBox("Create sub-folders based on year");
		years.setName("Year sub-folders");
		flip = new MyCheckBox("Puts year first, i.e. 08-smith-title.pdf");
		flip.setName("Put year first");
		upload = new MyCheckBox("Re-uploads PDFs with newer modified-by date");
		upload.setName("Re-upload changes");
		upload.setValue("True");
		cbs.add(tags);
		cbs.add(years);
		cbs.add(flip);
		cbs.add(upload);
		cbs.setOrientation(Toolbar.VERTICAL);
		cbs.setPosition(inX, localdir.getY() + localdir.getHeight() + 13);
		// t.layout();
		LayoutUtils.centerHorizontal(cbs, r3.x, r3.x + r3.width);

		Toolbar t = new Toolbar(this);
		t.add("...").setAction(this, "openDirectory");
		t.getStyle().set("f.fontSize", 12);
		t.getStyle().set("f.padX", 2);
		t.getStyle().set("f.padY", 2);
		t.setPosition(localdir.getX() + localdir.getWidth() + 10, localdir.getY());
		LayoutUtils.centerVertical(t, localdir.getY(), localdir.getY() + localdir.getHeight());
		// t.layout();

		l = new Label(this, "Folder:");
		l.setFontSize(localdir.getFontSize());
		l.setPositionByBaseline(localdir.getX() - 50, localdir.getBaselineY());

		syncMenu = new Toolbar(this);
		syncGo = syncMenu.add("Go").setAction(this, "sync");
		syncMenu.add("Pause").setAction(this, "syncPause");
		syncMenu.add("Stop").setAction(this, "syncStop");
		syncMenu.getStyle().set("f.padX", 8);
		syncMenu.setY(r3.y + r3.height - syncMenu.getHeight() - 10);
		LayoutUtils.centerHorizontal(syncMenu, r3);

		rects = new UIRectangle[] { r1, r2, r3 };

		LabelHyperlink lh = new LabelHyperlink(this, "http://www.andrewberman.org/");
		lh.setFontSize(10);
		lh.setY(r3.y + r3.height + 2);
		LayoutUtils.alignRight(lh, r3);

		lh = new LabelHyperlink(this, "http://www.citeulike.org/");
		lh.setFontSize(10);
		lh.setY(r3.y + r3.height + 2 + lh.getHeight());
		LayoutUtils.alignRight(lh, r3);

		Label version = new Label(this, SyncUThink.getVersionString());
		version.setFontSize(10);
		version.setPositionByCornerNW(r3.x, r3.y + r3.height + 2);

		validateAll();

		camera = new Camera(this);
		camera.skipTo(-width / 2, height / 2);
		camera.nudgeTo(width / 2, height / 2);

		// Use CookieMonster to load the cookies.
		monster = new CookieMonster(this);
		loadCookies();

	}

	final String USER = "syncUser";
	final String PASS = "syncPass";
	final String DIR = "syncDir";
	final String OPTS = "syncOpts";

	public void loadCookies()
	{
		String u = monster.taste(USER);
		tf.replaceText(u);
		String p = monster.taste(PASS);
		tf2.replaceText(p);
		String d = monster.taste(DIR);
		localdir.replaceText(d);
		/*
		 * The boolean options are stored as a concatenated String.
		 */
		String opts = monster.taste(OPTS);
		if (opts.length() > 0)
		{
			String[] options = opts.split("-");
			if (options.length < 4)
				return;
			tags.setValue(options[0]);
			years.setValue(options[1]);
			flip.setValue(options[2]);
			upload.setValue(options[3]);
		}
	}

	public void saveCookies()
	{
		monster.bake(USER, tf.getText(), 14);
		monster.bake(PASS, tf2.getText(), 14);
		monster.bake(DIR, localdir.getText(), 14);
		/*
		 * Concatenate the checkboxes into a single string of trues and falses separated by "-".
		 */
		String s = String.valueOf(tags.getValue());
		s += "-" + String.valueOf(years.getValue());
		s += "-" + String.valueOf(flip.getValue());
		s += "-" + String.valueOf(upload.getValue());
		monster.bake(OPTS, s, 14);
	}

	MenuItem syncGo;
	MenuItem searchGo;

	Label searchStatus;
	Label syncStatus;
	private CheckBox tags;
	private CheckBox years;
	private CheckBox flip;

	public void openDirectory()
	{
		try
		{
			File f = PDFDownloader.chooseDirectory();
			StringBuffer b = localdir.getTextModel();
			b.replace(0, b.length(), f.getCanonicalPath());
		} catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	PausibleThread searchT;
	PausibleThread syncT;

	public void search()
	{
		saveCookies();
		searchAutoClear = false;

		searchGo.setEnabled(false);
		PDFSearcher s = new PDFSearcher()
		{
			public void finished()
			{
				searchGo.setEnabled(true);
				searchAutoClear = true;
			}
		};
		s.setUsername(tf.getText());
		s.setPassword(tf2.getText());
		s.setDebug(this, "debugSearch");
		s.setTimeout(timeout.getValue());
		searchT = s;
		s.start();
	}

	public void searchPause()
	{
		pause(searchMenu, searchT);
	}

	public void syncPause()
	{
		pause(syncMenu, syncT);
	}

	void pause(MenuItem i, PausibleThread t)
	{
		if (t == null || t.isStopped())
			return;
		if (t.isPaused())
		{
			i.get("Resume").setName("Pause");
			t.resumeThread();
		} else
		{
			i.get("Pause").setName("Resume");
			t.pauseThread();
		}
		validateAll();
	}

	void validateAll()
	{
		searchMenu.layout();
		syncMenu.layout();
		LayoutUtils.centerHorizontal(searchMenu, r2);
		LayoutUtils.centerHorizontal(syncMenu, r3);
	}

	public void searchStop()
	{
		if (searchT.isPaused())
			searchPause();
		searchT.stopThread();
	}

	public void syncStop()
	{
		if (syncT.isPaused())
			syncPause();
		syncT.stopThread();
	}

	public void sync()
	{
		saveCookies();
		final SyncUThink ac = this;
		syncAutoClear = false;
		syncGo.setEnabled(false);
		String u = tf.getText();
		String p = tf2.getText();
		File d = new File(localdir.getText());
		System.out.println(d);
		PDFDownloader s = new PDFDownloader()
		{
			public void finished()
			{
				syncGo.setEnabled(true);
				syncAutoClear = true;
			}
		};
		s.setUsername(u);
		s.setPassword(p);
		s.setBaseDir(d);
		s.setSubTags(tags.getValue());
		s.setSubYears(years.getValue());
		s.setFlipFilename(flip.getValue());
		s.setUploadNewer(upload.getValue());
		s.setDebug(ac, "debugSync");
		s.start();
		syncT = s;
	}

	DelayedAction syncClear = new DelayedAction()
	{
		protected void run()
		{
			super.run();
			if (syncAutoClear)
				syncStatus.setLabel("");
		}
	};

	DelayedAction searchClear = new DelayedAction()
	{
		protected void run()
		{
			super.run();
			if (searchAutoClear)
				searchStatus.setLabel("");
		}
	};
	private Camera camera;

	boolean syncAutoClear = true;
	boolean searchAutoClear = true;
	LabelHyperlink userLink;
	Toolbar searchMenu;
	Toolbar syncMenu;
	private NumberScroller timeout;

	LabelHyperlink articleLink;

	public void setCurrentArticle(String label, String url)
	{
		articleLink.setLabel(label);
		articleLink.setURL(url);
	}

	public void debugSync(String s) {
		this.debugSync(s,false);
	}
	
	public void debugSync(String s, boolean ignoreWhileRunning)
	{
		// syncClear.trigger(10000);
		if (ignoreWhileRunning && this.syncT != null && this.syncT.isAlive())
			return;
		syncStatus.setLabel(s);
	}
	
	public boolean isRunning() {
		if (this.syncT != null && this.syncT.isAlive())
			return true;
		if (this.searchT != null && this.searchT.isAlive())
			return true;
		return false;
	}

	public void debugSearch(String s) {
		this.debugSearch(s,false);
	}
	public void debugSearch(String s, boolean ignoreWhileRunning)
	{
		// searchClear.trigger(10000);
		if (ignoreWhileRunning && this.searchT != null && this.syncT.isAlive())
			return;
		searchStatus.setLabel(s);
	}

	public void draw()
	{
		camera.update();
		background(240, 240, 240);

		fill(255, 255, 255);
		stroke(0);
		strokeWeight(1.5f);
		for (int i = 0; i < rects.length; i++)
		{
			UIRectangle r = rects[i];
			rect(r.x, r.y, r.width, r.height);
		}
	}

	public void keyPressed(KeyEvent arg0)
	{
		super.keyPressed(arg0);
		if (arg0.getKeyCode() == KeyEvent.VK_TAB)
		{
			Object o = FocusManager.instance.getFocusedObject();
			if (o == tf)
				FocusManager.instance.setFocus(tf2);
			else if (o == tf2)
				FocusManager.instance.setFocus(tf);
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		// syncMenu.setY(e.getY()/3f);
		// syncMenu.style.stroke = new BasicStroke(e.getX()/20f);
	}

	public void uiEvent(UIEvent e)
	{
		Object o = e.getSource();
		if (o == tf)
		{
			if (e.getID() == UIEvent.TEXT_VALUE)
			{
				String url = InheritMe.BASE_URL + "user/" + tf.getText();
				userLink.setLabel(url);
				userLink.setURL(url);
				float w = userLink.getWidth();
				userLink.setX(tf.getX() + tf.getWidth() - w);
			}
		}
	}

	class MyCheckBox extends CheckBox
	{
		String msg;

		public MyCheckBox(String msg)
		{
			super();
			this.msg = msg;
		}
		
		@Override
		protected void itemMouseEvent(MouseEvent e, Point tempPt)
		{
			super.itemMouseEvent(e, tempPt);
			if (containsPoint(tempPt))
			{
				debugSync(msg,true);
			}
		}
	}

}
