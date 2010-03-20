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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class PDFSearcher extends InheritMe
{

	static ArrayList<Pattern> linkPatterns = new ArrayList<Pattern>();

	static
	{
		linkPatterns.add(RegexUtils.hrefRegex("doi.org"));
		linkPatterns.add(RegexUtils.hrefRegex("pubmed"));
		linkPatterns.add(RegexUtils.hrefRegex("aps.org"));
		linkPatterns.add(RegexUtils.hrefRegex("acm.org"));
		/*
		 * HubMed is still really slow for me, so let's avoid it for now.
		 */
		// linkPatterns.add(hrefRegex("hubmed"));
		linkPatterns.add(RegexUtils.hrefRegex("interscience.wiley"));
		linkPatterns.add(RegexUtils.hrefRegex("jstor"));
		linkPatterns.add(RegexUtils.hrefRegex("link.aip.org"));
		linkPatterns.add(RegexUtils.hrefRegex("jlr.org"));
		linkPatterns.add(RegexUtils.hrefRegex("biophysj.org"));
	}

	public static void main(String[] args) throws Exception
	{
		JSAP jsap = new JSAP();

		boolean requireUserPass = true;

		FlaggedOption userOption =
				new FlaggedOption("username").setStringParser(JSAP.STRING_PARSER).setRequired(requireUserPass)
						.setShortFlag('u');
		userOption.setHelp("Your CiteULike username");
		jsap.registerParameter(userOption);

		FlaggedOption pwOption =
				new FlaggedOption("password").setStringParser(JSAP.STRING_PARSER).setRequired(requireUserPass)
						.setShortFlag('p');
		pwOption.setHelp("Your CiteULike password");
		jsap.registerParameter(pwOption);

		JSAPResult result = jsap.parse(args);

		PDFSearcher pdf = new PDFSearcher();

		String username = result.getString("username");
		String password = result.getString("password");
		pdf.setUsername(username);
		pdf.setPassword(password);

		pdf.setTimeout(30);

		pdf.start();
	}

	Pattern amp = Pattern.compile("&amp;");

	String curLocation;

	Pattern doiExtractor = Pattern.compile("dx.doi.org/(.*)");

	String doiString;

	Pattern findServerString = Pattern.compile("http://[^/]*?/");

	ArrayList<Pattern> patterns;

	// Pattern pdfURL = hrefRegex("/pdf/user/");
	boolean skipToNext;

	float timeout;

	Timer timer = new Timer();

	public PDFSearcher()
	{
	}

	ArrayList<String> handleArticlePage(String body) throws Exception
	{
		ArrayList<String> outLinks = new ArrayList<String>();

		Matcher m;
		for (int i = 0; i < linkPatterns.size(); i++)
		{
			Pattern p = linkPatterns.get(i);
			m = p.matcher(body);
			if (m.find())
			{
				outLinks.add(m.group(m.groupCount()));
			}
		}

		/*
		 * Special case: Look for a direct PDF link within the "Fulltext article" links.
		 */
		Pattern p = RegexUtils.grabUntilClosingElement("div", RegexUtils.createPattern("view fulltext"));
		m = p.matcher(body);
		while (m.find())
		{
			String s = m.group();
			Pattern p2 = RegexUtils.hrefRegex("pdf");
			Matcher m2 = p2.matcher(s);
			while (m2.find())
			{
				String couldbePDF = m2.group(m2.groupCount());
				outLinks.add(0, couldbePDF);
			}
		}

		return outLinks;
	}

	boolean hasProblem(String currentURL, String nextURL) throws Exception
	{
		String nlc = nextURL.toLowerCase();
		if (nlc.contains("arjournals")) {
			throw new Exception("Give up on Annual Reviews!");
		}
		if (nlc.contains("faqs"))
			return true;
		if (nlc.contains("covercontest"))
			return true;
		if (doiString != null && nlc.contains("blackwell"))
		{
			// err.println(nlc);
			if (!nlc.contains(doiString))
				return true;
		}
		if (currentURL.contains("icon_pdf"))
			return true;
		if (currentURL.contains("userimages")) // ACM journals give crap PDF links on article pages.
			return true;
		if (currentURL.contains("lib_rec.pdf")) // Journal of Geophysical Research gives crap PDF links on article pages. 
			return true;
		
		return false;
	}

	void initPatternArray()
	{
		patterns = new ArrayList<Pattern>();
		patterns.add(RegexUtils.elementIdRegex("div", "journal_info", ".pdf"));
		patterns.add(RegexUtils.generalRegex("content", "reprint"));
		patterns.add(RegexUtils.generalRegex("src", ".pdf"));
		patterns.add(RegexUtils.hrefRegex(".pdf"));
		patterns.add(RegexUtils.hrefRegex("articlerender.fcgi"));
		patterns.add(RegexUtils.hrefRegex("fref.fcgi"));
		patterns.add(RegexUtils.hrefRegex("printpage"));
		patterns.add(RegexUtils.hrefRegex("fulltext"));
		patterns.add(RegexUtils.hrefRegex("pdf"));
	}

	public GetMethod pdfScrape(String startingURL, int maxSearchDepth) throws Exception
	{
		doiString = null;
		GetMethod get = scrapeImpl(startingURL, startingURL, 1, maxSearchDepth);
		return get;
	}

	private GetMethod scrapeImpl(String curURL, String href, int curDepth, int maxDepth) throws Exception
	{
		waitOrExit();
		Thread.sleep(InheritMe.BETWEEN_WEB_SCRAPING_SLEEP_TIME);

		if (skipToNext)
		{
			skipToNext = false;
			status("Timed out.");
			debug("Timed out.");
			Thread.sleep(1500);
			throw new Exception();
		}

		/*
		 * Capture the DOI if it exists.
		 */
		if (doiString == null && curURL.contains("doi"))
		{
			Matcher doiM = doiExtractor.matcher(curURL);
			if (doiM.find())
			{
				doiString = doiM.group(doiM.groupCount());
			}
		}

		status("Searching..." + repeat(".", curDepth * 2));
		curDepth += 1;

		// URI uri = new URI(curURL, false);
		curURL = escapeURL(curURL);
		debug(repeat(" ", curDepth * 2) + curURL);
		GetMethod get = new GetMethod(curURL);
		get.setFollowRedirects(true);
		get.setDoAuthentication(true);
		httpclient.executeMethod(get);

		/*
		 * Check the response for the PDF type. If so, then we've found the PDF.
		 */
		Header h = get.getResponseHeader("Content-type");
		// System.out.println(h.getValue());
		String type = h.getValue().toLowerCase();
		if (type.contains("pdf"))
		{
			if (!hasProblem(curURL,"")) {
				return get;
			}
		}

		/*
		 * So it's not a PDF, let's get the content and search for more potential PDF links.
		 */
		String content = readInputStream(get.getResponseBodyAsStream());
		get.releaseConnection();

		/*
		 * Match it against the potentially useful links.
		 */
		for (int i = 0; i < patterns.size(); i++)
		{
			Pattern p = patterns.get(i);
			Matcher m = p.matcher(content);
			boolean foundAnything = false;
			while (m.find())
			{
				foundAnything = true;
				String s = m.group(m.groupCount());

				/*
				 * Create a new URI from this link string.
				 */
				s = relativeToAbsoluteURL(get.getURI().toString(), s);

				/*
				 * GJ 2009-02-20. Remove the "+html" from annoying PNAS links (yes, it's super-specific, but it must be done!)
				 */
				s = removeHTMLFromLink(s);

				/*
				 * Send the current URL and the potential match to a separate method for a chance to opt-out in special cases.
				 */
				if (hasProblem(curURL, s))
					continue;

				if (curDepth < maxDepth)
				{
					/*
					 * Recurse.
					 */
					GetMethod g = scrapeImpl(s, "", curDepth, maxDepth);
					if (g != null)
						return g;
				}
				
				Thread.sleep(400);
			}
			// if (foundAnything)
			// break;
		}
		return null;
	}

	public String removeHTMLFromLink(String s)
	{
		String regex = "html";
		if (s.contains(regex) && s.contains("pdf"))
		{
			System.out.println("Removing 'html' from pdf+html link...");
			s = s.replaceAll(regex, "");
		}
		return s;
	}

	public void setTimeout(float seconds)
	{
		timeout = seconds;
	}

	
	
	public void startMe() throws Exception
	{
		if (username.length() == 0 || password.length() == 0)
		{
			status("Error: Username or password is blank. Try again.");
			return;
		}

		initPatternArray();

		login();

		do
		{
			getArticleInfo();
			List<CiteULikeReference> articlesWithoutPDFs = getArticlesWithoutPDFs(this.refs);
			
			itemMax = articlesWithoutPDFs.size();
			itemNum = 0;

			utd += this.refs.size() - articlesWithoutPDFs.size();

			Thread.sleep(1000);

			for (int i = 0; i < articlesWithoutPDFs.size(); i++)
			{
				itemNum++;
				waitOrExit();

				cnt = String.valueOf(i + 1) + "/" + articlesWithoutPDFs.size();
				status("Searching...");

				/*
				 * Set the timeout timer.
				 */
				TimerTask task = new TimerTask()
				{
					public void run()
					{
						skipToNext = true;
					}
				};
				timer.schedule(task, (long) timeout * 1000);

				try
				{
					CiteULikeReference ref = articlesWithoutPDFs.get(i);
					System.out.println(ref.href);
					setArticleLink("Current article ID: " + ref.article_id, ref.href);

					waitOrExit();

					GetMethod get = null;
					for (String linkOut : ref.linkouts)
					{
						try
						{
							get = pdfScrape(linkOut, 5);
							if (get != null)
								break;
						} catch (Exception e)
						{
							System.err.println("Error retrieving article: "+e.getMessage());
							System.err.println("  Will continue to the next one...");
							continue;
						}
					}

					// Sorry, no PDF for you!
					if (get == null)
					{
						throw new Exception("No PDF was found!");
					}

					/*
					 * Looks like we really found a PDF. Let's download it.
					 */
					try
					{
						InputStream in = get.getResponseBodyAsStream();
						ByteArrayOutputStream ba = new ByteArrayOutputStream();

						waitOrExit();

						status("Downloading...");
						debug("Downloading...");
						int j = 0;
						int ind = 0;
						long length = get.getResponseContentLength();
						int starRatio = (int) length / 20;
						int numStars = 0;
						while ((j = in.read()) != -1)
						{
							if (length != -1 && ind % starRatio == 0)
							{
								status("Downloading..." + repeat(".", ++numStars));
							}
							if (ind % 1000 == 0)
							{
								waitOrExit();
							}
							ba.write(j);
							ind++;
						}
						/*
						 * Set up the Multipart POST file upload.
						 */
						//					String id = url.substring(url.lastIndexOf("/") + 1, url
						//							.length());
						StringPart p1 = new StringPart("article_id", ref.article_id);
						StringPart p2 = new StringPart("username", username);
						StringPart p3 = new StringPart("check", "v2");
						ByteArrayPartSource source = new ByteArrayPartSource("temp.pdf", ba.toByteArray());
						FilePart fp = new FilePart("file", source);
						fp.setName("file");

						Part[] parts = new Part[] { p1, p2, p3, fp };
						status("Uploading...");
						debug("Uploading...");

						waitOrExit();

						PostMethod filePost = new PostMethod(BASE_URL + "personal_pdf_upload");
						try
						{
							filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
							httpclient.executeMethod(filePost);
							String response = filePost.getResponseBodyAsString();
							System.out.println(response);
							if (response.contains("didn't work"))
								throw new Exception("CiteULike thinks the PDF is invalid!");
						} finally
						{
							ba = null;
							source = null;
							parts = null;
							filePost.releaseConnection();
						}

					} finally
					{
						get.releaseConnection();
					}

					status("Done!");
					Thread.sleep(BETWEEN_SEARCH_ITEMS_SLEEP_TIME);
					dl++;
				} catch (Exception e)
				{
					if (isStopped())
					{
						throw e;
					} else if (skipToNext)
					{
						err++;
						status("Timed out.");
						Thread.sleep(BETWEEN_SEARCH_ITEMS_SLEEP_TIME);
						continue;
					} else if (e instanceof Exception)
					{
						err++;
						status(e.getMessage());
						Thread.sleep(BETWEEN_SEARCH_ITEMS_SLEEP_TIME);
						continue;
					} else
					{
						err++;
						e.printStackTrace();
						status("Failed. See the Java console for more info.");
						Thread.sleep(BETWEEN_SEARCH_ITEMS_SLEEP_TIME);
						continue;
					}
				} finally
				{
					task.cancel();
					skipToNext = false;
				}
			}
		} while (this.refs.size() != 0);

		setArticleLink("", "");
		this.pageNum = 0;
		status("Finished. " + dl + " found, " + utd + " existing and " + err + " failed.");
	}
}
