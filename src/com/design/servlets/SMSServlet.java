/**
 * This servlet deals with SMS queries
 */

package com.design.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.design.communicate.ProcessUser;
import com.design.data.Maps;
import com.design.data.News;
import com.design.data.Weather;
import com.design.data.Wolfram;
import com.design.persistence.Queries;
import com.design.persistence.Users;
import com.example.designgui.Broadcaster;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classification;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifiedClass;
import com.twilio.sdk.verbs.TwiMLResponse;

@WebServlet("/sms")
public class SMSServlet extends HttpServlet {
	
	public static double queryTime = System.currentTimeMillis();
	
	private static final long serialVersionUID = 1L;
	
	public String from;
	
	public Users user;
	
    public SMSServlet() {
        super();
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	TwiMLResponse twiml = new TwiMLResponse();
    	if (request.getParameter("From") != null) {
    		//System.out.println(request.getParameter("From"));
            //System.out.println("Query: " + request.getParameter("Body"));
    		
    		from = request.getParameter("From");
    		user = ProcessUser.userExists(from);
    		
    		
    		 response.setContentType(null);
             response.getWriter().print(twiml.toXML());
             
             Queries qu = new Queries();
             qu.setPhone(user);
             qu.setQuery(request.getParameter("Body"));
             qu.setType("sms");
             System.out.println("broadcasting");
             Broadcaster.broadcast("input", qu);
    		
            processQuery(request.getParameter("Body"));
            
            System.out.println("Users #" + user.getPhone());
            if (user == null) {
            	System.out.println("user = null");
            } 
            
            System.out.println(request.getParameter("Body"));
    	}

    }
    
    // Uses IBM natural language classifier
    // Directs code to correct data source
    public void processQuery (String body) {

    	NaturalLanguageClassifier service = new NaturalLanguageClassifier();
    	service.setUsernameAndPassword("6e7a6f54-5d89-4454-8f59-1ba52696f989", "TvAzv6xg9Up8");

    	Classification classification = service.classify("c7fa4ax22-nlc-10514", body);
    	List <ClassifiedClass> confidence = classification.getClasses(); // List of classes
    	
    	double largest = classification.getTopConfidence(); // Get largest
    	boolean success = true;
    	int secondLargestI; // Used to keep track of index of 2nd highest confidence if difference is < 30% between highest & 2nd highest

    	// Assigns initial value to 2nd largest
    	if (classification.getTopClass().equals(confidence.get(0).getName())) {
    		secondLargestI = 1;
    	} else {
    		secondLargestI = 0;
    	}
    	
    	// Loop through confidence classes
    	for (int i = 0; i < confidence.size(); i++) {
    		
    		if (!confidence.get(i).getName().equals(classification.getTopClass())) {
    			if (largest - 0.30 < confidence.get(i).getConfidence()) {
        			success = false;
        		}
        		
        		if (confidence.get(i).getConfidence() > confidence.get(secondLargestI).getConfidence()) {
        			secondLargestI = i;
        		}
    		}
    		
    	}
  
    	Queries query = new Queries();
		query.setConfidence(classification.getTopConfidence());
		query.setDirections(null);
		query.setNews(null);
		query.setPhone(user);

		query.setQuery(body);
		query.setTime(new Date());
		query.setType("sms");
    	
    	if (success) { // If successful map to correct data source
    		System.out.println(classification.getTopClass());
    		if (classification.getTopClass().equals("directions")) {
    			query.setClass1("directions");
    			Broadcaster.broadcast("class", query);
    			Maps.googleMaps(query);
    		} else if (classification.getTopClass().equals("math")) {
    			query.setClass1("math");
    			Broadcaster.broadcast("class", query);
    			Wolfram.wolframAlpha(query);
    		} else if (classification.getTopClass().equals("weather")) {
    			query.setClass1("weather");
    			Broadcaster.broadcast("class", query);
    			Weather.weather(query);
    		} else if (classification.getTopClass().equals("news")) {
    			query.setClass1("news");
    			Broadcaster.broadcast("class", query);
    			News.getNews(query);
    		}
    		else
    		{
    			query.setClass1("math");
    			Broadcaster.broadcast("class", query);
    			Wolfram.wolframAlpha(query);
    			
    		}
    	} else { // Otherwise unsuccessful
    		query.setClass1("");
    		query.setSuccessful(false);
    		
    	}
    }
    

}
