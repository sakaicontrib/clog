CREATE TABLE CLOG_POST (
                POST_ID CHAR(36) NOT NULL,
                SITE_ID VARCHAR(255),
                TITLE VARCHAR(255) NOT NULL,
                CONTENT MEDIUMTEXT NOT NULL,
                CREATED_DATE DATETIME NOT NULL,
                MODIFIED_DATE DATETIME,
                CREATOR_ID VARCHAR(255) NOT NULL,
                KEYWORDS VARCHAR(255),
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
                KEYWORDS VARCHAR(255),
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

CREATE TABLE CLOG_PREFERENCES (
                USER_ID VARCHAR (36) NOT NULL, 
                SITE_ID VARCHAR (255) NOT NULL, 
                EMAIL_FREQUENCY VARCHAR(32) NOT NULL,
                CONSTRAINT clog_preferences_pk PRIMARY KEY (USER_ID,SITE_ID));

CREATE TABLE CLOG_GLOBAL_PREFERENCES (
                USER_ID VARCHAR(36) NOT NULL, 
                SHOW_BODY TINYINT(1) NOT NULL DEFAULT '1',
                CONSTRAINT clog_global_preferences_pk PRIMARY KEY (USER_ID));
