package org.sakaiproject.blog.impl.sql;

public class OracleSQLGenerator extends SQLGenerator
{
	public OracleSQLGenerator()
	{
		BLOB="BLOB";
		BIGINT = "NUMBER";
		TIMESTAMP = "TIMESTAMP";
		VARCHAR = "VARCHAR2";
		TEXT  = "LONG";
	}

}
