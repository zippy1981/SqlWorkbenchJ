/*
 * WbGenDelete.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DeleteScriptGenerator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.ColumnData;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 * A SqlCommand to create a script of DELETE statement to delete specific rows from a table respecting FK constraints.
 *
 * @author Thomas Kellerer
 */
public class WbGenDelete
	extends SqlCommand
{
	public static final String VERB = "WBGENERATEDELETE";

	public static final String PARAM_TABLE = "table";
	public static final String PARAM_FILE = "outputFile";
	public static final String PARAM_COLUMN_VAL = "columnValue";
	public static final String PARAM_DO_FORMAT = "formatSql";
	public static final String PARAM_INCLUDE_COMMIT = "includeCommit";
	public static final String PARAM_APPEND = "appendFile";

	private DeleteScriptGenerator generator;

	public WbGenDelete()
	{
		super();
		this.isUpdatingCommand = true;
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_FILE, ArgumentType.StringArgument);
		cmdLine.addArgument(PARAM_DO_FORMAT, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_TABLE, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_COLUMN_VAL, ArgumentType.Repeatable);
		cmdLine.addArgument(PARAM_INCLUDE_COMMIT, ArgumentType.BoolSwitch);
		cmdLine.addArgument(PARAM_APPEND, ArgumentType.BoolSwitch);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String args = getCommandLine(sql);
		cmdLine.parse(args);

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrGenDeleteWrongParam"));
			result.setFailure();
			return result;
		}

		if (!cmdLine.hasArguments())
		{
			result.addMessage(ResourceMgr.getString("ErrGenDropWrongParam"));
			result.setFailure();
			return result;
		}

		String tname = cmdLine.getValue(PARAM_TABLE);
		TableIdentifier table = currentConnection.getMetadata().findTable(new TableIdentifier(tname));

		if (table == null)
		{
			result.addMessage(ResourceMgr.getFormattedString("ErrTableNotFound", tname));
			result.setFailure();
			return result;
		}

		List<String> cols = cmdLine.getList(PARAM_COLUMN_VAL);
		List<ColumnData> values = new ArrayList<ColumnData>();
		for (String def : cols)
		{
			String[] pair = def.split("=");
			if (pair.length == 2)
			{
				String column = pair[0];
				String value = pair[1];
				ColumnData data = new ColumnData(value, new ColumnIdentifier(column,ColumnIdentifier.NO_TYPE_INFO));
				values.add(data);
			}
			else
			{
				result.addMessage("Illegal column specification: " + def);
				result.setFailure();
				return result;
			}
		}

		generator = new DeleteScriptGenerator(this.currentConnection);
		generator.setTable(table);
		generator.setFormatSql(cmdLine.getBoolean(PARAM_DO_FORMAT, true));
		CharSequence script = generator.getScriptForValues(values);

		WbFile output = evaluateFileArgument(cmdLine.getValue(PARAM_FILE));
		if (output != null)
		{
			boolean append = cmdLine.getBoolean(PARAM_APPEND, false);
			try
			{
				FileUtil.writeString(output, script.toString(), append);
				if (cmdLine.getBoolean(PARAM_INCLUDE_COMMIT))
				{
					FileUtil.writeString(output, "\ncommit;\n", true);
				}
				result.addMessage(ResourceMgr.getFormattedString("MsgScriptWritten", output.getFullPath()));
				result.setSuccess();
			}
			catch (IOException io)
			{
				result.addMessageByKey("ErrFileCreate");
				result.addMessage(ExceptionUtil.getDisplay(io));
				result.setFailure();
			}
		}
		else
		{
			result.addMessage(script);
			result.setSuccess();
		}
		return result;
	}


	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (generator != null)
		{
			generator.cancel();
		}
	}

	@Override
	public void done()
	{
		super.done();
		generator = null;
	}

	@Override
	public boolean isUpdatingCommand(WbConnection con, String sql)
	{
		return false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

}