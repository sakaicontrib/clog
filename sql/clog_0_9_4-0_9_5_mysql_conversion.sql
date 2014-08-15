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
