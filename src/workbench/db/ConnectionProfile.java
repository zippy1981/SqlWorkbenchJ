/*
 * ConnectionInfo.java
 *
 * Created on December 26, 2001, 3:32 PM
 */
package workbench.db;

import java.util.HashMap;
import java.util.Comparator;
import java.util.StringTokenizer;
import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.util.WbCipher;
import workbench.util.WbPersistence;

/**
 *	Supplies connection information as stored in
 *	the configuration files. This is used to read & parse
 *	the xml file which stores user defined configuration.
 *
 *	@author  thomas
 */
public class ConnectionProfile
{
	private static final String CRYPT_PREFIX = "@*@";
	private String name;
	private String url;
	private String driverclass;
	private String driverlib;
	private String username;
	private String password;
	private boolean autocommit;
	private String description;
	private int id;
	private static int nextId = 1;
	private boolean changed;

	static
	{
		WbPersistence.makeTransient(ConnectionProfile.class, "inputPassword");
	}
	
	public ConnectionProfile()
	{
		this.id = getNextId();
	}

	private static synchronized int getNextId()
	{
		return nextId++;
	}
	
	public ConnectionProfile(String driverClass, String url, String userName, String pwd)
	{
		this();
		this.setUrl(url);
		this.setDriverclass(driverClass);
		this.setUsername(userName);
		this.setPassword(pwd);
		this.setName(url);
		this.changed = false;
	}
	
	public ConnectionProfile(String aName, String driverClass, String url, String userName, String pwd)
	{
		this();
		this.setUrl(url);
		this.setDriverclass(driverClass);
		this.setUsername(userName);
		this.setPassword(pwd);
		this.setName(aName);
		this.changed = false;
	}
	
	/**
	 *	Sets the current password. If the password
	 *	is not already encrypted, it will be encrypted
	 *
	 *	@see #getPassword()
	 *	@see workbench.util.WbCipher#encryptString(String)
	 */
	public void setPassword(String aPwd)
	{
		if (aPwd == null) 
		{
			this.password = null;
			this.changed = true;
			return;
		}
		
		// check encryption settings when reading the profiles...
		if (WbManager.getSettings().getUseEncryption())
		{
			if (!this.isEncrypted(aPwd))
			{
				aPwd = this.encryptPassword(aPwd);
			}				
		}
		else
		{
			if (this.isEncrypted(aPwd))
			{
				aPwd = this.decryptPassword(aPwd);
			}
		}
			
		if (!aPwd.equals(this.password))
		{
			this.password = aPwd;
			this.changed = true;
		}
	}

	
	/**
	 *	Returns the encrypted version of the password.
	 *	@see #decryptPassword(String)
	 */
	public String getPassword() 
	{ 
		return this.password; 
	}
	
	public String getInputPassword()
	{
		return this.decryptPassword();
	}
	
	public void setInputPassword(String aPassword)
	{
		this.setPassword(aPassword);
	}

	/**
	 *	Returns the plain text version of the
	 *	current password.
	 *
	 *	@see #decryptPassword(String)
	 */
	public String decryptPassword()
	{
		return this.decryptPassword(this.password);
	}

	/**
	 *	Returns the plain text version of the given
	 *	password. This is not put into the getPassword()
	 *	method because the XMLEncode would write the
	 *	password in plain text into the XML file.
	 *	A method beginning with decrypt is not 
	 *	regarded as a property and thus not written
	 *	to the XML file.
	 *
	 *	@parm the encrypted password
	 */
	public String decryptPassword(String aPwd)
	{
		if (aPwd == null) return null;
		if (!aPwd.startsWith(CRYPT_PREFIX))
		{
			return aPwd;
		}
		else
		{
			WbCipher des = WbManager.getInstance().getDesCipher();
			return des.decryptString(aPwd.substring(CRYPT_PREFIX.length()));
		}
	}

	public boolean isEncrypted(String aPwd)
	{
		return aPwd.startsWith(CRYPT_PREFIX);
	}
	
	private String encryptPassword(String aPwd)
	{
		if (WbManager.getSettings().getUseEncryption())
		{
			if (!this.isEncrypted(aPwd))
			{
				WbCipher des = WbManager.getInstance().getDesCipher();
				aPwd = CRYPT_PREFIX + des.encryptString(aPwd);
			}				
		}
		return aPwd;
	}
	
	/**
	 *	Returns the name of the Profile
	 */
	public String toString() { return this.name; }

	/** Two connection profiles are equal if:
	 *  <ul>
	 * 	<li>the url are equal</li>
	 *  <li>the driver classes are equal</li>
	 *	<li>the usernames are equal</li>
	 *	<li>the (encrypted) passwords are equal</li>
	 *  </ul> 
	 */	
	public boolean equals(Object other)
	{
		try 
		{
			ConnectionProfile prof = (ConnectionProfile)other;
			return this.id == prof.id;
			/*
			return this.url.equals(prof.url) && 
						 this.driverclass.equals(prof.driverclass) &&
						 this.username.equals(prof.username) &&
						 this.password.equals(prof.password);
			*/
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}
	
	public String getUrl() { return this.url; }
	public void setUrl(String aUrl) 
	{ 
		if (aUrl == null || !aUrl.equals(this.url))
		{
			this.url = aUrl; 
			this.changed = true;
		}
	}
	
	public String getDriverclass() { return this.driverclass; }
	public void setDriverclass(String aDriverclass) 
	{ 
		this.driverclass = aDriverclass; 
		this.changed = true;
	}
	
	public String getUsername() { return this.username; }
	public void setUsername(java.lang.String aUsername) 
	{ 
		this.username = aUsername; 
		this.changed = true;
	}

	public boolean getAutocommit() { return this.autocommit; }
	public void setAutocommit(boolean aFlag) 
	{ 
		if (aFlag != this.autocommit)
		{
			this.autocommit = aFlag;
			this.changed = true;
		}
	}
	
	public String getName() { return this.name; }
	public void setName(String aName) 
	{ 
		this.name = aName;	
		this.changed = true;
	}
	
	public String getDescription() { return this.description; }
	
	public void setDescription(String description) 
	{ 
		this.changed = true;
		this.description = description; 
	}
	
	public ConnectionProfile createCopy()
	{
		ConnectionProfile result = new ConnectionProfile();
		result.setAutocommit(this.autocommit);
		result.setDescription(this.description);
		result.setDriverclass(this.driverclass);
		result.setName(this.name);
		result.setPassword(this.getPassword());
		result.setUrl(this.url);
		result.setUsername(this.username);
		result.changed = false;
		return result;
	}
	
	public static Comparator getNameComparator()
	{
		return new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				if (o1 instanceof ConnectionProfile && o2 instanceof ConnectionProfile)
				{
					String name1 = ((ConnectionProfile)o1).name;
					String name2 = ((ConnectionProfile)o2).name;
					return name1.compareTo(name2);				
				}
				return 0;
			}
		};
	}
	
}
