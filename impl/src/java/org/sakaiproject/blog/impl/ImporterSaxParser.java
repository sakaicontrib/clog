package org.sakaiproject.blog.impl;

import java.io.StringReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.sakaiproject.blog.api.SakaiProxy;
import org.sakaiproject.blog.api.datamodel.Comment;
import org.sakaiproject.blog.api.datamodel.Post;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

public class ImporterSaxParser extends DefaultHandler
{
	private Logger logger = Logger.getLogger(getClass());
	
	private Connection connection = null;
	
	private SAXParser parser = null;
	private SakaiProxy sakaiProxy = null;
	private Post post = null;
	
	private boolean processingTitle = false;
	private boolean processingShortText = false;
	private boolean processingDate = false;
	private boolean processingKeyword = false;
	private boolean processingCreator = false;
	private boolean processingVisibility = false;
	private boolean processingAllowComments = false;
	private boolean processingParagraph = false;
	private boolean processingCommentText = false;
	private boolean processingCommentDate = false;
	private boolean processingCommentCreator = false;
	private boolean processingImageId = false;
	private boolean processingFileId = false;
	private boolean processingFileDescription = false;
	private boolean processingLinkRuleDescription = false;
	private boolean processingLinkExpression = false;
	
	private String currentFileId = "";
	private String currentFileName = "";
	
	private String currentLinkRuleDescription = "";
	private String currentLinkExpression = "";
	
	private String collectedText = "";
	private Comment comment = null;
	
	public ImporterSaxParser(Connection connection,SakaiProxy sakaiProxy) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		parser = factory.newSAXParser();
		
		this.connection = connection;
		this.sakaiProxy = sakaiProxy;
	}
	
	public void populatePost(String xml,Post post) throws Exception
	{
		this.post = post;
		parser.parse(new InputSource(new StringReader(xml)), this);
	}
	
	public void startElement(String uri,String localName,String qName,Attributes attrs)
	{
		if("post".equals(qName))
			collectedText = "";
		else if("title".equals(qName))
			processingTitle = true;
		else if("shortText".equals(qName))
			processingShortText = true;
		else if("date".equals(qName))
			processingDate = true;
		else if("keyword".equals(qName))
			processingKeyword = true;
		else if("idCreator".equals(qName))
			processingCreator = true;
		else if("visibility".equals(qName))
			processingVisibility = true;
		else if("allowComments".equals(qName))
			processingAllowComments = true;
		else if("paragraph".equals(qName))
			processingParagraph = true;
		else if("comment".equals(qName))
			comment = new Comment();
		else if("commentText".equals(qName))
			processingCommentText = true;
		else if("commentDate".equals(qName))
			processingCommentDate = true;
		else if("idCommentCreator".equals(qName))
			processingCommentCreator = true;
		else if("imageId".equals(qName))
			processingImageId = true;
		else if("fileId".equals(qName))
			processingFileId = true;
		else if("fileDescription".equals(qName))
			processingFileDescription = true;
		else if("linkRuleDescription".equals(qName))
			processingLinkRuleDescription = true;
		else if("linkExpression".equals(qName))
			processingLinkExpression = true;
	}
	
	public void endElement(String uri,String localName,String qName)
	{
		if("post".equals(qName))
			post.setContent(collectedText);
		else if("title".equals(qName))
			processingTitle = false;
		else if("shortText".equals(qName))
			processingShortText = false;
		else if("date".equals(qName))
			processingDate = false;
		else if("keyword".equals(qName))
			processingKeyword = false;
		else if("idCreator".equals(qName))
			processingCreator = false;
		else if("visibility".equals(qName))
			processingVisibility = false;
		else if("allowComments".equals(qName))
			processingAllowComments = false;
		else if("paragraph".equals(qName))
			processingParagraph = false;
		else if("comment".equals(qName))
			post.addComment(comment);
		else if("commentText".equals(qName))
			processingCommentText = false;
		else if("commentDate".equals(qName))
			processingCommentDate = false;
		else if("idCommentCreator".equals(qName))
			processingCommentCreator = false;
		else if("imageId".equals(qName))
			processingImageId = false;
		else if("fileId".equals(qName))
			processingFileId = false;
		else if("fileDescription".equals(qName))
			processingFileDescription = false;
		else if("file".equals(qName))
		{
			Statement st = null;
			
			try
			{
				st = connection.createStatement();
				ResultSet rs = st.executeQuery("SELECT * FROM BLOGGER_FILE WHERE FILE_ID = '" + currentFileId + "'");
				if(rs.next())
				{
					byte[] blob = rs.getBytes("FILE_CONTENT");
					String resourceId = sakaiProxy.storeResource(blob,post.getSiteId(),post.getCreatorId());
					if(resourceId != null)
					{
						String fullUrl = sakaiProxy.getServerUrl() + "/access/content" + resourceId;
						String link = "<a href=\"" + fullUrl + "\">" + currentFileName + "</a><br /><br />";
						collectedText += link;
					}
				}
				
				rs.close();
			}
			catch(Exception e)
			{
				logger.error("Caught exception whilst storing file.",e);
			}
			finally
			{
				if(st != null)
				{
					try
					{
						st.close();
					}
					catch(Exception e) {}
				}
			}
			
		}
		else if("linkRuleDescription".equals(qName))
			processingLinkRuleDescription = false;
		else if("linkExpression".equals(qName))
			processingLinkExpression = false;
		else if("linkRule".equals(qName))
		{
			String link = "<a href=\"" + currentLinkExpression + "\">" + currentLinkRuleDescription + "</a><br /><br />";
			collectedText += link;
		}
	}
	
	public void characters(char[] ch,int start,int length)
	{
		String data = new String(ch,start,length);
		if(processingTitle)
			post.setTitle(data);
		else if(processingShortText)
			collectedText += "<i>" + data + "</i><br /><br />";
		else if(processingDate)
		{
			long date = Long.parseLong(data);
			post.setCreatedDate(date);
			post.setModifiedDate(date);
		}
		else if(processingKeyword)
			post.addKeyword(data);
		else if(processingCreator)
		{
			logger.error("Creator ID: " + data);
			post.setCreatorId(data);
		}
		else if(processingVisibility)
		{
			int vis = Integer.parseInt(data);
			switch(vis)
			{
				case 0:
				{
					post.setVisibility("PRIVATE");
					break;
				}
				case 1:
				{
					post.setVisibility("READY");
					break;
				}
				case 2:
				{
					post.setVisibility("PUBLIC");
					break;
				}
				case 3:
				{
					post.setVisibility("TUTOR");
					break;
				}
			}
		}
		else if(processingAllowComments)
			post.setCommentable(Boolean.parseBoolean(data));
		else if(processingParagraph)
			collectedText += data;
		else if(processingCommentText)
			comment.setContent(data);
		else if(processingCommentDate)
		{
			long date = Long.parseLong(data);
			comment.setCreatedDate(date);
			comment.setModifiedDate(date);
		}
		else if(processingCommentCreator)
			comment.setCreatorId(data);
		else if(processingImageId)
		{
			Statement st = null;
			
			try
			{
				st = connection.createStatement();
				ResultSet rs = st.executeQuery("SELECT * FROM BLOGGER_IMAGE WHERE IMAGE_ID = '" + data + "'");
				if(rs.next())
				{
					byte[] blob = rs.getBytes("IMAGE_CONTENT");
					String resourceId = sakaiProxy.storeResource(blob,post.getSiteId(),post.getCreatorId());
					if(resourceId != null)
					{
						String fullUrl = sakaiProxy.getServerUrl() + "/access/content" + resourceId;
						String img = "<img src=\"" + fullUrl + "\"/><br /><br />";
						collectedText += img;
					}
				}
				
				rs.close();
			}
			catch(Exception e)
			{
				logger.error("Caught exception whilst storing image.",e);
			}
			finally
			{
				if(st != null)
				{
					try
					{
						st.close();
					}
					catch(Exception e) {}
				}
			}
			
		}
		else if(processingFileId)
			currentFileId = data;
		else if(processingFileDescription)
			currentFileName = data;
		else if(processingLinkRuleDescription)
			currentLinkRuleDescription = data;
		else if(processingLinkExpression)
			currentLinkExpression = data;
	}
}
