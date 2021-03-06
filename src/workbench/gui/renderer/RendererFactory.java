/*
 * RendererFactory.java
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
package workbench.gui.renderer;

import java.lang.reflect.Constructor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import workbench.log.LogMgr;

/**
 * A factory for TableCellRenderers.
 * <br/>
 * Classes are created using  Class.forName() to avoid unnecessary class loading during startup.
 * This is used from within WbTable
 *
 * @see workbench.gui.components.WbTable#initDefaultRenderers()
 *
 * @author Thomas Kellerer
 */
public class RendererFactory
{

	private static TableCellRenderer createRenderer(String className)
	{
		TableCellRenderer rend = null;
		try
		{
			Class cls = Class.forName(className);
			rend = (TableCellRenderer)cls.newInstance();
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.createRenderer()", "Error creating renderer", e);
			rend = new DefaultTableCellRenderer();
		}
		return rend;
	}

	public static TableCellRenderer getDateRenderer(String format)
	{
		TableCellRenderer rend = null;
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.DateColumnRenderer");
			Class[] types = new Class[] { String.class };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { format };
			rend = (TableCellRenderer)cons.newInstance(args);
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getDateRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
		return rend;
	}

	public static TableCellRenderer getSqlTypeRenderer()
	{
		return createRenderer("workbench.gui.renderer.SqlTypeRenderer");
	}

}
