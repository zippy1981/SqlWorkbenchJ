/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.parser;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public enum ParserType
{
	Standard,
	Postgres,
	SqlServer,
	Oracle,
	MySQL;

	public static ParserType getTypeFromConnection(WbConnection conn)
	{
		if (conn == null) return Standard;
		return getTypeFromDBID(conn.getDbId());
	}

	public static ParserType getTypeFromDBID(String dbid)
	{
		if (dbid == null) return Standard;

		// This will properly handle Postgres' dollar quoting
		if (DbMetadata.DBID_PG.equals(dbid)) return Postgres;
		if ("vertica_database".equals(dbid)) return Postgres;

		// This will allow mixing the standard delimiter with the alternate delimiter
		if (DbMetadata.DBID_ORA.equals(dbid)) return Oracle;

		// This will properly deal with the stupid [..] "quotes" in T-SQL
		if (DbMetadata.DBID_MS.equals(dbid)) return SqlServer;
		if ("adaptive_server_enterprise".equals(dbid)) return SqlServer;
		if ("excel".equals(dbid)) return SqlServer;

		// This will use a different lexer that supports MySQL's stupid backticks
		// and non-standard line comments
		if (DbMetadata.DBID_MYSQL.equals(dbid)) return MySQL;

		// SQLite also allows these stupid [...] quoting style
		// As currently this is the only thing that makes the Lexer for SQL server
		// different from the standard Lexer I'm using it for SQLite as well.
		if ("sqlite".equals(dbid)) return SqlServer;

		String name = Settings.getInstance().getProperty("workbench.db." + dbid + ".parsertype", null);

		if (name != null)
		{
			try
			{
				return ParserType.valueOf(name);
			}
			catch (Throwable th)
			{
				// ignore
			}
		}

		return Standard;
	}

}
