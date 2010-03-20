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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.andrewberman.unsorted.PausibleThread;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public abstract class InheritMe extends PausibleThread
{
	//static Pattern articleURL = RegexUtils.hrefRegex("/user/\\w*/article/(\\w*)");
	static Pattern articleURL = RegexUtils.createPattern("link:.?'(/user/\\w*/article/(\\w*))'");
	static String BASE_URL = "http://www.citeulike.org/";
	// static Pattern pdfFinder = RegexUtils.createPattern("<" + nb + "href=\"("
	// + nb + "\\.pdf)\"" + nb + ">");
	static Pattern chunkGrabber = RegexUtils.grabUntilClosingElement("span", articleURL);

	static PrintStream errS = System.err;
	static String nb = RegexUtils.nb;

	static String nonQuote = RegexUtils.nq;
	public static PrintStream out = System.out;
	//static Pattern pdfURL = RegexUtils.hrefRegex("pdf/user/\\w*/article/(\\w*)/");
	static Pattern pdfURL = RegexUtils.createPattern("link:.?'(/pdf/user/##USERNAME##/article/(\\w*?)/.*?)'");

	static String sep = System.getProperty("file.separator");

	static final String SKIP_STRING = "nopdf";
	protected static final int BETWEEN_SEARCH_ITEMS_SLEEP_TIME = 500;
	protected static final int BETWEEN_WEB_SCRAPING_SLEEP_TIME = 500;
	static final int BETWEEN_PDF_DOWNLOADS_SLEEP_TIME = 500;
	static Pattern tagFinder = RegexUtils.createPattern("/tag/(.+?)[\"']");
	static URLCodec urlEncoder = new URLCodec();
	static String escapeURL(String url) throws Exception
	{
		/*
		 * UPDATE 2008-03-08: Changed the constructor from new URI(url,false) to URI(url,true).
		 * 
		 * Seems to work with fancy ACM links, but not sure whether I'm breaking something else...
		 */
		URI newURL;
		try {
			newURL = new URI(url, true);
		} catch (Exception e) {
			newURL = new URI(url,false);
		}
		return newURL.getEscapedURI();
	}
	static List<CiteULikeReference> getArticlesFromPage(JSONArray json) throws Exception
	{
		ArrayList<CiteULikeReference> refs = new ArrayList<CiteULikeReference>();
		for (int i=0; i < json.length(); i++) {
			JSONObject jo = json.getJSONObject(i);
			CiteULikeReference ref = new CiteULikeReference(jo);
			refs.add(ref);
		}
		return refs;
	}
	
	static List<CiteULikeReference> getArticlesWithoutPDFs(List<CiteULikeReference> refs) {
		ArrayList<CiteULikeReference> withoutPDFs = new ArrayList<CiteULikeReference>();
		for (CiteULikeReference ref : refs) {
			if (ref.getUserfiles().keySet().size() == 0) {
				withoutPDFs.add(ref);
			}
		}
		return withoutPDFs;
	}
	static LinkedHashMap<String, String> getArticlesFromPage(String pageURL, String pageContent) throws Exception
	{
		System.out.println(pageURL);
		LinkedHashMap<String, String> idToURL = new LinkedHashMap<String, String>();
		Matcher m = articleURL.matcher(pageContent);
		while (m.find())
		{
			String aid = m.group(m.groupCount());
			String url = m.group(m.groupCount() - 1);
//			System.out.printf("%s %s\n", aid,url);
			/*
			 * If it's a PDF link, then we don't want to keep it in here.
			 */
			if (pdfURL.matcher(url).matches())
				continue;
			url = relativeToAbsoluteURL(pageURL, url);
			idToURL.put(aid, url);
		}
		return idToURL;
	}

	public static String getMd5(File f)
	{
		try
		{
			MessageDigest complete = MessageDigest.getInstance("MD5");
			InputStream fis = new FileInputStream(f);
			byte[] buffer = new byte[1024];
			int numRead;
			do
			{
				numRead = fis.read(buffer);
				if (numRead > 0)
				{
					complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
			fis.close();
			byte[] digest = complete.digest();
			BigInteger bigInt = new BigInteger(1, digest);
			return new String(bigInt.toString(16));
		} catch (Exception e)
		{
			e.printStackTrace();
			return "";
		}
	}

	static LinkedHashMap<String, String> getPDFsFromPage(String pageURL, String pageContent) throws Exception
	{
		LinkedHashMap<String, String> idToURL = new LinkedHashMap<String, String>();
		Matcher m = pdfURL.matcher(pageContent);
		while (m.find())
		{
			String aid = m.group(m.groupCount());
			String url = m.group(m.groupCount() - 1);
//			System.out.printf("  PDF %s %s\n",aid,url);
			url = relativeToAbsoluteURL(pageURL, url);
			idToURL.put(aid, url);
		}
		return idToURL;
	}

	
	static LinkedHashMap<String, ArrayList<String>> getTagsFromPage(String pageURL, String pageContent)
			throws Exception
	{
		LinkedHashMap<String, ArrayList<String>> idToURL = new LinkedHashMap<String, ArrayList<String>>();
		Matcher m = chunkGrabber.matcher(pageContent);
		while (m.find())
		{
			/*
			 * Chunk contains the article URL, and everything up until the next <li> element.
			 */
			String chunk = m.group();
			/*
			 * First, extract the article ID.
			 */
			Matcher m2 = articleURL.matcher(chunk);
			m2.find();
			String aid = m2.group(m2.groupCount());

			/*
			 * Now we want to send the chunk through tagFinder and get the tags.
			 */
			ArrayList<String> tags = new ArrayList<String>();
			Matcher m3 = tagFinder.matcher(chunk);
			while (m3.find())
			{
				String tag = m3.group(m3.groupCount());
				tags.add(tag);
			}
			idToURL.put(aid, tags);
		}
		return idToURL;
	}

	static void printHelpAndDie(JSAPResult result, JSAP jsap)
	{
		// print out specific error messages describing the problems
		// with the command line, THEN print usage, THEN print full
		// help. This is called "beating the user with a clue stick."
		for (java.util.Iterator errs = result.getErrorMessageIterator(); errs.hasNext();)
		{
			System.err.println("Error: " + errs.next());
		}

		System.err.println();
		System.err.println("Usage: java " + PDFDownloader.class.getName());
		System.err.println("                " + jsap.getUsage());
		System.err.println();
		System.err.println(jsap.getHelp());
		System.exit(1);
	}

	static String relativeToAbsoluteURL(String base, String href) throws Exception
	{
//		System.out.println(href);
		href = StringEscapeUtils.unescapeHtml(href);
//		System.out.println(href);
		href = urlEncoder.decode(href);
//		System.out.println(href);
		
		base = StringEscapeUtils.unescapeHtml(base);
		base = urlEncoder.decode(base);

		URI baseURI = new URI(base, false);
		URI childURI = new URI(baseURI, href, false);

//		System.out.println(childURI.toString());
		return childURI.getURI();
		// return childURI.toString();
	}

	public static String repeat(String s, int i)
	{
		String tst = "";
		for (int j = 0; j < i; j++)
		{
			tst = tst + s;
		}
		return tst;
	}

	public static void writeFile(File f, String text) throws Exception
	{
		FileWriter fw = new FileWriter(f);
		StringReader read = new StringReader(text);
		int c;
		while ((c = read.read()) != -1)
		{
			fw.write(c);
		}
		fw.close();
		read.close();
	}

	String cnt;

	int dl;
	int err;
	HttpClient httpclient;
	protected int itemMax;
	protected int itemNum;
	JSAP jsap;
	protected int pageNum;
	
	String password;
	Method statusM;
	Object statusO;
	Set<String> tagSet = new LinkedHashSet<String>();
	List<CiteULikeReference> refs = new ArrayList<CiteULikeReference>();

	String username;

	int utd;

	public InheritMe()
	{
		// System.setProperty("org.apache.commons.logging.Log",
		// "org.apache.commons.logging.impl.SimpleLog");
		// System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
		// "true");
		// System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header",
		// "debug");
		// System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
		// "debug");

	}

	void debug(Object debugMe)
	{
		System.err.println(debugMe);
	}

	int retriesLeft = 0;
	void downloadURLToFile(String url, File destFile) throws Exception
	{
		String origUrl = url;
		File origFile = destFile;
		
		Thread.sleep(200);
		try
		{
			destFile.getParentFile().mkdirs();
			destFile.createNewFile();
		} catch (Exception e)
		{
			errS.println("Error creating new file: " + destFile + " . Skipping this PDF...");
			throw e;
		}
		out.print("   Downloading: ");

		url = StringEscapeUtils.escapeHtml(url);
		System.out.println(url);
		url = url.replaceAll(" ", "%20");
		GetMethod get = new GetMethod(url);
		ByteArrayOutputStream outS = new ByteArrayOutputStream();
		try
		{
			System.out.println("     Executing get...");
			httpclient.executeMethod(get);
			System.out.println("     Done!");
			
			BufferedInputStream in = new BufferedInputStream(get.getResponseBodyAsStream());
			int i = 0;
			int ind = 0;
			long length = get.getResponseContentLength();
			int starRatio = (int) length / 20;
			int numStars = 0;
			while ((i = in.read()) != -1)
			{
				if (length != -1 && ind % starRatio == 0)
				{
					status(" Downloading..." + repeat(".", ++numStars));
					out.print("*");
				}
				if (ind % 512 == 0)
				{
					waitOrExit();
				}
				outS.write(i);
				ind++;
			}

			in.close();
			outS.flush();

			RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
			raf.write(outS.toByteArray());
//			raf.write(get.getResponseBody());
			raf.close();
		} catch (java.net.SocketTimeoutException ste) {
			ste.printStackTrace();
			if (this.retriesLeft > 0) {
				this.retriesLeft--;
				System.out.println("Retries left: "+this.retriesLeft);
				this.downloadURLToFile(origUrl, origFile);
			} else {
				throw ste;
			}
		}finally
		{
			outS.close();
			get.releaseConnection();
			outS = null;
			out.print("\n");
		}
	}

	public void finished()
	{
		/*
		 * Simple finished callback. To be subclassed, perhaps anonymously.
		 */
	}

	String get(String urlString,String encoding) throws Exception {
		GetMethod get = new GetMethod(urlString);
		try
		{
			get.setFollowRedirects(true);
			httpclient.executeMethod(get);
			String s;
			if (encoding != null) {
				byte[] bytes = get.getResponseBody();
				s = new String(bytes,encoding);	
			} else {
				s = get.getResponseBodyAsString();
			}
			return s;
		} finally
		{
			get.releaseConnection();
		}
	}
	
	String get(String urlString) throws Exception
	{
		return this.get(urlString,null);
	}

	/**
	 * Searches through the users' library, 1 page at a time, and sorts articles
	 * into entries that either have PDFs, or need PDFs.
	 * 
	 * In order to avoid memory problems when dealing with huge libraries
	 * (typically >4000 articles), this will only handle 1 page at a time. Thus,
	 * it should be called repeatedly until the mapURLs map is empty.
	 * 
	 * @throws Exception
	 */
	void getArticleInfo() throws Exception
	{
		/*
		 * Clean up from the last page.
		 */
		this.refs.clear();
		
		// Continue onwards.
		pageNum++;

		itemNum = -1;
		itemMax = -1;
		status("loading data...");

		waitOrExit();

		String pageURL = BASE_URL + "json/user/"+username+"?per_page=50&page="+pageNum;
		String pageContent = get(pageURL,"UTF-8");
		
//		pageContent = pageContent.replaceAll("\"sha1\",", "\"sha1\":");
//		pageContent = pageContent.replaceAll("\"path\",", "\"path\":");
//		System.out.println(pageContent);
		JSONArray json = new JSONArray(pageContent);
		
		this.refs = getArticlesFromPage(json);

		/*
		 * Throw all the tags into the tagSet for safe keeping.
		 */
		for (CiteULikeReference ref : this.refs)
		{
			tagSet.addAll(ref.getTags());	
		}
	}

	public String getPassword()
	{
		return password;
	}

	String getURLLibrary(String user)
	{
		return BASE_URL + "user/" + user;
	}

	String getURLTag(String user, String tag)
	{
		return BASE_URL + "user/" + user + "/tag/" + tag;
	}

	public String getUsername()
	{
		return username;
	}

	NameValuePair[] hashToValuePairs(HashMap vars)
	{
		Set set = vars.keySet();
		Iterator i = set.iterator();
		NameValuePair[] pairs = new NameValuePair[set.size()];
		int ind = 0;
		while (i.hasNext())
		{
			String s = (String) i.next();
			pairs[ind] = new NameValuePair(s, (String) vars.get(s));
			ind++;
		}
		return pairs;
	}

	void init() throws Exception
	{
		httpclient = new HttpClient();
//		MultiThreadedHttpConnectionManager hcm = new MultiThreadedHttpConnectionManager();
//		httpclient.setHttpConnectionManager(hcm);
		httpclient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		httpclient.getParams().setBooleanParameter("http.protocol.single-cookie-header", true);
//		httpclient.getParams().setIntParameter("http.socket.timeout", 20000);
		Header h = new Header("Connection", "close");
		ArrayList<Header> headers = new ArrayList<Header>();
		headers.add(h);
//	    httpclient.getParams().setParameter("http.default-headers", headers);
		httpclient.getParams().setParameter(HttpMethodParams.USER_AGENT,
	     "SyncUThink "+this.getVersionString());

		String proxyHost = null;
		int proxyPort = -1;
		try
		{
			proxyHost = System.getProperty("https.proxyHost");
			proxyPort = Integer.parseInt(System.getProperty("https.proxyPort"));
			if (proxyHost == null || proxyPort < 0)
				throw new Exception();
		} catch (Exception e)
		{
			proxyHost = null;
			proxyPort = -1;
			try
			{
				proxyHost = System.getProperty("http.proxyHost");
				proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
			} catch (Exception e1)
			{
				proxyHost = null;
				proxyPort = -1;
			}
		}
		if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0)
		{
			status("Found proxy: " + proxyHost + ":" + proxyPort);
			httpclient.getHostConfiguration().setProxy(proxyHost, proxyPort);
			try
			{
				Thread.sleep(500);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private String getVersionString() {
		
		return SyncUThink.getVersionString() + " / user:"+this.username;
	}
	void init(String[] args) throws Exception
	{
		jsap = new JSAP();
		boolean requireUserPass = true;

		FlaggedOption userOption = new FlaggedOption("username").setStringParser(JSAP.STRING_PARSER).setRequired(
			requireUserPass).setShortFlag('u');
		userOption.setHelp("Your CiteULike username");
		jsap.registerParameter(userOption);

		FlaggedOption pwOption = new FlaggedOption("password").setStringParser(JSAP.STRING_PARSER).setRequired(
			requireUserPass).setShortFlag('p');
		pwOption.setHelp("Your CiteULike password");
		jsap.registerParameter(pwOption);

		FlaggedOption dirOption = new FlaggedOption("dir").setStringParser(JSAP.STRING_PARSER).setRequired(false)
				.setShortFlag('d');
		dirOption
				.setHelp("The directory with which to sync your CiteULike PDFs. If no directory is specified, then I'll try to open a graphical file chooser to choose one. No guarantees it'll work, though!");
		jsap.registerParameter(dirOption);
		JSAPResult result = jsap.parse(args);

		/*
		 * If something went wrong with params, do the good deed.
		 */
		if (!result.success())
			printHelpAndDie(result, jsap);

		username = result.getString("username", "");
		password = result.getString("password", "");

	}

	void login() throws Exception
	{
		init();
		/*
		 * Login to CiteULike using the username and password.
		 */
		status("Logging in...");
		HashMap vars = new HashMap();
		vars.put("username", username);
		System.out.println(username);
		vars.put("password", password);
		vars.put("from", "");
		if (username.length() == 0 || password.length() == 0)
		{
			throw new Exception("Error: Username or password is blank. Try again.");
		}
		String response = post(BASE_URL + "login.do", vars);
		if (response.contains("login-failed"))
		{
			throw new Exception("Login failed. Check your spelling and try again.");
		} else
		{
			return;
		}
	}

	public synchronized void pauseThread()
	{
		super.pauseThread();
		status("Paused.");
	}
	
	String post(String urlString, HashMap vars) throws Exception
	{
		PostMethod post = new PostMethod(urlString);
		try
		{
			NameValuePair[] params = hashToValuePairs(vars);
			post.setRequestBody(params);
			httpclient.executeMethod(post);
			String s = post.getResponseBodyAsString();

			return s;
		} finally
		{
			post.releaseConnection();
		}
	}

	String readFile(File f) throws Exception
	{
		return readInputStream(new FileInputStream(f));
	}

	String readInputStream(InputStream in) throws Exception
	{
		StringBuffer b = new StringBuffer();
		String s;
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		while ((s = r.readLine()) != null)
		{
			b.append(s);
			b.append("\n");
		}
		return b.toString();
	}

	public void run()
	{
		resumeThread();
		try
		{
			startMe();
		} catch (Exception e)
		{
			if (isStopped())
			{
				status("Stopped.");
				return;
			} else
			{
				e.printStackTrace();
				status(e.getMessage());
			}
		} finally
		{
			finished();
		}
	}

	// String getHeaders(HttpMessage rsp)
	// {
	// Header[] headers = rsp.getAllHeaders();
	// StringBuffer sb = new StringBuffer();
	// for (int i = 0; i < headers.length; i++)
	// {
	// sb.append(headers[i].getValue());
	// }
	// return sb.toString();
	// }
	//
	// void printHeaders(HttpMessage rsp)
	// {
	// out.println("----------------------------------------");
	// // out.println(rsp.getStatusLine());
	// Header[] headers = rsp.getAllHeaders();
	// for (int i = 0; i < headers.length; i++)
	// {
	// System.out.println(headers[i]);
	// }
	// out.println("----------------------------------------");
	// }

	protected void setArticleLink(String s, String url)
	{
		if (statusO == null)
			return;
		try
		{
			Method m = statusO.getClass().getMethod("setCurrentArticle", String.class, String.class);
			m.invoke(statusO, s, url);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setDebug(Object o, String method)
	{
		try
		{
			statusO = o;
			statusM = o.getClass().getMethod(method, new Class[] { String.class });
		} catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	boolean shouldWeSkip(CiteULikeReference ref)
	{
		List<String> articleTags = ref.getTags();
		for (String tag : articleTags)
		{
			if (tag.toLowerCase().contains(SKIP_STRING))
				return true;
		}
		return false;
	}

	String spaces(int depth)
	{
		String s = new String();
		for (int i = 0; i < depth * 2; i++)
		{
			s += " ";
		}
		return s;
	}

	//	
	//	public static void main(String[] args)
	//	{
	//		InheritMe ih = new InheritMe(){
	//
	//			@Override
	//			protected void startMe() throws Exception
	//			{
	//				init();
	//				System.out.println(get("http://www.google.com/"));
	//			}
	//			
	//		};
	//		try
	//		{
	//			ih.startMe();
	//		} catch (Exception e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}

	protected abstract void startMe() throws Exception;

	void status(String message)
	{
		status(message, false);
	}

	void status(String message, boolean log)
	{
		if (pageNum > 0)
		{
			String prefix = "Page " + pageNum;
			if (itemMax != -1 && itemNum != -1)
				prefix += " " + itemNum + "/" + itemMax + " ";
			message = prefix + ": " + message;
		}
		if (log)
			debug(message);

		if (statusM == null || statusO == null)
			return;
		try
		{
			statusM.invoke(statusO, new Object[] { message });
		} catch (Exception e)
		{
			e.printStackTrace();
			statusM = null;
			return;
		}
	}
}
