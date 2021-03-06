/**
 * The Maps Module, to handle all location based data
 */
package com.design.data;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.design.communicate.Communicate;
import com.design.communicate.ProcessUser;
import com.design.persistence.Directions;
import com.design.persistence.Queries;
import com.design.servlets.SMSServlet;

public class Maps {

	 public static String googleMapss (Queries query) {
		 String [] results = detDirections(query);
		 return getAPIResults(results, query); 
	 }
	
	 public static void googleMaps (Queries query) {
		 String [] results = detDirections(query);
		 String some = getAPIResults(results, query); 
	 }
	 
	 private static String getAPIResults (String [] parameters, Queries qu) {		 
		 String query = "https://maps.googleapis.com/maps/api/directions/json?origin=" + parameters[0] +"&"
		 		+ "destination=" + parameters[1] + "&key=AIzaSyBbcq6id_NE2X_M-Fr7vXqCV6DLLYcDZ78";
		 query = query.replace(" ", "+");
		 
		 int distance = 0;
		 int time = 0;
		 
		 URL url = null;
		 try {
			url = new URL(query);
		 } catch (MalformedURLException e) {
			
			qu.setSuccessful(false);
			qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
			ProcessUser.persistDirection(parameters, qu, distance, time);
			e.printStackTrace();
			
			return null;
		 }
			
		 try {
			InputStream is = url.openStream();
			JsonReader json = Json.createReader(is);
			JsonObject obj = json.readObject();	
			
			JsonObject objn = null;
			JsonArray arr = null;
			
			try {
				objn = obj.getJsonArray("routes").getJsonObject(0).getJsonArray("legs").getJsonObject(0);
				distance=objn.getJsonObject("distance").getJsonNumber("value").intValue()/1000;
				time = objn.getJsonObject("duration").getJsonNumber("value").intValue()/3600;
				arr = objn.getJsonArray("steps");
			} catch (Exception ex) {
				qu.setSuccessful(false);
				qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
				ProcessUser.persistDirection(parameters, qu, distance, time);
				ex.printStackTrace();
				
				return null;
			}
			
			
			
			
			String directions = "";
			
			for (int i = 0; i < arr.size(); i++) {
				directions += (i + 1) +". ";
				directions += arr.getJsonObject(i).getJsonString("html_instructions").getString();
				directions += "\n";
			}
			
			String [] dumbPatterns = {"\u003cb\u003e", "\u003c/b\u003e"};
			for (int i=0; i < dumbPatterns.length; i++)
			{
				directions = directions.replace(dumbPatterns[i], "");
			}
			
			directions = directions.replace("<div style=\"font-size:0.9em\">", " ");
			directions = directions.replace("</div>", "");
			
			
			//System.out.println(directions);
			
			qu.setSuccessful(true);
			qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
			ProcessUser.persistDirection(parameters, qu, distance, time, directions);
			return directions;
			
		 } catch (Exception ex) {
			 qu.setSuccessful(false);
			 qu.setResponseTime(((double) System.currentTimeMillis() - SMSServlet.queryTime)/1000);
			 ProcessUser.persistDirection(parameters, qu, distance, time);
			 ex.printStackTrace();
				
			 return null;
		 }
		 
	 }
	 
	 
	 private static String [] detDirections(Queries qu)
	 {
		 String query = qu.getQuery();
		 String [] returnVariable = {"", ""};
		 
		 query=query.toLowerCase();
		 String junkarray[]= {" how ", " directions ", " direction "," go "," starting "," at ", " in ", " get "};
		 for(int i=0; i<junkarray.length; i++)
		 {
			 query=query.replace(junkarray[i], " ");
		 }
		 String brokenArray[]= query.split(" ");
                 
                 for (int i = 0; i < junkarray.length; i++) {
                     if (brokenArray[0].equals(junkarray[i].substring(1, junkarray[i].length() - 1))) {
                         brokenArray[0] = "";
                     }
                 }
		 
		 List <Integer> toList = new ArrayList<Integer>();
		 List <Integer> fromList = new ArrayList<Integer>();
		 
		 for (int i=0;i<brokenArray.length;i++)
		 {
			 if (brokenArray[i].equals("to"))
			 {
				toList.add(i);
			 }
			 else if (brokenArray[i].equals("from"))
			 {
				 fromList.add(i);
			 }
		 }
		 
		 
		 if (toList.size() == 0)
		 {
			 if (fromList.size() == 0) 
			 {
				 //Communicate.sendText("Unable to parse your directions query. Please try rephrasing.");
				 returnVariable[0] = "";
				 returnVariable[1] = "";
			 } 
			 else
			 {
				int from = fromList.get(fromList.size() - 1);
				for (int i = 0; i < from; i++) 
				{
					returnVariable[1] += brokenArray[i] +" ";
				}
				for (int i = from + 1; i < brokenArray.length; i++) 
				{
					returnVariable[0] += brokenArray[i] + " ";
				}
			 }
		 }
		 else
		 {
			 if(fromList.size()==0)
			 {
				 int to = toList.get(toList.size() - 1);
					for (int i = 0; i < to; i++) 
					{
						returnVariable[0] += brokenArray[i] +" ";
					}
					for (int i = to + 1; i < brokenArray.length; i++) 
					{
						returnVariable[1] += brokenArray[i] + " ";
					}
			 }
			 else
			 {
				int to = toList.get(toList.size() - 1);
				int from = fromList.get(fromList.size() - 1);
				
				if (from < to)
				{
					for (int i=from+1; i<to ; i++)
					{
						returnVariable[0] += brokenArray[i] +" ";
					}
					for (int i=to+1; i<brokenArray.length ; i++)
					{
						returnVariable[1] += brokenArray[i] +" ";
					}	
				}
				else
				{
					for (int i=to+1; i<from; i++)
					{
						returnVariable[1] += brokenArray[i] +" ";
					}
					for (int i=from+1; i<brokenArray.length; i++)
					{
						returnVariable[0] += brokenArray[i] +" ";
					}
				}
			 }
		 }
		 
                 return returnVariable;
	 }
	 
	 
	 public static String [] getLatLong (String text) {
	    	String key = "AIzaSyAjXKpbYwL4CFbXVtNbLKKE9cOrlrsI05Q";
	    	String query = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=" + text + "&types=geocode&key=" + key;
	    	
	    	
	    	URL url = null;
			try {
				url = new URL(query);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			String lat = null;
			String lon = null;
			
			try {
				InputStream is = url.openStream();
				JsonReader json = Json.createReader(is);
				JsonObject obj = json.readObject();
				String place = obj.getJsonArray("predictions").getJsonObject(0).getJsonString("place_id").getString();
				
				query = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + place + "&key=" + key;
				url = new URL(query);
				
				is = url.openStream();
				json = Json.createReader(is);
				obj = json.readObject().getJsonObject("result").getJsonObject("geometry").getJsonObject("location");
				
				lat = String.valueOf(obj.getJsonNumber("lat"));
				lon = String.valueOf(obj.getJsonNumber("lng"));
				
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}	
			
			
			String [] arr = {lat, lon};
			return arr;
	    }
	
}
