/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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
package workbench.db;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbProperties;

/**
 *
 * @author Thomas Kellerer
 */
public class IniProfileStorage
  implements ProfileStorage
{
  public static final String EXTENSION = "properties";
  public static final String DEFAULT_FILE_NAME = "wb-profiles." + EXTENSION;

  private static final String PROP_PREFIX = "profile";
  private static final String PROP_NAME = ".name";
  private static final String PROP_URL = ".url";
  private static final String PROP_PWD = ".pwd";
  private static final String PROP_USERNAME = ".username";
  private static final String PROP_DRIVERNAME = ".drivername";
  private static final String PROP_DRIVERCLASS = ".driverclass";
  private static final String PROP_AUTOCOMMMIT = ".autocommmit";
  private static final String PROP_FETCHSIZE = ".fetchsize";
  private static final String PROP_ALT_DELIMITER = ".alternate.delimiter";
  private static final String PROP_STORE_PWD = ".store.pwd";
  private static final String PROP_ROLLBACK_DISCONNECT = ".rollback.disconnect";

  private static final String PROP_GROUP = ".group";
  private static final String PROP_WORKSPACE = ".workspace";
  private static final String PROP_ICON = ".icon";
  private static final String PROP_CONNECTION_TIMEOUT = ".connection.timeout";
  private static final String PROP_HIDE_WARNINGS = ".hide.warnings";
  private static final String PROP_REMEMEMBER_SCHEMA = ".rememember.schema";
  private static final String PROP_REMOVE_COMMENTS = ".remove.comments";
  private static final String PROP_INCLUDE_NULL_ON_INSERT = ".include.null.insert";
  private static final String PROP_EMPTY_STRING_IS_NULL = ".empty.string.is.null";
  private static final String PROP_PROMPTUSERNAME = ".prompt.username";
  private static final String PROP_CONFIRM_UPDATES = ".confirm.updates";
  private static final String PROP_PREVENT_NO_WHERE = ".prevent.no.where.clause";
  private static final String PROP_READONLY = ".readonly";
  private static final String PROP_DETECTOPENTRANSACTION = ".detect.open.transaction";
  private static final String PROP_ORACLESYSDBA = ".oracle.sysdba";
  private static final String PROP_TRIMCHARDATA = ".trim.char.data";
  private static final String PROP_IGNOREDROPERRORS = ".ignore.drop.errors";
  private static final String PROP_SEPARATECONNECTION = ".separate.connection";
  private static final String PROP_STORECACHE = ".store.cache";
  private static final String PROP_IDLE_TIME = ".idle.time";
  private static final String PROP_SCRIPT_IDLE = ".script.idle";
  private static final String PROP_SCRIPT_DISCONNECT = ".script.disconnect";
  private static final String PROP_SCRIPT_CONNECT = ".script.connect";
  private static final String PROP_MACROFILE = ".macro.file";
  private static final String PROP_COPY_PROPS = ".copy.props";
  private static final String PROP_CONN_PROPS = ".connection.properties";
  private static final String PROP_INFO_COLOR = ".info.color";
  private static final String PROP_SCHEMA_FILTER = ".schema.filter";
  private static final String PROP_CATALOG_FILTER = ".catalog.filter";

  @Override
  public List<ConnectionProfile> readProfiles(String filename)
  {
    LogMgr.logInfo("IniProfileStorage.readProfiles()", "Loading connection profiles from " + filename);
    WbProperties props = new WbProperties(1);
    BufferedReader reader = null;
    List<ConnectionProfile> profiles = new ArrayList<>(25);

    try
    {
      reader = new BufferedReader(new FileReader(filename));
      props.loadFromReader(reader);
      Set<String> keys = getProfileKeys(props);
      for (String key : keys)
      {
        ConnectionProfile profile = readProfile(key, props);
        if (profile != null)
        {
          profiles.add(profile);
        }
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("IniProfileStorage.readProfiles()", "Could not read profiles from: " + filename, ex);
    }
    finally
    {
      FileUtil.closeQuietely(reader);
    }
    return profiles;
  }

  private ConnectionProfile readProfile(String key, WbProperties props)
  {
    key = "." + key;
    String url = props.getProperty(PROP_PREFIX + key + PROP_URL, null);
    String name = props.getProperty(PROP_PREFIX + key + PROP_NAME, null);
    String driverClass = props.getProperty(PROP_PREFIX + key + PROP_DRIVERCLASS, null);
    String driverName = props.getProperty(PROP_PREFIX + key + PROP_DRIVERNAME, null);
    String group = props.getProperty(PROP_PREFIX + key + PROP_GROUP, null);
    String user = props.getProperty(PROP_PREFIX + key + PROP_USERNAME, null);
    String pwd = props.getProperty(PROP_PREFIX + key + PROP_PWD, null);
    String icon = props.getProperty(PROP_PREFIX + key + PROP_ICON, null);
    String wksp = props.getProperty(PROP_PREFIX + key + PROP_WORKSPACE, null);
    String delimiter = props.getProperty(PROP_PREFIX + key + PROP_ALT_DELIMITER, null);
    String macroFile = props.getProperty(PROP_PREFIX + key + PROP_MACROFILE, null);
    String postConnect = props.getProperty(PROP_PREFIX + key + PROP_SCRIPT_CONNECT, null);
    String preDisconnect = props.getProperty(PROP_PREFIX + key + PROP_SCRIPT_DISCONNECT, null);
    String idleScript = props.getProperty(PROP_PREFIX + key + PROP_SCRIPT_IDLE, null);
    String xmlProps = props.getProperty(PROP_PREFIX + key + PROP_CONN_PROPS, null);
    String colorValue = props.getProperty(PROP_PREFIX + key + PROP_INFO_COLOR, null);

    Properties connProps = toProperties(xmlProps);
    Color color = Settings.stringToColor(colorValue);

    boolean autoCommit = props.getBoolProperty(PROP_PREFIX + key + PROP_AUTOCOMMMIT, false);
    boolean storeCache = props.getBoolProperty(PROP_PREFIX + key + PROP_STORECACHE, false);
    boolean storePwd = props.getBoolProperty(PROP_PREFIX + key + PROP_STORE_PWD, true);
    boolean rollback = props.getBoolProperty(PROP_PREFIX + key + PROP_ROLLBACK_DISCONNECT, false);
    boolean seperateConnection = props.getBoolProperty(PROP_PREFIX + key + PROP_SEPARATECONNECTION, false);
    boolean ignoreDropError = props.getBoolProperty(PROP_PREFIX + key + PROP_IGNOREDROPERRORS, false);
    boolean trimCharData  = props.getBoolProperty(PROP_PREFIX + key + PROP_TRIMCHARDATA, false);
    boolean sysDBA  = props.getBoolProperty(PROP_PREFIX + key + PROP_ORACLESYSDBA, false);
    boolean detectOpen = props.getBoolProperty(PROP_PREFIX + key + PROP_DETECTOPENTRANSACTION, false);
    boolean readonly = props.getBoolProperty(PROP_PREFIX + key + PROP_READONLY, false);
    boolean preventNoWhere = props.getBoolProperty(PROP_PREFIX + key + PROP_PREVENT_NO_WHERE, false);
    boolean confirmUpdates = props.getBoolProperty(PROP_PREFIX + key + PROP_CONFIRM_UPDATES, false);
    boolean promptUsername = props.getBoolProperty(PROP_PREFIX + key + PROP_PROMPTUSERNAME, false);
    boolean emptyStringIsNull = props.getBoolProperty(PROP_PREFIX + key + PROP_EMPTY_STRING_IS_NULL, false);
    boolean includeNullInInsert = props.getBoolProperty(PROP_PREFIX + key + PROP_INCLUDE_NULL_ON_INSERT, true);
    boolean removeComments = props.getBoolProperty(PROP_PREFIX + key + PROP_REMOVE_COMMENTS, false);
    boolean rememberExplorerSchema = props.getBoolProperty(PROP_PREFIX + key + PROP_REMEMEMBER_SCHEMA, false);
    boolean hideWarnings = props.getBoolProperty(PROP_PREFIX + key + PROP_HIDE_WARNINGS, false);
    boolean copyProps = props.getBoolProperty(PROP_PREFIX + key + PROP_COPY_PROPS, false);
    int idleTime = props.getIntProperty(PROP_PREFIX + key + PROP_IDLE_TIME, -1);
    int size = props.getIntProperty(PROP_PREFIX + key + PROP_FETCHSIZE, -1);
    int timeOut = props.getIntProperty(PROP_PREFIX + key + PROP_CONNECTION_TIMEOUT, -1);

    Integer fetchSize = null;
    if (size >= 0)
    {
      fetchSize = Integer.valueOf(size);
    }

    Integer connectionTimeOut = null;
    if (timeOut > 0)
    {
      connectionTimeOut = Integer.valueOf(timeOut);
    }

    ObjectNameFilter schemaFilter = getSchemaFilter(props, key);
    ObjectNameFilter catalogFilter = getCatalogFilter(props, key);

    ConnectionProfile profile = new ConnectionProfile();
    profile.setName(name);
    profile.setUsername(user);
    profile.setUrl(url);
    profile.setInputPassword(pwd);
    profile.setDriverclass(driverClass);
    profile.setDriverName(driverName);
    profile.setGroup(group);
    profile.setDefaultFetchSize(fetchSize);
    profile.setOracleSysDBA(sysDBA);
    profile.setReadOnly(readonly);
    profile.setWorkspaceFile(wksp);
    profile.setIcon(icon);
    profile.setConnectionTimeout(connectionTimeOut);
    profile.setRollbackBeforeDisconnect(rollback);
    profile.setUseSeparateConnectionPerTab(seperateConnection);
    profile.setAlternateDelimiterString(delimiter);
    profile.setMacroFilename(macroFile);
    profile.setIgnoreDropErrors(ignoreDropError);
    profile.setTrimCharData(trimCharData);
    profile.setDetectOpenTransaction(detectOpen);
    profile.setPreventDMLWithoutWhere(preventNoWhere);
    profile.setConfirmUpdates(confirmUpdates);
    profile.setPromptForUsername(promptUsername);
    profile.setEmptyStringIsNull(emptyStringIsNull);
    profile.setIncludeNullInInsert(includeNullInInsert);
    profile.setRemoveComments(removeComments);
    profile.setStoreExplorerSchema(rememberExplorerSchema);
    profile.setHideWarnings(hideWarnings);
    profile.setStoreCacheLocally(storeCache);
    profile.setAutocommit(autoCommit);
    profile.setPreDisconnectScript(preDisconnect);
    profile.setPostConnectScript(postConnect);
    profile.setIdleScript(idleScript);
    profile.setIdleTime(idleTime);
    profile.setStorePassword(storePwd);
    profile.setCopyExtendedPropsToSystem(copyProps);
    profile.setConnectionProperties(connProps);
    profile.setInfoDisplayColor(color);
    profile.setSchemaFilter(schemaFilter);
    profile.setCatalogFilter(catalogFilter);

    return profile;
  }

  @Override
  public void saveProfiles(List<ConnectionProfile> profiles, String filename)
  {
    LogMgr.logDebug("IniProfileStorage.saveProfiles()", "Saving profiles to: " + filename);
    WbProperties props = new WbProperties(2);

    for (int i=0; i < profiles.size(); i++)
    {
      String key = Integer.toString(i + 1);
      storeProfile(key, profiles.get(i), props);
    }

    try
    {
      props.saveToFile(new WbFile(filename));
    }
    catch (IOException ex)
    {
      LogMgr.logError("IniProfileStorage.saveProfiles()", "Error saving profiles to: " + filename, ex);
    }
  }

  private void storeProfile(String key, ConnectionProfile profile, WbProperties props)
  {
    key = "." + key;
    props.setProperty(PROP_PREFIX + key + PROP_URL, profile.getUrl());
    props.setProperty(PROP_PREFIX + key + PROP_NAME, profile.getName());
    props.setProperty(PROP_PREFIX + key + PROP_DRIVERCLASS, profile.getDriverclass());
    props.setProperty(PROP_PREFIX + key + PROP_DRIVERNAME, profile.getDriverName());
    props.setProperty(PROP_PREFIX + key + PROP_GROUP, profile.getGroup());
    props.setProperty(PROP_PREFIX + key + PROP_USERNAME, profile.getUsername());
    if (profile.getStorePassword())
    {
      props.setProperty(PROP_PREFIX + key + PROP_PWD, profile.getPassword());
    }
    props.setProperty(PROP_PREFIX + key + PROP_ICON, profile.getIcon());
    props.setProperty(PROP_PREFIX + key + PROP_WORKSPACE, profile.getWorkspaceFile());
    props.setProperty(PROP_PREFIX + key + PROP_ALT_DELIMITER, profile.getAlternateDelimiterString());
    props.setProperty(PROP_PREFIX + key + PROP_MACROFILE, profile.getMacroFilename());
    props.setProperty(PROP_PREFIX + key + PROP_SCRIPT_CONNECT, profile.getPostConnectScript());
    props.setProperty(PROP_PREFIX + key + PROP_SCRIPT_DISCONNECT, profile.getPreDisconnectScript());
    props.setProperty(PROP_PREFIX + key + PROP_SCRIPT_IDLE, profile.getIdleScript());
    props.setProperty(PROP_PREFIX + key + PROP_AUTOCOMMMIT, profile.getAutocommit());
    props.setProperty(PROP_PREFIX + key + PROP_STORECACHE, profile.getStoreCacheLocally());
    props.setProperty(PROP_PREFIX + key + PROP_ROLLBACK_DISCONNECT, profile.getRollbackBeforeDisconnect());
    props.setProperty(PROP_PREFIX + key + PROP_SEPARATECONNECTION, profile.getUseSeparateConnectionPerTab());
    props.setProperty(PROP_PREFIX + key + PROP_IGNOREDROPERRORS, profile.getIgnoreDropErrors());
    props.setProperty(PROP_PREFIX + key + PROP_TRIMCHARDATA, profile.getTrimCharData());
    if (profile.getOracleSysDBA())
    {
      props.setProperty(PROP_PREFIX + key + PROP_ORACLESYSDBA, profile.getOracleSysDBA());
    }
    props.setProperty(PROP_PREFIX + key + PROP_DETECTOPENTRANSACTION, profile.getDetectOpenTransaction());
    props.setProperty(PROP_PREFIX + key + PROP_READONLY, profile.isReadOnly());
    props.setProperty(PROP_PREFIX + key + PROP_PREVENT_NO_WHERE, profile.getPreventDMLWithoutWhere());
    props.setProperty(PROP_PREFIX + key + PROP_CONFIRM_UPDATES, profile.getConfirmUpdates());
    props.setProperty(PROP_PREFIX + key + PROP_PROMPTUSERNAME, profile.getPromptForUsername());
    props.setProperty(PROP_PREFIX + key + PROP_EMPTY_STRING_IS_NULL, profile.getEmptyStringIsNull());
    props.setProperty(PROP_PREFIX + key + PROP_INCLUDE_NULL_ON_INSERT, profile.getEmptyStringIsNull());
    props.setProperty(PROP_PREFIX + key + PROP_REMOVE_COMMENTS, profile.getRemoveComments());
    props.setProperty(PROP_PREFIX + key + PROP_REMEMEMBER_SCHEMA, profile.getStoreExplorerSchema());
    props.setProperty(PROP_PREFIX + key + PROP_HIDE_WARNINGS, profile.isHideWarnings());
    props.setProperty(PROP_PREFIX + key + PROP_IDLE_TIME, Long.toString(profile.getIdleTime()));
    props.setProperty(PROP_PREFIX + key + PROP_INFO_COLOR, Settings.colorToString(profile.getInfoDisplayColor()));

    Integer fetchSize = profile.getDefaultFetchSize();
    if (fetchSize != null)
    {
      props.setProperty(PROP_PREFIX + key + PROP_FETCHSIZE, fetchSize.intValue());
    }
    Integer timeout = profile.getConnectionTimeout();
    if (timeout != null)
    {
      props.setProperty(PROP_PREFIX + key + PROP_CONNECTION_TIMEOUT, timeout.intValue());
    }

    ObjectNameFilter filter = profile.getSchemaFilter();
    if (filter != null)
    {
      String expr = getFilterString(filter);
      props.setProperty(PROP_PREFIX + key + PROP_SCHEMA_FILTER, expr);
      props.setProperty(PROP_PREFIX + key + PROP_SCHEMA_FILTER + ".include", filter.isInclusionFilter());
    }

    filter = profile.getCatalogFilter();
    if (filter != null)
    {
      String expr = getFilterString(filter);
      props.setProperty(PROP_PREFIX + key + PROP_CATALOG_FILTER, expr);
      props.setProperty(PROP_PREFIX + key + PROP_CATALOG_FILTER+ ".include", filter.isInclusionFilter());
    }

    String xml = toXML(profile.getConnectionProperties());
    props.setProperty(PROP_PREFIX + key + PROP_CONN_PROPS, xml);
  }

  private String toXML(Properties props)
  {
    if (props == null || props.size() == 0) return null;

    try
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream(props.size() * 20);
      props.storeToXML(out, null, "ISO-8859-1");
      String xml = out.toString("ISO-8859-1");
      return xml.replaceAll(StringUtil.REGEX_CRLF, "");
    }
    catch (Throwable th)
    {
      LogMgr.logError("IniProfileStorage.toXM()", "Could not convert connection properties to XML", th);
      return null;
    }
  }

  private Properties toProperties(String xml)
  {
    if (StringUtil.isBlank(xml)) return null;

    try
    {
      Properties props = new Properties();
      ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("ISO-8859-1"));
      props.loadFromXML(in);
      return props;
    }
    catch (Throwable th)
    {
      LogMgr.logError("IniProfileStorage.toProperties()", "Could not convert XML properties", th);
      return null;
    }
  }

  private Set<String> getProfileKeys(WbProperties props)
  {
    Set<String> uniqueKeys = new TreeSet<>();
    List<String> keys = props.getKeysWithPrefix(PROP_PREFIX);
    for (String key : keys)
    {
      String[] elements = key.split("\\.");
      if (elements.length > 2)
      {
        uniqueKeys.add(elements[1]);
      }
    }
    return uniqueKeys;
  }

  private ObjectNameFilter getSchemaFilter(WbProperties props, String key)
  {
    return getFilter(props, key, PROP_SCHEMA_FILTER);
  }

  private ObjectNameFilter getCatalogFilter(WbProperties props, String key)
  {
    return getFilter(props, key, PROP_CATALOG_FILTER);
  }

  private ObjectNameFilter getFilter(WbProperties props, String key, String prop)
  {
    String filterList = props.getProperty(PROP_PREFIX + key + prop);
    if (StringUtil.isBlank(filterList)) return null;
    boolean inclusion = props.getBoolProperty(PROP_PREFIX + key + prop + ".include", false);
    ObjectNameFilter filter = new ObjectNameFilter();
    filter.setExpressionList(filterList);
    filter.setInclusionFilter(inclusion);
    return filter;
  }

  private String getFilterString(ObjectNameFilter filter)
  {
    if (filter == null) return null;
    Collection<String> expressions = filter.getFilterExpressions();
    if (CollectionUtil.isEmpty(expressions)) return null;
    String result = "";
    for (String exp : expressions)
    {
      if (result.length() > 0) result += ";";

      if (exp.indexOf(';') > -1)
      {
        result += "\"" + exp + "\"";
      }
      else
      {
        result += exp;
      }
    }
    return result;
  }
}