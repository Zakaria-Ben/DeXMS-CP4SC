package org.zefxis.dexms.dex.protocols.coaps;




import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.zefxis.dexms.dex.protocols.coaps.CoapsObserver;
import org.zefxis.dexms.dex.protocols.coaps.CoapsObservableServer;
//import org.zefxis.dexms.dex.protocols.coaps.CoapsObserver;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;

import org.zefxis.dexms.gmdl.utils.Scope;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.json.simple.JSONObject;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfigDefaults;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;






public class MediatorCoapsSubcomponent extends MediatorGmSubcomponent {
	private static CoapClient client = null;
	private CoapsObservableServer server;
	private CoapsObserver observer = null;
	private Thread observerThread = null;
	private BlockingQueue<String> waitingQueue = new LinkedBlockingDeque<>();
	
	public MediatorCoapsSubcomponent(MediatorConfiguration bcConfiguration,
			GmServiceRepresentation serviceRepresentation) {
		
		super(bcConfiguration);
		this.serviceRepresentation = serviceRepresentation;
					
		switch (this.bcConfiguration.getSubcomponentRole()) {

		case SERVER:
			server = new CoapsObservableServer(serviceRepresentation, this.bcConfiguration.getServiceAddress(), this.bcConfiguration.getSubcomponentPort(), waitingQueue);


    	break;
    	
		case CLIENT:
			observer = new CoapsObserver(this, serviceRepresentation, bcConfiguration);

	    	break;
		}
	}

	
	// When the JAR file of the mediator is started
	@Override
	public void start() {
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
			server.start();
		
		        
		       
		break;
	case CLIENT:
		try {
			observerThread = new Thread(observer);
			observerThread.start();	
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
			observerThread.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		break;
		case CLIENT:
			try {
				 this.client.shutdown();
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


	@Override
	public void postOneway(final String destination, final Scope scope, final List<Data<?>> datas, final long lease) {
		// TODO Auto-generated method stub
		System.out.println("This is postOneway CoAPs");

		JSONObject jsonObject = new JSONObject();

		for (Data<?> data : datas) {

			jsonObject.put(data.getName(), String.valueOf(data.getObject()));

		}
		if(jsonObject.containsKey("message_id")) {
			
			String message_id = (String) jsonObject.get("message_id");
		}
		
		String datasStream = jsonObject.toJSONString();
		System.out.println("This is the value sent:"+datasStream);
		waitingQueue.add(datasStream);
		
	}


	@Override
	public void mgetOneway(Scope scope, Object exchange) {
		// TODO Auto-generated method stub
		System.out.println("This is mgetOneway CoAPS");
		System.out.println(exchange);
		
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);
	}


	@Override
	public void xmgetOneway(String source, Scope scope, Object exchange) {
		// TODO Auto-generated method stub
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
		this.nextComponent.postTwowayAsync(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange,
				0);
	}


	@Override
	public void postBackTwowayAsync(String source, Scope scope, Data<?> data, long lease, Object exchange) {
		// TODO Auto-generated method stub
		
	}
	
	
	}