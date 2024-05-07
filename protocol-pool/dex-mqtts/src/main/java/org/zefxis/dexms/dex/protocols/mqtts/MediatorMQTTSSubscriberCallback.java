package org.zefxis.dexms.dex.protocols.mqtts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.Data.Context;
import org.zefxis.dexms.gmdl.utils.Data.MediaType;
import org.zefxis.dexms.gmdl.utils.enums.OperationType;


public class MediatorMQTTSSubscriberCallback implements MqttCallback {

	private MediatorMQTTSSubcomponent mediatorMQTTSSubComponent = null;
	private GmServiceRepresentation serviceRepresentation = null;

	public MediatorMQTTSSubscriberCallback(MediatorMQTTSSubcomponent mediatorMQTTSSubComponent,
			GmServiceRepresentation serviceRepresentation) {
		super();

		this.mediatorMQTTSSubComponent = mediatorMQTTSSubComponent;
		this.serviceRepresentation = serviceRepresentation;
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {

		
		String receivedText = new String(msg.getPayload());
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		
		try {

			jsonObject = (JSONObject) parser.parse(receivedText);

		} catch (ParseException e) {

			System.err.println(e.getMessage());
		}

		Context context = null;
		MediaType media = null;
		for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations().entrySet()) {
			if (en.getKey().equals(topic)) {
				Operation op = en.getValue();
				List<Data<?>> datas = new ArrayList<>();

				for (Data<?> data : op.getGetDatas()) {
					String stringValue = String.valueOf(jsonObject.get(data.getName()));
				    boolean parsingSuccessful = false; // Flag to track if parsing was successful
					try {
						int intValue = Integer.parseInt(stringValue);
						@SuppressWarnings("unchecked")
						Data<Integer> d = new Data(data.getName(), "Integer", true,
								intValue, data.getContext(),
								data.getMediaType());
						datas.add(d);
						System.out.println("This is data: "+ d);
						context = data.getContext();
						media = data.getMediaType();
						parsingSuccessful = true; // Set flag to true
					}
					catch (NumberFormatException e) {
						
					}
					if (!parsingSuccessful) {
	
					try {
						Float floatValue = Float.parseFloat(stringValue);
						@SuppressWarnings("unchecked")
						Data<Float> d = new Data(data.getName(), "Float", true,
								floatValue, data.getContext(),
								data.getMediaType());
						datas.add(d);
						System.out.println("This is data: "+ d);
						context = data.getContext();
						media = data.getMediaType();
						parsingSuccessful = true; // Set flag to true
					}
					catch (NumberFormatException e) {
						
					}
					}
					
					if (!parsingSuccessful) {
					
					try {
						Long longValue = Long.parseLong(stringValue);
						@SuppressWarnings("unchecked")
						Data<Long> d = new Data(data.getName(), "Long", true,
								longValue, data.getContext(),
								data.getMediaType());
						datas.add(d);
						System.out.println("This is data: "+ d);
						context = data.getContext();
						media = data.getMediaType();
						parsingSuccessful = true; // Set flag to true
					}
					catch (NumberFormatException e) {
						
					}
					if (!parsingSuccessful) {
						// Parsing failed for all types, treat as String
				        Data<String> d = new Data<>(data.getName(), "String", true,
				                                     stringValue, data.getContext(), data.getMediaType());
				        datas.add(d);
				        context = data.getContext();
				        media = data.getMediaType();
				    }
					
					}
				}
				if (op.getOperationType() == OperationType.TWO_WAY_SYNC) {

					String response = mediatorMQTTSSubComponent.mgetTwowaySync(op.getScope(), datas);
					//mediatorMQTTSubComponent.serverPublisher.publish(topic + "Reply", response.getBytes(), 2, false);

				} else if (op.getOperationType() == OperationType.ONE_WAY) {

					mediatorMQTTSSubComponent.mgetOneway(op.getScope(), datas);
				}
			}
		}
	}

	@Override
	public void connectionLost(Throwable arg0) {

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {

	}

}