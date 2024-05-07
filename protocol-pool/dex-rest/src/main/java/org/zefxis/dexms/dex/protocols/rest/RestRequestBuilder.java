package org.zefxis.dexms.dex.protocols.rest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;


import org.json.XML;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.zefxis.dexms.dex.protocols.builders.RequestBuilder;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.Data.Context;
import org.zefxis.dexms.gmdl.utils.Scope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;



public class RestRequestBuilder implements RequestBuilder {
	public static Request buildRestPostRequest(final String destination, final Scope scope,
			final List<Data<?>> datas) {

		return buildRestRequest(Method.POST, destination, scope, datas);
	}

	public static Request buildRestGetRequest(final String destination, final Scope scope, final List<Data<?>> datas) {

		return buildRestRequest(Method.GET, destination, scope, datas);
	}

	private static Request buildRestRequest(final Method method, final String destination, final Scope scope,
			final List<Data<?>> datas) {
		Request request = new Request();

		request.setResourceRef("");

		UriBuilder builder = UriBuilder.fromPath(destination + scope.getUri());
		buildRequestHeaders(request, datas);
		buildRequestPath(builder, datas);
		buildRequestQuery(request, datas);
		buildRequestForm(request, datas);
		buildRequestBody(request, datas);

		String params = request.getResourceRef().toString();
		try {
			// request.setResourceRef(java.net.URLDecoder.decode(builder.toString(),
			// "UTF-8"));
			request.setResourceRef(java.net.URLDecoder.decode(destination + scope.getUri() + params, "UTF-8"));

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		request.setMethod(method);
		return request;
	}

	private static void buildRequestBody(final Request request, final List<Data<?>> datas) {
		StringBuilder json = new StringBuilder();
		boolean isXmlMediaType = false;
		for (Data<?> data : datas) {
			if (data.getContext() == Context.BODY) {
				
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

				try {
					
					
					json.append(ow.writeValueAsString(data.getObject()));

				} catch (JsonProcessingException e) {

					e.printStackTrace();
				}
				
				if(data.getMediaType() == org.zefxis.dexms.gmdl.utils.Data.MediaType.XML){
					
					isXmlMediaType = true;
					
				}

			}

		}
		
		if(isXmlMediaType){
			
			org.json.JSONObject jsonObject = new org.json.JSONObject(json.toString());
			request.setEntity(XML.toString(jsonObject).toString(), MediaType.APPLICATION_ALL_XML);
			
		}else{
			
			request.setEntity(json.toString(), MediaType.APPLICATION_ALL_JSON);
		}
		
		
	}

	private static void buildRequestForm(final Request request, final List<Data<?>> datas) {
		for (Data<?> data : datas) {
			if (data.getContext() == Context.FORM){
				// TODO
			}
		}
	}

	private static void buildRequestQuery(final Request request, final List<Data<?>> datas) {
		
		for (Data<?> data : datas) {
			
			if (data.getContext() == Context.QUERY){
				
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				JSONParser parser = new JSONParser();
				JSONObject jsonObject = null;
				try{
					
					jsonObject = (JSONObject) parser.parse(ow.writeValueAsString(data.getObject()));
					Map<String, String> map = new HashMap<>(jsonObject.size());
					
			        for (Object jsonEntry : jsonObject.entrySet()) {
			            Map.Entry<?, ?> entry = ((Map.Entry<?, ?>) jsonEntry);
			            map.put(entry.getKey().toString(), entry.getValue().toString());
			            request.getResourceRef().addQueryParameter(entry.getKey().toString(), entry.getValue().toString());
			        }
					
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	private static void buildRequestPath(final UriBuilder builder, final List<Data<?>> datas) {
		for (Data<?> data : datas) {
			if (data.getContext() == Context.PATH) {
				builder.resolveTemplate(data.getName(), data.getObject());
			}
		}
	}

	private static void buildRequestHeaders(final Request request, final List<Data<?>> datas) {
		for (Data<?> data : datas) {
			if (data.getContext() == Context.HEADER) {
				
//				 request.getOnSent();
			}
		}
	}
}
