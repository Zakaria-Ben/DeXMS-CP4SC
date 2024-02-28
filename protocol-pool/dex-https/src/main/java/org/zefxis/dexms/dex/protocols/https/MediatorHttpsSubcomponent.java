package org.zefxis.dexms.dex.protocols.https;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.Scope;
import org.zefxis.dexms.gmdl.utils.enums.OperationType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


public class MediatorHttpsSubcomponent extends MediatorGmSubcomponent {
	
	private Server server = null;
	private Server server_client = null;
	
	private static MediatorHttpsSubcomponent bcHttpsSubcomponent;
	private static String op_name = null;
	private static Operation op = null;
	private static String message;
	private static SslContextFactory SslContextFactory = null;
	private static SSLContext sslContext;
	private static String hostAddress;
	private static String port;
	private static String JKSPathBus;
	private static String JKSPathService;
	
	public MediatorHttpsSubcomponent(MediatorConfiguration bcConfiguration,
			GmServiceRepresentation serviceRepresentation) {
		
		super(bcConfiguration);
		
		System.out.println("MediatorHttpsSubcomponent --> "+this.bcConfiguration.getSubcomponentRole());

		setGmServiceRepresentation(serviceRepresentation);
		System.out.print("serviceRepresentation:"+serviceRepresentation.getInterfaces().get(0).getOperations().entrySet());
		for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations().entrySet()) {

			op_name = en.getKey();
			op = en.getValue();
		}
		this.bcHttpsSubcomponent = this;
		System.out.println("this.bcConfiguration.getSubcomponentAddress(): "+this.bcConfiguration.getSubcomponentAddress());
		String[] parts = this.bcConfiguration.getSubcomponentAddress().split(":");
		
		JKSPathBus = this.bcConfiguration.getJKSPathBus();
		JKSPathService = this.bcConfiguration.getJKSPathService();

		if (parts.length == 2) {
            hostAddress = parts[0];
            port = parts[1];}
            else {
            	hostAddress = parts[0];	
                port = String.valueOf(this.bcConfiguration.getSubcomponentPort());;
            }
      
		//hostAddress = this.bcConfiguration.getSubcomponentAddress();
		System.out.println("hostAddress "+hostAddress);

		//int port = this.bcConfiguration.getSubcomponentPort();
		System.out.println("port "+port);
		
		int restservicePort = Integer.valueOf(port);
		//System.out.println("port:" + restservicePort);
		//System.out.println("op_name:" + op_name);
		System.out.println("Host:" + this.bcConfiguration.getSubcomponentAddress());
		
		switch (this.bcConfiguration.getSubcomponentRole()) {

		case SERVER:
			System.out.println("This server expose the data received using an HTTPS resource "+op_name);

			System.out.println("This is the TLS URL: https://"+ hostAddress+ ":"+port+"/"+op_name);

		server = new Server();
		try {
			SSLconf(restservicePort, hostAddress, server, "Bus");
			System.out.println("restservicePort: "+restservicePort);
			System.out.println("hostAddress: "+hostAddress);
			
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	break;
    	
		case CLIENT:
			int port_https_client = restservicePort; // This is a fixed port number --> you have to do POST requests on this port that is used by DeXMS
			System.out.println("This server is used to perform POST requests on it. The resource used is /postedmessage");
			System.out.println("This is the server endpoint: https://"+hostAddress+":"+port_https_client+"/postedmessage");
			
			server_client = new Server();
			try {
				SSLconf(port_https_client, hostAddress, server_client, "Service");
				
			} catch (UnrecoverableKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	break;
		}
	}
	
	
	// When the JAR file of the mediator is started
	@Override
	public void start() {
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
		
		try {
	    	ServletContextHandler handler = new ServletContextHandler(server, "/"+op_name);
	    	handler.addServlet(MyServlet.class, "");
	    	server.start();
				
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		break;
	case CLIENT:
		try {
	    	ServletContextHandler handler = new ServletContextHandler(server_client, "/postedmessage");
	    	handler.addServlet(ServletClient.class, "");
	    	server_client.start();
				
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		break;

		
	}
	}
	// When the JAR file of the mediator is stoped
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
		try {
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		break;
		case CLIENT:
			try {
				server_client.stop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		
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
	
	// Posting data on the Server so as to be exposed later
	@Override
	public void postOneway(String destination, Scope scope, List<Data<?>> data, long lease) {
		// TODO Auto-generated method stub
		System.out.println("This is POST ONE WAY OF HTTPS");
		// Handle the received data 
		// ------------------------------
		JSONObject msgObj = new JSONObject();
		String message_id = "";
		for (Data d : data) {

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
		// ------------------------------
	
		// Use an HTTPS client to post the data on the URL
		String URL = "https://"+this.bcConfiguration.getServiceAddress()+":"+port+"/"+op_name+"/";
		System.out.println("The URL I'm posting to is: "+URL);

	    // Create an HttpClient that supports SSL/TLS
	    // Create an HttpClient that supports SSL/TLS
      @SuppressWarnings("deprecation")
	CloseableHttpClient httpClient = HttpClients.custom()
              .setSslcontext(sslContext)
              .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
              .build();
		
      //HttpClient httpClient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(URL);
      String postData = message;
      try {
			httpPost.setEntity(new StringEntity(postData));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      try {
	        System.out.println("This is the print before sending the POST");    	  
			HttpResponse response = httpClient.execute(httpPost);
	        System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
	     // Check for redirection
	        if (response.getStatusLine().getStatusCode() == 302) {
	            org.apache.http.Header locationHeader = response.getFirstHeader("Location");
	            if (locationHeader != null) {
	                System.out.println("Redirected to: " + locationHeader.getValue());
	            }
	        }

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      try {
			httpClient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}	
	

	@Override
	public void mgetOneway(Scope scope, Object exchange) {
		// TODO Auto-generated method stub
		System.out.println("This is mget ONE WAY OF HTTPS");
		System.out.println("scope: "+scope);
		System.out.println("exchange: "+exchange);
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);

	}

	@Override
	public void xmgetOneway(String source, Scope scope, Object exchange) {
		// TODO Auto-generated method stub
		System.out.println("This is xmget ONE WAY ONE WAY OF HTTPS");
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);

	}

	@Override
	public <T> T postTwowaySync(String destination, Scope scope, List<Data<?>> datas, long lease) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void xtgetTwowaySync(String destination, Scope scope, long timeout, Object response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T mgetTwowaySync(Scope scope, Object exchange) {
		// TODO Auto-generated method stub
		return this.nextComponent.postTwowaySync(this.bcConfiguration.getServiceAddress(), scope,
				(List<Data<?>>) exchange, 0);
	}

	@Override
	public void postTwowayAsync(String destination, Scope scope, List<Data<?>> data, long lease) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void xgetTwowayAsync(String destination, Scope scope, Object response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mgetTwowayAsync(Scope scope, Object exchange) {
		// TODO Auto-generated method stub
		this.nextComponent.postTwowayAsync(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange,0);
		
	}

	@Override
	public void postBackTwowayAsync(String source, Scope scope, Data<?> data, long lease, Object exchange) {
		// TODO Auto-generated method stub
		
	}
	
	
	// This is used to configure the SSL certificate
	public static void SSLconf (int port, String host, Server server, String type) throws FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyStoreException {
    	// Configure SSL
        // Load your keystore (containing your SSL certificate) and truststore if needed
        KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance("JKS");
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //try (FileInputStream keyStoreFile = new FileInputStream("/Users/zbenomar/eclipse-workspace/Testjetty2/src/main/java/testjetty2/mykeystore.jks")) {
        
		if (type=="Bus") {
			try (FileInputStream keyStoreFile = new FileInputStream(JKSPathBus)){
	        	keyStore.load(keyStoreFile, "testtest".toCharArray());
			}	
		}
		
		else if (type=="Service") {
			try (FileInputStream keyStoreFile = new FileInputStream(JKSPathService)){
	        	keyStore.load(keyStoreFile, "testtest".toCharArray());
		}
		}
		
     // Initialize KeyManagerFactory with the keystore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "testtest".toCharArray());
       
     // Initialize TrustManagerFactory with your truststore (if needed)
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        //TrustManager[] trustAllCertificates = new TrustManager[]{new TrustAllCertificates()};

     // Initialize SSLContext with the KeyManagerFactory and TrustManagerFactory
        sslContext = SSLContext.getInstance("TLS");
        try {
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			//sslContext.init(null, trustAllCertificates, null);

        } catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

     // Create an SslConnectionFactory with the SSL context
        SslContextFactory sslFactory = new SslContextFactory();
        sslFactory.setSslContext(sslContext);
        //sslFactory.setProtocol("TLSv1.2");

        // Create a secure connector
        ServerConnector sslConnector = new ServerConnector(server, sslFactory);
        
        sslConnector.setPort(port); // Port for HTTPS
        sslConnector.setHost(host); // Host for HTTPS
        // Add the connector to the server
        server.addConnector(sslConnector);
        try {
			SslContextFactory = sslFactory;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	// This class is used to manage the Servlet. It is used in the MediatorHttpsSubcomponent

	public static class ServletClient extends HttpServlet {
	    private static final long serialVersionUID = 1L;
		private BlockingQueue<String> waitingQueue = new LinkedBlockingDeque<>();
		public ServletClient() {
			super();
		}
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	            throws ServletException, IOException {
			
			try {
		        String data_queue = ((LinkedBlockingDeque<String>) waitingQueue).peekLast();

		        // Parse the data_queue JSON string into a JsonObject
		        JsonObject dataQueueJson = JsonParser.parseString(data_queue).getAsJsonObject();

		        // Set the content type to JSON
		        resp.setContentType("application/json");

		        // Write the JSON object to the response output stream
		        resp.getWriter().println(dataQueueJson);
		    } catch (IOException e) {
		        // Handle exceptions here
		        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		        resp.getWriter().println("{\"error\": \"An error occurred.\"}");
		    }
			/*String data_queue;
			resp.setContentType("application/json");

			data_queue = ((LinkedBlockingDeque<String>) waitingQueue).peekLast();
			// Create a JSON object
			JsonObject jsonResponse = new JsonObject();
			// Serialize the JSON object to a JSON string
			String jsonResponseString = new Gson().toJson(jsonResponse);
	        resp.setStatus(HttpStatus.OK_200);
	        resp.getWriter().println(jsonResponseString);*/

	    }
		@Override
	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	            throws IOException {
			//The received data from protocol X is posted/exposed on the HTTPS server
			System.out.println("This is POST of HTTPS");
			// Get the content of the POST request 
			StringBuilder requestBody = new StringBuilder();
			try (BufferedReader reader = request.getReader()) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                requestBody.append(line);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			String receivedText = requestBody.toString();
			
			// Add the received data to the queue --> the same queue is used in the GET to read the data 
			waitingQueue.offer(receivedText);
			
			Object data = receivedText;
			// Managing the response of the POST request
			System.out.println("I received data");
			System.out.println(receivedText);
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);

			// Create a JSON object with status, message, and data fields
			JsonObject jsonResponse = new JsonObject();
			jsonResponse.addProperty("status", "success");
			jsonResponse.addProperty("message", "Data received successfully");
			// Get the response writer and write JSON data
			try (PrintWriter writer = response.getWriter()) {
			    writer.print(jsonResponse.toString());
			}
			processing(receivedText);

		}
	
	}
	public static void processing(String receivedText) {

		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		String message_id = "";
		String message = "";
		message = receivedText;
		if(receivedText.split("-").length > 1){
			
			message_id = receivedText.split("-")[1];
			message = receivedText.split("-")[0];
		}
		
		try {
			jsonObject = (JSONObject) parser.parse(message);
			// agentMget.fire(""+System.currentTimeMillis()+"-"+message_id);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		String op_name = (String) jsonObject.get("op_name");
		
				List<Data<?>> datas = new ArrayList<>();

				for (Data<?> data : op.getGetDatas()) {
					Data d = new Data<String>(data.getName(), "String", true, (String) jsonObject.get(data.getName()),
							data.getContext(), data.getMediaType());
					datas.add(d);
					// System.err.println("Added " + d);
				}
				//Data d = new Data<String>("op_name", "String", true, op_name, "BODY");
				//datas.add(d);
				//if(!message_id.equals("")){
					
				//	d = new Data<String>("message_id", "String", true, message_id, "BODY");
				//	datas.add(d);
				//}
				
				if (op.getOperationType() == OperationType.TWO_WAY_SYNC) {

					String response = bcHttpsSubcomponent.mgetTwowaySync(op.getScope(), datas);

				} else if (op.getOperationType() == OperationType.ONE_WAY) {

					bcHttpsSubcomponent.mgetOneway(op.getScope(), datas);
					System.out.println("This is what I'm sending to next component: "+datas);
				}
			}
	
	}