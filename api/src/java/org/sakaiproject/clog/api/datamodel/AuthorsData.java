package org.sakaiproject.clog.api.datamodel;

import java.util.List;

import org.sakaiproject.clog.api.ClogMember;

import lombok.Getter;
import lombok.Setter;

public class AuthorsData {

    public int authorsTotal;
    public List<ClogMember> authors;
    public String status = "MORE";
    @Getter @Setter
    public String siteId = "";
}
