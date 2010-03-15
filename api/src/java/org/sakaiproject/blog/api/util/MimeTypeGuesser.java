package org.sakaiproject.blog.api.util;

public class MimeTypeGuesser
{
	public static String guessMimeType(String fileName)
	{
		// GUESS !!!
		if(fileName.endsWith(".doc"))
			return "application/vnd.ms-word";
		else if(fileName.endsWith(".rtf"))
			return "text/richtext";
		else if(fileName.endsWith(".xls"))
			return "application/vnd.ms-excel";
		else if(fileName.endsWith(".pdf"))
			return "application/pdf";
		else if(fileName.endsWith(".ppt"))
			return "application/mspowerpoint";
		else if(fileName.endsWith(".txt"))
			return "text/plain";
		else if(fileName.endsWith(".mp3"))
			return "audio/mpeg";
		
		return "unknown";
	}

}
