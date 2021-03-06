/**
 * The Wolfram Module, deals with queries relevant to Wolfram Alpha
 * Specifically, this module handles:
 * 
 * @author Shayaan Syed Ali
 * @version Feb 27, 2016
 */
package com.design.data;

import java.util.Arrays;
import java.util.HashSet;

import com.design.communicate.ProcessUser;
import com.design.persistence.Queries;
import com.design.servlets.SMSServlet;
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

// This was originally an interface, it may be better as a class or it may not be
public class Wolfram {
	 
	static int NO_PODS_TO_INCL = 3;
	static HashSet<String> alwaysExclIDs = new HashSet<String>(Arrays.asList("", " ", "Plot", "Location", "Local map", "3DPlot", "ContourPlot", "Timeline", "Image")); // POD IDs (and titles) to always exclude, if they exist for a query
	static String[] arrExclIDs = {" ", "Plot", "Location", "Local map", "3DPlot", "ContourPlot", "Timeline"};

	public static String wolframAlpha(Queries qu)
	{
    	String queryStr = qu.getQuery();
		StringBuilder result = new StringBuilder("");
    	
    	// Basic engine setup
    	WAEngine engine = new WAEngine();
    	engine.setAppID("7AHUTR-UV58KYXA8Q");
    	 
    	// Set up query with necessary parameters
    	WAQuery query = engine.createQuery();
    	query.setInput(queryStr);
    	
    	// Exclude IDs
    	for (String ID: arrExclIDs)
    	{
    		query.addExcludePodID(ID);
    	}
    	
    	
    	// Query retrieval and error handling
    	WAQueryResult queryResult = null;
		try
		{
			queryResult = engine.performQuery(query);
		}
		catch (WAException e)
		{
			// WIP: Handle WA engine errors
			e.printStackTrace(); // Might not want this; need to generate a response
			qu.setSuccessful(false);
    		qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
    		ProcessUser.persistWolfram(qu);
    		return null;
		}
		
		// Error case
		if (queryResult.isError())
		{
			qu.setSuccessful(false);
    		qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
    		ProcessUser.persistWolfram(qu);
    		return null;
		}
		else if (!queryResult.isSuccess())
		{
			qu.setSuccessful(false);
    		qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
    		ProcessUser.persistWolfram(qu);
    		return null;
		}
		// Response generation
		else
		{
			// To do:
			// 1. Determine what the query is relevant to
			// 2. Organize the query as needed (if needed)
			// 3. Set up assumptions (as needed)
			// 4. Get WA engine response
			// 5. Sort through response
			// 6. Generate result with parts of response

			// Alt method:
			// Include first few pods in output
			// Have some pods, which if they exist for this entry, are always included
			
			// Retrieve result pods
			WAPod[] pods = queryResult.getPods();
			
			// Result generation
			
			// Include pods which must always be included first (if they exist)
			// WIP
			// First few pods included
//			int podI = 0;
//			if (pods[podI].getID().equals("Input")) // Exclude 'Input interpretation' pod
//			{
//				podI++;
//			}
			for (int podI = 0; podI < NO_PODS_TO_INCL && podI < pods.length; podI++)
			{
				if (!pods[podI].isError() && !alwaysExclIDs.contains(pods[podI].getID()) && !alwaysExclIDs.contains(pods[podI].getTitle()))
				{
					result.append(pods[podI].getTitle());
					result.append(": \n");
					WASubpod[] subpods = pods[podI].getSubpods();
					for (WASubpod subpod : subpods)
					{
						// Iterate through elements of the subpod and include plaintext elements
						for (Object element : subpod.getContents())
						{
							if (element instanceof WAPlainText)
							{
//								StringBuilder eText = new StringBuilder(((WAPlainText) element).getText());
//								if(eText.length()>0){eText.replace(0, 1, "" + Character.toUpperCase(eText.charAt(0)));}
//								result.append(eText);
								String str = ((WAPlainText) element).getText();
								str.trim();
								if (str.length()>0)
								{
									result.append(str);
								}
							}
						}
						result.append('\n');
					}
					result.append('\n');
				}
				else
				{
					podI--;
				}
			}
			
			// Include pods which must always be included at the end (if they exist)
			// WIP
			
			// Not enough pods
			if (pods.length <= 1)
			{
				result.append("A meaningful text response could not be generated");
			}
		}



    	 // Send results through Communicate Module
		qu.setSuccessful(true);
		qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
		ProcessUser.persistWolfram(qu, result.toString());
		
		return result.toString();
    	 
	} // wolframAlpha method
		
}