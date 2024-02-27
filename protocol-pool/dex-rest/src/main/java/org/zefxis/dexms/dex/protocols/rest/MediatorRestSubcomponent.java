 package org.zefxis.dexms.dex.protocols.rest;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.Scope;
import org.zefxis.dexms.gmdl.utils.enums.OperationType;
import org.zefxis.dexms.tools.logger.GLog;
import org.zefxis.dexms.tools.logger.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;




public class MediatorRestSubcomponent extends MediatorGmSubcomponent {

	private Client client = null;
	private Server server = null;
	private Component component;
	private Logger logger = GLog.initLogger();

	private Component printerComponent;
	private Server printerServer;
	private static String printermsg = "empty";
	private static MediatorRestSubcomponent bcRestSubcomponent;
	private static String message;
	private ExecutorService executor = null;
	private static String op_name = null;
	private static Operation op = null;
	private static String hostAddress;
	private static String port;
	private BlockingQueue<String> waitingQueue = new LinkedBlockingDeque<>();



	public MediatorRestSubcomponent(

			MediatorConfiguration bcConfiguration, GmServiceRepresentation serviceRepresentation

	) {
		super(bcConfiguration);
		
		System.out.println("This is the used JKS Path:"+this.bcConfiguration.getJKSPath());
		System.out.println("This is the used JKS Path:"+this.bcConfiguration.getJKSPath());
		System.out.println("This is the used JKS Path:"+this.bcConfiguration.getJKSPath());

		System.out.println("MediatorHttpsSubcomponent --> "+this.bcConfiguration.getSubcomponentRole());

		setGmServiceRepresentation(serviceRepresentation);
		System.out.print("serviceRepresentation:"+serviceRepresentation.getInterfaces().get(0).getOperations().entrySet());
		for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations().entrySet()) {

			op_name = en.getKey();
			op = en.getValue();
		}
		this.bcRestSubcomponent = this;
		System.out.println("this.bcConfiguration.getSubcomponentAddress(): "+this.bcConfiguration.getSubcomponentAddress());
		String[] parts = this.bcConfiguration.getSubcomponentAddress().split(":");
		
		if (parts.length == 2) {
            hostAddress = parts[0];
            port = parts[1];}
            else {
            	hostAddress = parts[0];	
                port = String.valueOf(this.bcConfiguration.getSubcomponentPort());;
            }
      
		int restservicePort = Integer.valueOf(bcConfiguration.getServicePort());
		switch (this.bcConfiguration.getSubcomponentRole()) {

		case SERVER:

			this.server = new Server(Protocol.HTTP, this.bcConfiguration.getSubcomponentAddress(),
					this.bcConfiguration.getSubcomponentPort());
			this.component = new Component();
			this.component.getServers().add(server);
			this.component.getContext().getParameters().add("maxThreads", "100");
			this.component.getContext().getParameters().add("maxTotalConnections", "-1");
			break;

		case CLIENT:

			executor = Executors.newFixedThreadPool(10);
			client = new Client(Protocol.HTTP);
			this.printerServer = new Server(Protocol.HTTP, restservicePort);
			this.printerComponent = new Component();
			this.printerComponent.getServers().add(printerServer);
			break;

		default:
			break;
		}
	}

	@Override
	public void start() {
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:

			try {
				this.component.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// this.component.setDefaultHost(this.bcConfiguration.getSubcomponentAddress());
			// this.component.getDefaultHost().attach("/",
			// RestServerResource.class);
			this.component.getDefaultHost().attach("/"+op_name+"/", new RestletRestService());

			break;
		case CLIENT:
			try {

				this.client.start();
				this.printerComponent.start();

			} catch (Exception e1) {
				e1.printStackTrace();
			}
			this.printerComponent.getDefaultHost().attach("/postedmessage/", RestServerResource.class);
			break;
		default:
			break;
		}
	}

	@Override
	public void stop() {
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
			try {

				this.component.stop();

			} catch (Exception e1) {

				e1.printStackTrace();
			}
			break;
		case CLIENT:
			try {
				this.client.stop();
				this.printerComponent.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void postOneway(final String destination, final Scope scope, final List<Data<?>> datas, final long lease) {
		System.out.println("This is PostOneWay for REST");
		JSONObject msgObj = new JSONObject();
		String message_id = "";
		for (Data d : datas) {

			if (!d.getName().equals("message_id")) {

				msgObj.put(d.getName(), d.getObject().toString());

			} else {

				message_id = d.getObject().toString();
			}

		}
		
		message = msgObj.toJSONString();
		if(!message_id.equals("")){
			
			message = msgObj.toJSONString() + "-" + message_id;
		}
		
		try {
		Request request = new Request();
		String URL = "http://"+bcConfiguration.getServiceAddress()+":"+bcConfiguration.getSubcomponentPort()+"/"+op_name+"/";
		//String URL = "http://"+bcConfiguration.getServiceAddress()+":"+"4000"+"/"+op_name+"/";
		request.setResourceRef(URL);
		System.out.println("I'm sending the POST request");
		request.setMethod(Method.POST);
		request.setEntity(message, MediaType.APPLICATION_JSON);
		client = new Client (Protocol.HTTP);
		client.handle(request);
		client.stop();}
		catch (Exception e) {System.out.println(e);}
		
		//catch (Exception e) {System.out.println(e);}
		
	//	executor.execute(new Runnable() {
	//	    @Override
	//	    public void run() {
	//	        try {
	//	    		Request request = new Request();
	//	    		request.setResourceRef(URL);
	//	    		request.setMethod(Method.POST);
	//	    		request.setEntity(message, MediaType.APPLICATION_JSON);
	//	    		client = new Client (Protocol.HTTP);
	//	    		client.handle(request);
	//	        }
		//        	System.out.println("I'm in Runnable");
		//        	Thread.sleep(10000);
		//            Request request = new Request();
		//            request.setResourceRef("http://" + bcConfiguration.getServiceAddress() + ":8893/randomValue");
		 //           request.setMethod(Method.POST);
		//            request.setEntity(message, MediaType.APPLICATION_JSON);

		//            client = new Client(Protocol.HTTP);
		//            client.handle(request);
		//        catch (Exception e) {
		            // Handle the exception here, for example, log the error message
		 //           System.out.println("Error occurred: " + e.getMessage());
		//           e.printStackTrace(); // Print the stack trace for debugging
		//        }
		//    }
		//});
		}
		
		


	@Override
	public void mgetOneway(final Scope scope, final Object exchange) {
		System.out.println("This is mgetOneWay for HTTP");
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);
	}

	@Override
	public void xmgetOneway(final String source, final Scope scope, final Object exchange) {

		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);
	}

	@Override
	public <T> T postTwowaySync(final String destination, final Scope scope, final List<Data<?>> datas,
			final long lease) {
		
		this.notifyStartEvent();
		Request request = null;

		switch (scope.getVerb()) {
		case GET:
			
			// Add in order to handle service for WP4 in Chorevolution Project
			if (scope.getName().equals("TrafficverketRoadcondition")
					|| scope.getName().equals("VasttrafikGenerateOAuth2token")
					|| scope.getName().equals("TrafficverketRoadcondition")
					|| scope.getName().equals("TrafficverketWeather")
					|| scope.getName().equals("VasttrafikrequestDepartureBoard")){

				request = CustomRestRequestBuilder.buildRestGetRequest(destination, scope, datas);
				
			} 
			// Default
			else {

				request = RestRequestBuilder.buildRestGetRequest(destination, scope, datas);
			}
			break;

		case POST:
			
			// Add in order to handle service for WP4 in Chorevolution Project
			if (scope.getName().equals("TrafficverketRoadcondition")
					|| scope.getName().equals("VasttrafikGenerateOAuth2token")
					|| scope.getName().equals("TrafficverketRoadcondition")
					|| scope.getName().equals("TrafficverketWeather")
					|| scope.getName().equals("VasttrafikrequestDepartureBoard")) {

				request = CustomRestRequestBuilder.buildRestPostRequest(destination, scope, datas);

			} 
			// Default
			else {

				request = RestRequestBuilder.buildRestPostRequest(destination, scope, datas);

			}
			
			break;

		default:

			printermsg = "Undefined method";
			logger.e(this.getClass().getName(), printermsg);
			break;
		}

		if(request.getMethod().equals(Method.GET)){
			
			printermsg = (request.toString());
			
		}else{
			
			printermsg = (request.getEntityAsText());
		}
		logger.i(this.getClass().getName() + "[request]", printermsg);
		
		Response response = this.client.handle(request);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		printermsg = response.getStatus().toString();
		logger.i(this.getClass().getName() + "[response]", printermsg);
		
		this.notifyReleaseEvent();
		return (T) response.getEntityAsText();
	}

	@Override
	public void xtgetTwowaySync(final String destination, final Scope scope, final long timeout,
			final Object response) {
		// TODO Auto-generated method stub
	}

	@Override
	public <T> T mgetTwowaySync(final Scope scope, final Object exchange) {

		return this.nextComponent.postTwowaySync(this.bcConfiguration.getServiceAddress(), scope,
				(List<Data<?>>) exchange, 0);
	}

	@Override
	public void postTwowayAsync(final String destination, final Scope scope, final List<Data<?>> data,
			final long lease) {
		// TODO Auto-generated method stub
	}

	@Override
	public void xgetTwowayAsync(final String destination, final Scope scope, final Object response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mgetTwowayAsync(final Scope scope, final Object exchange) {
		this.nextComponent.postTwowayAsync(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange,
				0);
	}

	@Override
	public void postBackTwowayAsync(final String source, final Scope scope, final Data<?> data, final long lease,
			final Object exchange) {
		// TODO Auto-generated method stub
	}

	public static class PrinterRestServerResource extends ServerResource {
		@Override
		protected Representation post(Representation entity) throws ResourceException {
			return new StringRepresentation("empty");
		}

		@Override
		protected Representation get() throws ResourceException{
			
			return new StringRepresentation(MediatorRestSubcomponent.printermsg);
		}

	}

	public class RestletRestService extends Restlet {

		private ExecutorService executor = null;

		public RestletRestService() {

			executor = Executors.newFixedThreadPool(10);
		}
		
		public void writeToFile(String data, String filePath) {
		    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
		        writer.write(data);
		    } catch (IOException e) {
		        e.printStackTrace();
		        // Handle any exceptions that occur during the file writing process
		    }
		}
		
		@Override
		public void handle(Request request, Response response) {
			System.out.println("I'm handling the request");
			if (request.getMethod().equals(Method.POST)) {
				try {
					String receivedText = request.getEntity().getText().toString();
					waitingQueue.offer(receivedText);
					//writeToFile(receivedText,"./file");
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				response.setAutoCommitting(false);
				response.setEntity("Done", MediaType.TEXT_PLAIN);
				final Response resp = response;
				response.setAutoCommitting(false);
				resp.setStatus(Status.SUCCESS_OK);				
				resp.commit();
			}
			else if (request.getMethod().equals(Method.GET)) {
			        String data_queue;
					data_queue = ((LinkedBlockingDeque<String>) waitingQueue).peekLast();

//			        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//			            StringBuilder content = new StringBuilder();
//			            String line;
//			            while ((line = reader.readLine()) != null) {
//			                content.append(line);
//			            }

					response.setEntity(data_queue.toString(), MediaType.APPLICATION_ALL_JSON);
					response.setStatus(Status.SUCCESS_OK);
			    } else {
			        response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST); // Method Not Allowed
			        response.setEntity("Error 2", MediaType.TEXT_PLAIN);
			    }
			}
				
				
				//executor.execute(new Runnable() {

					//@Override
					//public void run() {

						//resp.setStatus(Status.SUCCESS_OK);
						// resp.setEntity("OK Receive:)",
						// MediaType.TEXT_PLAIN);
						// resp.commit();
						//return;
					//}
				//});
			}

		//}

	//}

	public static class RestServerResource extends ServerResource{

		@Override
		protected Representation post(Representation entity) throws ResourceException {
			String receivedText = null;
			try {
				receivedText = entity.getText();
			} catch (IOException e1) {
				e1.printStackTrace();
				
			}

			//System.err.println(receivedText);
			System.out.println("This is the test I received: "+receivedText);
			bcRestSubcomponent.processing(receivedText);
			System.out.println("Processing Done");
			return new StringRepresentation("");

		}

		@Get
		public org.restlet.Response receiveGet() {
			String remainingPart = getReference().getRemainingPart();
			Request req = getRequest();

			// TODO: Remove source. Not needed
			String source = req.getHostRef().getHostDomain();
			int port = req.getHostRef().getHostPort();

			Request request = getRequest();
			Map<String, Object> map = request.getAttributes();
			Object headers = map.get(HeaderConstants.ATTRIBUTE_HEADERS);
			int connectionTimeout = 60000;// Default value
			String callback = null;
			try {
				@SuppressWarnings("unchecked")
				Series<Header> headerSeries = (Series<Header>) headers;
				Header timeoutHeader = headerSeries.getFirst("connectionTimeout");
				if (timeoutHeader != null) {
					connectionTimeout = Integer.valueOf(timeoutHeader.getValue());
				}
				Header callbackHeader = headerSeries.getFirst("callbackURL");
				if (callbackHeader != null) {
					callback = callbackHeader.getValue();
				}
			} catch (ClassCastException e) {
				// GLog.log.e(TAG, "Class cast exeption: " + e);
			} catch (NumberFormatException nfe) {
				// GLog.log.e(TAG, "Class cast exeption: " + nfe);
			}

			// Message_CS msg = new Message_CS("", 0, source, port, "GET",
			// remainingPart, "", connectionTimeout, "", 0);
			//
			// fr.inria.arles.lsb.commons.Response lsbResponse = null;
			//
			// if (callback == null) {
			// lsbResponse = connectorRef.invokeSync(msg);
			// } else {
			// lsbResponse = connectorRef.invokeAsync(msg);
			// }

			org.restlet.Response restletResponse = null;
			// restletResponse = getResponse();getResponse();
			// restletResponse.setStatus(lsbResponse.get_respStatus());
			// restletResponse.setEntity(new
			// StringRepresentation(lsbResponse.getDataString()));
			return restletResponse;
		}

	}

	public void processing(String receivedText) {
		System.out.println("I'm processing");
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		String message_id = "";
		String message = "";
		message = receivedText;
		System.out.println("The text received:"+receivedText);
		if(receivedText.split("-").length > 1){
			
			message_id = receivedText.split("-")[1];
			message = receivedText.split("-")[0];
		}
		
		try {
			jsonObject = (JSONObject) parser.parse(message);
			// agentMget.fire(""+System.currentTimeMillis()+"-"+message_id);
			System.out.println("this is the json object:"+jsonObject);
		} catch (ParseException e) {
			System.out.println("this is the error");
			e.printStackTrace();
		}
		//String op_name = "randomValue";	
		
		//String op_name = (String) jsonObject.get("op_name");
		//System.out.print("This is the op name :"+op_name);
		//this.notifyStartEvent();	
		//String op_name = (String) jsonObject.get("op_name");
		//System.out.print("serviceRepresentation:"+serviceRepresentation.getInterfaces().get(0).getOperations().entrySet());
		
		//for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations().entrySet()) {
		//	if (en.getKey().equals(op_name)) {
		//		Operation op = en.getValue();
				List<Data<?>> datas = new ArrayList<>();
				System.out.println("Starting the loop 1");
				for (Data<?> data : op.getGetDatas()) {
					Data d = new Data<String>(data.getName(), "String", true, (String) jsonObject.get(data.getName()),
							data.getContext(), data.getMediaType());
					datas.add(d);
					System.out.println("Added to datas");
					// System.err.println("Added " + d);
				}
				//Data d = new Data<String>("op_name", "String", true, op_name, "BODY");
				//datas.add(d);
				//if(!message_id.equals("")){
					
				//	d = new Data<String>("message_id", "String", true, message_id, "BODY");
				//	datas.add(d);
				//}
				
				if (op.getOperationType() == OperationType.TWO_WAY_SYNC) {
					String response = bcRestSubcomponent.mgetTwowaySync(op.getScope(), datas);

				} else if (op.getOperationType() == OperationType.ONE_WAY) {
					try {
					System.out.println("datas representation:"+datas);
					bcRestSubcomponent.mgetOneway(op.getScope(), datas);
					}
					
					catch (Exception e) {
						System.out.println("This is the error: "+e);
					}
					System.out.println("Terminated");
				}
			}
		
		
		
		
		
	

	public class SendMessage extends Thread {

		String message = null;
		String scope = null;

		public SendMessage(String message) {

			this.message = message;
		}

		public void run() {

			Request request = new Request();
			request.setResourceRef(bcConfiguration.getServiceAddress());
			request.setMethod(Method.POST);
			request.setEntity(message, MediaType.APPLICATION_JSON);
			client.handle(request);
		}

	}

	@Override
	public void setGmServiceRepresentation(GmServiceRepresentation serviceRepresentation) {
		// TODO Auto-generated method stub

	}

	@Override
	public GmServiceRepresentation getGmServiceRepresentation() {
		// TODO Auto-generated method stub
		return null;
	}
}