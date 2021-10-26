package org.sakaiproject.clog.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.clog.api.ClogFunctions;
import org.sakaiproject.clog.api.ClogManager;
import org.sakaiproject.clog.api.ClogMember;
import org.sakaiproject.clog.api.ClogSecurityManager;
import org.sakaiproject.clog.api.QueryBean;
import org.sakaiproject.clog.api.SakaiProxy;
import org.sakaiproject.clog.api.XmlDefs;
import org.sakaiproject.clog.api.datamodel.ClogGroup;
import org.sakaiproject.clog.api.datamodel.Comment;
import org.sakaiproject.clog.api.datamodel.Post;
import org.sakaiproject.clog.api.datamodel.Visibilities;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.memory.api.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Slf4j
public class ClogManagerImpl implements ClogManager {

    private PersistenceManager persistenceManager;

    @Setter
    private ClogSecurityManager clogSecurityManager;

    @Setter
    private SakaiProxy sakaiProxy;

    @Autowired
    private ServerConfigurationService serverConfigurationService;

    public void init() {
        
        log.debug("init()");

        log.info("Registering Clog functions ...");

        sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_CREATE);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_READ_ANY);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_UPDATE_ANY);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_UPDATE_OWN);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_DELETE_ANY);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_POST_DELETE_OWN);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_CREATE);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_READ_ANY);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_READ_OWN);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_UPDATE_ANY);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_UPDATE_OWN);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_DELETE_ANY);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_COMMENT_DELETE_OWN);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_MODIFY_PERMISSIONS);
        sakaiProxy.registerFunction(ClogFunctions.CLOG_TUTOR);

        log.info("Registered Clog functions ...");

        sakaiProxy.registerEntityProducer(this);

        persistenceManager = new PersistenceManager(sakaiProxy);

        if (serverConfigurationService.getBoolean("clog.importFromBlogWow", false)) {
            importBlogWowData();
        }
    }

    public Post getPost(String postId) throws Exception {

        log.debug("getPost({})", postId);

        Post post = persistenceManager.getPost(postId);
        if (clogSecurityManager.canCurrentUserReadPost(post))
            return post;
        else
            throw new Exception("The current user does not have permissions to read this post.");
    }

    public Comment getComment(String commentId) throws Exception {

        log.debug("getComment({})", commentId);

        Comment comment = persistenceManager.getComment(commentId);
        // if (securityManager.canCurrentUserReadPost(post))
        return comment;
        // else
        // throw new
        // Exception("The current user does not have permissions to read this post.");
    }

    public Post getPostHeader(String postId) throws Exception {

        log.debug("getUnfilteredPost({})", postId);

        Post post = persistenceManager.getPost(postId);
        post.setContent("");
        return post;
    }

    public List<Post> getPosts(String siteId) throws Exception {

        // Get all the posts for the supplied site and filter them through the
        // security manager
        List<Post> unfiltered = persistenceManager.getAllPost(siteId);
        return clogSecurityManager.filter(unfiltered, siteId);
    }

    public List<Post> getPosts(QueryBean query) throws Exception {
        return getPosts(query, true);
    }

    private List<Post> getPosts(QueryBean query, boolean filter) throws Exception {

        Cache cache = sakaiProxy.getCache(POST_CACHE);
        if (query.byPublic()) {
            if (cache.get(Visibilities.PUBLIC) == null) {
                cache.put(Visibilities.PUBLIC, persistenceManager.getPosts(query));
            }
            return (List<Post>) cache.get(Visibilities.PUBLIC);
        } else if (query.queryBySiteId()) {
            if (query.getVisibilities().contains(Visibilities.RECYCLED)) {
                if (filter) {
                    return clogSecurityManager.filter(persistenceManager.getPosts(query), query.getSiteId());
                } else {
                    return persistenceManager.getPosts(query);
                }
            } else {
                String siteId = query.getSiteId();

                if (cache.get(siteId) == null) {
                    log.debug("Cache miss on site id: {}", siteId);
                    cache.put(siteId, new HashMap<String, List<Post>>());
                } else {
                    log.debug("Cache hit on site id: {}", siteId);
                }

                Map<String, List<Post>> siteMap = (Map<String, List<Post>>) cache.get(siteId);

                String key = ALL;

                if (query.queryByCreator()) {
                    key = query.getCreator();
                } else if (query.queryByGroup()) {
                    key = query.getGroup();
                }

                log.debug("KEY: {}", key);

                if (siteMap != null && !siteMap.containsKey(key)) {
                    log.debug("Cache miss on '{}'. It will be added.", key);
                    siteMap.put(key, persistenceManager.getPosts(query));
                } else {
                    log.debug("Cache hit on '{}'", key);
                }
                if (siteMap != null) {
                    if (filter) {
                        return clogSecurityManager.filter((List<Post>) siteMap.get(key), query.getSiteId());
                    } else {
                        return (List<Post>) siteMap.get(key);
                    }
                } else {
                    if (filter) {
                        return clogSecurityManager.filter(persistenceManager.getPosts(query), null);
                    } else {
                        return persistenceManager.getPosts(query);
                    }
                }
            }
        } else {
            if (filter) {
                return clogSecurityManager.filter(persistenceManager.getPosts(query), null);
            } else {
                return persistenceManager.getPosts(query);
            }
        }
    }

    public boolean savePost(Post post) {
        try {
            if (persistenceManager.savePost(post)) {
                removeSiteFromCaches(post.getSiteId());
                return true;
            } else {
                log.error("Failed to save post");
            }
        } catch (Exception e) {
            log.error("Caught exception whilst saving post", e);
        }

        return false;
    }

    public boolean deletePost(String postId) {
        try {
            Post post = persistenceManager.getPost(postId);
            if (clogSecurityManager.canCurrentUserDeletePost(post))
                if (persistenceManager.deletePost(post)) {
                    // Invalidate all caches for this site
                    removeSiteFromCaches(post.getSiteId());
                    return true;
                }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean saveComment(Comment comment) {
        try {
            if (persistenceManager.saveComment(comment)) {
                removeSiteFromCaches(comment.getSiteId());
                return true;
            }
        } catch (Exception e) {
            log.error("Caught exception whilst saving comment", e);
        }

        return false;
    }

    public boolean deleteComment(String siteId, String commentId) {
        try {
            if (persistenceManager.deleteComment(commentId)) {
                removeSiteFromCaches(siteId);
                return true;
            }
        } catch (Exception e) {
            log.error("Caught exception whilst deleting comment.", e);
        }

        return false;
    }

    public boolean recyclePost(String postId) {
        try {
            Post post = persistenceManager.getPost(postId);

            if (clogSecurityManager.canCurrentUserDeletePost(post)) {
                if (persistenceManager.recyclePost(post)) {
                    post.setVisibility(Visibilities.RECYCLED);
                    removeSiteFromCaches(post.getSiteId());
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Caught an exception whilst recycling post '" + postId + "'");
        }

        return false;
    }

    public void setPersistenceManager(PersistenceManager pm) {
        this.persistenceManager = pm;
    }

    private String serviceName() {
        return ClogManager.class.getName();
    }

    public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments) {

        log.debug("archive(siteId:{},archivePath:{})", siteId, archivePath);

        StringBuilder results = new StringBuilder();

        results.append(getLabel() + ": Started.\n");

        int postCount = 0;

        try {
            // start with an element with our very own (service) name
            Element element = doc.createElement(serviceName());
            element.setAttribute("version", "11.x");
            ((Element) stack.peek()).appendChild(element);
            stack.push(element);

            Element clog = doc.createElement("clog");
            List<Post> posts = getPosts(siteId);
            if (posts != null && posts.size() > 0) {
                for (Post post : posts) {
                    Element postElement = post.toXml(doc, stack);
                    clog.appendChild(postElement);
                    postCount++;
                }
            }

            ((Element) stack.peek()).appendChild(clog);
            stack.push(clog);

            stack.pop();

            results.append(getLabel() + ": Finished. " + postCount + " post(s) archived.\n");
        } catch (Exception any) {
            results.append(getLabel() + ": exception caught. Message: " + any.getMessage());
            log.warn(getLabel() + " exception caught. Message: " + any.getMessage());
        }

        stack.pop();

        return results.toString();
    }

    /**
     * From EntityProducer
     */
    public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport) {

        log.debug("merge(siteId:{},root tagName:{},archivePath:{},fromSiteId:{}", siteId, root.getTagName(), archivePath, fromSiteId);

        StringBuilder results = new StringBuilder();

        int postCount = 0;

        NodeList postNodes = root.getElementsByTagName(XmlDefs.POST);
        final int numberPosts = postNodes.getLength();

        for (int i = 0; i < numberPosts; i++) {
            Node child = postNodes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                log.error("Post nodes should be elements. Skipping ...");
                continue;
            }

            Element postElement = (Element) child;

            Post post = new Post();
            post.fromXml(postElement);
            post.setSiteId(siteId);

            savePost(post);

            for (Comment comment : post.getComments()) {
                comment.setPostId(post.getId());
                comment.setSiteId(post.getSiteId());
                saveComment(comment);
            }

            postCount++;
        }

        results.append("Stored " + postCount + " posts.");

        return results.toString();
    }

    /**
     * From EntityProducer
     */
    public Entity getEntity(Reference ref) {

        log.debug("getEntity(Ref ID:{})", ref.getId());

        Entity rv = null;

        try {
            String reference = ref.getReference();

            String[] parts = reference.split(Entity.SEPARATOR);

            if (parts.length == 5) {
                String postId = parts[4];
                rv = getPost(postId);
            } else if (parts.length == 7) {
                String commentId = parts[6];
                rv = getComment(commentId);
            }
        } catch (Exception e) {
            log.warn("getEntity(): " + e);
        }

        return rv;
    }

    /**
     * From EntityProducer
     */
    public Collection getEntityAuthzGroups(Reference ref, String userId) {

        log.debug("getEntityAuthzGroups(Ref ID:{},userId:{})", ref.getId(), userId);

        List ids = new ArrayList();
        ids.add("/site/" + ref.getContext());
        return ids;
    }

    public String getEntityDescription(Reference arg0) {
        return null;
    }

    public ResourceProperties getEntityResourceProperties(Reference ref) {
        try {
            String reference = ref.getReference();

            int lastIndex = reference.lastIndexOf(Entity.SEPARATOR);
            String postId = reference.substring(lastIndex, reference.length() - lastIndex);
            Entity entity = getPost(postId);
            return entity.getProperties();
        } catch (Exception e) {
            log.warn("getEntity(): " + e);
            return null;
        }
    }

    /**
     * From EntityProducer
     */
    public String getEntityUrl(Reference ref) {
        return getEntity(ref).getUrl();
    }

    /**
     * From EntityProducer
     */
    public HttpAccess getHttpAccess() {
        return null;
    }

    /**
     * From EntityProducer
     */
    public String getLabel() {
        return "clog";
    }

    /**
     * From EntityProducer
     */
    public boolean parseEntityReference(String referenceString, Reference reference) {

        String[] parts = referenceString.split(Entity.SEPARATOR);

        if (parts.length < 2 || !parts[1].equals("clog")) // Leading slash adds
                                                          // an empty element
            return false;

        if (parts.length == 2) {
            reference.set("sakai:clog", "", "", null, "");
            return true;
        }

        String siteId = parts[2];
        String subType = parts[3];
        String entityId = parts[4];

        if ("posts".equals(subType)) {
            reference.set("sakai:clog", "posts", entityId, null, siteId);
            return true;
        }

        return false;
    }

    public boolean willArchiveMerge() {
        return true;
    }

    public String getEntityPrefix() {
        return ClogManager.ENTITY_PREFIX;
    }

    public boolean entityExists(String id) {
        String postId = id.substring(id.lastIndexOf(Entity.SEPARATOR));

        try {
            if (persistenceManager.postExists(postId))
                return true;
        } catch (Exception e) {
            log.error("entityExists threw an exception", e);
        }

        return false;
    }

    public List<ClogMember> getAuthors(String siteId, String sortedBy) {

        Cache cache = sakaiProxy.getCache(AUTHOR_CACHE);

        if ("!gateway".equals(siteId)) {
            if (cache.get("public") == null) {
                log.debug("Cache miss on \"public\". Caching empty map ...");
                cache.put("public", new HashMap<String, List<ClogMember>>());
            }

            Map<String, List<ClogMember>> publicMap = (Map<String, List<ClogMember>>) cache.get("public");
            return getOrCacheAuthors(publicMap, sortedBy, siteId); 
        } else {
            if (cache.get(siteId) == null) {
                log.debug("Cache miss on \"{}\". Caching empty map ...", siteId);
                cache.put(siteId, new HashMap<String, List<ClogMember>>());
            }

            Map<String, List<ClogMember>> siteMap = (Map<String, List<ClogMember>>) cache.get(siteId);
            return getOrCacheAuthors(siteMap, sortedBy, siteId); 
        }
    }

    private List<ClogMember> getOrCacheAuthors(Map<String, List<ClogMember>> map, String sortedBy, String siteId) {

        List<ClogMember> authors = new ArrayList<ClogMember>();

        String sort = (sortedBy == null) ? SORT_NAME_DOWN : sortedBy;

        if (map.containsKey(sort)) {
            log.debug("Cache hit on \"{}\".", sort);
            authors = map.get(sort);
        } else {
            log.debug("Cache miss on \"{}\".", sort);
            authors = (siteId == null) ? persistenceManager.getPublicBloggers() : sakaiProxy.getSiteMembers(siteId);
            for (ClogMember author : authors) {
                persistenceManager.populateAuthorData(author, siteId);
            }

            if (sort.equals(SORT_NAME_DOWN)) {
                Collections.sort(authors, new UserDisplayNameComparator());
            } else if (sort.equals(SORT_NAME_UP)) {
                Collections.sort(authors, new UserDisplayNameComparator());
                Collections.reverse(authors);
            } else if (sort.equals(SORT_POSTS_DOWN)) {
                Collections.sort(authors, new NumberOfPostsComparator());
            } else if (sort.equals(SORT_POSTS_UP)) {
                Collections.sort(authors, new NumberOfPostsComparator());
                Collections.reverse(authors);
            } else if (sort.equals(SORT_LAST_POST_DOWN)) {
                Collections.sort(authors, new DateOfLastPostComparator());
            } else if (sort.equals(SORT_LAST_POST_UP)) {
                Collections.sort(authors, new DateOfLastPostComparator());
                Collections.reverse(authors);
            } else if (sort.equals(SORT_COMMENTS_DOWN)) {
                Collections.sort(authors, new NumberOfCommentsComparator());
            } else if (sort.equals(SORT_COMMENTS_UP)) {
                Collections.sort(authors, new NumberOfCommentsComparator());
                Collections.reverse(authors);
            }

            map.put(sort, authors);
        }

        return authors;
    }

    public boolean restorePost(String postId) {

        try {
            Post post = persistenceManager.getPost(postId);
            if (persistenceManager.restorePost(post)) {
                removeSiteFromCaches(post.getSiteId());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("Caught an exception whilst restoring post '" + postId + "'");
        }

        return false;
    }

    public boolean deleteAutosavedCopy(String postId) {
        return persistenceManager.deleteAutosavedCopy(postId);
    }

    public List<ClogGroup> getSiteGroupsForCurrentUser(String siteId) {

        List<ClogGroup> groups = new ArrayList<ClogGroup>();

        Map<String, String> siteGroups = sakaiProxy.getSiteGroupsForCurrentUser(siteId);
        for (String groupId : siteGroups.keySet()) {
            ClogGroup clogGroup = persistenceManager.getClogGroup(groupId);
            clogGroup.setTitle(siteGroups.get(groupId));
            groups.add(clogGroup);
        }

        return groups;
    }

    private void importBlogWowData() {

        log.debug("Importing BlogWow data ...");

        Connection connection = null;
        Statement postST = null;
        ResultSet postRS = null;
        Statement commentST = null;
        ResultSet commentRS = null;

        int numberImported = 0;

        try {
            connection = sakaiProxy.borrowConnection();

            postST = connection.createStatement();
            commentST = connection.createStatement();
            postRS = postST.executeQuery("SELECT blogwow_entry.*,location FROM blogwow_entry,blogwow_blog WHERE blogwow_entry.blog_id = blogwow_blog.id");
            while (postRS.next()) {
                String id = postRS.getString("id");
                String title = postRS.getString("title");
                String text = postRS.getString("text");
                Date created = postRS.getTimestamp("dateCreated");
                Date modified = postRS.getTimestamp("dateModified");
                String privacySetting = postRS.getString("privacySetting");
                String ownerId = postRS.getString("ownerId");
                String location = postRS.getString("location");

                log.debug("Importing BlogWow post '{}' at location '{}' ...", title, location);

                String siteId = null;
                if (location.startsWith("/site/")) {
                    siteId = location.substring(location.lastIndexOf("/") + 1);
                } else {
                    log.debug("location {} does not represent a site. Skipping post ...", siteId);
                    continue;
                }

                QueryBean query = new QueryBean();
                query.setSiteId(siteId);
                query.setTitle(title);

                if (getPosts(query, false).size() > 0) {
                    // Already imported. Skip it.
                    continue;
                }

                Post post = new Post();
                post.setSiteId(siteId);
                post.setTitle(title);
                post.setCreatedDate(created.getTime());
                post.setModifiedDate(modified.getTime());
                post.setCreatorId(ownerId);
                post.setContent(text);

                if ("private".equals(privacySetting)) {
                    post.setVisibility(Visibilities.PRIVATE);
                } else {
                    post.setVisibility(Visibilities.SITE);
                }

                if (savePost(post)) {

                    commentRS = commentST.executeQuery("SELECT * FROM blogwow_comment WHERE entry_id = '" + id + "'");
                    while (commentRS.next()) {
                        String commentText = commentRS.getString("text");
                        Date commentCreated = commentRS.getTimestamp("dateCreated");
                        Date commentModified = commentRS.getTimestamp("dateModified");
                        String commentOwnerId = commentRS.getString("ownerId");

                        Comment comment = new Comment();
                        comment.setCreatorId(commentOwnerId);
                        comment.setCreatedDate(commentCreated.getTime());
                        comment.setModifiedDate(commentModified.getTime());
                        comment.setContent(commentText);
                        comment.setPostId(post.getId());
                        if(siteId != null) {
                            comment.setSiteId(siteId);
                        }

                        saveComment(comment);
                    }
                    commentRS.close();
                    numberImported++;
                }

            }
        } catch (Exception e) {
            log.error("Exception thrown whilst importing blog wow data", e);
        } finally {
            if (commentRS != null) {
                try {
                    commentRS.close();
                } catch (Exception e) {
                }
            }
            if (postRS != null) {
                try {
                    postRS.close();
                } catch (Exception e) {
                }
            }
            if (commentST != null) {
                try {
                    commentST.close();
                } catch (Exception e) {
                }
            }
            if (postST != null) {
                try {
                    postST.close();
                } catch (Exception e) {
                }
            }

            sakaiProxy.returnConnection(connection);
        }

        log.debug("Imported {} BlogWow posts", numberImported);
    }

    private class UserDisplayNameComparator implements Comparator<ClogMember> {

        public int compare(ClogMember o1, ClogMember o2) {
            return o1.getUserDisplayName().compareTo(o2.getUserDisplayName());
        }
    }

    private class NumberOfPostsComparator implements Comparator<ClogMember> {

        public int compare(ClogMember o1, ClogMember o2) {

            int n1 = o1.getNumberOfPosts();
            int n2 = o2.getNumberOfPosts();
            if (n1 < n2) return -1;
            else if (n1 == n2) return 0;
            else return 1;
        }
    }

    private class NumberOfCommentsComparator implements Comparator<ClogMember> {

        public int compare(ClogMember o1, ClogMember o2) {

            int n1 = o1.getNumberOfComments();
            int n2 = o2.getNumberOfComments();
            if (n1 < n2) return -1;
            else if (n1 == n2) return 0;
            else return 1;
        }
    }

    private class DateOfLastPostComparator implements Comparator<ClogMember> {

        public int compare(ClogMember o1, ClogMember o2) {

            long n1 = o1.getDateOfLastPost();
            long n2 = o2.getDateOfLastPost();
            if (n1 < n2) return -1;
            else if (n1 == n2) return 0;
            else return 1;
        }
    }

    private class DateOfLastCommentComparator implements Comparator<ClogMember> {

        public int compare(ClogMember o1, ClogMember o2) {

            long n1 = o1.getDateOfLastComment();
            long n2 = o2.getDateOfLastComment();
            if (n1 < n2) return -1;
            else if (n1 == n2) return 0;
            else return 1;
        }
    }

    private void removeSiteFromCaches(String siteId) {

        Cache postCache = sakaiProxy.getCache(POST_CACHE);
        postCache.remove(siteId);
        Cache authorCache = sakaiProxy.getCache(AUTHOR_CACHE);
        authorCache.remove(siteId);
    }
}
