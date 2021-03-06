/*
 * MacroGroupTest.java
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
package workbench.sql.macros;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroGroupTest
{

	@Test
	public void testCreateCopy()
	{
		MacroGroup group = new MacroGroup("Default Group");
		group.setSortOrder(2);
    group.setTooltip("Stuff");
		group.setVisibleInMenu(false);
		group.setVisibleInPopup(false);

		group.addMacro(new MacroDefinition("one", "test one"));

		MacroGroup copy = group.createCopy();
		assertFalse(copy.isModified());
		assertFalse(copy.isVisibleInMenu());
		assertFalse(copy.isVisibleInPopup());
		assertEquals(2, copy.getSortOrder());
		assertEquals(1, copy.getSize());
    assertEquals("Stuff", copy.getTooltip());

		group = new MacroGroup("Another Group");
    group.resetModified();
    group.setTooltip("Foo");
    assertTrue(group.isModified());
	}
}
