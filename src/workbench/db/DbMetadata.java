/*
 * DbMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.derby.DerbyConstraintReader;
import workbench.db.derby.DerbySynonymReader;
import workbench.db.firebird.FirebirdProcedureReader;
import workbench.db.firstsql.FirstSqlMetadata;
import workbench.db.hsqldb.HsqlSequenceReader;
import workbench.db.ibm.Db2SequenceReader;
import workbench.db.ibm.Db2SynonymReader;
import workbench.db.ingres.IngresMetadata;
import workbench.db.mckoi.McKoiMetadata;
import workbench.db.mssql.SqlServerConstraintReader;
import workbench.db.mssql.SqlServerProcedureReader;
import workbench.db.mysql.EnumReader;
import workbench.db.mysql.MySqlProcedureReader;
import workbench.db.oracle.DbmsOutput;
import workbench.db.oracle.OracleConstraintReader;
import workbench.db.oracle.OracleIndexReader;
import workbench.db.oracle.OracleMetadata;
import workbench.db.oracle.OracleSynonymReader;
import workbench.db.postgres.PostgresDDLFilter;
import workbench.db.postgres.PostgresIndexReader;
import workbench.db.postgres.PostgresSequenceReader;
import workbench.db.postgres.PostgresConstraintReader;
import workbench.db.postgres.PostgresProcedureReader;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.db.hsqldb.HsqlConstraintReader;
import workbench.db.firebird.FirebirdConstraintReader;
import workbench.db.h2database.H2ConstraintReader;
import workbench.db.h2database.H2SequenceReader;

/**
 * Retrieve meta data information from the database.
 * This class returns more information then the generic JDBC DatabaseMetadata.
 *  @author  support@sql-workbench.net
 */
public class DbMetadata
	implements PropertyChangeListener
{
	public static final String MVIEW_NAME = "MATERIALIZED VIEW";
	private String schemaTerm;
	private String catalogTerm;
	private String productName;
	private String dbId;

	protected MetaDataSqlManager metaSqlMgr;
	private DatabaseMetaData metaData;
	private WbConnection dbConnection;

	private OracleMetadata oracleMetaData;

	private ConstraintReader constraintReader;
	private SynonymReader synonymReader;
	private SequenceReader sequenceReader;
	private ProcedureReader procedureReader;
	private ErrorInformationReader errorInfoReader;
	private SchemaInformationReader schemaInfoReader;
	private IndexReader indexReader;
	private DDLFilter ddlFilter;

	private DbmsOutput oraOutput;

	private boolean caseSensitive;
	private boolean useJdbcCommit;
	private boolean ddlNeedsCommit;
	private boolean isOracle;
	private boolean isPostgres;
	private boolean isFirstSql;
	private boolean isHsql;
	private boolean isFirebird;
	private boolean isSqlServer;
	private boolean isMySql;
	private boolean isCloudscape;
	private boolean isApacheDerby;
	private boolean isExcel; 
	private boolean isAccess;
	
	private boolean trimDefaults = true;
	private boolean createInlineConstraints;
	private boolean useNullKeyword = true;
	private boolean fixOracleDateBug = false;
	private boolean columnsListInViewDefinitionAllowed = true;
	
	// This is set to true if identifiers starting with
	// a digit should always be quoted. This will 
	// be initialized through the Settings object
	private boolean quoteIdentifierWithDigits = false;
	
	private String quoteCharacter;
	private String dbVersion;
	private SqlKeywordHandler keywordHandler;
	
	private static final String SELECT_INTO_PG = "(?i)(?s)SELECT.*INTO\\p{Print}*\\s*FROM.*";
	private static final String SELECT_INTO_INFORMIX = "(?i)(?s)SELECT.*FROM.*INTO\\s*\\p{Print}*";
	private Pattern selectIntoPattern = null;

	private static final int UPPERCASE_NAMES = 1;
	private static final int LOWERCASE_NAMES = 2;
	private static final int MIXEDCASE_NAMES = 4;
	
	private int objectCaseStorage = -1;
	private int schemaCaseStorage = -1;

	private String tableTypeName;
	private boolean neverQuoteObjects;

	private String[] tableTypesTable; 
	private String[] tableTypesSelectable;
	private Set objectsWithData = null;
	private List schemasToIgnore;
	private List catalogsToIgnore;

	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		Settings settings = Settings.getInstance();
		this.dbConnection = aConnection;
		this.metaData = aConnection.getSqlConnection().getMetaData();

		try
		{
			this.schemaTerm = this.metaData.getSchemaTerm();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Schema term", e);
			this.schemaTerm = "Schema";
		}

		try
		{
			this.catalogTerm = this.metaData.getCatalogTerm();
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Catalog term", e);
			this.catalogTerm = "Catalog";
		}

		// Some JDBC drivers do not return a value for getCatalogTerm() or getSchemaTerm()
		// and don't throw an Exception. This is to ensure that our getCatalogTerm() will
		// always return something usable.
		if (StringUtil.isEmptyString(this.schemaTerm)) this.schemaTerm = "Schema";
		if (StringUtil.isEmptyString(this.catalogTerm))	this.catalogTerm = "Catalog";

		try
		{
			this.productName = this.metaData.getDatabaseProductName();
			this.dbId = null;
		}
		catch (SQLException e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "Could not retrieve Database Product name", e);
			this.productName = aConnection.getProfile().getDriverclass();
		}

		String productLower = this.productName.toLowerCase();

		if (productLower.indexOf("oracle") > -1)
		{
			this.isOracle = true;
			this.oracleMetaData = new OracleMetadata(this, this.dbConnection);
			this.constraintReader = new OracleConstraintReader();
			this.synonymReader = new OracleSynonymReader();

			// register with the Settings Object to be notified for
			// changes to the "enable dbms output" property
			settings.addPropertyChangeListener(this);
			
			this.sequenceReader = this.oracleMetaData;
			this.procedureReader = this.oracleMetaData;
			this.errorInfoReader = this.oracleMetaData;
			this.fixOracleDateBug = Settings.getInstance().getBoolProperty("workbench.db.oracle.date.usetimestamp", true);
			this.indexReader = new OracleIndexReader(this);
		}
		else if (productLower.indexOf("postgres") > - 1)
		{
			this.isPostgres = true;
			this.selectIntoPattern = Pattern.compile(SELECT_INTO_PG);
			this.constraintReader = new PostgresConstraintReader();
			this.sequenceReader = new PostgresSequenceReader(this.dbConnection.getSqlConnection());
			this.procedureReader = new PostgresProcedureReader(this);
			this.indexReader = new PostgresIndexReader(this);
			this.ddlFilter = new PostgresDDLFilter();
		}
		else if (productLower.indexOf("hsql") > -1)
		{
			this.isHsql = true;
			this.constraintReader = new HsqlConstraintReader(this.dbConnection.getSqlConnection());
			this.sequenceReader = new HsqlSequenceReader(this.dbConnection.getSqlConnection());
			try
			{
				int major = metaData.getDatabaseMajorVersion();
				int minor = metaData.getDriverMinorVersion();
				if (major == 1 && minor <= 7)
				{
					// HSQLDB 1.7.x does not support a column list in the view definition
					this.columnsListInViewDefinitionAllowed = false;
				}
			}
			catch (Exception e)
			{
				this.columnsListInViewDefinitionAllowed = false;
			}
		}
		else if (productLower.indexOf("firebird") > -1)
		{
			this.isFirebird = true;
			this.constraintReader = new FirebirdConstraintReader();
			this.procedureReader = new FirebirdProcedureReader(this);
			// Jaybird 2.0 reports the Firebird version in the 
			// productname. To ease the DBMS handling we'll use the same
			// product name that is reported with the 1.5 driver. 
			this.productName = "Firebird";
		}
		else if (productLower.indexOf("sql server") > -1)
		{
			this.isSqlServer = true;
			this.constraintReader = new SqlServerConstraintReader();
			boolean useJdbc = Settings.getInstance().getBoolProperty("workbench.db.mssql.usejdbcprocreader", true);
			if (!useJdbc)
			{
				this.procedureReader = new SqlServerProcedureReader(this);
			}
		}
		else if (productLower.indexOf("db2") > -1)
		{
			this.synonymReader = new Db2SynonymReader();
			this.sequenceReader = new Db2SequenceReader(this.dbConnection);
		}
		else if (productLower.indexOf("adaptive server") > -1) 
		{
			// this covers adaptive server Enterprise and Anywhere
			this.constraintReader = new ASAConstraintReader();
		}
		else if (productLower.indexOf("mysql") > -1)
		{
			this.procedureReader = new MySqlProcedureReader(this, this.dbConnection);
			this.isMySql = true;
		}
		else if (productLower.indexOf("informix") > -1)
		{
			this.selectIntoPattern = Pattern.compile(SELECT_INTO_INFORMIX);
		}
		else if (productLower.indexOf("cloudscape") > -1)
		{
			this.isCloudscape = true;
			this.constraintReader = new DerbyConstraintReader();
		}
		else if (productLower.indexOf("derby") > -1)
		{
			this.isApacheDerby = true;
			this.constraintReader = new DerbyConstraintReader();
			this.synonymReader = new DerbySynonymReader();
		}
		else if (productLower.indexOf("ingres") > -1)
		{
			IngresMetadata imeta = new IngresMetadata(this.dbConnection.getSqlConnection());
			this.synonymReader = imeta;
			this.sequenceReader = imeta;
		}
		else if (productLower.indexOf("mckoi") > -1)
		{
			// McKoi reports the version in the database product name
			// which makes setting up the meta data stuff lookups
			// too complicated, so we'll strip the version info
			int pos = this.productName.indexOf('(');
			if (pos == -1) pos = this.productName.length() - 1;
			this.productName = this.productName.substring(0, pos).trim();
			this.sequenceReader = new McKoiMetadata(this.dbConnection.getSqlConnection());
		}
		else if (productLower.indexOf("firstsql") > -1)
		{
			this.constraintReader = new FirstSqlMetadata();
			this.isFirstSql = true;
		}
		else if (productLower.indexOf("excel") > -1)
		{
			this.isExcel = true;
		}
		else if (productLower.indexOf("access") > -1)
		{
			this.isAccess = true;
		}
		else if (productLower.equals("h2"))
		{
			this.sequenceReader = new H2SequenceReader(this.dbConnection.getSqlConnection());
			this.constraintReader = new H2ConstraintReader();
		}

		// if the DBMS does not need a specific ProcedureReader
		// we use the default implementation
		if (this.procedureReader == null)
		{
			this.procedureReader = new JdbcProcedureReader(this);
		}

		if (this.indexReader == null)
		{
			this.indexReader = new JdbcIndexReader(this);
		}
		
		if (this.schemaInfoReader == null)
		{
			this.schemaInfoReader = new GenericSchemaInfoReader(this.getDbId());
		}
		
		try
		{
			this.quoteCharacter = this.metaData.getIdentifierQuoteString();
		}
		catch (Exception e)
		{
			this.quoteCharacter = null;
		}
		if (StringUtil.isEmptyString(quoteCharacter)) this.quoteCharacter = "\"";

		try
		{
			this.dbVersion = this.metaData.getDatabaseProductVersion();
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.<init>", "errro calling getDatabaseProductVersion()", e);
		}

		this.caseSensitive = settings.getCaseSensitivServers().contains(this.productName);
		this.useJdbcCommit = settings.getServersWhichNeedJdbcCommit().contains(this.productName);
		this.ddlNeedsCommit = settings.getServersWhereDDLNeedsCommit().contains(this.productName);
		this.createInlineConstraints = settings.getServersWithInlineConstraints().contains(this.productName);

		this.useNullKeyword = !settings.getServersWithNoNullKeywords().contains(this.getDbId());

		this.metaSqlMgr = new MetaDataSqlManager(this.getProductName());

		String regex = settings.getProperty("workbench.sql.selectnewtablepattern." + this.getDbId(), null);
		if (regex != null)
		{
			try
			{
				this.selectIntoPattern = Pattern.compile(regex);
			}
			catch (Exception e)
			{
				this.selectIntoPattern = null;
				LogMgr.logError("DbMetadata.<init>", "Invalid pattern to identify a SELECT INTO a new table: " + regex, e);
			}
		}

		String nameCase = settings.getProperty("workbench.db.objectname.case." + this.getDbId(), null);
		if (nameCase != null)
		{
			if ("lower".equals(nameCase))
			{
				this.objectCaseStorage = LOWERCASE_NAMES;
			}
			else if ("upper".equals(nameCase))
			{
				this.objectCaseStorage = UPPERCASE_NAMES;
			}
			else if ("mixed".equals(nameCase))
			{
				this.objectCaseStorage = MIXEDCASE_NAMES;
			}
		}
		
		nameCase = settings.getProperty("workbench.db.schemaname.case." + this.getDbId(), null);
		if (nameCase != null)
		{
			if ("lower".equals(nameCase))
			{
				this.schemaCaseStorage = LOWERCASE_NAMES;
			}
			else if ("upper".equals(nameCase))
			{
				this.schemaCaseStorage = UPPERCASE_NAMES;
			}
			else if ("mixed".equals(nameCase))
			{
				this.schemaCaseStorage = MIXEDCASE_NAMES;
			}
		}

		tableTypeName = settings.getProperty("workbench.db.basetype.table." + this.getDbId(), "TABLE");
		tableTypesTable = new String[] {tableTypeName};
		
		// The tableTypesSelectable array will be used
		// to fill the completion cache. In that case 
		// we do not want system tables include (which 
		// is done in the objectsWithData as that 
		// drives the "Data" tab in the DbExplorer
		Set types = getObjectsWithData();
		List realTypes = new LinkedList();
		
		Iterator itr = types.iterator();
		while (itr.hasNext())
		{
			String s = ((String)itr.next()).toUpperCase();
			if (s.indexOf("SYSTEM") == -1)
			{
				realTypes.add(s);
			}
		}
		tableTypesSelectable = StringUtil.toArray(realTypes);

		String quote = settings.getProperty("workbench.db.neverquote","");
		this.neverQuoteObjects = quote.indexOf(this.getDbId()) > -1;
		this.trimDefaults = settings.getBoolProperty("workbench.db." + getDbId() + ".trimdefaults", true);
		this.quoteIdentifierWithDigits = settings.getBoolProperty("workbench.db." + getDbId() + ".quotedigits", false);
	}

	public String getTableTypeName() { return tableTypeName; }
	public String getViewTypeName() 
	{ 
		return "VIEW"; 
	}

	public boolean getStripProcedureVersion()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.stripprocversion", "");
		List l = StringUtil.stringToList(ids, ",", true, true, false);
		return l.contains(this.dbId);
	}
	
	public String getProcVersionDelimiter()
	{
		return Settings.getInstance().getProperty("workbench.db.procversiondelimiter." + this.getDbId(), "");
	}
	
	public DatabaseMetaData getJdbcMetadata()
	{
		return this.metaData;
	}

	public WbConnection getWbConnection() { return this.dbConnection; }
	
	public Connection getSqlConnection()
	{
		return this.dbConnection.getSqlConnection();
	}

	/**
	 * Check if the given DB object type can contain data. i.e. if
	 * a SELECT FROM can be run against this type
	 */
	public boolean objectTypeCanContainData(String type)
	{
		if (type == null) return false;
		return objectsWithData.contains(type.toLowerCase());
	}

	private Set getObjectsWithData()
	{
		if (this.objectsWithData == null)
		{
			String keyPrefix = "workbench.db.objecttype.selectable.";
			String defValue = Settings.getInstance().getProperty(keyPrefix + "default", null);
			String types = Settings.getInstance().getProperty(keyPrefix + getDbId(), defValue);
			
			objectsWithData = new HashSet();
			
			if (types == null)
			{
				objectsWithData.add("table");
				objectsWithData.add("system table");
				objectsWithData.add("view");
				objectsWithData.add("system view");
				objectsWithData.add("synonym");
			}
			else
			{
				List l = StringUtil.stringToList(types.toLowerCase(), ",", true, true);
				objectsWithData.addAll(l);
			}
			
			if (this.isPostgres) 
			{
				objectsWithData.add("sequence");
			}
			
			if (this.isOracle) 
			{
				objectsWithData.add(MVIEW_NAME.toLowerCase());
			}
		}
		return objectsWithData;
	}
	/**
	 *	Return the name of the DBMS as reported by the JDBC driver
	 */
	public String getProductName()
	{
		return this.productName;
	}

	/**
	 * 	Return a clean version of the productname.
	 *  @see #getProductName()
	 */
	public String getDbId()
	{
		if (this.dbId == null)
		{
			this.dbId = this.productName.replaceAll("[ \\(\\)\\[\\]\\/$,.]", "_").toLowerCase();
			LogMgr.logInfo("DbMetadata", "Using DBID=" + this.dbId);
		}
		return this.dbId;
	}

	public String getDbVersion() { return this.dbVersion; }
	public boolean getDDLNeedsCommit() { return ddlNeedsCommit; }
	public boolean getUseJdbcCommit() { return this.useJdbcCommit; }
  public boolean isStringComparisonCaseSensitive() { return this.caseSensitive; }

	public boolean reportsRealSizeAsDisplaySize()
	{
		return this.isHsql;
	}

	public boolean supportSingleLineCommands()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.checksinglelinecmd", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}

	public boolean supportsQueryTimeout()
	{
		boolean result = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".supportquerytimeout", true);
		return result;
	}
	
	public boolean supportsGetPrimaryKeys()
	{
		boolean result = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".supportgetpk", true);
		return result;
	}
	
	public boolean supportShortInclude()
	{
		String ids = Settings.getInstance().getProperty("workbench.db.supportshortinclude", "");
		if ("*".equals(ids)) return true;
		List dbs = StringUtil.stringToList(ids, ",", true, true);
		return dbs.contains(this.getDbId());
	}
	
	/**
	 *	Returns true if the current DBMS supports a SELECT syntax
	 *	which creates a new table (e.g. SELECT .. INTO new_table FROM old_table)
	 */
	public boolean supportsSelectIntoNewTable()
	{
		return this.selectIntoPattern != null;
	}

	/**
	 *	Checks if the given SQL string is actually some kind of table
	 *	creation "disguised" as a SELECT. This will always return false
	 *	if supportsSelectIntoNewTable() returns false.
	 *	Otherwise it will check for the DB specific syntax.
	 */
	public boolean isSelectIntoNewTable(String sql)
	{
		if (sql == null || sql.length() == 0) return false;
		if (this.selectIntoPattern == null) return false;
		Matcher m = this.selectIntoPattern.matcher(sql);
		return m.find();
	}

	public boolean isMySql() { return this.isMySql; }
	public boolean isPostgres() { return this.isPostgres; }
  public boolean isOracle() { return this.isOracle; }
	public boolean isHsql() { return this.isHsql; }
	public boolean isFirebird() { return this.isFirebird; }
	public boolean isSqlServer() { return this.isSqlServer; }
	public boolean isCloudscape() { return this.isCloudscape; }
	public boolean isApacheDerby() { return this.isApacheDerby; }

	/**
	 * If a DDLFilter is registered for the current DBMS, this
	 * method will replace all "problematic" characters in the 
	 * SQL string, and will return a String that the DBMS will
	 * understand. 
	 * Currently this is only implemented for PostgreSQL to 
	 * mimic pgsql's $$ quoting for stored procedures
	 * 
	 * @see workbench.db.postgres.PostgresDDLFilter
	 */
	public String filterDDL(String sql)
	{
		if (this.ddlFilter == null) return sql;
		return this.ddlFilter.adjustDDL(sql);
	}
	
	public boolean ignoreSchema(String schema)
	{
		if (schema == null) return true;
		if (schemasToIgnore == null)
		{
			String ids = Settings.getInstance().getProperty("workbench.sql.ignoreschema." + this.getDbId(), null);
			if (ids != null)
			{
				schemasToIgnore = StringUtil.stringToList(ids, ",");
			}
			else
			{
				 schemasToIgnore = Collections.EMPTY_LIST;
			}
		}
		return schemasToIgnore.contains("*") || schemasToIgnore.contains(schema);
	}

	public boolean needsTableForDropIndex()
	{
		boolean needsTable = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".dropindex.needstable", false);
		return needsTable;
	}
	
	/**
	 * Check if the given {@link TableIdentifier} requires
	 * the usage of the schema for a DML (select, insert, update, delete)
	 * statement. By default this is not required for an Oracle
	 * connetion where the schema is the current user.
	 * For all other DBMS, the usage can be disabled by setting
	 * a property in the configuration file
	 */
	public boolean needSchemaInDML(TableIdentifier table)
	{
		try
		{
			String tblSchema = table.getSchema();
			if (ignoreSchema(tblSchema)) return false;

			if (this.isOracle)
			{
				// In oracle we don't need the schema if the it is the current user
				if (tblSchema == null) return false;
				return !this.getUserName().equalsIgnoreCase(tblSchema);
			}
		}
		catch (Throwable th)
		{
			return false;
		}
		return true;
	}

	public boolean needCatalogInDML(TableIdentifier table)
	{
		if (this.isAccess) return true;
		if (!this.supportsCatalogs()) return false;
		String cat = table.getCatalog();
		if (StringUtil.isEmptyString(cat)) return false;
		String currentCat = getCurrentCatalog();
		
		if (this.isExcel)
		{
			// Excel puts the directory into the catalog
			// so we need to normalize the directory name
			File c1 = new File(cat);
			File c2 = new File(currentCat);
			if (c1.equals(c2)) return false;
		}
		else
		{
			if (StringUtil.isEmptyString(currentCat)) return false;
		}
		return !cat.equalsIgnoreCase(currentCat);
	}
	
	public boolean ignoreCatalog(String catalog)
	{
		if (catalog == null) return true;
		String c = getCurrentCatalog();
		if (c != null && c.equalsIgnoreCase(catalog)) return true;
		if (catalogsToIgnore == null)
		{
			String cats = Settings.getInstance().getProperty("workbench.sql.ignorecatalog." + this.getDbId(), null);
			if (cats != null)
			{
				catalogsToIgnore = StringUtil.stringToList(cats, ",");
			}
			else
			{
				 catalogsToIgnore = Collections.EMPTY_LIST;
			}
		}
		return catalogsToIgnore.contains("*") || catalogsToIgnore.contains(catalog);
	}

	/**
	 * Wrapper for DatabaseMetaData.supportsBatchUpdates() that throws
	 * no exception. If any error occurs, false will be returned
	 */
	public boolean supportsBatchUpdates()
	{
		try
		{
			return this.metaData.supportsBatchUpdates();
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	/**
	 *	Return the verb which does a DROP ... CASCADE for the given
	 *  object type. If the current DBMS does not support cascaded dropping
	 *  of objects, then null will be returned.
	 *
	 *	@param aType the database object type to drop (TABLE, VIEW etc)
	 *  @return a String which can be appended to a DROP type name command in order to drop dependent objects as well
	 *          or null if the current DBMS does not support this.
	 */
	public String getCascadeConstraintsVerb(String aType)
	{
		if (aType == null) return null;
		String verb = Settings.getInstance().getProperty("workbench.db.drop." + aType.toLowerCase() + ".cascade." + this.getDbId(), null);
		return verb;
	}

	public Set getDbFunctions()
	{
		Set dbFunctions = new HashSet();
		try
		{
			String funcs = this.metaData.getSystemFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = this.metaData.getStringFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = this.metaData.getNumericFunctions();
			this.addStringList(dbFunctions, funcs);

			funcs = this.metaData.getTimeDateFunctions();
			this.addStringList(dbFunctions, funcs);
			
			// Add Standard ANSI SQL Functions
			this.addStringList(dbFunctions, Settings.getInstance().getProperty("workbench.db.syntax.functions", "COUNT,AVG,SUM,MAX,MIN"));
			
			// Add additional DB specific functions
			this.addStringList(dbFunctions, Settings.getInstance().getProperty("workbench.db." + getDbId() + ".syntax.functions", null));
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getDbFunctions()", "Error retrieving function list from DB", e);
		}
		return dbFunctions;
	}

	private void addStringList(Set target, String list)
	{
		if (list == null) return;
		List tokens = StringUtil.stringToList(list, ",", true, true, false);
		Iterator itr = tokens.iterator();
		while (itr.hasNext())
		{
			String keyword = (String)itr.next();
			target.add(keyword.toUpperCase().trim());
		}
	}

	/**
	 * Drop given table. If this is successful and the
	 * DBMS requires a COMMIT for DDL statements then
	 * the DROP will be commited (or rolled back in case
	 * of an error
	 */
	public void dropTable(TableIdentifier aTable)
		throws SQLException
	{
		Statement stmt = null;
		try
		{
			StringBuilder sql = new StringBuilder();
			sql.append("DROP TABLE ");
			sql.append(aTable.getTableExpression());
			String cascade = this.getCascadeConstraintsVerb("TABLE");
			if (cascade != null)
			{
				sql.append(' ');
				sql.append(cascade);
			}
			stmt = this.dbConnection.createStatement();
			stmt.executeUpdate(sql.toString());
			if (this.ddlNeedsCommit && !this.dbConnection.getAutoCommit())
			{
				this.dbConnection.commit();
			}
		}
		catch (SQLException e)
		{
			if (this.ddlNeedsCommit && !this.dbConnection.getAutoCommit())
			{
				try { this.dbConnection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			try { stmt.close(); } catch (Throwable th) {}
		}
	}

	public String getObjectType(TableIdentifier table)
	{
		String type = null;
		ResultSet rs = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			rs = this.metaData.getTables(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), null);
			if (rs.next())
			{
				type = rs.getString("TABLE_TYPE");
			}
		}
		catch (Exception e)
		{
			type = null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return type;
	}
	
	/**
	 * Return the name of the current user.
	 * Wrapper for DatabaseMetaData.getUserName() that throws no Exception
	 */
	public String getUserName()
	{
		try
		{
			return this.metaData.getUserName();
		}
		catch (Exception e)
		{
			return StringUtil.EMPTY_STRING;
		}
	}

	public String getExtendedViewSource(TableIdentifier tbl, boolean includeDrop)
		throws SQLException
	{
		return this.getExtendedViewSource(tbl, null, includeDrop);
	}

	/**
	 * Returns a complete SQL statement to (re)create the given view.
	 */
	public String getExtendedViewSource(TableIdentifier view, DataStore viewTableDefinition, boolean includeDrop)
		throws SQLException
	{
		GetMetaDataSql sql = metaSqlMgr.getViewSourceSql();
		if (sql == null)
		{
			SourceStatementsHelp help = new SourceStatementsHelp();
			return help.explainMissingViewSourceSql(this.getProductName());
		}

		if (viewTableDefinition == null)
		{
			viewTableDefinition = this.getTableDefinition(view);
		}
		String source = this.getViewSource(view);
		
		if (StringUtil.isEmptyString(source)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(source.length() + 100);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		String verb = SqlUtil.getSqlVerb(source);
		
		// ThinkSQL and DB2 return the full CREATE VIEW statement
		if (verb.equalsIgnoreCase("CREATE"))
		{
			String type = SqlUtil.getCreateType(source);
			result.append("DROP ");
			result.append(type);
			result.append(' ');
			result.append(view.getTableName());
			result.append(';');
			result.append(lineEnding);
			result.append(lineEnding);
			result.append(source);
			if (this.ddlNeedsCommit)
			{
				result.append(lineEnding);
				result.append(lineEnding);
				result.append("COMMIT;");
			}
			return result.toString();
		}

		result.append(generateCreateObject(includeDrop, view.getType(), view.getTableName()));

		if (columnsListInViewDefinitionAllowed && !MVIEW_NAME.equalsIgnoreCase(view.getType()))
		{
			result.append(lineEnding + "(" + lineEnding);
			int rows = viewTableDefinition.getRowCount();
			for (int i=0; i < rows; i++)
			{
				String colName = viewTableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				result.append("  ");
				result.append(quoteObjectname(colName));
				if (i < rows - 1)
				{
					result.append(',');
					result.append(lineEnding);
				}
			}
			result.append(lineEnding + ")");
		}
		
		result.append(lineEnding + "AS " + lineEnding);
		result.append(source);
		result.append(lineEnding);
		if (this.ddlNeedsCommit)
		{
			result.append("COMMIT;");
		}
		return result.toString();
	}

	/**
	 *	Return the source of a view definition as it is stored in the database.
	 *	Usually (depending on how the meta data is stored in the database) the DBMS
	 *	only stores the underlying SELECT statement, and that will be returned by this method.
	 *	To create a complete SQL to re-create a view, use {@link #getExtendedViewSource(TableIdentifier, DataStore, boolean)}
	 *
	 *	@return the view source as stored in the database.
	 */
	public String getViewSource(TableIdentifier viewId)
	{
		if (viewId == null) return null;

		if (this.isOracle && MVIEW_NAME.equalsIgnoreCase(viewId.getType()))
		{
			return oracleMetaData.getSnapshotSource(viewId);
		}
		
		StrBuffer source = new StrBuffer(500);
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			GetMetaDataSql sql = metaSqlMgr.getViewSourceSql();
			if (sql == null) return StringUtil.EMPTY_STRING;
			TableIdentifier tbl = viewId.createCopy();
			tbl.adjustCase(this.dbConnection);
			sql.setSchema(tbl.getSchema());
			sql.setObjectName(tbl.getTableName());
			sql.setCatalog(tbl.getCatalog());
			stmt = this.dbConnection.createStatement();
			String query = this.adjustHsqlQuery(sql.getSql());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("DbMetadata.getViewSource()", "Using query=\n" + query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					source.append(line.replaceAll("\r", StringUtil.EMPTY_STRING));
				}
			}
			source.rtrim();
			if (!source.endsWith(';')) source.append(';');
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getViewSource()", "Could not retrieve view definition for " + viewId.getTableExpression(), e);
			source = new StrBuffer(ExceptionUtil.getDisplay(e));
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return source.toString();
	}

	private StringBuilder generateCreateObject(boolean includeDrop, String type, String name)
	{
		StringBuilder result = new StringBuilder();
		boolean replaced = false;

		String prefix = "workbench.db.";
		String suffix = "." + type.toLowerCase() + ".sql." + this.getDbId();

		String replace = Settings.getInstance().getProperty(prefix + "replace" + suffix, null);
		if (replace != null)
		{
			replace = replace.replaceAll("%name%", name);
			result.append(replace);
			replaced = true;
		}

		if (includeDrop && !replaced)
		{
			String drop = Settings.getInstance().getProperty(prefix + "drop" + suffix, null);
			if (drop == null)
			{
				result.append("DROP ");
				result.append(type.toUpperCase());
				result.append(' ');
				result.append(quoteObjectname(name));
				String cascade = this.getCascadeConstraintsVerb(type);
				if (cascade != null)
				{
					result.append(' ');
					result.append(cascade);
				}
				result.append(";\n");
			}
			else
			{
				drop = drop.replaceAll("%name%", quoteObjectname(name));
				result.append(drop);
			}
			result.append('\n');
		}

		if (!replaced)
		{
			String create = Settings.getInstance().getProperty(prefix + "create" + suffix, null);
			if (create == null)
			{
				result.append("CREATE ");
				result.append(type.toUpperCase());
				result.append(' ');
				result.append(quoteObjectname(name));
			}
			else
			{
				create = create.replaceAll("%name%", quoteObjectname(name));
				result.append(create);
			}
		}
		return result;
	}

	public String getProcedureSource(String aCatalog, String aSchema, String aProcname, int type)
	{
		try
		{
			ProcedureDefinition def = new ProcedureDefinition(aCatalog, aSchema, aProcname, type);
			readProcedureSource(def);
			return def.getSource();
		}
		catch (NoConfigException e)
		{
			SourceStatementsHelp help = new SourceStatementsHelp();
			return help.explainMissingProcSourceSql(this.getProductName());
		}
	}	
	
	public void readProcedureSource(ProcedureDefinition def)
		throws NoConfigException
	{
		if (procedureReader != null)
		{
			this.procedureReader.readProcedureSource(def);
		}
	}

	private void initKeywordHandler()
	{
		this.keywordHandler = new SqlKeywordHandler(this.dbConnection.getSqlConnection(), this.getDbId());
	}
	
	public boolean isKeyword(String name)
	{
		if (this.keywordHandler == null) this.initKeywordHandler();
		return this.keywordHandler.isKeyword(name);
	}
	
	public Collection getSqlKeywords()
	{
		if (this.keywordHandler == null) this.initKeywordHandler();
		return this.keywordHandler.getSqlKeywords();
	}

	public String quoteObjectname(String aName)
	{
		return quoteObjectname(aName, false);
	}
	/**
	 *	Encloses the given object name in double quotes if necessary.
	 *	Quoting of names is necessary if the name is a reserved word in the
	 *	database. To check if the given name is a keyword, it is compared
	 *  to the words returned by getSQLKeywords().
	 *
	 *	If the given name is not a keyword, {@link workbench.util.SqlUtil#quoteObjectname(String)}
	 *  will be called to check if the name contains special characters which require
	 *	double quotes around the object name.
	 *
	 *  For Oracle and HSQL strings starting with a digit will
	 *  always be quoted.
	 */
	public String quoteObjectname(String aName, boolean quoteAlways)
	{
		if (aName == null) return null;
		if (aName.length() == 0) return aName;
		
		// already quoted?
		if (aName.startsWith("\"")) return aName;

		if (this.neverQuoteObjects) return StringUtil.trimQuotes(aName);

		try
		{
			boolean needQuote = quoteAlways;

			// Oracle and HSQL require identifiers starting with a number 
			// have to be quoted always. 
			if (needQuote || this.quoteIdentifierWithDigits)
			{
				needQuote = (Character.isDigit(aName.charAt(0)));
			}
			
			if (!needQuote && !this.storesMixedCaseIdentifiers())
			{
				if (this.storesLowerCaseIdentifiers() && !StringUtil.isLowerCase(aName))
				{
					needQuote = true;
				}
				else if (this.storesUpperCaseIdentifiers() && !StringUtil.isUpperCase(aName))
				{
					needQuote = true;
				}
			}
			
			if (needQuote || isKeyword(aName))
			{
				StringBuilder result = new StringBuilder(aName.length() + 4);
				result.append(this.quoteCharacter);
				result.append(aName.trim());
				result.append(this.quoteCharacter);
				return result.toString();
			}
			
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.quoteObjectName()", "Error when retrieving DB information", e);
		}

		// if it is not a keyword, we have to check for special characters such
		// as a space, $ etec
		return SqlUtil.quoteObjectname(aName);
	}

	/**
	 * Adjusts the case of the given schema name to the
	 * case in which the server stores objects
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition. Usually the getColumns()
	 * method is case sensitiv. If no special case
	 * for schemas is configured then this method
	 * is simply delegating to {@link #adjustObjectnameCase(String)}
	 */
	public String adjustSchemaNameCase(String schema)
	{
		if (schema == null) return null;
		if (this.schemaCaseStorage == -1)
		{
			return this.adjustObjectnameCase(schema);
		}
		schema = StringUtil.trimQuotes(schema);
		try
		{
			if (this.storesLowerCaseSchemas())
			{
				return schema.toUpperCase();
			}
			else if (this.storesLowerCaseSchemas())
			{
				return schema.toLowerCase();
			}
		}
		catch (Exception e)
		{
		}
		return schema.trim();
	}

	/**
	 * Returns true if the given object name needs quoting due 
	 * to mixed case writing or because the case of the name 
	 * does not match the case in which the database stores its objects
	 */
	public boolean isDefaultCase(String name)
	{
		if (name == null) return true;
		
		if (supportsMixedCaseIdentifiers()) return true;
	
		boolean isUpper = StringUtil.isUpperCase(name);
		boolean isLower = StringUtil.isLowerCase(name);
		boolean isMixed = (!isUpper && !isLower);
		
		if (isMixed && supportsMixedCaseQuotedIdentifiers()) return false;
		if (isUpper && this.storesUpperCaseIdentifiers())  return true;
		if (isLower && this.storesLowerCaseIdentifiers()) return true;
		
		return false;
	}
	
	
	/**
	 * Adjusts the case of the given object to the
	 * case in which the server stores objects
	 * This is needed e.g. when the user types a
	 * table name, and that value is used to retrieve
	 * the table definition. Usually the getColumns()
	 * method is case sensitiv.
	 */
	public String adjustObjectnameCase(String name)
	{
		if (name == null) return null;
		// if we have quotes, keep them...
		if (name.indexOf("\"") > -1) return name.trim();
		
		try
		{
			if (this.storesUpperCaseIdentifiers())
			{
				return name.toUpperCase();
			}
			else if (this.storesLowerCaseIdentifiers())
			{
				return name.toLowerCase();
			}
		}
		catch (Exception e)
		{
		}
		return name.trim();
	}

	/**
	 * Returns the "active" schema. Currently this is only
	 * implemented for Oracle where the "current" schema
	 * is the user name.
	 * Note that in Oracle this could be changed
	 * using ALTER SESSION SET SCHEMA=...
	 * This is not taken into account in this method
	 */
	public String getCurrentSchema()
	{
		if (this.schemaInfoReader != null)
		{
			return this.schemaInfoReader.getCurrentSchema(this.dbConnection);
		}
		return null;
	}

	/**
	 * Returns the schema that should be used for the current user
	 * This essential call {@link #getCurrentSchema()}. The method 
	 * then checks if the schema should be ignored for the current
	 * dbms by calling {@link #ignoreSchema(String)}. If the 
	 * Schema should not be ignored, the it's returned, otherwise
	 * the method will return null
	 */
	public String getSchemaToUse()
	{
		String schema = this.getCurrentSchema();
		if (schema == null) return null;
		if (this.ignoreSchema(schema)) return null;
		return schema;
	}

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's name
	 */
	public final static int COLUMN_IDX_TABLE_LIST_NAME = 0;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's type. The available types can be retrieved
	 * using {@link #getTableTypes()}
	 */
	public final static int COLUMN_IDX_TABLE_LIST_TYPE = 1;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's catalog
	 */
	public final static int COLUMN_IDX_TABLE_LIST_CATALOG = 2;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's schema
	 */
	public final static int COLUMN_IDX_TABLE_LIST_SCHEMA = 3;

	/**
	 * The column index of the column in the DataStore returned by getTables()
	 * the stores the table's comment
	 */
	public final static int COLUMN_IDX_TABLE_LIST_REMARKS = 4;

	public DataStore getTables()
		throws SQLException
	{
		String user = this.getCurrentSchema();
		return this.getTables(null, user, (String[])null);
	}

	public DataStore getTables(String schema, String[] types)
		throws SQLException
	{
		return this.getTables(null, schema, null, types);
	}

	public DataStore getTables(String aCatalog, String aSchema, String[] types)
		throws SQLException
	{
		return getTables(aCatalog, aSchema, null, types);
	}

	public DataStore getTables(String aCatalog, String aSchema, String tables, String[] types)
		throws SQLException
	{
		if ("*".equals(aSchema) || "%".equals(aSchema)) aSchema = null;
		if ("*".equals(tables) || "%".equals(tables)) tables = null;

		if (aSchema != null) aSchema = StringUtil.replace(aSchema, "*", "%");
		if (tables != null) tables = StringUtil.replace(tables, "*", "%");
		String[] cols = new String[] {"NAME", "TYPE", catalogTerm.toUpperCase(), schemaTerm.toUpperCase(), "REMARKS"};
		int coltypes[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30,12,10,10,20};

		DataStore result = new DataStore(cols, coltypes, sizes);
		
		boolean sequencesReturned = false;
		boolean checkOracleInternalSynonyms = (isOracle && typeIncluded("SYNONYM", types));
		boolean checkOracleSnapshots = (isOracle && Settings.getInstance().getBoolProperty("workbench.db.oracle.detectsnapshots", true) && typeIncluded("TABLE", types));
		boolean checkSyns = typeIncluded("SYNONYM", types);
		boolean synRetrieved = false;
		
		String excludeSynsRegex = Settings.getInstance().getProperty("workbench.db.oracle.exclude.synonyms", null);
		Pattern synPattern = null;
		if (checkOracleInternalSynonyms && excludeSynsRegex != null)
		{
			try
			{
				synPattern = Pattern.compile(excludeSynsRegex);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getTables()", "Invalid RegEx for excluding public synonyms specified. RegEx ignored", e);
				synPattern = null;
			}
		}
		
		String excludeTablesRegex = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".exclude.tables", null);
		Pattern excludeTablePattern = null;
		if (excludeTablesRegex != null && typeIncluded("TABLE", types))
		{
			try
			{
				excludeTablePattern = Pattern.compile(excludeTablesRegex);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getTables()", "Invalid RegEx for excluding tables. RegEx '" + excludeTablesRegex + "' ignored", e);
				excludeTablePattern = null;
			}
			LogMgr.logInfo("DbMetadata.getTables()", "Excluding tables that match the following regex: " + excludeTablesRegex);
		}

		if (isPostgres && types == null)
		{
			// The current PG drivers to not adhere to the JDBC javadocs
			// and return nothing when passing null for the types
			// so we retrieve all possible types, and pass them 
			// as this is the meaning of "null" for the types parameter
			Collection typeList = this.getTableTypes();
			types = StringUtil.toArray(typeList);
		}
		
		Set snapshotList = Collections.EMPTY_SET;
		if (checkOracleSnapshots)
		{
			snapshotList = this.oracleMetaData.getSnapshots(aSchema);
		}
		
		boolean hideIndexes = hideIndexes();

		ResultSet tableRs = null;
		try
		{
			tableRs = this.metaData.getTables(StringUtil.trimQuotes(aCatalog), StringUtil.trimQuotes(aSchema), StringUtil.trimQuotes(tables), types);
			if (tableRs == null)
			{
				LogMgr.logError("DbMetadata.getTables()", "Driver returned a NULL ResultSet from getTables()",null);
				return result;
			}
			
			while (tableRs.next())
			{
				String cat = tableRs.getString(1);
				String schem = tableRs.getString(2);
				String name = tableRs.getString(3);
				String ttype = tableRs.getString(4);
				if (name == null) continue;

				// filter out "internal" synonyms for Oracle
				if (checkOracleInternalSynonyms)
				{
					if (name.indexOf('/') > -1) continue;
					if (synPattern != null)
					{
						Matcher m = synPattern.matcher(name);
						if (m.matches()) continue;
					}
				}
			
				// prevent duplicate retrieval of SYNONYMS if the driver
				// returns them already, but the Settings have enabled
				// Synonym retrieval as well
				// (e.g. because an upgraded Driver now returns the synonyms)
				if (checkSyns && !synRetrieved && "SYNONYM".equals(ttype))
				{
					synRetrieved = true;
				}
				
				if (excludeTablePattern != null && ttype.equalsIgnoreCase("TABLE"))
				{
					Matcher m = excludeTablePattern.matcher(name);
					if (m.matches()) continue;
				}
				
				if (hideIndexes && isIndexType(ttype)) continue;
				
				if (checkOracleSnapshots)
				{
					StringBuilder t = new StringBuilder(30);
					t.append(schem);
					t.append('.');
					t.append(name);
					if (snapshotList.contains(t.toString()))
					{
						ttype = MVIEW_NAME;
					}
				}

				String rem = tableRs.getString(5);
				int row = result.addRow();
				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, name);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, ttype);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, cat);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, schem);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, rem);
				if (!sequencesReturned && "SEQUENCE".equals(ttype)) sequencesReturned = true;
			}
		}
		finally
		{
			SqlUtil.closeResult(tableRs);
		}

		if (this.sequenceReader != null && typeIncluded("SEQUENCE", types) &&
				"true".equals(Settings.getInstance().getProperty("workbench.db." + this.getDbId() + ".retrieve_sequences", "true"))
				&& !sequencesReturned)
		{
			List seq = this.sequenceReader.getSequenceList(aSchema);
			Iterator itr = seq.iterator();
			while (itr.hasNext())
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, (String)itr.next());
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, "SEQUENCE");
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, aSchema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, null);
			}
		}

		boolean retrieveSyns = (this.synonymReader != null && Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".retrieve_synonyms", false));
		if (retrieveSyns && typeIncluded("SYNONYM", types) && !synRetrieved)
		{
			LogMgr.logDebug("DbMetadata.getTables()", "Retrieving synonyms...");
			List syns = this.synonymReader.getSynonymList(this.dbConnection.getSqlConnection(), aSchema);
			int count = syns.size();
			for (int i=0; i < count; i++)
			{
				int row = result.addRow();

				result.setValue(row, COLUMN_IDX_TABLE_LIST_NAME, (String)syns.get(i));
				result.setValue(row, COLUMN_IDX_TABLE_LIST_TYPE, "SYNONYM");
				result.setValue(row, COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_SCHEMA, aSchema);
				result.setValue(row, COLUMN_IDX_TABLE_LIST_REMARKS, null);
			}
		}
		return result;
	}

	private boolean typeIncluded(String type, String[] types)
	{
		if (types == null) return true;
		if (type == null) return false;
		int l = types.length;
		for (int i=0; i < l; i++)
		{
			if (types[i].equals("*")) return true;
			if (type.equalsIgnoreCase(types[i])) return true;
		}
		return false;
	}

	/**
	 * Check if the given table exists in the database
	 */
	public boolean tableExists(TableIdentifier aTable)
	{
		return objectExists(aTable, tableTypesTable);
	}
	
	public boolean objectExists(TableIdentifier aTable, String type)
	{
		String[] types = null;
		if (type != null)
		{
			types = new String[] { type };
		}
		return objectExists(aTable, types);
	}
	
	public boolean objectExists(TableIdentifier aTable, String[] types)
	{
		if (aTable == null) return false;
		boolean exists = false;
		ResultSet rs = null;
		TableIdentifier tbl = aTable.createCopy();
		try
		{
      tbl.adjustCase(this.dbConnection);
			String c = tbl.getRawCatalog();
			String s = tbl.getRawSchema();
			String t = tbl.getRawTableName();
			rs = this.metaData.getTables(c, s, t, types);
			exists = rs.next();
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.tableExists()", "Error checking table existence", e);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return exists;
	}

	protected boolean supportsMixedCaseIdentifiers()
	{
		try
		{
			return this.metaData.supportsMixedCaseIdentifiers();
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	protected boolean supportsMixedCaseQuotedIdentifiers()
	{
		try
		{
			return this.metaData.supportsMixedCaseQuotedIdentifiers();
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Returns true if the server stores identifiers in mixed case.
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings with the property:
	 * workbench.db.objectname.case.<dbid>
	 */
	public boolean storesMixedCaseIdentifiers()
	{
		if (this.objectCaseStorage != -1)
		{
			return this.objectCaseStorage == MIXEDCASE_NAMES;
		}
		try
		{
			return this.metaData.storesMixedCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}
	
	/**
	 * Returns true if the server stores identifiers in lower case.
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings with the property:
	 * workbench.db.objectname.case.<dbid>
	 */
	public boolean storesLowerCaseIdentifiers()
	{
		if (this.objectCaseStorage != -1)
		{
			return this.objectCaseStorage == LOWERCASE_NAMES;
		}
		try
		{
			return this.metaData.storesLowerCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	public boolean storesUpperCaseSchemas()
	{
		return this.schemaCaseStorage == UPPERCASE_NAMES;
	}

	public boolean storesLowerCaseSchemas()
	{
		return this.schemaCaseStorage == LOWERCASE_NAMES;
	}

	/**
	 * Returns true if the server stores identifiers in upper case.
	 * Usually this is delegated to the JDBC driver, but as some drivers
	 * (e.g. Frontbase) implement this incorrectly, this can be overriden
	 * in workbench.settings
	 */
	public boolean storesUpperCaseIdentifiers()
	{
		if (this.objectCaseStorage != -1)
		{
			return this.objectCaseStorage == UPPERCASE_NAMES;
		}
		try
		{
			return this.metaData.storesUpperCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	public DataStore getProcedureColumns(String aCatalog, String aSchema, String aProcname)
		throws SQLException
	{
		return this.procedureReader.getProcedureColumns(aCatalog, aSchema, aProcname);
	}

	/**
	 * Construct the SQL verb for the given SQL datatype.
	 * This is used when re-recreating the source for a table
	 */
	public static String getSqlTypeDisplay(String aTypeName, int sqlType, int size, int digits)
	{
		String display = aTypeName;

		switch (sqlType)
		{
			case Types.VARCHAR:
			case Types.CHAR:
				if ("text".equals(aTypeName) && size == Integer.MAX_VALUE) return aTypeName;
				if (size > 0) 
				{
					display = aTypeName + "(" + size + ")";
				}
				else
				{
					display = aTypeName;
				}
				break;
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.FLOAT:
				if (aTypeName.equalsIgnoreCase("money")) // SQL Server
				{
					display = aTypeName;
				}
				else if ((aTypeName.indexOf('(') == -1))
				{
					if (digits > 0 && size > 0)
					{
						display = aTypeName + "(" + size + "," + digits + ")";
					}
					else if (size <= 0 && digits > 0)
					{
						display = aTypeName + "(" + digits + ")";
					}
					else if (size > 0 && digits <= 0)
					{
						display = aTypeName + "(" + size + ")";
					}
//					else if (size > 0 && ("NUMBER".equals(aTypeName) || "FLOAT".equals(aTypeName))) // Oracle specific
//					{
//						display = aTypeName + "(" + size + ")";
//					}
				}
				break;

			case Types.OTHER:
				// Oracle specific datatypes
				if ("NVARCHAR2".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("NCHAR".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("UROWID".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("RAW".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				break;
			default:
				display = aTypeName;
				break;
		}
		return display;
	}

	public boolean procedureExists(ProcedureDefinition def)
	{
		return procedureReader.procedureExists(def.getCatalog(), def.getSchema(), def.getProcedureName(), def.getResultType());
	}
	
	/**
	 * Return a list of stored procedures that are available
	 * in the database. This call is delegated to the
	 * currently defined {@link workbench.db.ProcedureReader}
	 * If no DBMS specific reader is used, this is the {@link workbench.db.JdbcProcedureReader}
	 * 
	 * @return a DataStore with the list of procedures.
	 */
	public DataStore getProcedures(String aCatalog, String aSchema)
		throws SQLException
	{
		return this.procedureReader.getProcedures(aCatalog, aSchema);
	}
	
	/**
	 * Return a List of {@link workbench.db.ProcedureDefinition} objects
	 * for Oracle only one object per definition is returned (although
	 * the DbExplorer will list each function of the packages.
	 */
	public List getProcedureList(String aCatalog, String aSchema)
		throws SQLException
	{
		assert(procedureReader != null);
		
		List result = new LinkedList();
		DataStore procs = this.procedureReader.getProcedures(aCatalog, aSchema);
		if (procs == null || procs.getRowCount() == 0) return result;
		procs.sortByColumn(ProcedureReader.COLUMN_IDX_PROC_LIST_NAME, true);
		int count = procs.getRowCount();
		Set oraPackages = new HashSet();
		
		for (int i = 0; i < count; i++)
		{
			String schema  = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_SCHEMA);
			String cat = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_CATALOG);
			String procName = procs.getValueAsString(i, ProcedureReader.COLUMN_IDX_PROC_LIST_NAME);
			int type = procs.getValueAsInt(i, ProcedureReader.COLUMN_IDX_PROC_LIST_TYPE, DatabaseMetaData.procedureNoResult);
			ProcedureDefinition def = null;
			if (this.isOracle && cat != null)
			{
				// The package name for Oracle is reported in the catalog column.
				// each function/procedure of the package is listed separately,
				// but we only wnat to create one ProcedureDefinition for the whole package
				if (!oraPackages.contains(cat))
				{
					def = ProcedureDefinition.createOraclePackage(schema, cat);
					oraPackages.add(cat);
				}
			}
			else
			{
				def = new ProcedureDefinition(cat, schema, procName, type);
			}
			if (def != null) result.add(def);
		}
		return result;
	}

	/**
	 * Enable Oracle's DBMS_OUTPUT package with a default buffer size
	 * @see #enableOutput(long)
	 */
	public void enableOutput()
	{
		this.enableOutput(-1);
	}

	/**
	 * Enable Oracle's DBMS_OUTPUT package.
	 * @see workbench.db.oracle.DbmsOutput#enable(long)
	 */
	public void enableOutput(long aLimit)
	{
    if (!this.isOracle)	return;

		if (this.oraOutput == null)
		{
      try
      {
  			this.oraOutput = new DbmsOutput(this.dbConnection.getSqlConnection());
      }
      catch (Exception e)
      {
        LogMgr.logError("DbMetadata.enableOutput()", "Could not create DbmsOutput", e);
        this.oraOutput = null;
      }
    }

    if (this.oraOutput != null)
    {
			try
			{
				this.oraOutput.enable(aLimit);
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbMetadata.enableOutput()", "Error when enabling DbmsOutput", e);
			}
		}
	}

	/**
	 * Disable Oracle's DBMS_OUTPUT package
	 * @see workbench.db.oracle.DbmsOutput#disable()
	 */
	public void disableOutput()
	{
    if (!this.isOracle) return;
		
		if (this.oraOutput != null)
		{
			try
			{
				this.oraOutput.disable();
        this.oraOutput = null;
			}
			catch (Throwable e)
			{
				LogMgr.logError("DbMetadata.disableOutput()", "Error when disabling DbmsOutput", e);
			}
		}
	}

	/**
	 * Return any server side messages. Currently this is only implemented
	 * for Oracle (and is returning messages that were "printed" using
	 * the DBMS_OUTPUT package
	 */
	public String getOutputMessages()
	{
		String result = StringUtil.EMPTY_STRING;

		if (this.oraOutput != null)
		{
			try
			{
				result = this.oraOutput.getResult();
			}
			catch (Throwable th)
			{
				LogMgr.logError("DbMetadata.getOutputMessages()", "Error when retrieving Output Messages", th);
				result = StringUtil.EMPTY_STRING;
			}
		}
		return result;
	}

	/**
	 * Release any resources for this object. After a call
	 * to close(), this object should not be used any longer
	 */
	public void close()
	{
		Settings.getInstance().removePropertyChangeLister(this);
		if (this.oraOutput != null) this.oraOutput.close();
		if (this.oracleMetaData != null) this.oracleMetaData.columnsProcessed();
	}

	/**
	 *	Return a list of ColumnIdentifier's for the given table
	 */
	public List getTableColumns(TableIdentifier aTable)
		throws SQLException
	{
		ColumnIdentifier[] cols = getColumnIdentifiers(aTable);
		List result = new ArrayList(cols.length);
		for (int i=0; i < cols.length; i++)
		{
			result.add(cols[i]);
		}
		return result;
	}

	public int fixColumnType(int type)
	{
		if (!this.fixOracleDateBug) return type;
		if (type == Types.DATE) return Types.TIMESTAMP;
		return type;
	}
	
	public ColumnIdentifier[] getColumnIdentifiers(TableIdentifier table)
		throws SQLException
	{
		String type = table.getType();
		//if (type == null) type = tableTypeName;
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		DataStore ds = this.getTableDefinition(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName(), type, false);
		return createColumnIdentifiers(ds);
	}

	private ColumnIdentifier[] createColumnIdentifiers(DataStore ds)
	{
		int count = ds.getRowCount();
		ColumnIdentifier[] result = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			String col = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			int type = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, Types.OTHER);
			boolean pk = "YES".equals(ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG));
			ColumnIdentifier ci = new ColumnIdentifier(SqlUtil.quoteObjectname(col), fixColumnType(type), pk);
			int size = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_SIZE, 0);
			int digits = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_DIGITS, 0);
			String nullable = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_NULLABLE);
			int position = ds.getValueAsInt(i, COLUMN_IDX_TABLE_DEFINITION_POSITION, 0);
			String dbmstype = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE);
			String comment = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_REMARKS);
			String def = ds.getValueAsString(i, COLUMN_IDX_TABLE_DEFINITION_DEFAULT);
			ci.setColumnSize(size);
			ci.setDecimalDigits(digits);
			ci.setIsNullable(StringUtil.stringToBool(nullable));
			ci.setDbmsType(dbmstype);
			ci.setComment(comment);
			ci.setDefaultValue(def);
			ci.setPosition(position);
			result[i] = ci;
		}
		return result;
	}

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the column name
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_COL_NAME = 0;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the DBMS specific data type string
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE = 1;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the primary key flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_PK_FLAG = 2;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the nullable flag
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_NULLABLE = 3;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the default value for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_DEFAULT = 4;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the remark for this column
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_REMARKS = 5;

	/** The column index for a {@link workbench.storage.DataStore} returned
	 *  by {@link #getTableDefinition(String, String, String)} that holds
	 *  the integer value of the java datatype from {@link java.sql.Types}
	 */
	public final static int COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE = 6;
	public final static int COLUMN_IDX_TABLE_DEFINITION_SIZE = 7;
	public final static int COLUMN_IDX_TABLE_DEFINITION_DIGITS = 8;
	public final static int COLUMN_IDX_TABLE_DEFINITION_POSITION = 9;

	public DataStore getTableDefinition(TableIdentifier aTable)
		throws SQLException
	{
		return getTableDefinition(aTable, true);
	}
	
	/**
	 * Returns the definition of the given
	 * table in a {@link workbench.storage.DataStore }
	 * @return definiton of the datastore
	 * @param id The identifier of the table
	 * @param adjustCase whether to adjust the case of the tablename
	 * @throws SQLException If the table was not found or an error occurred 
	 * @see #getTableDefinition(String, String, String, String, boolean)
	 */
	public DataStore getTableDefinition(TableIdentifier id, boolean adjustCase)
		throws SQLException
	{
		if (id == null) return null;
		String type = id.getType();
		if (type == null) type = tableTypeName;
		return this.getTableDefinition(id.getRawCatalog(), id.getRawSchema(), id.getRawTableName(), type, adjustCase);
	}

	public static final String[] TABLE_DEFINITION_COLS = {"COLUMN_NAME", "DATA_TYPE", "PK", "NULLABLE", "DEFAULT", "REMARKS", "java.sql.Types", "SCALE/SIZE", "PRECISION", "POSITION"};
	
	private DataStore createTableDefinitionDataStore()
	{
		final int[] types = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER};
		final int[] sizes = {20, 18, 5, 8, 10, 25, 18, 2, 2, 2};
		DataStore ds = new DataStore(TABLE_DEFINITION_COLS, types, sizes);
		return ds;
	}
	/** Return a DataStore containing the definition of the given table.
	 * @param aCatalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the table is defined. This should be null if the DBMS does not support schemas
	 * @param aTable The name of the table
	 * @param aType The type of the table
	 * @param adjustNames If true the object names will be quoted if necessary
	 * @throws SQLException
	 * @return A DataStore with the table definition.
	 * The individual columns should be accessed using the
	 * COLUMN_IDX_TABLE_DEFINITION_xxx constants.
	 */
	protected DataStore getTableDefinition(String aCatalog, String aSchema, String aTable, String aType, boolean adjustNames)
		throws SQLException
	{
		if (aTable == null) throw new IllegalArgumentException("Tablename may not be null!");

		DataStore ds = this.createTableDefinitionDataStore();

		aCatalog = StringUtil.trimQuotes(aCatalog);
		aSchema = StringUtil.trimQuotes(aSchema);
		aTable = StringUtil.trimQuotes(aTable);

		if (aSchema == null && this.isOracle()) 
		{
			aSchema = this.getSchemaToUse();
		}
		
		if (adjustNames)
		{
			aCatalog = this.adjustObjectnameCase(aCatalog);
			aSchema = this.adjustObjectnameCase(aSchema);
			aTable = this.adjustObjectnameCase(aTable);
		}

		if (this.sequenceReader != null && "SEQUENCE".equalsIgnoreCase(aType))
		{
			DataStore seqDs = this.sequenceReader.getSequenceDefinition(aSchema, aTable);
			if (seqDs != null) return seqDs;
		}

		if ("SYNONYM".equalsIgnoreCase(aType))
		{
			TableIdentifier id = this.getSynonymTable(aSchema, aTable);
			if (id != null)
			{
				aSchema = id.getSchema();
				aTable = id.getTableName();
				aCatalog = null;
			}
		}

		ArrayList keys = new ArrayList();
		if (this.supportsGetPrimaryKeys())
		{
			ResultSet keysRs = null;
			try
			{
				keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, aTable);
				while (keysRs.next())
				{
					keys.add(keysRs.getString("COLUMN_NAME").toLowerCase());
				}
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("DbMetaData.getTableDefinition()", "Error retrieving key columns", e);
			}
			finally
			{
				SqlUtil.closeResult(keysRs);
			}
		}
		
		boolean hasEnums = false;

		ResultSet rs = null;
		
		boolean checkOracleCharSemantics = this.isOracle && Settings.getInstance().useOracleCharSemanticsFix();
		
		try
		{
			// Oracle's JDBC driver does not return varchar lengths
			// correctly if the NLS_LENGTH_SEMANTICS is set to CHARACTER (and not byte)
			// so we'll need to use our own statement
			if (this.oracleMetaData != null)
			{
				rs = this.oracleMetaData.getColumns(aCatalog, aSchema, aTable, "%");
			}
			else
			{
				rs = this.metaData.getColumns(aCatalog, aSchema, aTable, "%");
			}

			while (rs.next())
			{
				int row = ds.addRow();

				String colName = rs.getString("COLUMN_NAME");
				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				if (this.isMySql && !hasEnums)
				{
					hasEnums = typeName.startsWith("enum") || typeName.startsWith("set");
				}

				int size = rs.getInt("COLUMN_SIZE");
				int digits = rs.getInt("DECIMAL_DIGITS");
				if (this.isPostgres && (sqlType == java.sql.Types.NUMERIC || sqlType == java.sql.Types.DECIMAL))
				{
					if (size == 65535) size = 0;
					if (digits == 65531) digits = 0;
				}
				String rem = rs.getString("REMARKS");
				String def = rs.getString("COLUMN_DEF");
				if (def != null && this.trimDefaults)
				{
					def = def.trim();
				}
				int position = rs.getInt("ORDINAL_POSITION");
				String nul = rs.getString("IS_NULLABLE");

				String display = null;
				
				// Hack to get Oracle's VARCHAR2(xx Byte) or VARCHAR2(xxx Char) display correct
				// Our own statement to retrieve column information in OracleMetaData
				// will return the byte/char semantics in the field SQL_DATA_TYPE
				// Oracle's JDBC driver does not supply this information (because
				// the JDBC standard does not define a column for this)
				if (checkOracleCharSemantics && sqlType == Types.VARCHAR && this.oracleMetaData != null)
				{
					int byteOrChar = rs.getInt("SQL_DATA_TYPE");
					display = this.oracleMetaData.getVarcharType(typeName, size, byteOrChar);
					if (display == null)
					{
						display = getSqlTypeDisplay(typeName, sqlType, size, digits);
					}
				}
				else
				{
					display = getSqlTypeDisplay(typeName, sqlType, size, digits);
				}
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_COL_NAME, colName);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DATA_TYPE, display);
				if (keys.contains(colName.toLowerCase()))
					ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, "YES");
				else
					ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_PK_FLAG, "NO");

				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_NULLABLE, nul);

				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DEFAULT, def);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_REMARKS, rem);
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_JAVA_SQL_TYPE, new Integer(sqlType));
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_SIZE, new Integer(size));
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_DIGITS, new Integer(digits));
				ds.setValue(row, COLUMN_IDX_TABLE_DEFINITION_POSITION, new Integer(position));
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
			if (this.oracleMetaData != null)
			{
				this.oracleMetaData.columnsProcessed();
			}
		}

		if (hasEnums)
		{
			EnumReader.updateEnumDefinition(aTable, ds, this.dbConnection);
		}

		return ds;
	}

	public static final int COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME = 0;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG = 1;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG = 2;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_COL_DEF = 3;
	public static final int COLUMN_IDX_TABLE_INDEXLIST_TYPE = 4;
	
	private Map indexTypeMapping;
	public static final String IDX_TYPE_NORMAL = "NORMAL";
	
	private String mapIndexType(int type)
	{
		if (indexTypeMapping == null)
		{
			this.indexTypeMapping = new HashMap();
			String map = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".indextypes", null);
			if (map != null)
			{
				List entries = StringUtil.stringToList(map, ";", true, true);
				Iterator itr = entries.iterator();
				while (itr.hasNext())
				{
					String entry = (String)itr.next();
					String[] mapping = entry.split(",");
					if (mapping.length != 2) continue;
					int value = StringUtil.getIntValue(mapping[0], -42);
					if (value != -42)
					{
						indexTypeMapping.put(new Integer(value), mapping[1]);
					}
				}
			}
		}
		String dbmsType = (String)this.indexTypeMapping.get(new Integer(type));
		if (dbmsType == null) 
		{
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logDebug("DbMetadata.mapIndexType()", "No mapping for type = " + type);
			}
			return IDX_TYPE_NORMAL;
		}
		return dbmsType;
	}
	
	public IndexDefinition[] getIndexList(TableIdentifier tbl)
	{
		Collection l = this.getTableIndexList(tbl);
		int count = l.size();
		IndexDefinition[] result = new IndexDefinition[count];
		Iterator itr = l.iterator();
		int i = 0;
		while (itr.hasNext())
		{
			result[i] = (IndexDefinition)itr.next();
			i++;
		}
		return result;
	}
	
	public DataStore getTableIndexInformation(TableIdentifier table)
	{
		String[] cols = {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION", "TYPE"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {30, 7, 6, 40, 10};
		DataStore idxData = new DataStore(cols, types, sizes);
		Collection indexes = getTableIndexList(table);
		Iterator itr = indexes.iterator();
		while (itr.hasNext())
		{
			int row = idxData.addRow();
			IndexDefinition idx = (IndexDefinition)itr.next();
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME, idx.getName());
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG, (idx.isUnique() ? "YES" : "NO"));
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG, (idx.isPrimaryKeyIndex() ? "YES" : "NO"));
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_COL_DEF, idx.getExpression());
			idxData.setValue(row, COLUMN_IDX_TABLE_INDEXLIST_TYPE, idx.getIndexType());
		}
		idxData.sortByColumn(0, true);
		return idxData;
	}
	
	/**
	 * Returns a list of IndexDefinition entries
	 */
	public Collection getTableIndexList(TableIdentifier table)
	{
		
		ResultSet idxRs = null;
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		
		// This will map an indexname to an IndexDefinition object
		// getIndexInfo() returns one row for each column
		HashMap defs = new HashMap();
		
		try
		{
			// Retrieve the name of the PK index
			String pkName = "";
			if (this.supportsGetPrimaryKeys())
			{
				ResultSet keysRs = null;
				try
				{
					keysRs = this.metaData.getPrimaryKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
					while (keysRs.next())
					{
						pkName = keysRs.getString("PK_NAME");
					}
				}
				catch (Exception e)
				{
					LogMgr.logWarning("DbMetadata.getTableIndexInformation()", "Error retrieving PK information", e);
					pkName = "";
				}
				finally
				{
					SqlUtil.closeResult(keysRs);
				}
			}
			
			idxRs = this.indexReader.getIndexInfo(tbl, false);
			
			while (idxRs.next())
			{
				boolean unique = idxRs.getBoolean("NON_UNIQUE");
				String indexName = idxRs.getString("INDEX_NAME");
				if (idxRs.wasNull()) continue;
				if (indexName == null) continue;
				String colName = idxRs.getString("COLUMN_NAME");
				String dir = idxRs.getString("ASC_OR_DESC");
				
				int type = idxRs.getInt("TYPE");
				
				
				IndexDefinition def = (IndexDefinition)defs.get(indexName);
				if (def == null)
				{
					def = new IndexDefinition(indexName, null);
					def.setIndexType(mapIndexType(type));
					def.setUnique(!unique);
					def.setPrimaryKeyIndex(pkName.equals(indexName));
					defs.put(indexName, def);
				}
				if (dir != null)
				{
					def.addColumn(colName + " " + dir);
				}
				else
				{
					def.addColumn(colName);
				}
				
				if (this.isOracle)
				{
					String oraType = idxRs.getString("INDEX_TYPE");
					def.setIndexType(oraType);
				}
			}
			
			this.indexReader.processIndexList(tbl, defs.values());
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getTableIndexInformation()", "Could not retrieve indexes!", e);
		}
		finally
		{
			try { idxRs.close(); } catch (Throwable th) {}
			this.indexReader.indexInfoProcessed();
		}
		return defs.values();
	}

	public List getTableList(String schema, String[] types)
		throws SQLException
	{
		if (schema == null) schema = this.getCurrentSchema();
		return getTableList(null, adjustSchemaNameCase(schema), types);
	}
	
	public List getTableList()
		throws SQLException
	{
		String schema = this.getCurrentSchema();
		return getTableList(null, schema, tableTypesTable);
	}

	public List getTableList(String table, String schema)
		throws SQLException
	{
		return getTableList(table, adjustSchemaNameCase(schema), tableTypesTable);
	}

	public List getSelectableObjectsList(String schema)
		throws SQLException
	{
		return getTableList(null, schema, tableTypesSelectable, false);
	}

	public List getTableList(String table, String schema, String[] types)
		throws SQLException
	{
		return getTableList(table, schema, types, false);
	}
		/**
	 * Return a list of tables for the given schema
	 * if the schema is null, all tables will be returned
	 */
	public List getTableList(String table, String schema, String[] types, boolean returnAllSchemas)
		throws SQLException
	{
		DataStore ds = getTables(null, schema, table, types);
		int count = ds.getRowCount();
		List tables = new ArrayList(count);
		for (int i=0; i < count; i++)
		{
			String t = ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_NAME);
			String s = ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_SCHEMA);
			String c = ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_CATALOG);
			if (!returnAllSchemas && this.ignoreSchema(s))
			{
				s = null;
			}
			if (this.ignoreCatalog(c))
			{
				c = null;
			}
			TableIdentifier tbl = new TableIdentifier(c, s, t);
			tbl.setNeverAdjustCase(true);
			tbl.setType(ds.getValueAsString(i, COLUMN_IDX_TABLE_LIST_TYPE));
			tables.add(tbl);
		}
		return tables;
	}

	/** 	Return the current catalog for this connection. If no catalog is defined
	 * 	or the DBMS does not support catalogs, an empty string is returned.
	 *
	 * 	This method works around a bug in Microsoft's JDBC driver which does
	 *  not return the correct database (=catalog) after the database has
	 *  been changed with the USE <db> command from within the Workbench.
	 * @return The name of the current catalog or an empty String if there is no current catalog
	 */
	public String getCurrentCatalog()
	{
		String catalog = null;

		if (this.isSqlServer)
		{
			// for some reason, getCatalog() does not return the correct
			// information when using Microsoft's JDBC driver.
			// So we are using SQL Server's db_name() function to retrieve
			// the current catalog
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = this.dbConnection.createStatement();
				rs = stmt.executeQuery("SELECT db_name()");
				if (rs.next()) catalog = rs.getString(1);
			}
			catch (Exception e)
			{
				LogMgr.logError("DbMetadata.getCurrentCatalog()", "Error retrieving catalog", e);
				catalog = null;
			}
			finally
			{
				SqlUtil.closeAll(rs, stmt);
			}
		}

		if (catalog == null)
		{
			try
			{
				catalog = this.dbConnection.getSqlConnection().getCatalog();
			}
			catch (Exception e)
			{
				catalog = StringUtil.EMPTY_STRING;
			}
		}
		if (catalog == null) catalog = StringUtil.EMPTY_STRING;

		return catalog;
	}

	public boolean supportsTruncate()
	{
		String s = Settings.getInstance().getProperty("workbench.db.truncatesupported", StringUtil.EMPTY_STRING);
		List l = StringUtil.stringToList(s, ",");
		return l.contains(this.getDbId());
	}

	public boolean supportsCatalogs()
	{
		boolean supportsCatalogs = false;
		try
		{
			supportsCatalogs = metaData.supportsCatalogsInDataManipulation()
		                  || metaData.supportsCatalogsInTableDefinitions();
		}
		catch (Exception e)
		{
			supportsCatalogs = false;
		}
		return supportsCatalogs;
	}
	
	/**
	 * Changes the current catalog using Connection.setCatalog()
	 * and notifies the connection object about the change.
	 *
	 * @param newCatalog the name of the new catalog/database that should be selected
	 * @see WbConnection#catalogChanged(String, String)
	 */
	public boolean setCurrentCatalog(String newCatalog)
		throws SQLException
	{
		if (StringUtil.isEmptyString(newCatalog)) return false;
	
		String old = getCurrentCatalog();
		boolean useSetCatalog = Settings.getInstance().getBoolProperty("workbench.db." + this.getDbId() + ".usesetcatalog", true);
		
		// MySQL does not seem to like changing the current database by executing a USE command
		// through Statement.execute(), so we'll use setCatalog() instead
		// which seems to work with SQL Server as well. 
		// If for some reason this does not work, it could be turned off
		if (useSetCatalog)
		{
			this.dbConnection.getSqlConnection().setCatalog(trimQuotes(newCatalog));
		}
		else
		{
			Statement stmt = null;
			try 
			{
				stmt = this.dbConnection.createStatement();
				stmt.execute("USE " + newCatalog);
			}
			finally
			{
				SqlUtil.closeStatement(stmt);
			}
		}
		
		String newCat = getCurrentCatalog();
		if (!StringUtil.equalString(old, newCat))
		{
			this.dbConnection.catalogChanged(old, newCatalog);
		}
		LogMgr.logDebug("DbMetadata.setCurrentCatalog", "Catalog changed to " + newCat);
		
		return true;
	}
	
	/**
	 * Remove quotes from an object's name. 
	 * For MS SQL Server this also removes [] brackets
	 * around the identifier.
	 */
	private String trimQuotes(String s)
	{
		if (s.length() < 2) return s;
		if (this.isSqlServer)
		{
			String clean = s.trim();
			int len = clean.length();
			if (clean.charAt(0)=='[' && clean.charAt(len-1)==']')
				return clean.substring(1,len-1);
		}
		
		return StringUtil.trimQuotes(s);
}
	/**
	 *	Returns a list of all catalogs in the database.
	 *	Some DBMS's do not support catalogs, in this case the method
	 *	will return an empty Datastore.
	 */
	public DataStore getCatalogInformation()
	{

		String[] cols = { this.getCatalogTerm().toUpperCase() };
		int[] types = { Types.VARCHAR };
		int[] sizes = { 10 };

		DataStore result = new DataStore(cols, types, sizes);
		ResultSet rs = null;
		try
		{
			rs = this.metaData.getCatalogs();
			while (rs.next())
			{
				String cat = rs.getString(1);
				if (cat != null)
				{
					int row = result.addRow();
					result.setValue(row, 0, cat);
				}
			}
		}
		catch (Exception e)
		{
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		
		if (result.getRowCount() == 1)
		{
			String cat = result.getValueAsString(0, 0);
			if (cat.equals(this.getCurrentCatalog()))
			{
				result.reset();
			}
		}
		
		return result;
	}

	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the name of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME = 0;
	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the type (INSERT, UPDATE etc) of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE = 1;
	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the event (before, after) of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT = 2;

	/**
	 *	Return the list of defined triggers for the given table.
	 */
//	public DataStore getTableTriggers(String aCatalog, String aSchema, String aTable)
	public DataStore getTableTriggers(TableIdentifier table)
		throws SQLException
	{
		String[] cols = {"NAME", "TYPE", "EVENT"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {30, 30, 20};

		DataStore result = new DataStore(cols, types, sizes);

		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);

		GetMetaDataSql sql = metaSqlMgr.getListTriggerSql();
		if (sql == null)
		{
			return result;
		}

		sql.setSchema(tbl.getSchema());
		sql.setCatalog(tbl.getCatalog());
		sql.setObjectName(tbl.getTableName());
		Statement stmt = this.dbConnection.createStatementForQuery();
		String query = this.adjustHsqlQuery(sql.getSql());

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTableTriggers()", "Using query=\n" + query);
		}
		ResultSet rs = stmt.executeQuery(query);
		try
		{
			while (rs.next())
			{
				int row = result.addRow();
				String value = rs.getString(1);
				if (!rs.wasNull() && value != null) value = value.trim();
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME, value);

				value = rs.getString(2);
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE, value);

				value = rs.getString(3);
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT, value);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	/**
	 * Retrieve the SQL Source of the given trigger.
	 * @param aCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param aTriggername
	 * @throws SQLException
	 * @return the trigger source
	 */
	public String getTriggerSource(String aCatalog, String aSchema, String aTriggername)
		throws SQLException
	{
		StrBuffer result = new StrBuffer(500);

		if ("*".equals(aCatalog)) aCatalog = null;
		if ("*".equals(aSchema)) aSchema = null;

		GetMetaDataSql sql = metaSqlMgr.getTriggerSourceSql();
		if (sql == null) return StringUtil.EMPTY_STRING;

		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTriggername);
		Statement stmt = this.dbConnection.createStatement();
		String query = this.adjustHsqlQuery(sql.getSql());

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTriggerSource()", "Using query=\n" + query);
		}
		
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		ResultSet rs = null;
		try
		{
			// for some DBMS (e.g. SQL Server)
			// we need to run a exec which might not work 
			// when using executeQuery() (depending on the JDBC driver)
			stmt.execute(query);
			rs = stmt.getResultSet();
			
			if (rs != null)
			{
				int colCount = rs.getMetaData().getColumnCount();
				while (rs.next())
				{
					for (int i=1; i <= colCount; i++)
					{
						result.append(rs.getString(i));
					}
				}
			}
			String warn = SqlUtil.getWarnings(this.dbConnection, stmt, true);
			if (warn != null && result.length() > 0) result.append(nl + nl);
			result.append(warn);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DbMetadata.getTriggerSource()", "Error reading trigger source", e);
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
			result.append(ExceptionUtil.getDisplay(e));
			SqlUtil.closeAll(rs, stmt);
			return result.toString();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		boolean replaceNL = Settings.getInstance().getBoolProperty("workbench.db." + getDbId() + ".replacenl.triggersource", false);

		String source = result.toString();
		if (replaceNL)
		{
			source = StringUtil.replace(source, "\\n", nl);
		}
		return source;
	}

	/** Returns the list of schemas as returned by DatabaseMetadata.getSchemas()
	 * @return List
	 */
	public List getSchemas()
	{
		ArrayList result = new ArrayList();
		ResultSet rs = null;
		try
		{
			rs = this.metaData.getSchemas();
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
		}
		catch (Exception e)
		{
        LogMgr.logWarning("DbMetadata.getSchemas()", "Error retrieving schemas: " + e.getMessage(), null);
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		if (this.isOracle)
		{
			result.add("PUBLIC");
			Collections.sort(result);
		}
		return result;
	}

	private boolean isIndexType(String type)
	{
		if (type == null) return false;
		return (type.indexOf("INDEX") > -1);
	}

	private boolean hideIndexes()
	{
		return (isPostgres && Settings.getInstance().getBoolProperty("workbench.db.postgres.hideindex", true));
	}

	public Collection getTableTypes()
	{
		TreeSet result = new TreeSet();
		ResultSet rs = null;
		boolean hideIndexes = hideIndexes();

		try
		{
			rs = this.metaData.getTableTypes();
			while (rs.next())
			{
				String type = rs.getString(1);
				if (type == null) continue;
				// for some reason oracle sometimes returns
				// the types padded to a fixed length. I'm assuming
				// it doesn't harm for other DBMS as well to
				// trim the returned value...
				type = type.trim();
				if (hideIndexes && isIndexType(type)) continue;
				result.add(type);
			}
			String additional = Settings.getInstance().getProperty("workbench.db." + this.getDbId() + ".additional.tabletypes",null);
			List addTypes = StringUtil.stringToList(additional, ",", true, true);
			result.addAll(addTypes);
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableTypes()", "Error retrieving table types", e);
		}
		finally
		{
			try { rs.close(); }	 catch (Throwable e) {}
		}
		return result;
	}

	public String getSchemaTerm() { return this.schemaTerm; }
	public String getCatalogTerm() { return this.catalogTerm; }

	public static final int COLUMN_IDX_FK_DEF_FK_NAME = 0;
	public static final int COLUMN_IDX_FK_DEF_COLUMN_NAME = 1;
	public static final int COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME = 2;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE = 3;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE = 4;
	public static final int COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE = 5;
	public static final int COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE = 6;

	public DataStore getExportedKeys(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		return getRawKeyList(aCatalog, aSchema, aTable, true);
	}

	public DataStore getImportedKeys(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		return getRawKeyList(aCatalog, aSchema, aTable, false);
	}

	private DataStore getRawKeyList(String aCatalog, String aSchema, String aTable, boolean exported)
		throws SQLException
	{
		aCatalog = this.adjustObjectnameCase(aCatalog);
		aSchema = this.adjustObjectnameCase(aSchema);
		aTable = this.adjustObjectnameCase(aTable);
		ResultSet rs;
		if (exported)
			rs = this.metaData.getExportedKeys(aCatalog, aSchema, aTable);
		else
			rs = this.metaData.getImportedKeys(aCatalog, aSchema, aTable);

		DataStore ds = new DataStore(rs, false);
		try
		{
			while (rs.next())
			{
				int row = ds.addRow();
				ds.setValue(row, 0, rs.getString(1));
				ds.setValue(row, 1, rs.getString(2));
				ds.setValue(row, 2, rs.getString(3));
				ds.setValue(row, 3, rs.getString(4));
				ds.setValue(row, 4, rs.getString(5));
				ds.setValue(row, 5, rs.getString(6));
				ds.setValue(row, 6, rs.getString(7));
				ds.setValue(row, 7, rs.getString(8));
				ds.setValue(row, 8, new Integer(rs.getInt(9)));
				ds.setValue(row, 9, new Integer(rs.getInt(10)));
				ds.setValue(row, 10, rs.getString(11));
				String fk_name = this.fixFKName(rs.getString(12));
				ds.setValue(row, 11, fk_name);
				ds.setValue(row, 12, rs.getString(13));
				ds.setValue(row, 13, new Integer(rs.getInt(14)));
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

	/**
	 *	Works around a bug in Postgres' JDBC driver.
	 *	For Postgres strips everything after \000 for any
	 *  other DBMS the given name is returned without change
	 */
	private String fixFKName(String aName)
	{
		if (aName == null) return null;
		if (!this.isPostgres) return aName;
		if (aName.indexOf("\\000") > -1)
		{
			// the Postgres JDBC driver seems to have a bug here,
			// because it appends the whole FK information to the fk name!
			// the actual FK name ends at the first \000
			return aName.substring(0, aName.indexOf("\\000"));
		}
		return aName;
	}

	public DataStore getForeignKeys(TableIdentifier table, boolean includeNumericRuleValue)
	{
		DataStore ds = this.getKeyList(table, true, includeNumericRuleValue);
		return ds;
	}

	public DataStore getReferencedBy(TableIdentifier table)
	{
		DataStore ds = this.getKeyList(table, false, false);
		return ds;
	}

	private DataStore getKeyList(TableIdentifier tableId, boolean getOwnFk, boolean includeNumericRuleValue)
	{
		String cols[];
		String refColName;

		if (getOwnFk)
		{
			refColName = "REFERENCES";
		}
		else
		{
			refColName = "REFERENCED BY";
		}
		int types[];
		int sizes[];

		if (includeNumericRuleValue)
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE", "UPDATE_RULE_VALUE", "DELETE_RULE_VALUE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER};
			sizes = new int[] {25, 10, 30, 12, 12, 1, 1};
		}
		else
		{
			cols = new String[] { "FK_NAME", "COLUMN", refColName , "UPDATE_RULE", "DELETE_RULE"};
			types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
			sizes = new int[] {25, 10, 30, 12, 12};
		}
		DataStore ds = new DataStore(cols, types, sizes);
		ResultSet rs = null;

		try
		{
			TableIdentifier tbl = tableId.createCopy();
			tbl.adjustCase(this.dbConnection);
			
			int tableCol;
			int fkNameCol;
			int colCol;
			int fkColCol;
			int deleteActionCol;
			int updateActionCol;
			int schemaCol;

			if (getOwnFk)
			{
				rs = this.metaData.getImportedKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
				tableCol = 3;
				schemaCol = 2;
				fkNameCol = 12;
				colCol = 8;
				fkColCol = 4;
				updateActionCol = 10;
				deleteActionCol = 11;
			}
			else
			{
				rs = this.metaData.getExportedKeys(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
				tableCol = 7;
				schemaCol = 6;
				fkNameCol = 12;
				colCol = 4;
				fkColCol = 8;
				updateActionCol = 10;
				deleteActionCol = 11;
			}
			while (rs.next())
			{
				String table = rs.getString(tableCol);
				String fk_col = rs.getString(fkColCol);
				String col = rs.getString(colCol);
				String fk_name = this.fixFKName(rs.getString(fkNameCol));
				String schema = rs.getString(schemaCol);
				if (schema != null && !schema.equals(this.getCurrentSchema()))
				{
					table = schema + "." + table;
				}
				int updateAction = rs.getInt(updateActionCol);
				String updActionDesc = this.getRuleTypeDisplay(updateAction);
				int deleteAction = rs.getInt(deleteActionCol);
				String delActionDesc = this.getRuleTypeDisplay(deleteAction);
				int row = ds.addRow();
				ds.setValue(row, COLUMN_IDX_FK_DEF_FK_NAME, fk_name);
				ds.setValue(row, COLUMN_IDX_FK_DEF_COLUMN_NAME, col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME, table + "." + fk_col);
				ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE, updActionDesc);
				ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE, delActionDesc);
				if (includeNumericRuleValue)
				{
					ds.setValue(row, COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, new Integer(deleteAction));
					ds.setValue(row, COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, new Integer(updateAction));
				}
			}
		}
		catch (Exception e)
		{
			ds.reset();
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return ds;
	}

	/**
	 *	Translates the numberic constants of DatabaseMetaData for trigger rules
	 *	into text (e.g DatabaseMetaData.importedKeyNoAction --> NO ACTION)
	 *
	 *	@param aRule the numeric value for a rule as defined by DatabaseMetaData.importedKeyXXXX constants
	 *	@return String
	 */
	public String getRuleTypeDisplay(int aRule)
	{
		String display = this.getRuleAction(aRule);
		if (display != null) return display;
		switch (aRule)
		{
			case DatabaseMetaData.importedKeyNoAction:
				return "NO ACTION";
			case DatabaseMetaData.importedKeyRestrict:
				return "RESTRICT";
			case DatabaseMetaData.importedKeySetNull:
				return "SET NULL";
			case DatabaseMetaData.importedKeyCascade:
				return "CASCADE";
			case DatabaseMetaData.importedKeySetDefault:
				return "SET DEFAULT";
			case DatabaseMetaData.importedKeyInitiallyDeferred:
				return "INITIALLY DEFERRED";
			case DatabaseMetaData.importedKeyInitiallyImmediate:
				return "INITIALLY IMMEDIATE";
			case DatabaseMetaData.importedKeyNotDeferrable:
				return "NOT DEFERRABLE";
			default:
				return StringUtil.EMPTY_STRING;
		}
	}

	private String getRuleAction(int rule)
	{
		String key;
		switch (rule)
		{
			case DatabaseMetaData.importedKeyNoAction:
				key = "workbench.sql.fkrule.noaction";
				break;
			case DatabaseMetaData.importedKeyRestrict:
				key = "workbench.sql.fkrule.restrict";
				break;
			case DatabaseMetaData.importedKeySetNull:
				key = "workbench.sql.fkrule.setnull";
				break;
			case DatabaseMetaData.importedKeyCascade:
				key = "workbench.sql.fkrule.cascade";
				break;
			case DatabaseMetaData.importedKeySetDefault:
				key = "workbench.sql.fkrule.setdefault";
				break;
			case DatabaseMetaData.importedKeyInitiallyDeferred:
				key = "workbench.sql.fkrule.initiallydeferred";
				break;
			case DatabaseMetaData.importedKeyInitiallyImmediate:
				key = "workbench.sql.fkrule.initiallyimmediate";
				break;
			case DatabaseMetaData.importedKeyNotDeferrable:
				key = "workbench.sql.fkrule.notdeferrable";
				break;
			default:
				return null;
		}
		key = key + "." + this.getDbId();
		return Settings.getInstance().getProperty(key, null);
	}

	private String getPkIndexName(DataStore anIndexDef)
	{
		if (anIndexDef == null) return null;
		int count = anIndexDef.getRowCount();

		String name = null;
		for (int row = 0; row < count; row ++)
		{
			String is_pk = anIndexDef.getValue(row, COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG).toString();
			if ("YES".equalsIgnoreCase(is_pk))
			{
				name = anIndexDef.getValue(row, COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME).toString();
				break;
			}
		}
		return name;
	}

	public String getSequenceSource(String fullName)
	{
		String sequenceName = fullName;
		String schema = null;

		int pos = fullName.indexOf('.');
		if (pos > 0)
		{
			sequenceName = fullName.substring(pos);
			schema = fullName.substring(0, pos - 1);
		}
		return this.getSequenceSource(null, schema, sequenceName);
	}

	public String getSequenceSource(String aCatalog, String aSchema, String aSequence)
	{
		if (this.sequenceReader != null)
		{
			if (aSchema == null)
			{
				aSchema = this.getCurrentSchema();
			}
			return this.sequenceReader.getSequenceSource(aSchema, aSequence);
		}
		return StringUtil.EMPTY_STRING;
	}

	public boolean isViewType(String type)
	{
		if (type == null) return false;
		if (type.toUpperCase().indexOf("VIEW") > -1) return true;
		String viewTypes = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".additional.viewtypes", "view").toLowerCase();
		List types = StringUtil.stringToList(viewTypes, ",", true, true, false);
		return (types.contains(type.toLowerCase()));
	}
	
	public boolean isSynonymType(String type)
	{
		if (type == null) return false;
		String synTypes = Settings.getInstance().getProperty("workbench.db." + getDbId() + ".synonymtypes", "synonym").toLowerCase();
		List types = StringUtil.stringToList(synTypes, ",", true, true, false);
		return (types.contains(type.toLowerCase()));
	}
	
	/**
	 *	Return the underlying table of a synonym.
	 *
	 *	@return the table to which the synonym points.
	 */
	public TableIdentifier getSynonymTable(String anOwner, String aSynonym)
	{
		if (this.synonymReader == null) return null;
		TableIdentifier id = null;
		try
		{
			id = this.synonymReader.getSynonymTable(this.dbConnection.getSqlConnection(), anOwner, aSynonym);
			if (id == null && this.isOracle)
			{
				id = this.synonymReader.getSynonymTable(this.dbConnection.getSqlConnection(), "PUBLIC", aSynonym);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getSynonymTable()", "Could not retrieve table for synonym", e);
		}
		return id;
	}

	/**
	 *	Return the SQL statement to recreate the given synonym.
	 *	@return the SQL to create the synonym.
	 */
	public String getSynonymSource(String anOwner, String aSynonym)
	{
		if (this.synonymReader == null) return StringUtil.EMPTY_STRING;
		String result = null;

		try
		{
			result = this.synonymReader.getSynonymSource(this.dbConnection.getSqlConnection(), anOwner, aSynonym);
		}
		catch (Exception e)
		{
			result = StringUtil.EMPTY_STRING;
		}

		return result;
	}

	/** 	
   * Return the SQL statement to re-create the given table. (in the dialect for the
	 * current DBMS)
   *
	 * @return the SQL statement to create the given table.
	 * @param table the table for which the source should be retrievedcatalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param includeDrop If true, a DROP TABLE statement will be included in the generated SQL script.
	 * @param includeFk if true, the foreign key constraints will be added after the CREATE TABLE
	 * @throws SQLException
	 */
	public String getTableSource(TableIdentifier table, boolean includeDrop, boolean includeFk)
		throws SQLException
	{
		DataStore tableDef = this.getTableDefinition(table, !table.getNeverAdjustCase());
		ColumnIdentifier[] cols = createColumnIdentifiers(tableDef);
		DataStore index = this.getTableIndexInformation(table);
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		DataStore fkDef = null;
		if (includeFk) fkDef = this.getForeignKeys(tbl, false);
		String source = this.getTableSource(table, cols, index, fkDef, includeDrop, null, includeFk);
		return source;
	}

	public String getTableSource(TableIdentifier table, DataStore aTableDef, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop)
	{
		ColumnIdentifier[] cols = createColumnIdentifiers(aTableDef);
		return getTableSource(table, cols, aIndexDef, aFkDef, includeDrop, null);
	}

	public String getTableSource(TableIdentifier table, ColumnIdentifier[] columns, String tableNameToUse)
	{
		return getTableSource(table, columns, null, null, false, tableNameToUse, true);
	}

	public String getTableSource(TableIdentifier table, ColumnIdentifier[] columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse)
	{
		return getTableSource(table, columns, aIndexDef, aFkDef, includeDrop, tableNameToUse, true);
	}
	
	public String getTableSource(TableIdentifier table, ColumnIdentifier[] columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse, boolean includeFk)
	{
		if (columns == null || columns.length == 0) return StringUtil.EMPTY_STRING;

		StrBuffer result = new StrBuffer();

		Map columnConstraints = this.getColumnConstraints(table);

		result.append(generateCreateObject(includeDrop, "TABLE", (tableNameToUse == null ? table.getTableName() : tableNameToUse)));
		result.append("\n(\n");
		int count = columns.length;
		//StringBuilder pkCols = new StringBuilder(1000);
		List pkCols = new LinkedList();
		int maxColLength = 0;
		int maxTypeLength = 0;

		// calculate the longest column name, so that the display can be formatted
		for (int i=0; i < count; i++)
		{
			String colName = quoteObjectname(columns[i].getColumnName());
			String type = columns[i].getDbmsType();
			maxColLength = Math.max(maxColLength, colName.length());
			maxTypeLength = Math.max(maxTypeLength, type.length());
		}
		maxColLength++;
		maxTypeLength++;
		
		// Some RDBMS require the "DEFAULT" clause before the [NOT] NULL clause
		boolean defaultBeforeNull = Settings.getInstance().getBoolProperty("workbench.db.defaultbeforenull." + this.getDbId(), false);//this.isOracle || this.isFirebird || this.isIngres;
		String nullKeyword = Settings.getInstance().getProperty("workbench.db.nullkeyword." + getDbId(), "NULL");
		boolean includeCommentInTableSource = Settings.getInstance().getBoolProperty("workbench.db.colcommentinline." + this.getDbId(), false);
		
		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		
		for (int i=0; i < count; i++)
		{
			String colName = columns[i].getColumnName();
			String quotedColName = quoteObjectname(colName);
			String type = columns[i].getDbmsType();
			String def = columns[i].getDefaultValue();

			result.append("   ");
			result.append(quotedColName);
			if (columns[i].isPkColumn() && (!this.isFirstSql || this.isFirstSql && !type.equals("sequence")))
			{
				pkCols.add(colName.trim());
			}
			
			for (int k=0; k < maxColLength - quotedColName.length(); k++) result.append(' ');
			result.append(type);
			
			// Check if any additional keywords are coming after
			// the datatype. If yes, we fill the line with spaces
			// to align the keywords properly
			if ( !StringUtil.isEmptyString(def) || 
				   (!columns[i].isNullable()) ||
				   (columns[i].isNullable() && this.useNullKeyword)
					)
			{
				for (int k=0; k < maxTypeLength - type.length(); k++) result.append(' ');
			}
			

			if (defaultBeforeNull && !StringUtil.isEmptyString(def))
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}

			if (this.isFirstSql && "sequence".equals(type))
			{
				// with FirstSQL a column of type "sequence" is always the primary key
				result.append(" PRIMARY KEY");
			}
			else if (columns[i].isNullable())
			{
				if (this.useNullKeyword)
				{
					result.append(' ');
					result.append(nullKeyword);
				}
			}
			else
			{
				result.append(" NOT NULL");
			}

			if (!defaultBeforeNull && !StringUtil.isEmptyString(def))
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}

			String constraint = (String)columnConstraints.get(colName);
			if (constraint != null && constraint.length() > 0)
			{
				result.append(' ');
				result.append(constraint);
			}
			
			if (includeCommentInTableSource && !StringUtil.isEmptyString(columns[i].getComment()))
			{
				result.append(" COMMENT '");
				result.append(columns[i].getComment());
				result.append('\'');
			}
			
			if (i < count - 1) result.append(',');
			result.append(lineEnding);
		}

		String cons = this.getTableConstraints(table, "   ");
		if (cons != null && cons.length() > 0)
		{
			result.append("   ,");
			result.append(cons);
			result.append(lineEnding);
		}
		String realTable = (tableNameToUse == null ? table.getTableName() : tableNameToUse);

		if (this.createInlineConstraints && pkCols.size() > 0)
		{
			result.append(lineEnding + "   ,PRIMARY KEY (");
			result.append(StringUtil.listToString(pkCols, ','));
			result.append(")" + lineEnding);

			if (includeFk)
			{
				StringBuilder fk = this.getFkSource(table.getTableName(), aFkDef, tableNameToUse);
				if (fk.length() > 0)
				{
					result.append(fk);
				}
			}
		}

		result.append(");" + lineEnding); // end of CREATE TABLE
		
		
		if (!this.createInlineConstraints && pkCols.size() > 0)
		{
			String name = this.getPkIndexName(aIndexDef);
			StringBuilder pkSource = getPkSource(realTable, pkCols, name);
			result.append(pkSource);
			result.append(lineEnding);
			result.append(lineEnding);
		}
		StringBuilder indexSource = this.indexReader.getIndexSource(table, aIndexDef, tableNameToUse);
		result.append(indexSource);
		if (!this.createInlineConstraints && includeFk) result.append(this.getFkSource(table.getTableName(), aFkDef, tableNameToUse));

		String comments = this.getTableCommentSql(table);
		if (comments != null && comments.length() > 0)
		{
			result.append(lineEnding);
			result.append(comments);
			result.append(lineEnding);
		}

		comments = this.getTableColumnCommentsSql(table, columns);
		if (comments != null && comments.length() > 0)
		{
			result.append(lineEnding);
			result.append(comments);
			result.append(lineEnding);
		}

		StrBuffer grants = this.getTableGrantSource(table);
		if (grants.length() > 0)
		{
			result.append(grants);
		}
		
		if (this.ddlNeedsCommit)
		{
			result.append(lineEnding);
			result.append("COMMIT;");
			result.append(lineEnding);
		}

		return result.toString();
	}

	private boolean isSystemConstraintName(String name)
	{
		if (name == null) return false;
		String regex = Settings.getInstance().getProperty("workbench.db." + this.getDbId() + ".constraints.systemname", null);
		if (StringUtil.isEmptyString(regex)) return false;
		try
		{
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(name);
			return m.matches();
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.isSystemConstraintName()", "Error in regex", e);
		}
		return false;
	}
	
	public StringBuilder getPkSource(String tablename, List pkCols, String pkName)
	{
		String template = metaSqlMgr.getPrimaryKeyTemplate();
		StringBuilder result = new StringBuilder();
		if (StringUtil.isEmptyString(template)) return result;
		
		template = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tablename);
		template = StringUtil.replace(template, MetaDataSqlManager.COLUMNLIST_PLACEHOLDER, StringUtil.listToString(pkCols, ','));
		
		if (pkName == null)
		{
			if (Settings.getInstance().getAutoGeneratePKName()) pkName = "pk_" + tablename.toLowerCase();
		}
		else if (isSystemConstraintName(pkName))
		{
			pkName = null;
		}
		
		if (isKeyword(pkName)) pkName = "\"" + pkName + "\"";
		if (StringUtil.isEmptyString(pkName)) 
		{
			pkName = ""; // remove placeholder if no name is available
			template = StringUtil.replace(template, " CONSTRAINT ", ""); // remove CONSTRAINT KEYWORD if not name is available
		}

		template = StringUtil.replace(template, MetaDataSqlManager.PK_NAME_PLACEHOLDER, pkName);
		result.append(template);
		result.append(';');
		return result;
	}
	
	/**
	 * Return constraints defined for each column in the given table.
	 * @param table The table to check
	 * @return A Map with columns and their constraints. The keys to the Map are column names
	 * The value is the SQL source for the column. The actual retrieval is delegated to a {@link ConstraintReader}
	 * @see ConstraintReader#getColumnConstraints(java.sql.Connection, TableIdentifier)
	 */
	public Map getColumnConstraints(TableIdentifier table)
	{
		Map columnConstraints = Collections.EMPTY_MAP;
		if (this.constraintReader != null)
		{
			try
			{
				columnConstraints = this.constraintReader.getColumnConstraints(this.dbConnection.getSqlConnection(), table);
			}
			catch (Exception e)
			{
				if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
				columnConstraints = Collections.EMPTY_MAP;
			}
		}
		return columnConstraints;
	}

	/**
	 * Return the SQL source for check constraints defined for the table. This is
	 * delegated to a {@link ConstraintReader}
	 * @return A String with the table constraints. If no constrains exist, a null String is returned
	 * @param tbl The table to check
	 * @param indent A String defining the indention for the source code
	 */
	public String getTableConstraints(TableIdentifier tbl, String indent)
	{
		if (this.constraintReader == null) return null;
		String cons = null;
		try
		{
			cons = this.constraintReader.getTableConstraints(dbConnection.getSqlConnection(), tbl, indent);
		}
		catch (SQLException e)
		{
			if (this.isPostgres) try { this.dbConnection.rollback(); } catch (Throwable th) {}
			cons = null;
		}
		return cons;
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given columns.
	 * The syntax to be used, can be configured in the ColumnCommentStatements.xml file.
	 */
	public String getTableColumnCommentsSql(TableIdentifier table, ColumnIdentifier[] columns)
	{
		String columnStatement = metaSqlMgr.getColumnCommentSql();
		if (columnStatement == null || columnStatement.trim().length() == 0) return null;
		StringBuilder result = new StringBuilder(500);
		int cols = columns.length;
		for (int i=0; i < cols; i ++)
		{
			String column = columns[i].getColumnName();
			String remark = columns[i].getComment();
			if (column == null || remark == null || remark.trim().length() == 0) continue;
			String comment = columnStatement.replaceAll(MetaDataSqlManager.COMMENT_TABLE_PLACEHOLDER, table.getTableName());
			comment = comment.replaceAll(MetaDataSqlManager.COMMENT_COLUMN_PLACEHOLDER, column);
			comment = comment.replaceAll(MetaDataSqlManager.COMMENT_PLACEHOLDER, remark.replaceAll("'", "''"));
			result.append(comment);
			result.append("\n");
		}
		return result.toString();
	}

	/**
	 * Return the SQL that is needed to re-create the comment on the given table.
	 * The syntax to be used, can be configured in the TableCommentStatements.xml file.
	 */
	public String getTableCommentSql(TableIdentifier table)
	{
		String commentStatement = metaSqlMgr.getTableCommentSql();
		if (commentStatement == null || commentStatement.trim().length() == 0) return null;

		String comment = this.getTableComment(table);
		String result = null;
		if (comment != null && comment.trim().length() > 0)
		{
			result = commentStatement.replaceAll(MetaDataSqlManager.COMMENT_TABLE_PLACEHOLDER, table.getTableName());
			result = result.replaceAll(MetaDataSqlManager.COMMENT_PLACEHOLDER, comment);
		}
		return result;
	}

	public String getTableComment(TableIdentifier tbl)
	{
		TableIdentifier table = tbl.createCopy();
		table.adjustCase(this.dbConnection);
		ResultSet rs = null;
		String result = null;
		try
		{
			rs = this.metaData.getTables(table.getCatalog(), table.getSchema(), table.getTableName(), null);
			if (rs.next())
			{
				result = rs.getString("REMARKS");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableComment()", "Error retrieving comment for table " + table.getTableExpression(), e);
			result = null;
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		return result;
	}

	public StringBuilder getFkSource(TableIdentifier table)
	{
		DataStore fkDef = this.getForeignKeys(table, false);
		return getFkSource(table.getTableName(), fkDef, null);
	}
	
	/**
	 *	Return a SQL script to re-create the Foreign key definition for the given table.
	 *
	 *	@param aTable the tablename for which the foreign keys should be created
	 *  @param aFkDef a DataStore with the FK definition as returned by #getForeignKeys()
	 *
	 *	@return a SQL statement to add the foreign key definitions to the given table
	 */
	public StringBuilder getFkSource(String aTable, DataStore aFkDef, String tableNameToUse)
	{
		if (aFkDef == null) return StringUtil.emptyBuffer();
		int count = aFkDef.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();

		String template = metaSqlMgr.getForeignKeyTemplate(this.createInlineConstraints);

		// collects all columns from the base table mapped to the
		// defining foreign key constraing.
		// The fk name is the key.
		// to the hashtable. The entry will be a HashSet containing the column names
		// this ensures that each column will only be used once per fk definition
		// (the postgres driver returns some columns twice!)
		HashMap fkCols = new HashMap();

		// this hashmap contains the columns of the referenced table
		HashMap fkTarget = new HashMap();

		HashMap fks = new HashMap();
		HashMap updateRules = new HashMap();
		HashMap deleteRules = new HashMap();

		String name;
		String col;
		String fkCol;
		String updateRule;
		String deleteRule;
		List colList;
		//String entry;

		for (int i=0; i < count; i++)
		{
			//"FK_NAME", "COLUMN_NAME", "REFERENCES"};
			name = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_FK_NAME);
			col = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_COLUMN_NAME);
			fkCol = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
			updateRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_UPDATE_RULE);
			deleteRule = aFkDef.getValueAsString(i, COLUMN_IDX_FK_DEF_DELETE_RULE);
			colList = (List)fkCols.get(name);
			if (colList == null)
			{
				colList = new LinkedList();
				fkCols.put(name, colList);
			}
			colList.add(col);
			updateRules.put(name, updateRule);
			deleteRules.put(name, deleteRule);

			colList = (List)fkTarget.get(name);
			if (colList == null)
			{
				colList = new LinkedList();
				fkTarget.put(name, colList);
			}
			colList.add(fkCol);
		}

		// now put the real statements together
		Iterator names = fkCols.entrySet().iterator();
		while (names.hasNext())
		{
			Map.Entry mapentry = (Map.Entry)names.next();
			name = (String)mapentry.getKey();
			colList = (List)mapentry.getValue();

			String stmt = (String)fks.get(name);
			if (stmt == null)
			{
				// first time we hit this FK definition in this loop
				stmt = template;
			}
			String entry = null;
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? aTable : tableNameToUse));
			
			if (this.isSystemConstraintName(name))
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, "");
				stmt = StringUtil.replace(stmt, " CONSTRAINT ", "");
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, name);
			}
			
			entry = StringUtil.listToString(colList, ',');
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.COLUMNLIST_PLACEHOLDER, entry);
			String rule = (String)updateRules.get(name);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_UPDATE_RULE, " ON UPDATE " + rule);
			rule = (String)deleteRules.get(name);
			if (this.isOracle())
			{
				// Oracle does not allow ON DELETE RESTRICT, so we'll have to
				// remove the placeholder completely
				if ("restrict".equalsIgnoreCase(rule))
				{
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, StringUtil.EMPTY_STRING);
				}
				else
				{
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, " ON DELETE " + rule);
				}
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, " ON DELETE " + rule);
			}

			colList = (List)fkTarget.get(name);
			if (colList == null)
			{
				LogMgr.logError("DbMetadata.getFkSource()", "Retrieved a null list for constraing [" + name + "] but should contain a list for table [" + aTable + "]",null);
				continue;
			}
			
			Iterator itr = colList.iterator();
			StringBuilder colListBuffer = new StringBuilder(30);
			String targetTable = null;
			boolean first = true;
			//while (tok.hasMoreTokens())
			while (itr.hasNext())
			{
				col = (String)itr.next();//tok.nextToken();
				int pos = col.lastIndexOf('.');
				if (targetTable == null)
				{
					targetTable = col.substring(0, pos);
				}
				if (!first)
				{
					colListBuffer.append(',');
				}
				else
				{
					first = false;
				}
				colListBuffer.append(col.substring(pos + 1));
			}
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_TARGET_TABLE_PLACEHOLDER, targetTable);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_TARGET_COLUMNS_PLACEHOLDER, colListBuffer.toString());
			fks.put(name, stmt.trim());
		}
		StringBuilder fk = new StringBuilder();

		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
		Iterator values = fks.values().iterator();
		while (values.hasNext())
		{
			if (this.createInlineConstraints)
			{
				fk.append("   ,");
				fk.append((String)values.next());
				fk.append(nl);
			}
			else
			{
				fk.append((String)values.next());
				fk.append(';');
				fk.append(nl);fk.append(nl);
			}
		}
		return fk;
	}

	/**
	 * 	Build the SQL statement to create an Index on the given table.
	 * 	@param aTable - The table name for which the index should be constructed
	 * 	@param indexName - The name of the Index
	 * 	@param unique - Should the index be unique
	 *  @param columnList - The columns that should build the index
	 */
	public String buildIndexSource(TableIdentifier aTable, String indexName, boolean unique, String[] columnList)
	{
		return this.indexReader.buildCreateIndexSql(aTable, indexName, unique, columnList);
	}

	/**	The column index for the DataStore returned by getTableGrants() which contains the object's name */
	public static final int COLUMN_IDX_TABLE_GRANTS_OBJECT_NAME = 0;
	/**	The column index for the DataStore returned by getTableGrants() which contains the name of the user which granted the access (GRANTOR) */
	public static final int COLUMN_IDX_TABLE_GRANTS_GRANTOR = 1;
	/**	The column index for the DataStore returned by getTableGrants() which contains the name of the user to which the privilege was granted */
	public static final int COLUMN_IDX_TABLE_GRANTS_GRANTEE = 2;
	/**	The column index for the DataStore returned by getTableGrants() which contains the privilege's name (SELECT, UPDATE etc) */
	public static final int COLUMN_IDX_TABLE_GRANTS_PRIV = 3;
	/** The column index for th DataStore returned by getTableGrants() which contains the information if the GRANTEE may grant the privilege to other users */
	public static final int COLUMN_IDX_TABLE_GRANTS_GRANTABLE = 4;

	/**
	 *	Return a String to recreate the GRANTs given for the passed table.
	 *
	 *	Some JDBC drivers return all GRANT privileges separately even if the original
	 *  GRANT was a GRANT ALL ON object TO user.
	 *
	 *	The COLUMN_IDX_TABLE_GRANTS_xxx constants should be used to access the DataStore's columns.
	 *
	 *	@return a DataStore which contains the grant information.
	 */
	public DataStore getTableGrants(TableIdentifier table)
	{
		String[] columns = new String[] { "TABLENAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "GRANTABLE" };
		int[] colTypes = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };
		DataStore result = new DataStore(columns, colTypes);
		ResultSet rs = null;
		try
		{
			TableIdentifier tbl = table.createCopy();
			tbl.adjustCase(this.dbConnection);
			rs = this.metaData.getTablePrivileges(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
			while (rs.next())
			{
				int row = result.addRow();
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_OBJECT_NAME, rs.getString(3));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_GRANTOR, rs.getString(4));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_GRANTEE, rs.getString(5));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_PRIV, rs.getString(6));
				result.setValue(row, COLUMN_IDX_TABLE_GRANTS_GRANTABLE, rs.getString(7));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DbMetadata.getTableGrants()", "Error when retrieving table privileges",e);
			result.reset();
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
		return result;
	}

	/**
	 *	Creates an SQL Statement which can be used to re-create the GRANTs on the
	 *  given table.
	 *
	 *	@return SQL script to GRANT access to the table.
	 */
	public StrBuffer getTableGrantSource(TableIdentifier table)
	{
		DataStore ds = this.getTableGrants(table);
		StrBuffer result = new StrBuffer(200);
		int count = ds.getRowCount();

		// as several grants to several users can be made, we need to collect them
		// first, in order to be able to build the complete statements
		HashMap grants = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			String grantee = ds.getValueAsString(i, COLUMN_IDX_TABLE_GRANTS_GRANTEE);
			String priv = ds.getValueAsString(i, COLUMN_IDX_TABLE_GRANTS_PRIV);
			if (priv == null) continue;
			StrBuffer privs;
			if (!grants.containsKey(grantee))
			{
				privs = new StrBuffer(priv.trim());
				grants.put(grantee, privs);
			}
			else
			{
				privs = (StrBuffer)grants.get(grantee);
				if (privs == null) privs = new StrBuffer();
				privs.append(", ");
				privs.append(priv.trim());
			}
		}
		Set entries = grants.entrySet();
		Iterator itr = entries.iterator();
		String user = dbConnection.getCurrentUser();
		while (itr.hasNext())
		{
			Entry entry = (Entry)itr.next();
			String grantee = (String)entry.getKey();
			if (user.equalsIgnoreCase(grantee)) continue;
			StrBuffer privs = (StrBuffer)entry.getValue();
			result.append("GRANT ");
			result.append(privs);
			result.append(" ON ");
			result.append(table.getTableExpression(this.dbConnection));
			result.append(" TO ");
			result.append(grantee);
			result.append(";\n");
		}
		return result;
	}

	/**
	 * Returns the errors available for the given object and type. This call
	 * is delegated to the available {@link ErrorInformationReader}
	 * @return extended error information if the current DBMS is Oracle. An empty string otherwise.
	 * @see ErrorInformationReader
	 */
  public String getExtendedErrorInfo(String schema, String objectName, String objectType)
  {
    if (this.errorInfoReader == null) return StringUtil.EMPTY_STRING;
		return this.errorInfoReader.getErrorInfo(schema, objectName, objectType);
  }

	/**
	 * With v1.8 of HSQLDB the tables that list table and view
	 * information, are stored in the INFORMATION_SCHEMA schema.
	 * Although the table names are the same, prior to 1.8 you
	 * cannot use the schema, so it needs to be removed
	 */
	private String adjustHsqlQuery(String query)
	{
		if (!this.isHsql) return query;
		if (this.dbVersion.startsWith("1.8")) return query;

		Pattern p = Pattern.compile("\\sINFORMATION_SCHEMA\\.", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(query);
		return m.replaceAll(" ");
	}

	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		String prop = evt.getPropertyName();
		if ("workbench.sql.enable_dbms_output".equals(prop))
		{
			boolean enable = Settings.getInstance().getEnableDbmsOutput();
			if (enable)
			{
				this.enableOutput();
			}
			else
			{
				this.disableOutput();
			}
		}
	}

}
