package org.sakaiproject.clog.api.datamodel;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class GroupData {

    private String id = "";
    private String title = "";
    private int numberOfPosts = 0;
    private long dateOfLastPost = -1L;
    private long dateOfLastComment = -1L;
}
