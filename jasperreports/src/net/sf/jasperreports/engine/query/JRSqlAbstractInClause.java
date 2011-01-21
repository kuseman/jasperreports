/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2009 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.engine.query;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sf.jasperreports.engine.JRRuntimeException;


/**
 * Base (NOT) IN clause function for SQL queries.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @version $Id$
 */
public abstract class JRSqlAbstractInClause implements JRClauseFunction
{
	
	protected static final int POSITION_DB_COLUMN = 1;
	protected static final int POSITION_PARAMETER = 2;

	protected static final String CLAUSE_TRUISM = "0 = 0";
	protected static final String OPERATOR_IS_NULL = "IS NULL";
	protected static final String OPERATOR_IS_NOT_NULL = "IS NOT NULL";
	
	protected JRSqlAbstractInClause()
	{
	}

	/**
	 * Creates a (NOT) IN SQL clause.
	 * 
	 * <p>
	 * The function expects two clause tokens (after the ID token):
	 * <ul>
	 * 	<li>The first token is the SQL column to be used in the clause.</li>
	 * 	<li>The second token is the name of the report parameter that contains the value list.
	 * 		<br/>
	 * 		The value of this parameter has to be an array, a <code>java.util.Collection</code>
	 * 		or <code>null</code>.
	 * 	</li>
	 * </p>
	 * 
	 * <p>
	 * The function constructs a <code>column [NOT] IN (?, ?, .., ?)</code> clause.
	 * If the values list is null or empty, the function generates a SQL clause that
	 * will always evaluate to true (e.g. <code>0 = 0</code>).
	 * </p>
	 */
	public void apply(JRClauseTokens clauseTokens, JRQueryClauseContext queryContext)
	{
		String col = clauseTokens.getToken(POSITION_DB_COLUMN);
		String param = clauseTokens.getToken(POSITION_PARAMETER);
		
		if (col == null)
		{
			throw new JRRuntimeException("SQL IN clause missing DB column token");
		}
		
		if (param == null)
		{
			throw new JRRuntimeException("SQL IN clause missing parameter token");
		}
		
		StringBuffer sbuffer = queryContext.queryBuffer();
		
		Object paramValue = queryContext.getValueParameter(param).getValue();
		if (paramValue == null)
		{
			handleNoValues(queryContext);
		}
		else
		{
			Collection paramCollection = convert(param, paramValue);
			int count = paramCollection.size();
			Iterator<?> it = paramCollection.iterator();

			if (count == 0)
			{
				handleNoValues(queryContext);
			}
			else
			{
				StringBuffer nullSbuffer = new StringBuffer();
				StringBuffer notNullSbuffer = new StringBuffer();
				boolean nullFound = false;
				boolean notNullFound = false;
				int idx = 0;
				List notNullQueryParameters = new ArrayList();
				
				while(it.hasNext())
				{
					Object element = it.next();
					if(element == null)
					{
						if(!nullFound)
						{
							nullFound = true;
							nullSbuffer.append(col);
							nullSbuffer.append(' ');
							appendNullOperator(nullSbuffer);
						}
					}
					else
					{
						if(!notNullFound)
						{
							notNullFound = true;
							notNullSbuffer.append(col);
							notNullSbuffer.append(' ');
							appendInOperator(notNullSbuffer);
							notNullSbuffer.append(' ');
							notNullSbuffer.append('(');
						}
					
						if (idx > 0)
						{
							notNullSbuffer.append(", ");
						}
						notNullSbuffer.append('?');
						notNullQueryParameters.add(element);
						idx++;
					}
				}
				if(nullFound)
				{
					sbuffer.append(nullSbuffer);
					if(notNullFound)
					{
						appendAndOrOperator(sbuffer);
					}
				}
				if(notNullFound)
				{
					notNullSbuffer.append(')');
					sbuffer.append(notNullSbuffer);
					queryContext.addQueryMultiParameters(param, count);
				}
			}
		}
	}
	
	protected void handleNoValues(JRQueryClauseContext queryContext)
	{
		queryContext.queryBuffer().append(CLAUSE_TRUISM);
	}

	protected int valuesCount(String paramName, Object paramValue)
	{
		int count;
		if (paramValue.getClass().isArray())
		{
			count = Array.getLength(paramValue);
		}
		else if (paramValue instanceof Collection)
		{
			count = ((Collection) paramValue).size();
		}
		else
		{
			throw new JRRuntimeException("Invalid type " + paramValue.getClass().getName() + 
					" for parameter " + paramName + " used in an IN clause; the value must be an array or a collection.");
		}
		return count;
	}

	protected Collection convert(String paramName, Object paramValue)
	{
		Collection paramCollection;
		if (paramValue.getClass().isArray())
		{
			paramCollection = Arrays.asList(paramValue);
		}
		else if (paramValue instanceof Collection)
		{
			paramCollection = (Collection) paramValue;
		}
		else
		{
			throw new JRRuntimeException("Invalid type " + paramValue.getClass().getName() + 
					" for parameter " + paramName + " used in an IN clause; the value must be an array or a collection.");
		}
		return paramCollection;
	}

	protected abstract void appendInOperator(StringBuffer sBuffer);
	protected abstract void appendNullOperator(StringBuffer sBuffer);
	protected abstract void appendAndOrOperator(StringBuffer sBuffer);
}
