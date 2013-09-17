/*
 * DdlObjectInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.util.Set;

import workbench.log.LogMgr;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

import static workbench.util.SqlUtil.getKnownTypes;
import static workbench.util.SqlUtil.getTypesWithoutNames;
import static workbench.util.SqlUtil.removeObjectQuotes;

/**
 *
 * @author Thomas Kellerer
 */
public class DdlObjectInfo
{
	private String objectType;
	private String objectName;

	public DdlObjectInfo(CharSequence sql)
	{
		parseSQL(sql);
	}

	@Override
	public String toString()
	{
		return "Type: " + objectType + ", name: " + objectName;
	}

	public String getDisplayType()
	{
		return StringUtil.capitalize(objectType);
	}

	public boolean isValid()
	{
		return objectType != null && objectName != null;
	}

	public String getObjectType()
	{
		return objectType;
	}

	public String getObjectName()
	{
		return objectName;
	}

	private void parseSQL(CharSequence sql)
	{
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken t = lexer.getNextToken(false, false);

		if (t == null) return;
		String verb = t.getContents();
		Set<String> verbs = CollectionUtil.caseInsensitiveSet("DROP", "RECREATE", "ALTER", "ANALYZE");

		if (!verb.startsWith("CREATE") && !verbs.contains(verb)) return;

		try
		{
			boolean typeFound = false;
			SQLToken token = lexer.getNextToken(false, false);
			while (token != null)
			{
				String c = token.getContents();
				if (getKnownTypes().contains(c))
				{
					typeFound = true;
					this.objectType = c.toUpperCase();
					break;
				}
				token = lexer.getNextToken(false, false);
			}

			if (!typeFound) return;

			// if a type was found we assume the next keyword is the name
			if (!getTypesWithoutNames().contains(this.objectType))
			{
				SQLToken name = lexer.getNextToken(false, false);
				if (name == null) return;
				String content = name.getContents();
				if (content.equals("IF NOT EXISTS") || content.equals("IF EXISTS") || content.equals("#"))
				{
					name = lexer.getNextToken(false, false);
					if (name == null) return;

					if (name.getContents().equals("#"))
					{
						// SQL Server temporary tables using ##
						content = "##";
						name = lexer.getNextToken(false, false);
						if (name == null) return;
					}
				}

				SQLToken next = lexer.getNextToken(false, false);
				if (next != null && next.getContents().equals("."))
				{
					next = lexer.getNextToken();
					if (next != null) name = next;
				}

				this.objectName = name.getContents();

				if (content.startsWith("#"))
				{
					this.objectName = content + this.objectName;
				}

				if (this.objectName != null)
				{
					this.objectName = removeObjectQuotes(this.objectName);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DdlObjectInfo.parseSQL()", "Error finding object info", e);
			this.objectName = null;
			this.objectType = null;
		}
	}
}