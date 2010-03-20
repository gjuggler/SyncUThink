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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.time.DateUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

public class PDFDownloader extends InheritMe
{

	int PAUSE_TIME = 1000;

	//	  file = {:C\:\\Users\\Greg\\Documents\\Papers\\CiteULike\\general\\winzeler_malariazzzz.pdf:PDF},

	public static void main(String[] args) throws Exception
	{
		JSAP jsap = new JSAP();
		boolean requireUserPass = false;

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

		FlaggedOption dirOption =
				new FlaggedOption("dir").setStringParser(JSAP.STRING_PARSER).setRequired(false).setShortFlag('d');
		dirOption
				.setHelp("The directory with which to sync your CiteULike PDFs. If no directory is specified, then I'll try to open a graphical file chooser to choose one.");
		jsap.registerParameter(dirOption);

		FlaggedOption tagOption =
				new FlaggedOption("tagfolders").setStringParser(JSAP.BOOLEAN_PARSER).setRequired(false).setShortFlag(
					't');

		tagOption
				.setHelp("If set, then the PDFs will be organized into subfolders by tag. A PDF with multiple tags will be downloaded multiple times.");
		jsap.registerParameter(tagOption);

		FlaggedOption yearOption =
				new FlaggedOption("yearfolders").setStringParser(JSAP.BOOLEAN_PARSER).setRequired(false).setShortFlag(
					'y');
		yearOption
				.setHelp("If set, then the PDFs will be organized into subfolders by year. This is independent of the 'tagfolders' parameter, but the tag subdirectories are created first.");
		jsap.registerParameter(yearOption);
		JSAPResult result = jsap.parse(args);

		/*
		 * If something went wrong with params, do the good deed.
		 */
		if (!result.success())
			printHelpAndDie(result, jsap);

		File f;
		if (result.contains("dir"))
		{
			f = new File(result.getString("dir"));
		} else
		{
			out.println("No directory was specified. Attempting to load a graphical file chooser...");
			f = chooseDirectory();
			// f = new File("c:/users/greg/Documents/Papers to
			// Read/CiteULike/");
			// f = new File(System.getProperty("user.home"));
		}

		PDFDownloader pdf = new PDFDownloader();
		String username = result.getString("username");
		String password = result.getString("password");
		boolean subTags = result.getBoolean("tagfolders");
		boolean subYears = result.getBoolean("yearfolders");
		pdf.setUsername(username);
		pdf.setPassword(password);
		pdf.setSubTags(subTags);
		pdf.setSubYears(subYears);
		pdf.setBaseDir(f);
		pdf.start();
	}

	File baseDir;
	boolean flipFilename = false;
	boolean subTags = false;
	boolean subYears = false;
	private boolean uploadNewer;
	HashMap<String, File> idToFile = new HashMap<String, File>();

	public PDFDownloader()
	{
	}

	protected void startMe() throws Exception
	{
		login();
		retrievePDFsFromHTML();
	}

	static File returnNewer(File a, File b)
	{
		if (a == null)
			return b;
		if (b == null)
			return a;

		long mA = a.lastModified();
		long mB = b.lastModified();
		if (mA > mB)
			return a;
		else
			return b;
	}

	/**
	 * Uploads and downloads the bibtex library file, to synchronize library
	 * changes.
	 */
	void syncBibTex() throws Exception
	{
		String base = baseDir.getCanonicalPath() + sep;
		itemMax = -1;
		final File bibFile = new File(base + username + ".bib");
		//		final File md5File = new File(base + ".md5s.txt");
		final File dateFile = new File(base + "sync_info.txt");
		// Check the MD5s, if the .md5s.txt file exists.

		long bibModified = bibFile.lastModified();
		long lastDownload = 0;

		boolean download = true;
		boolean upload = true;
		if (dateFile.exists())
		{
			String fileS = readFile(dateFile);
			String[] props = fileS.split("\n");
			for (String prop : props)
			{
				String[] keyval = prop.split("=");
				if (keyval[0].equals("date"))
				{
					lastDownload = Long.valueOf(keyval[1]);
				}
			}
		}

		if (lastDownload >= bibModified)
		{
			upload = false;
		}

		boolean uploadSuccess = false;
		if (bibFile.exists() && uploadNewer && upload)
		{
			BufferedReader br = null;
			try
			{
				status("Uploading BibTex file...");
				FilePartSource fsrc = new FilePartSource(bibFile);
				FilePart fp = new FilePart("file", fsrc);
				br = new BufferedReader(new FileReader(bibFile));
				StringBuffer sb = new StringBuffer();
				String s;
				while ((s = br.readLine()) != null)
				{
					sb.append(s + "\n");
				}
				String str = sb.toString();

				final Part[] parts =
						new Part[] { new StringPart("btn_bibtex", "Import BibTeX file..."),
								new StringPart("to_read", "2"), new StringPart("tag", ""),
								new StringPart("private", "t"), new StringPart("update_allowed", "t"),
								new StringPart("update_id", "cul-id"), new StringPart("replace_notes", "t"),
								new StringPart("replace_tags", "t"), fp };
				waitOrExit();
				PostMethod filePost = new PostMethod(BASE_URL + "/profile/" + username + "/import_do");
				try
				{
					filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
					httpclient.executeMethod(filePost);
					String response = filePost.getResponseBodyAsString();
					//				System.out.println(response);
					uploadSuccess = true;
					System.out.println("Bibtex upload success!");
				} finally
				{
					filePost.releaseConnection();
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			} finally
			{
				if (br != null)
					br.close();
			}
		}

		if (download || upload)
		{
			status("Downloading BibTeX file...");
			String pageURL = BASE_URL + "bibtex/user/" + username + "";
			FileWriter fw = new FileWriter(bibFile);
						try
						{
							GetMethod get = new GetMethod(pageURL);
							httpclient.executeMethod(get);
							InputStreamReader read = new InputStreamReader(get.getResponseBodyAsStream());
							int c;
							while ((c = read.read()) != -1)
							{
								waitOrExit();
								fw.write(c);
							}
							read.close();
						} finally
						{
							fw.close();
						}
		}

		// Store the checksums.
		if (uploadSuccess)
		{
			//			if (fileChecksum == null)
			//			{
			//				fileChecksum = getMd5(bibFile);
			//				remoteChecksum = get(BASE_URL + "bibtex/user/" + username + "?md5=true");
			//			}
			//			String md5S = fileChecksum + "\n" + remoteChecksum;
			//			writeFile(md5File, md5S);
			String dateS = "date=" + Calendar.getInstance().getTimeInMillis();
			writeFile(dateFile, dateS);
		}
	}

	void retrievePDFsFromHTML() throws Exception
	{
		if (!baseDir.exists() || !baseDir.canWrite() || !baseDir.isDirectory())
		{
			throw new Exception("Error: Destination is read-only or does not exist.");
		}
		String base = baseDir.getCanonicalPath() + sep;

		syncBibTex();

		do
		{
			Thread.sleep(InheritMe.BETWEEN_PDF_DOWNLOADS_SLEEP_TIME);
			getArticleInfo();

			/*
			 * First thing's first, create folders and insert the URL links if necessary.
			 */
			if (subTags)
			{
				status("Creating folders and links...");
				createTagFoldersAndLinks(tagSet);
				status("Folders and links created!");
				//				Thread.sleep(PAUSE_TIME/10);
			}

			List<CiteULikeReference> articlesWithoutPDFs = this.getArticlesWithoutPDFs(this.refs);
			List<CiteULikeReference> articlesWithPDFs = new ArrayList<CiteULikeReference>();
			articlesWithPDFs.addAll(this.refs);
			articlesWithPDFs.removeAll(articlesWithoutPDFs);

			itemMax = articlesWithPDFs.size();
			itemNum = 0;

			int i = -1;
			for (CiteULikeReference ref : articlesWithPDFs)
			{
				System.out.println(ref.userfiles);
				itemNum++;
				i++;
				waitOrExit();

				setArticleLink("Current article ID: " + ref.article_id, ref.href);

				try
				{
					waitOrExit();
					// Grab the article page's text to get the date.
					Date remote = null;
					//					if (this.uploadNewer) {
					String articleContent = get(ref.href);
					remote = getStampFromArticlePage(articleContent);
					System.out.println("Remote timestamp: " + remote);
					//					}

					status("Checking for existing file...");

					for (String fileName : ref.userfiles.keySet())
					{
						waitOrExit();
						String fullPath = ref.userfiles.get(fileName);
						System.out.println(fileName+" -> " +fullPath);

						/*
						 * Try and put the year first.
						 */
						String[] bits = fileName.split("_");
						String yearS = "";
						String otherFilename = "";
						String targetFilename = "";
						if (bits.length == 3)
						{
							String flipped = bits[1] + "_" + bits[0] + "_" + bits[2];
							if (flipFilename)
							{
								otherFilename = fileName;
								targetFilename = flipped;
							} else
							{
								targetFilename = fileName;
								otherFilename = flipped;
							}
							if (subYears)
							{
								yearS = String.valueOf(bits[1]);
							}
						} else
						{
							targetFilename = fileName;
							otherFilename = fileName;
						}

						/*
						 * If we're organized by tags, add a destination file for each of this PDf's tags.
						 * If not, then just add the one outputFile to the outputFiles array. 
						 */
						ArrayList<File> outputFiles = new ArrayList<File>();
						ArrayList<File> maybeNewFiles = new ArrayList<File>();
						File altFile = null;
						File myFile = null;

						// Keep track of the newest file and its timestamp.
						File newestFile = null;
						long newestStamp = 0;

						if (subTags && ref.getTags().size() > 0)
						{
							for (String tag : ref.getTags())
							{
								if (tag.equalsIgnoreCase("no-tag"))
									tag = "";
								String curDir = base + tag + sep;
								curDir += yearS;
								myFile = new File(curDir + sep + targetFilename);
								altFile = new File(curDir + sep + otherFilename);
								if (myFile.exists())
								{
									newestFile = returnNewer(newestFile, myFile);
								} else if (altFile.exists())
								{
									try
									{
										altFile.renameTo(myFile);
									} catch (Exception e)
									{
										e.printStackTrace();
									}
									//								outputFiles.add(myFile);
								} else
								{
									outputFiles.add(myFile);
								}
							}
						} else
						{
							myFile = new File(base + yearS + sep + targetFilename);
							altFile = new File(base + yearS + sep + otherFilename);
							maybeNewFiles.add(myFile);
							maybeNewFiles.add(altFile);
							if (myFile.exists())
							{
								newestFile = returnNewer(newestFile, myFile);
							} else if (altFile.exists())
							{
								try
								{
									altFile.renameTo(myFile);
								} catch (Exception e)
								{
									e.printStackTrace();
									System.exit(0);
								}
								//							outputFiles.add(myFile);
							} else
							{
								outputFiles.add(myFile);
							}
						}

						// If we have a newest file, check against the server to see if it's much newer.
						if (newestFile != null && remote != null)
						{
							Date local = new Date(newestFile.lastModified());
							long lT = local.getTime();
							long rT = remote.getTime();
							if (lT - rT > (1000) * (60) * (1)) // Fudge factor of 1 minute.
							{
								if (uploadNewer)
								{
									System.err.println("Local file is newer than remote! Uploading...");
									status("Local file was modified! Uploading...");
									
									// OK. Since CiteULike now uses hashing to evaluate the identity of files, we have to first delete
									// the existing attachment with this filename.
									String url = this.BASE_URL+ "personal_pdf_delete?";
									url += "username="+this.username;
									url += "&article_id="+ref.article_id;
									
									// Parse the userfile_id from the filename:
									String userFileId = ref.userfileIds.get(fileName);
									url += "&userfile_id="+userFileId;
									System.out.println(url);
									get(url);
									
									uploadPDF(ref.article_id, newestFile);

									// Re-collect the timestamp, and re-stamp the local file. This is done so they match after re-uploading.
																		String newContent = get(ref.href);
																		Date remote2 = getStampFromArticlePage(newContent);
																		newestFile.setLastModified(remote2.getTime());
								}
							}
						}
						if (outputFiles.size() == 0)
						{
							status("Already up-to-date!");
							utd++;
							continue;
						}

						Thread.sleep(200);
						/*
						 * Download the PDF to the first file.
						 */
						waitOrExit();
						status("Downloading...");
						File f = outputFiles.remove(0);

						try
						{
							String fileUrl = this.BASE_URL + fullPath;
							this.retriesLeft = 2;
							downloadURLToFile(fileUrl, f);
							if (remote != null)
								f.setLastModified(remote.getTime());
						} catch (Exception e)
						{
							e.printStackTrace();
							f.delete();
							throw e;
						}
						dl++;

						/*
						 * Go through rest of tags, and copy file accordingly.
						 * NOTE: This is only entered if this file needs to be copied over locally.
						 */
						for (int j = 0; j < outputFiles.size(); j++)
						{
							status("Copying PDF...");
							File f2 = outputFiles.get(j);
							if (f2.exists())
							{
								if (f2.lastModified() > newestStamp)
								{
									newestFile = f2;
									newestStamp = f2.lastModified();
								}
								continue;
							}
							f2.getParentFile().mkdirs();
							f2.createNewFile();
							RandomAccessFile in = new RandomAccessFile(f, "r");
							RandomAccessFile out = new RandomAccessFile(f2, "rw");

							byte[] b = new byte[(int) in.length()];
							in.readFully(b);
							out.write(b);

							in.close();
							out.close();

							f2.setLastModified(remote.getTime());
						}
					}
				} catch (Exception e)
				{
					err++;
					e.printStackTrace();
					status("Failed. See the Java console for more info.");
					Thread.sleep(PAUSE_TIME / 2);
					continue;
				}
			}
		} while (this.refs.size() > 0);
		this.pageNum = 0;
		status("Finished. " + dl + " new, " + utd + " up-to-date and " + err + " failed.");
		out.println("Done!");
	}

	private Date getStampFromArticlePage(String articleContent)
	{
		Date d = null;

		Matcher m = stampPattern.matcher(articleContent);
		while (m.find())
		{
			/*
			 * Chunk contains the article URL, and everything up until the next <li> element.
			 */
			String chunk = m.group(1);

			try
			{
				//				System.out.println("Chunk: " + chunk);
				d = DateUtils.parseDate(chunk, new String[] { "y-M-d H:m:s" });
				//				System.out.println("Date: " + d);
			} catch (ParseException e)
			{
				e.printStackTrace();
			}
		}

		return d;
	}

	static Pattern stampPattern = RegexUtils.createPattern(", (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");

	void createTagFoldersAndLinks(Set<String> tags) throws Exception
	{
		tags.add("");
		for (String s : tags)
		{
			if (s.toLowerCase().contains(SKIP_STRING))
				continue;
			else if (s.toLowerCase().equals("no-tag"))
				continue;
			String base = baseDir.getCanonicalPath() + sep;
			File f = new File(base + s);
			f.mkdirs();
			File link = new File(f.getCanonicalPath() + sep + "link.url");
			if (link.createNewFile())
			{
				FileWriter wr = new FileWriter(link);
				wr.write("[InternetShortcut]\n");
				if (s.equals(""))
					wr.write("URL=" + getURLLibrary(username));
				else
					wr.write("URL=" + getURLTag(username, s));
				wr.close();
			}
		}
	}

	static File chooseDirectory() throws Exception
	{
		JFileChooser fc = new JFileChooser();
		// fc.setDialogTitle("Choose Output Directory");

		// Start in current directory
		fc.setCurrentDirectory(new File("."));

		// Choose only directories only.
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		// Open chooser dialog
		int result = fc.showSaveDialog(new JFrame());

		if (result == JFileChooser.CANCEL_OPTION)
		{
			out.println("Directory choice was canceled. Exiting...");
			// System.exit(1);
			throw new Exception();
		}
		File f = fc.getSelectedFile();
		out.println("Chosen directory:" + f);
		return f;
	}

	private void uploadPDF(String id, File file) throws Exception
	{
		/*
		 * Set up the Multipart POST file upload.
		 */
		StringPart p1 = new StringPart("article_id", id);
		StringPart p2 = new StringPart("username", username);
		StringPart p3 = new StringPart("check", "v2");
		StringPart p4 = new StringPart("keep_name", "yes");
		
		// Read the file into a byte array.
		InputStream is = new FileInputStream(file);
		long length = file.length();
		byte[] bytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
		{
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length)
		{
			throw new IOException("Could not completely read file " + file.getName());
		}
		// Close the input stream and return bytes
		is.close();

		ByteArrayPartSource source = new ByteArrayPartSource(file.getName(), bytes);
		FilePart fp = new FilePart("file", source);
		fp.setName("file");

		Part[] parts = new Part[] { p1, p2, p3, p4, fp };
		//		status("Uploading...");
		//		debug("Uploading...");

		waitOrExit();

		PostMethod filePost = new PostMethod(BASE_URL + "personal_pdf_upload");
		try
		{
			filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
			httpclient.executeMethod(filePost);
			String response = filePost.getResponseBodyAsString();
//			System.out.println(response);
			if (response.contains("didn't work"))
				throw new Exception("CiteULike thinks the PDF is invalid!");
		} finally
		{
			is.close();
			is = null;
			bytes = null;
			source = null;
			parts = null;
			filePost.releaseConnection();
		}
	}

	public boolean isSubTags()
	{
		return subTags;
	}

	public void setSubTags(boolean subTags)
	{
		this.subTags = subTags;
	}

	public boolean isSubYears()
	{
		return subYears;
	}

	public void setSubYears(boolean subYears)
	{
		this.subYears = subYears;
	}

	public File getBaseDir()
	{
		return baseDir;
	}

	public void setBaseDir(File f)
	{
		this.baseDir = f;
	}

	public boolean isFlipFilename()
	{
		return flipFilename;
	}

	public void setFlipFilename(boolean flipFilename)
	{
		this.flipFilename = flipFilename;
	}

	public void setUploadNewer(boolean uploadNewer)
	{
		this.uploadNewer = uploadNewer;
	}

}
