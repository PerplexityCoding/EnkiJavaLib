package com.ichi2.utils;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtility {
	
	public static Logger log = LoggerFactory.getLogger(HttpUtility.class);
	
	public static Boolean postReport(String url, List<NameValuePair> values) {
    	HttpClient httpClient = new DefaultHttpClient();  
        HttpPost httpPost = new HttpPost(url);  
      
        try {  
        	httpPost.setEntity(new UrlEncodedFormEntity(values));  
            HttpResponse response = httpClient.execute(httpPost);  
            
            switch(response.getStatusLine().getStatusCode()) {
	            case 200:
	            	log.error(String.format("feedback report posted to %s", url));
	            	return true;
	            	
            	default:
            		log.error(String.format("feedback report posted to %s message", url));
            		log.error(String.format("%d: %s", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
	            	break;
            }
        } catch (ClientProtocolException ex) {  
        	log.error(ex.toString());
        } catch (IOException ex) {  
        	log.error(ex.toString());  
        }
        
        return false;
	}
}
