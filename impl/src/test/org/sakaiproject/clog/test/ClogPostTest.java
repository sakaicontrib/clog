package org.sakaiproject.clog.test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;

import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Visibilities;

public class ClogPostTest extends TestCase {
	
	public void testSetId() {
		
		Post post = new Post();
		String id = UUID.randomUUID().toString();
		post.setId(id);
		assertEquals("The id returned did not match the one set",id,post.getId());
	}
	
	public void testTitle() {
		
		Post post = new Post();
		String title = "Space Monkeys";
		post.setTitle(title);
		assertEquals("The title returned did not match the one set",title,post.getTitle());
	}
	
	public void testSetCreatorId() {
		
		Post post = new Post();
		String creatorId = UUID.randomUUID().toString();
		post.setCreatorId(creatorId);
		assertEquals("The creator id returned did not match the one set.",creatorId,post.getCreatorId());
	}
	
	public void testKeywords() {
		
		String keywordsTextTest = "java,python,scala";
		List<String> keywordsListTest = Arrays.asList("java","python","scala");
		
		// First test that individual keyword addition is returned ok
		Post post = new Post();
		post.addKeyword("java");
		post.addKeyword("python");
		post.addKeyword("scala");
		assertEquals("Keywords not concatenated correctly.",keywordsTextTest,post.getKeywordsText());
		
		// Now test that setting the keyword csv returns ok
		post = new Post();
		post.setKeywordsText(keywordsTextTest);
		assertEquals("CSV keywords not returned as list correctly",post.getKeywords(),keywordsListTest);
		
		// Now test that setting the keywords list returns ok
		post = new Post();
		post.setKeywords(keywordsListTest);
		assertEquals("List keywords not returned as CSV correctly",keywordsTextTest,post.getKeywordsText());
	}
	
	public void testComments() {
		
		Comment comment1 = new Comment("Nice one!");
		Comment comment2 = new Comment("Could do better.");
		Comment comment3 = new Comment("See me (and wear something nice).");
		
		List<Comment> commentsTest = Arrays.asList(comment1,comment2,comment3);
		
		// First test that list comment setting is returned ok
		Post post = new Post();
		post.setComments(commentsTest);
		assertEquals("Comments not returned correctly.",post.getComments(),commentsTest);
		
		// Now test that individual comment setting is returned ok
		post = new Post();
		post.addComment(comment1);
		post.addComment(comment2);
		post.addComment(comment3);
		assertEquals("Comments not returned correctly.",post.getComments(),commentsTest);
	}
	
	public void testSetVisibility() {
		
		Post post = new Post();
		
		post.setVisibility(Visibilities.PRIVATE);
		assertTrue(post.isPrivate());
		
		post.setVisibility(Visibilities.PUBLIC);
		assertTrue(post.isPublic());
		
		post.setVisibility(Visibilities.RECYCLED);
		assertTrue(post.isRecycled());
		
		post.setVisibility(Visibilities.TUTOR);
		assertTrue(post.isVisibleToTutors());
		
		post.setVisibility(Visibilities.SITE);
		assertTrue(post.isVisibleToSite());
		
		post.setVisibility(Visibilities.AUTOSAVE);
		assertTrue(post.isAutoSave());
	}
	
	public void testDates() {
		
		Post post = new Post();
		
		Date createdDate = new Date();
		post.setCreatedDate(createdDate.getTime());
		assertEquals("Created date not returned correctly.",post.getCreatedDate(),createdDate.getTime());
		
		Date modifiedDate = new Date();
		post.setModifiedDate(modifiedDate.getTime());
		assertEquals("Modified date not returned correctly.",post.getModifiedDate(),modifiedDate.getTime());
	}
}
