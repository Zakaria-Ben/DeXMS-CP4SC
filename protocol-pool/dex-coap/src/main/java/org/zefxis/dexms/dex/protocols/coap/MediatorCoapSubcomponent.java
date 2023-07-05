package org.zefxis.dexms.dex.protocols.coap;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;

import org.eclipse.californium.core.CoapClient;
import org.json.simple.JSONObject;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.Scope;


import org.zefxis.dexms.gmdl.utils.Data;


public class MediatorCoapSubcomponent extends MediatorGmSubcomponent {

	private static CoapClient client = null;
	private CoapObservableServer server;
	private CoapObserver observer = null;
	private Thread observerThread = null;
	private BlockingQueue<String> waitingQueue = new LinkedBlockingDeque<>();
	
	public MediatorCoapSubcomponent(MediatorConfiguration bcConfiguration, GmServiceRepresentation serviceRepresentation) {
		super(bcConfiguration);
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
		
			this.serviceRepresentation = serviceRepresentation;
			server = new CoapObservableServer(serviceRepresentation, this.bcConfiguration.getServiceAddress(), this.bcConfiguration.getSubcomponentPort(), waitingQueue);
			
			
			break;

		case CLIENT:
			
			observer = new CoapObserver(this, serviceRepresentation, bcConfiguration);
			

			break;
		default:
			break;
		}
	}

	@Override
	public void start(){
		System.out.println("waaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
			
			server.start();
			

			break;
		case CLIENT:
			
			
			observerThread = new Thread(observer);
			observerThread.start();
			
			break;
		default:
			break;
		}
	}

	

	@SuppressWarnings("deprecation")
	@Override
	public void stop() {
		switch (this.bcConfiguration.getSubcomponentRole()){
		case SERVER:
			// server.stop();
			observerThread.stop();
			break;
		case CLIENT:
			try {
				 this.client.shutdown();
			} catch (Exception e){
				
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void postOneway(final String destination, final Scope scope, final List<Data<?>> datas, final long lease) {
		
		JSONObject jsonObject = new JSONObject();

		for (Data<?> data : datas) {

			jsonObject.put(data.getName(), String.valueOf(data.getObject()));

		}
		if(jsonObject.containsKey("message_id")) {
			
			String message_id = (String) jsonObject.get("message_id");
		}
		
		String datasStream = jsonObject.toJSONString();
		waitingQueue.add(datasStream);
		 
	}

	@Override
	public void mgetOneway(final Scope scope, final Object exchange) {
		
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);
		
	}

	@Override
	public void xmgetOneway(final String source, final Scope scope, final Object exchange) {
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);
	}

	@Override
	public <T> T postTwowaySync(final String destination, final Scope scope, final List<Data<?>> datas,
			final long lease) {

//		T responseText = (T) this.client.sendTwoWayRequest(destination, scope, datas, lease);
		return null;
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
			final Object exchange){
		// TODO Auto-generated method stub
	}
	
	
	


	@Override
	public void setGmServiceRepresentation(GmServiceRepresentation serviceRepresentation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GmServiceRepresentation getGmServiceRepresentation(){
		// TODO Auto-generated method stub
		return null;
	}

}
