/*
 * PostgresProcedureReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.JdbcProcedureReader;
import workbench.db.JdbcUtils;
import workbench.db.NoConfigException;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read procedure and function definitions from a Postgres database.
 *
 * @author  Thomas Kellerer
 */
public class PostgresProcedureReader
	extends JdbcProcedureReader
{
	// Maps PG type names to Java types.
	private Map<String, Integer> pgType2Java;
	private PGTypeLookup pgTypes;
	private PGType voidType;

	public PostgresProcedureReader(WbConnection conn)
	{
		super(conn);
		try
		{
			// all Postgres versions support Savepoinst (they were introduced with 8.0
			this.useSavepoint = conn.supportsSavepoints();
		}
		catch (Throwable th)
		{
			this.useSavepoint = false;
		}
	}

	private Map<String, Integer> getJavaTypeMapping()
	{
		if (pgType2Java == null)
		{
			// This mapping has been copied from the JDBC driver.
			// This map is a private attribute of the class org.postgresql.jdbc2.TypeInfoCache
			// so, even if I hardcoded references to the Postgres driver I wouldn't be able
			// to use the information.
			pgType2Java = new HashMap<String, Integer>();
			pgType2Java.put("int2", Integer.valueOf(Types.SMALLINT));
			pgType2Java.put("int4", Integer.valueOf(Types.INTEGER));
			pgType2Java.put("integer", Integer.valueOf(Types.INTEGER));
			pgType2Java.put("oid", Integer.valueOf(Types.BIGINT));
			pgType2Java.put("int8", Integer.valueOf(Types.BIGINT));
			pgType2Java.put("money", Integer.valueOf(Types.DOUBLE));
			pgType2Java.put("numeric", Integer.valueOf(Types.NUMERIC));
			pgType2Java.put("float4", Integer.valueOf(Types.REAL));
			pgType2Java.put("float8", Integer.valueOf(Types.DOUBLE));
			pgType2Java.put("char", Integer.valueOf(Types.CHAR));
			pgType2Java.put("bpchar", Integer.valueOf(Types.CHAR));
			pgType2Java.put("varchar", Integer.valueOf(Types.VARCHAR));
			pgType2Java.put("text", Integer.valueOf(Types.VARCHAR));
			pgType2Java.put("name", Integer.valueOf(Types.VARCHAR));
			pgType2Java.put("bytea", Integer.valueOf(Types.BINARY));
			pgType2Java.put("bool", Integer.valueOf(Types.BIT));
			pgType2Java.put("bit", Integer.valueOf(Types.BIT));
			pgType2Java.put("date", Integer.valueOf(Types.DATE));
			pgType2Java.put("time", Integer.valueOf(Types.TIME));
			pgType2Java.put("timetz", Integer.valueOf(Types.TIME));
			pgType2Java.put("timestamp", Integer.valueOf(Types.TIMESTAMP));
			pgType2Java.put("timestamptz", Integer.valueOf(Types.TIMESTAMP));
    }
		return pgType2Java;
	}

	private Integer getJavaType(String pgType)
	{
		Integer i = getJavaTypeMapping().get(pgType);
		if (i == null) return Integer.valueOf(Types.OTHER);
		return i;
	}

	protected PGTypeLookup getTypeLookup()
	{
		if (pgTypes == null)
		{
			Map<Long, PGType> typeMap = new HashMap<Long, PGType>(300);
			Statement stmt = null;
			ResultSet rs = null;
			Savepoint sp = null;
			String sql = "select oid, format_type(oid, null) from pg_type";

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresProcedureReader.getTypeLookup()", "Using query=" + sql);
			}

			try
			{
				sp = connection.setSavepoint();
				stmt = connection.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next())
				{
					long oid = rs.getLong(1);
					String typeName = rs.getString(2);
					if (typeName.equals("character varying"))
					{
						typeName = "varchar";
					}
					PGType typ = new PGType(StringUtil.trimQuotes(typeName), oid);
					typeMap.put(Long.valueOf(oid), typ);
					if (typ.isVoid())
					{
						voidType = typ;
					}
				}
				connection.releaseSavepoint(sp);
			}
			catch (SQLException e)
			{
				connection.rollback(sp);
				LogMgr.logError("PostgresProcedureReqder.getPGTypes()", "Could not read postgres data types", e);
				typeMap = Collections.emptyMap();
			}
			finally
			{
				SqlUtil.closeAll(rs, stmt);
			}
			pgTypes = new PGTypeLookup(typeMap);
		}
		return pgTypes;
	}

	private String getTypeNameFromOid(long oid)
	{
		PGType typ = getTypeLookup().getTypeFromOID(Long.valueOf(oid));
		return typ.getTypeName();
	}

	@Override
	public DataStore getProcedures(String catalog, String schemaPattern, String procName)
		throws SQLException
	{
		if ("*".equals(schemaPattern) || "%".equals(schemaPattern))
		{
			schemaPattern = null;
		}

		String namePattern = null;
		if ("*".equals(procName) || "%".equals(procName))
		{
			namePattern = null;
		}
		else if (StringUtil.isNonBlank(procName))
		{
			PGProcName pg = new PGProcName(procName, getTypeLookup());
			namePattern = pg.getName();
		}

		Statement stmt = null;
		Savepoint sp = null;
		ResultSet rs = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}

			String sql =
						"SELECT n.nspname AS proc_schema, \n" +
						"       p.proname AS proc_name, \n" +
						"       d.description AS remarks, \n" +
						"       coalesce(array_to_string(proallargtypes, ';'), array_to_string(proargtypes, ';')) as arg_types, \n" +
						"       array_to_string(p.proargnames, ';') as arg_names, \n" +
						"       array_to_string(p.proargmodes, ';') as arg_modes, \n"+
						"       case when p.proisagg then 'aggregate' else 'function' end as proc_type \n" +
						" FROM pg_catalog.pg_proc p \n " +
						"   JOIN pg_catalog.pg_namespace n on p.pronamespace=n.oid \n" +
						"   LEFT JOIN pg_catalog.pg_description d ON (p.oid=d.objoid) \n" +
						"   LEFT JOIN pg_catalog.pg_class c ON (d.classoid=c.oid AND c.relname='pg_proc') \n" +
						"   LEFT JOIN pg_catalog.pg_namespace pn ON (c.relnamespace=pn.oid AND pn.nspname='pg_catalog')";

			boolean whereNeeded = true;
			if (StringUtil.isNonBlank(schemaPattern))
			{
				sql += "\n WHERE n.nspname LIKE '" + schemaPattern + "' ";
				whereNeeded = false;
			}

			if (StringUtil.isNonBlank(namePattern))
			{
				sql += whereNeeded ? "\n WHERE " : "\n  AND ";
				sql += "p.proname LIKE '" + namePattern + "' ";
				whereNeeded = false;
			}

			if (connection.getDbSettings().returnAccessibleProceduresOnly())
			{
				sql += whereNeeded ? "\n WHERE " : "\n  AND ";
				sql += "has_function_privilege(p.oid,'execute')";
				whereNeeded = false;
			}

			sql += "\nORDER BY proc_schema, proc_name ";

			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("PostgresProcedureReader.getProcedures()", "Query to retrieve procedures: \n" + sql);
			}
			stmt = connection.createStatementForQuery();

			rs = stmt.executeQuery(sql);
			DataStore ds = buildProcedureListDataStore(this.connection.getMetadata(), false);

			while (rs.next())
			{
				String schema = rs.getString("proc_schema");
				String name = rs.getString("proc_name");
				String remark = rs.getString("remarks");
				String args = rs.getString("arg_types");
				String names = rs.getString("arg_names");
				String modes = rs.getString("arg_modes");
				String type = rs.getString("proc_type");
				int row = ds.addRow();

				PGProcName pname = new PGProcName(name, args, getTypeLookup());

				ProcedureDefinition def = new ProcedureDefinition(null, schema, name, java.sql.DatabaseMetaData.procedureReturnsResult);

				List<String> argNames = StringUtil.stringToList(names, ";", true, true);
				List<String> argTypes = StringUtil.stringToList(args, ";", true, true);
				List<String> argModes = StringUtil.stringToList(modes, ";", true, true);
				List<ColumnIdentifier> cols = convertToColumns(argNames, argTypes, argModes);
				def.setParameters(cols);
				def.setDisplayName(pname.getFormattedName());
				def.setDbmsProcType(type);
				def.setComment(remark);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG, null);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA, schema);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, pname.getFormattedName());
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, java.sql.DatabaseMetaData.procedureReturnsResult);
				ds.setValue(row, ProcedureReader.COLUMN_IDX_PROC_LIST_REMARKS, remark);
				ds.getRow(row).setUserObject(def);
			}

			this.connection.releaseSavepoint(sp);
			ds.resetStatus();
			return ds;
		}
		catch (SQLException sql)
		{
			this.connection.rollback(sp);
			throw sql;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Override
	public DataStore getProcedureColumns(ProcedureDefinition def)
		throws SQLException
	{
		if (Settings.getInstance().getBoolProperty("workbench.db.postgresql.fixproctypes", true)
			  && JdbcUtils.hasMinimumServerVersion(connection, "8.1"))
		{
			PGProcName pgName = new PGProcName(def.getDisplayName(), getTypeLookup());
			return getColumns(def.getCatalog(), def.getSchema(), pgName);
		}
		else
		{
			return super.getProcedureColumns(def.getCatalog(), def.getSchema(), def.getProcedureName());
		}
	}

	@Override
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		boolean usePGFunction = Settings.getInstance().getBoolProperty("workbench.db.postgresql.procsource.useinternal", false);

		if (usePGFunction && JdbcUtils.hasMinimumServerVersion(connection, "8.4") && "function".equals(def.getDbmsProcType()))
		{
			readFunctionDef(def);
			return;
		}

		PGProcName name = new PGProcName(def.getDisplayName(), getTypeLookup());

		String sql =
			"SELECT p.prosrc, \n" +
			"       l.lanname as lang_name, \n" +
			"       n.nspname as schema_name, \n";

		if (JdbcUtils.hasMinimumServerVersion(connection, "8.4"))
		{
			sql += "       pg_get_function_result(p.oid) as formatted_return_type, \n";
		}
		else
		{
			sql += "       null::text as formatted_return_type, \n";
		}
		sql +=	"       p.prorettype as return_type_oid, \n" +
						"       coalesce(array_to_string(p.proallargtypes, ';'), array_to_string(p.proargtypes, ';')) as argtypes, \n" +
						"       array_to_string(p.proargnames, ';') as argnames, \n" +
						"       array_to_string(p.proargmodes, ';') as argmodes, \n" +
						"       p.prosecdef, \n" +
						"       p.proretset, \n" +
						"       p.provolatile, \n" +
						"       p.proisstrict, \n" +
						"       p.proisagg, \n" +
			      "       obj_description(p.oid, 'pg_proc') as remarks ";

		boolean hasCost = JdbcUtils.hasMinimumServerVersion(connection, "8.3");
		if (hasCost)
		{
			sql += ",\n       p.procost ,\n       p.prorows ";
		}

		sql +=
			"\nFROM pg_proc p \n" +
			"   JOIN pg_language l ON p.prolang = l.oid \n" +
			"   JOIN pg_namespace n ON p.pronamespace = n.oid \n";

		sql += "WHERE p.proname = '" + name.getName() + "' \n";
		if (StringUtil.isNonBlank(def.getSchema()))
		{
			sql += "  AND n.nspname = '" + def.getSchema() + "' \n";
		}

		String oids = name.getOIDs();
		if (StringUtil.isNonBlank(oids))
		{
			String array = "ARRAY[" + oids.replace(' ', ',') + "]::oid[]";

			sql +=
				"  AND (   (p.proallargtypes is null AND p.proargtypes = cast('" + oids + "' as oidvector)) \n " +
				"       OR (p.proallargtypes = " + array + "))\n";
		}
		else
		{
			sql += " AND (p.proargtypes IS NULL OR array_length(p.proargtypes,1) = 0)";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresProcedureReader.readProcedureSource()", "Query to retrieve procedure source:\n" + sql);
		}

		StringBuilder source = new StringBuilder(500);

		ResultSet rs = null;
		Savepoint sp = null;
		Statement stmt = null;

		boolean isAggregate = false;
		String comment = null;
		String schema = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);

			boolean hasRow = rs.next();

			if (hasRow)
			{
				comment = rs.getString("remarks");
				isAggregate = rs.getBoolean("proisagg");
				schema = rs.getString("schema_name");
			}


			if (!isAggregate && hasRow)
			{
				source.append("CREATE OR REPLACE FUNCTION ");
				source.append(schema);
				source.append('.');
				source.append(name.getName());

				String src = rs.getString(1);
				if (rs.wasNull() || src == null) src = "";

				String lang = rs.getString("lang_name");
				long retTypeOid = rs.getLong("return_type_oid");
				String readableReturnType = rs.getString("formatted_return_type");

				String types = rs.getString("argtypes");
				String names = rs.getString("argnames");
				String modes = rs.getString("argmodes");
				boolean returnSet = rs.getBoolean("proretset");


				boolean securityDefiner = rs.getBoolean("prosecdef");
				boolean strict = rs.getBoolean("proisstrict");
				String volat = rs.getString("provolatile");

				Double cost = null;
				Double rows = null;
				if (hasCost)
				{
					cost = rs.getDouble("procost");
					rows = rs.getDouble("prorows");
				}
				List<String> argNames = StringUtil.stringToList(names, ";", true, true);
				List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
				List<String> argModes = StringUtil.stringToList(modes, ";", true, true);

				source.append('(');
				int paramCount = 0;

				for (int i=0; i < argTypes.size(); i++)
				{
					if (paramCount > 0) source.append(", ");

					if (i < argModes.size())
					{
						String mode = argModes.get(i);
						if ("o".equals(mode)) source.append("OUT ");
						if ("b".equals(mode)) source.append("INOUT ");
					}

					if (i < argNames.size())
					{
						source.append(argNames.get(i));
						source.append(' ');
					}

					long typeOid = StringUtil.getLongValue(argTypes.get(i), voidType.getOid());
					source.append(getTypeNameFromOid(typeOid));
					paramCount ++;
				}

				source.append(")\n  RETURNS ");
				if (readableReturnType == null)
				{
					if (returnSet)
					{
						source.append("SETOF ");
					}
					source.append(getTypeNameFromOid(retTypeOid));
				}
				else
				{
					source.append(readableReturnType);
				}
				source.append("\n  LANGUAGE ");
				source.append(lang);
				source.append("\nAS\n$body$\n");
				src = src.trim();
				source.append(StringUtil.makePlainLinefeed(src));
				if (!src.endsWith(";")) source.append(';');
				source.append("\n$body$\n");
				if (volat.equals("i"))
				{
					source.append(" IMMUTABLE");
				}
				else if (volat.equals("s"))
				{
					source.append(" STABLE");
				}
				else
				{
					source.append(" VOLATILE");
				}
				if (strict)
				{
					source.append(" STRICT");
				}
				if (securityDefiner)
				{
					source.append("\n SECURITY DEFINER");
				}

				if (cost != null)
				{
					source.append("\n COST ");
					source.append(cost.longValue());
				}

				if (rows != null && returnSet)
				{
					source.append("\n ROWS ");
					source.append(rows.longValue());
				}
				source.append('\n');
				source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
				source.append('\n');
				if (StringUtil.isNonBlank(comment))
				{
					source.append("\nCOMMENT ON FUNCTION ");
					source.append(name.getFormattedName());
					source.append(" IS '");
					source.append(SqlUtil.escapeQuotes(def.getComment()));
					source.append("'\n" );
					source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
					source.append('\n');
				}
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.readProcedureSource()", "Error retrieving source for " + name.getFormattedName(), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		if (isAggregate)
		{
			source.append(getAggregateSource(name, def.getSchema()));
			if (StringUtil.isNonBlank(comment))
			{
				source.append("\n\nCOMMENT ON AGGREGATE IS '");
				source.append(SqlUtil.escapeQuotes(def.getComment()));
				source.append("';\n\n");
			}
		}
		def.setSource(source);
	}

	/**
	 * Read the definition of a function using pg_get_functiondef()
	 *
	 * @param def
	 */
	protected void readFunctionDef(ProcedureDefinition def)
	{
		PGProcName name = new PGProcName(def.getDisplayName(), getTypeLookup());
		String funcname = def.getSchema() + "." + name.getFormattedName();
		String sql = "select pg_get_functiondef('" + funcname + "'::regprocedure)";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresProcedureReader.readFunctionDef()", "Using SQL=" + sql);
		}

		StringBuilder source = null;
		ResultSet rs = null;
		Savepoint sp = null;
		Statement stmt = null;
		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				String s = rs.getString(1);
				if (StringUtil.isNonBlank(s))
				{
					source = new StringBuilder(s.length() + 50);
					source.append(s);
					if (!s.endsWith("\n"))	source.append('\n');
					source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
					source.append('\n');
					if (StringUtil.isNonBlank(def.getComment()))
					{
						source.append("\nCOMMENT ON FUNCTION ");
						source.append(name.getFormattedName());
						source.append(" IS '");
						source.append(SqlUtil.escapeQuotes(def.getComment()));
						source.append("'\n" );
						source.append(Settings.getInstance().getAlternateDelimiter(connection).getDelimiter());
						source.append('\n');
					}
				}
			}
		}
		catch (SQLException e)
		{
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.readProcedureSource()", "Error retrieving source for " + name.getFormattedName(), e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		def.setSource(source);
	}

	protected StringBuilder getAggregateSource(PGProcName name, String schema)
	{
		String baseSelect = "SELECT a.aggtransfn, a.aggfinalfn, format_type(a.aggtranstype, null) as stype, a.agginitval, op.oprname ";
	  String from =
			 " FROM pg_proc p \n" +
       "  JOIN pg_namespace n ON p.pronamespace = n.oid \n" +
       "  JOIN pg_aggregate a ON a.aggfnoid = p.oid \n" +
       "  LEFT JOIN pg_operator op ON op.oid = a.aggsortop ";

		boolean hasSort = JdbcUtils.hasMinimumServerVersion(connection, "8.1");
		if (hasSort)
		{
			baseSelect += ", a.aggsortop ";
		}

		String sql = baseSelect + "\n" + from;
		sql += " WHERE p.proname = '" + name.getName() + "' ";
		if (StringUtil.isNonBlank(schema))
		{
			sql += " and n.nspname = '" + schema + "' ";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresProcedureReader.getAggregateSource()", "Query to retrieve aggregate source:\n" + sql);
		}
		StringBuilder source = new StringBuilder();
		ResultSet rs = null;
		Statement stmt = null;
		Savepoint sp = null;

		try
		{
			if (useSavepoint)
			{
				sp = this.connection.setSavepoint();
			}
			stmt = connection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{

				source.append("CREATE AGGREGATE ");
				source.append(name.getFormattedName());
				source.append("\n(\n");
				String sfunc = rs.getString("aggtransfn");
				source.append("  sfunc = ");
				source.append(sfunc);

				String stype = rs.getString("stype");
				source.append(",\n  stype = ");
				source.append(stype);

				String sortop = rs.getString("oprname");
				if (StringUtil.isNonBlank(sortop))
				{
					source.append(",\n  sortop = ");
					source.append(connection.getMetadata().quoteObjectname(sortop));
				}

				String finalfunc = rs.getString("aggfinalfn");
				if (StringUtil.isNonBlank(finalfunc) && !finalfunc.equals("-"))
				{
					source.append(",\n  finalfunc = ");
					source.append( finalfunc);
				}

				String initcond = rs.getString("agginitval");
				if (StringUtil.isNonBlank(initcond))
				{
					source.append(",\n  initcond = '");
					source.append(initcond);
					source.append('\'');
				}
				source.append("\n);\n");
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			source = null;
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.readProcedureSource()", "Error retrieving aggregate source for " + name, e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source;

	}


	/**
	 * A workaround for pre 8.3 drivers so that argument names are retrieved properly
	 * from the database. This was mainly inspired by the source code of pgAdmin III
	 * and the 8.3 driver sources
	 *
	 * @param catalog
	 * @param schema
	 * @param procname
	 * @return a DataStore with the argumens of the procedure
	 * @throws java.sql.SQLException
	 */
	private DataStore getColumns(String catalog, String schema, PGProcName procname)
		throws SQLException
	{
		String sql =
				"SELECT format_type(p.prorettype, NULL) as formatted_type, \n" +
				"       t.typname as pg_type, \n" +
				"       coalesce(array_to_string(proallargtypes, ';'), array_to_string(proargtypes, ';')) as argtypes, \n" +
				"       array_to_string(p.proargnames, ';') as argnames, \n" +
				"       array_to_string(p.proargmodes, ';') as modes, \n" +
				"       t.typtype \n" +
				"FROM pg_catalog.pg_proc p \n" +
				"   JOIN pg_catalog.pg_namespace n ON p.pronamespace = n.oid \n" +
				"   JOIN pg_catalog.pg_type t ON p.prorettype = t.oid \n" +
				"WHERE n.nspname = ? \n" +
				"  AND p.proname = ? \n";

		DataStore result = createProcColsDataStore();

		Savepoint sp = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			sp = connection.setSavepoint();

			String oids = procname.getOIDs();
			if (StringUtil.isNonBlank(oids))
			{
				sql += "  AND p.proargtypes = cast('" + oids + "' as oidvector)";
			}

			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			stmt.setString(2, procname.getName());

			if (Settings.getInstance().getDebugMetadataSql())
			{
				// Postgres JDBC statements implement toString() such that parameters are visible..
				LogMgr.logDebug("PostgresProcedureReader.getColumns()", "Query to retrieve procedure columns:\n" + stmt.toString());
			}

			rs = stmt.executeQuery();
			if (rs.next())
			{
				String typeName = rs.getString("formatted_type");
				String pgType = rs.getString("pg_type");
				String types = rs.getString("argtypes");
				String names = rs.getString("argnames");
				String modes = rs.getString("modes");
				String returnTypeType = rs.getString("typtype");

				// pgAdmin II distinguishes functions from procedures using only the "modes" information
				// the driver uses the returnTypeType as well
				boolean isFunction = (returnTypeType.equals("b") || returnTypeType.equals("d") || (returnTypeType.equals("p") && modes == null));

				if (isFunction)
				{
					int row = result.addRow();
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, "returnValue");
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, "RETURN");
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, getJavaType(pgType));
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, StringUtil.trimQuotes(typeName));
				}

				List<String> argNames = StringUtil.stringToList(names, ";", true, true);
				List<String> argTypes = StringUtil.stringToList(types, ";", true, true);
				List<String> argModes = StringUtil.stringToList(modes, ";", true, true);

				List<ColumnIdentifier> columns = convertToColumns(argNames, argTypes, argModes);

				for (ColumnIdentifier col : columns)
				{
					int row = result.addRow();
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_RESULT_TYPE, col.getArgumentMode());
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_JDBC_DATA_TYPE, col.getDataType());
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_DATA_TYPE, col.getDbmsType());
					result.setValue(row, ProcedureReader.COLUMN_IDX_PROC_COLUMNS_COL_NAME, col.getColumnName());
				}
			}
			else
			{
				LogMgr.logWarning("PostgresProcedureReader.getProcedureHeader()", "No columns returned for procedure: " + procname.getName(), null);
				return super.getProcedureColumns(catalog, schema, procname.getName());
			}

			connection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			connection.rollback(sp);
			LogMgr.logError("PostgresProcedureReader.getProcedureHeader()", "Error retrieving header", e);
			return super.getProcedureColumns(catalog, schema, procname.getName());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	private List<ColumnIdentifier> convertToColumns(List<String> argNames, List<String> argTypes, List<String> argModes)
	{
		List<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(argTypes.size());
		for (int i=0; i < argTypes.size(); i++)
		{
			int typeOid = StringUtil.getIntValue(argTypes.get(i), -1);
			String pgt = getTypeNameFromOid(typeOid);

			String nm = "$" + (i + 1);
			if (argNames != null && i < argNames.size())
			{
				nm = argNames.get(i);
			}
			ColumnIdentifier col = new ColumnIdentifier(nm);
			col.setDataType(getJavaType(pgt));
			col.setDbmsType(getTypeNameFromOid(typeOid));

			String md = "IN";
			if (argModes != null && i < argModes.size())
			{
				String m = argModes.get(i);
				if ("o".equals(m))
				{
					md = "OUT";
				}
				else if ("b".equals(m))
				{
					md = "INOUT";
				}
			}
			col.setArgumentMode(md);
			result.add(col);
		}
		return result;
	}
}
