/*
 * PostgresTableSourceBuilder.java
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
package workbench.db.postgres;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.DomainIdentifier;
import workbench.db.EnumIdentifier;
import workbench.db.IndexDefinition;
import workbench.db.JdbcUtils;
import workbench.db.ObjectSourceOptions;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTableSourceBuilder
	extends TableSourceBuilder
{

	public PostgresTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

  @Override
	public void readTableOptions(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		ObjectSourceOptions option = table.getSourceOptions();
		if (option.isInitialized()) return;

		PostgresRuleReader ruleReader = new PostgresRuleReader();
		CharSequence rule = ruleReader.getTableRuleSource(dbConnection, table);
		if (rule != null)
		{
			option.setAdditionalSql(rule.toString());
		}

		if ("FOREIGN TABLE".equals(table.getType()))
		{
			readForeignTableOptions(table);
		}
		else
		{
			readTableOptions(table);
		}
		option.setInitialized();
	}

	private void readTableOptions(TableIdentifier tbl)
	{
		ObjectSourceOptions option = tbl.getSourceOptions();
		StringBuilder inherit = readInherits(tbl);

		StringBuilder tableSql = new StringBuilder();

		if (inherit != null)
		{
			if (tableSql.length() > 0) tableSql.append('\n');
			tableSql.append(inherit);
		}

		String optionsCol = null;
		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.1"))
		{
			optionsCol = "array_to_string(ct.reloptions, ', ')";
		}
		else
		{
			optionsCol = "null as reloptions";
		}

		String tempCol = null;
		if (JdbcUtils.hasMinimumServerVersion(dbConnection, "9.1"))
		{
			tempCol = "ct.relpersistence";
		}
		else if (JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4"))
		{
			tempCol = "case when ct.relistemp then 't' else null::char end as relpersitence";
		}
		else
		{
			tempCol = "null::char as relpersistence";
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql =
			"select " + tempCol + ", ct.relkind, " + optionsCol + ", spc.spcname, own.rolname as owner \n" +
			"from pg_catalog.pg_class ct \n" +
			"  join pg_catalog.pg_namespace cns on ct.relnamespace = cns.oid \n " +
			"  join pg_catalog.pg_roles own on ct.relowner = own.oid \n " +
			"  left join pg_catalog.pg_tablespace spc on spc.oid = ct.reltablespace \n" +
			" where cns.nspname = ? \n" +
			"   and ct.relname = ?";

		Savepoint sp = null;
		try
		{
			sp = dbConnection.setSavepoint();
			pstmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
			pstmt.setString(1, tbl.getSchema());
			pstmt.setString(2, tbl.getTableName());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresTableSourceBuilder.readTableOptions()", "Using sql: " + pstmt.toString());
			}
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				String persistence = rs.getString(1);
				String type = rs.getString(2);
				String tblSettings = rs.getString(3);
				String tableSpace = rs.getString(4);
				String owner = rs.getString("owner");
				tbl.setOwner(owner);

				if (StringUtil.isNonEmpty(persistence))
				{
					switch (persistence.charAt(0))
					{
						case 'u':
							option.setTypeModifier("UNLOGGED");
							break;
						case 't':
							option.setTypeModifier("TEMPORARY");
							break;
					}
				}
				if ("f".equalsIgnoreCase(type))
				{
					option.setTypeModifier("FOREIGN");
				}
				tbl.setTablespace(tableSpace);
				if (StringUtil.isNonEmpty(tblSettings))
				{
					setConfigSettings(tblSettings, option);
					if (tableSql.length() > 0) tableSql.append('\n');
					tableSql.append("WITH (");
					tableSql.append(tblSettings);
					tableSql.append(")");
				}
				if (StringUtil.isNonBlank(tableSpace))
				{
					tableSql.append("\nTABLESPACE ");
					tableSql.append(tableSpace);
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("PostgresTableSourceBuilder.readTableOptions()", "Error retrieving table options", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		option.setTableOption(tableSql.toString());
	}

	private void setConfigSettings(String options, ObjectSourceOptions tblOption)
	{
		List<String> l = StringUtil.stringToList(options, ",", true, true, false, true);
		for (String s : l)
		{
			String[] opt = s.split("=");
			if (opt.length == 2)
			{
				tblOption.addConfigSetting(opt[0], opt[1]);
			}
		}
	}

	private StringBuilder readInherits(TableIdentifier table)
	{
		if (table == null) return null;

		StringBuilder result = null;
    PostgresInheritanceReader reader = new PostgresInheritanceReader();

    List<TableIdentifier> parents = reader.getParents(dbConnection, table);
    if (CollectionUtil.isEmpty(parents)) return null;

    result = new StringBuilder(parents.size() * 30);
    result.append("INHERITS (");

    for (int i=0; i < parents.size(); i++)
    {
      TableIdentifier tbl = parents.get(i);
      table.getSourceOptions().addConfigSetting("inherits", tbl.getTableName());
      result.append(tbl.getTableName());
      if (i > 0) result.append(',');
    }
    result.append(')');

		return result;
	}

	public void readForeignTableOptions(TableIdentifier table)
	{
		ObjectSourceOptions option = table.getSourceOptions();

		String sql =
			"select ft.ftoptions, fs.srvname \n" +
			"from pg_foreign_table ft \n" +
			"  join pg_class tbl on tbl.oid = ft.ftrelid  \n" +
			"  join pg_namespace ns on tbl.relnamespace = ns.oid  \n" +
			"  join pg_foreign_server fs on ft.ftserver = fs.oid \n " +
			" WHERE tbl.relname = ? \n" +
			"   and ns.nspname = ? ";

		PreparedStatement stmt = null;
		ResultSet rs = null;
		StringBuilder result = new StringBuilder(100);
		Savepoint sp = null;
		try
		{
			sp = dbConnection.setSavepoint();
			stmt = dbConnection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTableName());
			stmt.setString(2, table.getSchema());

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresTableSourceBuilder.readForeignTableOptions()", "Retrieving table options using:\n" + stmt);
			}

			rs = stmt.executeQuery();
			if (rs.next())
			{
				Array array = rs.getArray(1);
				String[] options = array == null ? null : (String[])array.getArray();
				String serverName = rs.getString(2);
				result.append("SERVER ");
				result.append(serverName);
				if (options != null && options.length > 0)
				{
					result.append("\nOPTIONS (");
					for (int i = 0; i < options.length; i++)
					{
						if (i > 0)
						{
							result.append(", ");
						}
            String[] optValues = options[i].split("=");
            if (optValues.length == 2)
            {
              result.append(optValues[0] + " '" + optValues[1] + "'");
            }
          }
					result.append(')');
				}
				option.setTableOption(result.toString());
			}
		}
		catch (SQLException ex)
		{
			dbConnection.rollback(sp);
			sp = null;
			LogMgr.logError("PostgresTableSourceBuilder.readForeignTableOptions()", "Could not retrieve table options using:\n" + stmt, ex);
		}
		finally
		{
			dbConnection.releaseSavepoint(sp);
			SqlUtil.closeAll(rs, stmt);
		}
	}

	/**
	 * Return domain information for columns in the specified table.
	 *
	 */
	@Override
	public String getAdditionalTableInfo(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
	{
		String schema = table.getSchemaToUse(this.dbConnection);
		CharSequence enums = getEnumInformation(columns, schema);
		CharSequence domains = getDomainInformation(columns, schema);
		CharSequence sequences = getColumnSequenceInformation(table, columns);
		CharSequence children = getChildTables(table);
		StringBuilder storage = getColumnStorage(table, columns);
		String owner = getOwnerSql(table);

		if (StringUtil.isEmptyString(enums) && StringUtil.isEmptyString(domains) &&
			StringUtil.isEmptyString(sequences) && StringUtil.isEmptyString(children) &&
			StringUtil.isEmptyString(owner) && StringUtil.isEmptyString(storage)) return null;

		int enumLen = (enums != null ? enums.length() : 0);
		int domainLen = (domains != null ? domains.length() : 0);
		int childLen = (children != null ? children.length() : 0);
		int ownerLen = (owner != null ? owner.length() : 0);
		int storageLen = (storage != null ? storage.length() : 0);

		StringBuilder result = new StringBuilder(enumLen + domainLen + childLen + ownerLen + storageLen);

		if (storage != null) result.append(storage);
		if (enums != null) result.append(enums);
		if (domains != null) result.append(domains);
		if (sequences != null) result.append(sequences);
		if (children != null) result.append(children);
		if (owner != null) result.append(owner);

		return result.toString();
	}

	private StringBuilder getColumnStorage(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		StringBuilder result = null;
		String tname = table.getTableExpression(dbConnection);

		for (ColumnIdentifier col : columns)
		{
			int storage = col.getPgStorage();
			String option = PostgresColumnEnhancer.getStorageOption(storage);
			if (option != null && !isDefaultStorage(col.getDataType(), storage))
			{
				if (result == null)
				{
					result = new StringBuilder(50);
					result.append('\n');
				}
				result.append("ALTER TABLE ");
				result.append(tname);
				result.append(" ALTER ");
				result.append(dbConnection.getMetadata().quoteObjectname(col.getColumnName()));
				result.append(" SET STORAGE ");
				result.append(option);
				result.append(";\n");
			}
		}
		return result;
	}

	private boolean isDefaultStorage(int columnType, int storage)
	{
		if (columnType == Types.NUMERIC && storage == PostgresColumnEnhancer.STORAGE_MAIN) return true;
		return storage == PostgresColumnEnhancer.STORAGE_EXTENDED;
	}

	private String getOwnerSql(TableIdentifier table)
	{
		DbSettings.GenerateOwnerType genType = dbConnection.getDbSettings().getGenerateTableOwner();
		if (genType == DbSettings.GenerateOwnerType.never) return null;

		String owner = table.getOwner();
		if (StringUtil.isBlank(owner)) return null;

		if (genType == DbSettings.GenerateOwnerType.whenNeeded)
		{
			String user = dbConnection.getCurrentUser();
			if (user.equalsIgnoreCase(owner)) return null;
		}

		return "\nALTER TABLE " + table.getFullyQualifiedName(dbConnection) + " SET OWNER TO " + SqlUtil.quoteObjectname(owner) + ";";
	}

	private CharSequence getColumnSequenceInformation(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		if (!JdbcUtils.hasMinimumServerVersion(this.dbConnection, "8.4")) return null;
		if (table == null) return null;
		if (CollectionUtil.isEmpty(columns)) return null;
		String tblname = table.getTableExpression(dbConnection);
		ResultSet rs = null;
		Statement stmt = null;
		StringBuilder b = new StringBuilder(100);

		Savepoint sp = null;

		try
		{
			sp = dbConnection.setSavepoint();
			stmt = dbConnection.createStatementForQuery();
			for (ColumnIdentifier col : columns)
			{
				String defaultValue = col.getDefaultValue();
				// if the default value is shown as nextval, the sequence name is already visible
				if (defaultValue != null && defaultValue.toLowerCase().startsWith("nextval")) continue;
				String colname = StringUtil.trimQuotes(col.getColumnName());
				rs = stmt.executeQuery("select pg_get_serial_sequence('" + tblname + "', '" + colname + "')");
				if (rs.next())
				{
					String seq = rs.getString(1);
					if (StringUtil.isNonBlank(seq))
					{
						String msg = ResourceMgr.getFormattedString("TxtSequenceCol", col.getColumnName(), seq);
						b.append("\n-- ");
						b.append(msg);
					}
				}
			}
			dbConnection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			dbConnection.rollback(sp);
			LogMgr.logWarning("PostgresTableSourceBuilder.getColumnSequenceInformation()", "Error reading sequence information", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		if (b.length() == 0) return null;
		return b;
	}

	private CharSequence getEnumInformation(List<ColumnIdentifier> columns, String schema)
	{
		PostgresEnumReader reader = new PostgresEnumReader();
		Map<String, EnumIdentifier> enums = reader.getEnumInfo(dbConnection, schema, null);
		if (enums == null || enums.isEmpty()) return null;
		StringBuilder result = null;

		for (ColumnIdentifier col : columns)
		{
			String dbType = col.getDbmsType();
			EnumIdentifier enumDef = enums.get(dbType);
			if (enumDef != null)
			{
				if (result == null) result = new StringBuilder(50);
				result.append("\n-- enum '");
				result.append(dbType);
				result.append("': ");
				result.append(StringUtil.listToString(enumDef.getValues(), ",", true, '\''));
			}
		}

		return result;
	}

	public CharSequence getDomainInformation(List<ColumnIdentifier> columns, String schema)
	{
		PostgresDomainReader reader = new PostgresDomainReader();
		Map<String, DomainIdentifier> domains = reader.getDomainInfo(dbConnection, schema);
		if (domains == null || domains.isEmpty()) return null;
		StringBuilder result = null;

		for (ColumnIdentifier col : columns)
		{
			String dbType = col.getDbmsType();
			DomainIdentifier domain = domains.get(dbType);
			if (domain != null)
			{
				if (result == null) result = new StringBuilder(50);
				result.append("\n-- domain '");
				result.append(dbType);
				result.append("': ");
				result.append(domain.getSummary());
			}
		}

		return result;
	}

	protected CharSequence getChildTables(TableIdentifier table)
	{
		if (table == null) return null;

		StringBuilder result = null;

    PostgresInheritanceReader reader = new PostgresInheritanceReader();

    List<InheritanceEntry> tables = reader.getChildren(dbConnection, table);
		final boolean is84 = JdbcUtils.hasMinimumServerVersion(dbConnection, "8.4");

    for (int i = 0; i < tables.size(); i++)
    {
      if (i == 0)
      {
        result = new StringBuilder(50);
        if (is84)
        {
          result.append("\n/* Inheritance tree:\n\n");
          result.append(table.getSchema());
          result.append('.');
          result.append(table.getTableName());
        }
        else
        {
          result.append("\n-- Child tables:");
        }
      }
      String tableName = tables.get(i).getTable().getTableName();
      String schemaName = tables.get(i).getTable().getSchema();
      int level = tables.get(i).getLevel();
      if (is84)
      {
        result.append('\n');
        result.append(StringUtil.padRight(" ", level * 2));
      }
      else
      {
        result.append("\n--  ");
      }
      result.append(schemaName);
      result.append('.');
      result.append(tableName);
    }
    if (is84 && result != null)
    {
      result.append("\n*/");
    }
		return result;
	}

}
