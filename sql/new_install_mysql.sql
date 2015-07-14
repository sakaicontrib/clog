CREATE TABLE CLOG_POST (
                POST_ID CHAR(36) NOT NULL,
                SITE_ID VARCHAR(255),
                TITLE VARCHAR(255) NOT NULL,
                CONTENT MEDIUMTEXT NOT NULL,
                CREATED_DATE DATETIME NOT NULL,
                MODIFIED_DATE DATETIME,
                CREATOR_ID VARCHAR(255) NOT NULL,
                ALLOW_COMMENTS INT,
                VISIBILITY VARCHAR(16) NOT NULL,
                CONSTRAINT clog_post_pk PRIMARY KEY(POST_ID)
                );

CREATE TABLE CLOG_AUTOSAVED_POST (
                POST_ID CHAR(36) NOT NULL,
                SITE_ID VARCHAR(255),
                TITLE VARCHAR(255) NOT NULL,
                CONTENT MEDIUMTEXT NOT NULL,
                CREATED_DATE DATETIME NOT NULL,
                MODIFIED_DATE DATETIME,
                CREATOR_ID VARCHAR(255) NOT NULL,
                ALLOW_COMMENTS INT,
                VISIBILITY VARCHAR(16) NOT NULL,
                CONSTRAINT clog_autosaved_post_pk PRIMARY KEY(POST_ID)
                );

CREATE TABLE CLOG_COMMENT (
                COMMENT_ID CHAR(36) NOT NULL,
                POST_ID CHAR(36) NOT NULL,
                SITE_ID VARCHAR(255) NOT NULL,
                CREATOR_ID CHAR(36) NOT NULL,
                CREATED_DATE DATETIME NOT NULL,
                MODIFIED_DATE DATETIME NOT NULL,
                CONTENT MEDIUMTEXT NOT NULL,
                CONSTRAINT clog_comment_pk PRIMARY KEY (COMMENT_ID));

CREATE TABLE CLOG_AUTHOR (
                USER_ID CHAR(36) NOT NULL,
                SITE_ID VARCHAR(255) NOT NULL,
                TOTAL_POSTS INT NOT NULL,
                LAST_POST_DATE DATETIME,
                TOTAL_COMMENTS INT NOT NULL,
                CONSTRAINT clog_author_pk PRIMARY KEY (USER_ID,SITE_ID));

CREATE TABLE CLOG_POST_GROUP (
                POST_ID CHAR(36) NOT NULL,
                GROUP_ID VARCHAR(99) NOT NULL,
                MODIFIED_DATE DATETIME NOT NULL,
                CONSTRAINT clog_post_group_pk PRIMARY KEY (POST_ID,GROUP_ID));

CREATE TABLE CLOG_GROUP_DATA (
                GROUP_ID VARCHAR(99) NOT NULL,
                TOTAL_POSTS INT NOT NULL,
                LAST_POST_DATE DATETIME,
                CONSTRAINT clog_group_data_pk PRIMARY KEY (GROUP_ID));
