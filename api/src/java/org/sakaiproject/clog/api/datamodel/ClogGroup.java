package org.sakaiproject.clog.api.datamodel;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClogGroup {

    private String id;
    private String title;
    private int totalPosts;
    private long lastPostDate;
}
