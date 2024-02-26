package org.zefxis.dexms.dex.protocols.mqtts;

import java.util.UUID;
import java.util.Map.Entry;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.zefxis.dexms.dex.protocols.mqtts.MediatorMQTTSSubscriberCallback;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;

import org.zefxis.dexms.gmdl.utils.Scope;

import com.google.gson.JsonObject;

import org.zefxis.dexms.gmdl.utils.Data;

public class MediatorMQTTSSubcomponent extends MediatorGmSubcomponent{
	
	private static MqttConnectOptions options;
	MqttConnectOptions options_server;
	private static String broker;
	private static String clientId;
	private MqttClient client = null;
	MqttClient serverPublisher = null;
	private static String JKSPath;
	private static String op_name = null;
	private static Operation op = null;
	
	public MediatorMQTTSSubcomponent(MediatorConfiguration bcConfiguration,
			GmServiceRepresentation serviceRepresentation) {
		super(bcConfiguration);
		
		
		this.serviceRepresentation = serviceRepresentation;
		System.out.println("MQTTS "+ this.bcConfiguration.getSubcomponentRole()+" ssl://" + this.bcConfiguration.getSubcomponentAddress() + ":"
				+ this.bcConfiguration.getSubcomponentPort());
		System.out.println("JKS PATH:" + bcConfiguration.getJKSPath());
		System.out.println("JKS PATH:" + bcConfiguration.getJKSPath());
		System.out.println("JKS PATH:" + bcConfiguration.getJKSPath());
		JKSPath = this.bcConfiguration.getJKSPath();
		for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations().entrySet()) {

			op_name = en.getKey();
			op = en.getValue();
		}
				
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
			System.out.println("Configuring the MQTT subscriber");
			String JKSPath;
			JKSPath = bcConfiguration.getJKSPath();

			System.out.println("TLS handshake in progress");
			try {
			options_server = SSL_conf ();
			System.out.println("TLS handshake succesfully terminated, OK");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("A problem while doing the TLS handshake");
				e.printStackTrace();
			}		
			broker = "ssl://"+this.bcConfiguration.getServiceAddress()+":"+this.bcConfiguration.getSubcomponentPort();
			clientId = "Dexclient"+UUID.randomUUID();
			System.out.println("Client configuration:");
			System.out.println("The broker address: ssl://"+ this.bcConfiguration.getSubcomponentAddress() +":"+ this.bcConfiguration.getSubcomponentPort());
			System.out.println("The client ID used:"+ clientId);
			
			try {
				serverPublisher = new MqttClient(broker, clientId, new MemoryPersistence());

			} catch (MqttException e1) {
				e1.printStackTrace();
			}
			
			break;
		case CLIENT:
			// Configuration of the client: TLS handshake, ID,...
			System.out.println("Configuring the MQTT subscriber");
			String JKSPath_client;
			JKSPath_client = bcConfiguration.getJKSPath();
					
			System.out.println("TLS handshake in progress");
			try {
			options = SSL_conf ();
			System.out.println("TLS handshake succesfully terminated, OK");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("A problem while doing the TLS handshake");
				e.printStackTrace();
			}		
			broker = "ssl://"+this.bcConfiguration.getServiceAddress()+":"+this.bcConfiguration.getSubcomponentPort();
			clientId = "Dexclient"+UUID.randomUUID();
			System.out.println("Client configuration:");
			System.out.println("The broker address: ssl://"+ this.bcConfiguration.getSubcomponentAddress() +":"+ this.bcConfiguration.getSubcomponentPort());
			System.out.println("The client ID used:"+ clientId);
			
			try {
				
				client = new MqttClient(broker, clientId, new MemoryPersistence());
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			client.setCallback(new MediatorMQTTSSubscriberCallback(this, serviceRepresentation));
			
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
				String topic =  "randomValue";
				serverPublisher.connect(options_server);

			} catch (MqttException e) {
				e.printStackTrace();
			}
			
			break;
		case CLIENT:
				try {
					client.connect(options);
				} catch (MqttSecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			System.out.println("subscriber");
			
			System.out.println(" ServiceRepresentation  "+(Object) serviceRepresentation.toString());
        
			//client.connect(options);
        
     // Create an SslConnectionFactory with the SSL context
       
			// Subscribe to a topic
			
			System.out.println(" ServiceRepresentation  "+serviceRepresentation.getInterfaces().get(0).getOperations()
					.entrySet().size());
			
			for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations()
					.entrySet()) {
				try {
					client.subscribe((String) en.getKey());
					if(client.isConnected()){
						System.out.println(client.getClientId()+"subribes topic "+(String) en.getKey());
						
					}else{
						
						System.out.println("Not connected");
					}
				} catch (MqttException e) {
					e.printStackTrace();
				}
				
			}
			break;
			
			
			
			
//			String topic = this.bcConfiguration.get;
//			try {
//				client.subscribe(topic);
//			} catch (MqttException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

//			break;

		default:

//			break;
		}
	}

	@Override
	public void stop() {
		switch (this.bcConfiguration.getSubcomponentRole()) {
		case SERVER:
			try {
				serverPublisher.disconnect();
				serverPublisher.close();
				System.out.println("Stoping the MQTTs publisher... OK");
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			break;
		case CLIENT:
			try {
				client.close();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void postOneway(final String destination, final Scope scope, final List<Data<?>> datas, final long lease) {
		// TODO Auto-generated method stub
		System.out.println("This is postOneway of MQTTS");
		MqttMessage message = new MqttMessage();
		message.setQos(0);
		JSONObject jsonObject = new JSONObject();
		for (Data data : datas){
			jsonObject.put(data.getName(), String.valueOf(data.getObject()));

		}
		message.setPayload(jsonObject.toJSONString().getBytes());
		try {
		
			serverPublisher.publish("randomValue", message);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
		
	

	@Override
	public void mgetOneway(final Scope scope, final Object exchange) {
		System.out.println("This is mgetOneWay of MQTT, I'm sendint to setNextComponent this data: "+exchange.toString());
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);

	}

	@Override
	public void xmgetOneway(final String source, final Scope scope, final Object exchange) {
		this.nextComponent.postOneway(this.bcConfiguration.getServiceAddress(), scope, (List<Data<?>>) exchange, 0);
	}

	@Override
	public <T> T postTwowaySync(final String destination, final Scope scope, final List<Data<?>> datas,
			final long lease) {
		// TODO Auto-generated method stub
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
			final Object exchange) {
		// TODO Auto-generated method stub
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
	
	
	// This function is used for the TLS/SSL configuration

	public MqttConnectOptions SSL_conf () {
		KeyStore keyStore = null;
	try {
		keyStore = KeyStore.getInstance("JKS");
	} catch (KeyStoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}


	FileInputStream keyStoreFile = null;
	try {
		//keyStoreFile = new FileInputStream("/Users/zbenomar/Desktop/certificates-CP4SC/keystore.jks");
		keyStoreFile = new FileInputStream(JKSPath);

	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		keyStore.load(keyStoreFile, "testtest".toCharArray());
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (CertificateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	        
	        
	     // Initialize KeyManagerFactory with the keystore
	KeyManagerFactory keyManagerFactory = null;
	try {
		keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		keyManagerFactory.init(keyStore, "testtest".toCharArray());
	} catch (UnrecoverableKeyException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (KeyStoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	       
	     // Initialize TrustManagerFactory with your truststore (if needed)
	TrustManagerFactory trustManagerFactory = null;
	try {
		trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		trustManagerFactory.init(keyStore);
	} catch (KeyStoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	        //TrustManager[] trustAllCertificates = new TrustManager[]{new TrustAllCertificates()};

	     // Initialize SSLContext with the KeyManagerFactory and TrustManagerFactory
	       // sslContext = SSLContext.getInstance("TLS");
	        //SslContext sslContext = new SslContext(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(), null);
	        
	SSLContext sslContext_c = null;
	try {
		sslContext_c = SSLContext.getInstance("TLS");
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	        
	        try {
				sslContext_c.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        MqttConnectOptions conf_options = new MqttConnectOptions();
	        conf_options.setSocketFactory(sslContext_c.getSocketFactory());
			return conf_options;

			
        
	}
}