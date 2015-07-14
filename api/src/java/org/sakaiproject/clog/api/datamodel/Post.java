package org.sakaiproject.clog.api.datamodel;

import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import lombok.Getter;
import lombok.Setter;

import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.XmlDefs;
import org.sakaiproject.clog.api.cover.SakaiProxy;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.util.BaseResourceProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Post implements Entity {

    private static final String CDATA_SUFFIX = "]]>";
    private static final String CDATA_PREFIX = "<![CDATA[";

    @Getter @Setter
    private String id = "";

    @Getter @Setter
    private String title = "";

    @Getter @Setter
    private String content = "";

    @Getter @Setter
    private long createdDate = -1L;

    @Getter @Setter
    private long modifiedDate = -1L;

    private String visibility = Visibilities.PRIVATE;

    @Getter @Setter
    private boolean commentable = true;

    @Getter @Setter
    private String creatorId = null;

    @Getter @Setter
    private String creatorDisplayName = null;

    @Getter
    private List<Comment> comments = new ArrayList<Comment>();

    @Getter @Setter
    private String siteId;

    @Getter @Setter
    private Post autosavedVersion = null;
    
    @Getter
    private int numberOfComments = 0;

    @Getter @Setter
    private List<String> groups = new ArrayList<String>();

    public Post() {

        long now = new Date().getTime();
        createdDate = now;
        modifiedDate = now;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        numberOfComments += 1;
    }
    
    public void setComments(List<Comment> comments) {
        this.comments = comments;
        numberOfComments = comments.size();
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isPrivate() {
        return Visibilities.PRIVATE.equals(visibility);
    }

    public boolean isPublic() {
        return Visibilities.PUBLIC.equals(visibility);
    }

    public boolean isGroup() {
        return Visibilities.GROUP.equals(visibility);
    }

    /**
     * @see org.sakaiproject.entity.api.Entity#getProperties()
     */
    public ResourceProperties getProperties() {
        ResourceProperties rp = new BaseResourceProperties();

        rp.addProperty("id", getId());
        return rp;
    }

    /**
     * @see org.sakaiproject.entity.api.Entity#getReference()
     * 
     * @return
     */
    public String getReference() {
        return ClogManager.REFERENCE_ROOT + Entity.SEPARATOR + siteId + Entity.SEPARATOR + "posts" + Entity.SEPARATOR + id;
    }

    /**
     * @see org.sakaiproject.entity.api.Entity#getReference()
     * 
     * @return
     */
    public String getReference(String rootProperty) {
        return getReference();
    }

    /**
     * @see org.sakaiproject.entity.api.Entity#getUrl()
     */
    public String getUrl() {
        String toolId = SakaiProxy.getClogToolId(siteId);
        return SakaiProxy.getServerUrl() + "/portal/directtool/" + toolId + "?state=post&postId=" + getId();
    }

    /**
     * @see org.sakaiproject.entity.api.Entity#getUrl()
     */
    public String getUrl(String rootProperty) {
        return getUrl();
    }

    /**
     * @see org.sakaiproject.entity.api.Entity#toXml()
     * 
     * @return
     */
    public Element toXml(Document doc, Stack stack) {
        Element postElement = doc.createElement(XmlDefs.POST);

        if (stack.isEmpty()) {
            doc.appendChild(postElement);
        } else {
            ((Element) stack.peek()).appendChild(postElement);
        }

        stack.push(postElement);

        postElement.setAttribute(XmlDefs.COMMENTABLE, ((isCommentable()) ? "true" : "false"));
        postElement.setAttribute(XmlDefs.VISIBILITY, getVisibility());

        Element idElement = doc.createElement(XmlDefs.ID);
        idElement.setTextContent(id);
        postElement.appendChild(idElement);

        Element createdDateElement = doc.createElement(XmlDefs.CREATEDDATE);
        createdDateElement.setTextContent(Long.toString(createdDate));
        postElement.appendChild(createdDateElement);

        Element modifiedDateElement = doc.createElement(XmlDefs.MODIFIEDDATE);
        modifiedDateElement.setTextContent(Long.toString(modifiedDate));
        postElement.appendChild(modifiedDateElement);

        Element creatorIdElement = doc.createElement(XmlDefs.CREATORID);
        creatorIdElement.setTextContent(creatorId);
        postElement.appendChild(creatorIdElement);

        Element titleElement = doc.createElement(XmlDefs.TITLE);
        titleElement.setTextContent(wrapWithCDATA(title));
        postElement.appendChild(titleElement);

        if (comments.size() > 0) {
            Element commentsElement = doc.createElement(XmlDefs.COMMENTS);

            for (Comment comment : comments) {
                Element commentElement = doc.createElement(XmlDefs.COMMENT);
                commentElement.setAttribute(XmlDefs.ID, comment.getId());
                commentElement.setAttribute(XmlDefs.CREATORID, comment.getCreatorId());
                commentElement.setAttribute(XmlDefs.CREATEDDATE, Long.toString(comment.getCreatedDate()));
                commentElement.setAttribute(XmlDefs.MODIFIEDDATE, Long.toString(comment.getModifiedDate()));
                commentElement.setTextContent(wrapWithCDATA(comment.getContent()));

                commentsElement.appendChild(commentElement);
            }

            postElement.appendChild(commentsElement);
        }

        stack.pop();

        return postElement;
    }

    private String wrapWithCDATA(String s) {
        return CDATA_PREFIX + s + CDATA_SUFFIX;
    }

    private String stripCDATA(String s) {
        if (s.startsWith(CDATA_PREFIX) && s.endsWith(CDATA_SUFFIX)) {
            s = s.substring(CDATA_PREFIX.length());
            s = s.substring(0, s.length() - CDATA_SUFFIX.length());
        }

        return s;
    }

    public void fromXml(Element postElement) {
        if (!postElement.getTagName().equals(XmlDefs.POST)) {
            return;
        }

        String commentable = postElement.getAttribute(XmlDefs.COMMENTABLE);
        setCommentable((commentable.equals("true")) ? true : false);

        String visibility = postElement.getAttribute(XmlDefs.VISIBILITY);
        setVisibility(visibility);

        NodeList children = postElement.getElementsByTagName(XmlDefs.CREATORID);
        setCreatorId(children.item(0).getFirstChild().getTextContent());

        children = postElement.getElementsByTagName(XmlDefs.CREATEDDATE);
        setCreatedDate(Long.parseLong(children.item(0).getFirstChild().getTextContent()));

        children = postElement.getElementsByTagName(XmlDefs.MODIFIEDDATE);
        setModifiedDate(Long.parseLong(children.item(0).getFirstChild().getTextContent()));

        children = postElement.getElementsByTagName(XmlDefs.TITLE);
        if (children.getLength() > 0) {
            setTitle(stripCDATA(children.item(0).getFirstChild().getTextContent()));
        }

        children = postElement.getElementsByTagName(XmlDefs.COMMENT);
        int numChildren = children.getLength();
        for (int i = 0; i < numChildren; i++) {
            Element commentElement = (Element) children.item(i);

            String commentCreatorId = commentElement.getAttribute(XmlDefs.CREATORID);
            String commentCreatedDate = commentElement.getAttribute(XmlDefs.CREATEDDATE);
            String commentModifiedDate = commentElement.getAttribute(XmlDefs.MODIFIEDDATE);
            String text = commentElement.getFirstChild().getTextContent();

            Comment comment = new Comment();
            comment.setCreatorId(commentCreatorId);
            comment.setCreatedDate(Long.parseLong(commentCreatedDate));
            comment.setModifiedDate(Long.parseLong(commentModifiedDate));
            comment.setContent(stripCDATA(text));

            addComment(comment);
        }
    }

    public boolean hasComments() {
        return comments.size() > 0;
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
    }

    public boolean isRecycled() {
        return Visibilities.RECYCLED.equals(visibility);
    }

    public boolean isReady() {
        return Visibilities.SITE.equals(visibility) || Visibilities.TUTOR.equals(visibility);
    }

    public boolean isAutoSave() {
        return Visibilities.AUTOSAVE.equals(visibility);
    }

    public boolean isVisibleToSite() {
        return Visibilities.SITE.equals(visibility);
    }

    public boolean isVisibleToTutors() {
        return Visibilities.TUTOR.equals(visibility);
    }
    
    public void minimise() {
        content = "";
        numberOfComments = comments.size();
        comments = new ArrayList<Comment>();
    }
}
