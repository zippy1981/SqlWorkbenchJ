/*
 * LessOrEqualComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage.filter;

/**
 * @author Thomas Kellerer
 */
public class LessOrEqualComparator
	implements ColumnComparator
{

	public boolean supportsIgnoreCase()
	{
		return false;
	}

	public String getValueExpression(Object value)
	{
		return (value == null ? "" : value.toString());
	}

	public String getOperator()
	{
		return "\u2264";
	}

	public String getDescription()
	{
		return "less than or equal";
	}

	public boolean needsValue()
	{
		return true;
	}

	public boolean comparesEquality()
	{
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean evaluate(Object reference, Object value, boolean ignoreCase)
	{
		if (reference == null || value == null)
		{
			return false;
		}
		try
		{
			return ((Comparable) reference).compareTo((Comparable) value) >= 0;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public boolean supportsType(Class valueClass)
	{
		return Comparable.class.isAssignableFrom(valueClass);
	}

	public boolean equals(Object other)
	{
		return (other instanceof LessOrEqualComparator);
	}

	public boolean validateInput(Object value)
	{
		return (value instanceof Comparable);
	}
}
