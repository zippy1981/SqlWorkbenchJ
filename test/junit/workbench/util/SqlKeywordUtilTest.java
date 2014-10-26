/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
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

import workbench.sql.ParserType;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlKeywordUtilTest
{

	public SqlKeywordUtilTest()
	{
	}

	@Test
	public void testGetFromPart()
	{
		String sql =
			"with some_data as (\n" +
			"  select foo,\n" +
			"         bar \n" +
			"  from foobar f \n" +
			"  where f.id = 42\n" +
			")\n" +
			"select foo, \n" +
			"       count(*) as hit_count \n" +
			"from some_data d\n" +
			"group by d.foo\n" +
			"order by 2 desc";

		SqlParsingUtil util = new SqlParsingUtil(ParserType.Standard);
		int pos = util.getFromPosition(sql);
		int fromPos = sql.indexOf("from some_data d");
		assertEquals(fromPos, pos);

		String from = util.getFromPart(sql);
		assertEquals("some_data d", from.trim());

		sql = "select a.id, b.pid from foo a join bar b where a.id > 42;";

		from = util.getFromPart(sql);
		assertEquals("foo a join bar b", from.trim());
	}


}