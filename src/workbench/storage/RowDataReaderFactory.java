/*
 * RowDataReaderFactory.java
 *
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
package workbench.storage;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class RowDataReaderFactory
{
	public static RowDataReader createReader(ResultInfo info, WbConnection conn)
	{
		if (conn != null  && conn.getMetadata().isOracle() && Settings.getInstance().getBoolProperty("workbench.db.oracle.fix.timstamptz", true))
		{
			try
			{
				return new OracleRowDataReader(info, conn);
			}
			catch (ClassNotFoundException cnf)
			{
				LogMgr.logError("RowDataReaderFactory.createReader()", "Could not instantiate OracleRowDataReader", cnf);
				// disable the usage of the OracleRowDataReader for now, to avoid unnecessary further attempts
				System.setProperty("workbench.db.oracle.fix.timstamptz", "false");
			}
		}
		return new RowDataReader(info, conn);
	}
}
