/*
 * ObjectInfoTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import workbench.WbTestCase;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectInfoTest
	extends WbTestCase
{

	private WbConnection db;

	public ObjectInfoTest()
	{
		super("ObjectInfoTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
		TestUtil util = getTestUtil();
		db = util.getConnection();
		TestUtil.executeScript(db,
			"CREATE TABLE person (nr integer primary key, person_name varchar(100)); \n"
			+ "CREATE TABLE person_group (person_nr integer, group_nr integer); \n"
			+ "ALTER TABLE person_group ADD CONSTRAINT fk_pg_p FOREIGN KEY (person_nr) REFERENCES person (nr); \n"
			+ "CREATE VIEW v_person (pnr, pname) AS SELECT nr, person_name FROM PERSON; \n"
			+ "create sequence seq_id; \n"
			+ "commit;");

}

	@Test
	public void testGetFullObjectInfo()
		throws Exception
	{
		String objectName = "person";
		ObjectInfo info = new ObjectInfo();
		Settings.getInstance().setProperty("workbench.db.objectinfo.includefk", true);
		StatementRunnerResult tableInfo = info.getObjectInfo(db, objectName, false,false);
		assertTrue(tableInfo.hasDataStores());
		DataStore ds = tableInfo.getDataStores().get(0);
		assertEquals(2, ds.getRowCount());
		assertEquals("NR", ds.getValueAsString(0, 0));
		assertEquals("PERSON_NAME", ds.getValueAsString(1, 0));

		tableInfo = info.getObjectInfo(db, objectName, true, false);
		assertTrue(tableInfo.hasDataStores());
		assertEquals(3, tableInfo.getDataStores().size());

		DataStore indexes = tableInfo.getDataStores().get(1);
		assertEquals("PERSON - Indexes", indexes.getResultName());
		assertEquals(1, indexes.getRowCount());

		DataStore fk = tableInfo.getDataStores().get(2);
		assertEquals("PERSON - Referenced by", fk.getResultName());
		assertEquals(1, fk.getRowCount());

		tableInfo = info.getObjectInfo(db, "PERSON_GROUP", true, false);
		assertTrue(tableInfo.hasDataStores());
		assertEquals(3, tableInfo.getDataStores().size());

		indexes = tableInfo.getDataStores().get(1);
		assertEquals("PERSON_GROUP - Indexes", indexes.getResultName());
		assertEquals(1, indexes.getRowCount());

		fk = tableInfo.getDataStores().get(2);
		assertEquals("PERSON_GROUP - References", fk.getResultName());
		assertEquals(1, fk.getRowCount());

		StatementRunnerResult viewInfo = info.getObjectInfo(db, "v_person", false, true);
//		System.out.println(viewInfo.getSourceCommand());
		assertTrue(viewInfo.getSourceCommand().startsWith("CREATE FORCE VIEW"));
		assertTrue(viewInfo.hasDataStores());

		DataStore viewDs = viewInfo.getDataStores().get(0);
		assertEquals(2, viewDs.getRowCount());
		assertEquals("PNR", viewDs.getValueAsString(0, 0));
		assertEquals("PNAME", viewDs.getValueAsString(1, 0));

		StatementRunnerResult seqInfo = info.getObjectInfo(db, "seq_id", false, true);
//		System.out.println(seqInfo.getSourceCommand());
		assertTrue(seqInfo.hasDataStores());
		assertEquals(1, seqInfo.getDataStores().get(0).getRowCount());
	}

	@Test
	public void testGetPartialObjectInfo()
		throws Exception
	{
		String objectName = "person";
		ObjectInfo info = new ObjectInfo();
		Settings.getInstance().setProperty("workbench.db.objectinfo.includefk", false);
		StatementRunnerResult tableInfo = info.getObjectInfo(db, objectName, false, true);
		assertTrue(tableInfo.hasDataStores());
		DataStore ds = tableInfo.getDataStores().get(0);
		assertEquals(2, ds.getRowCount());
		assertEquals("NR", ds.getValueAsString(0, 0));
		assertEquals("PERSON_NAME", ds.getValueAsString(1, 0));

		tableInfo = info.getObjectInfo(db, objectName, true, true);
		assertTrue(tableInfo.hasDataStores());
		assertEquals(2, tableInfo.getDataStores().size());

		DataStore indexes = tableInfo.getDataStores().get(1);
		assertEquals("PERSON - Indexes", indexes.getResultName());
		assertEquals(1, indexes.getRowCount());
	}
}
