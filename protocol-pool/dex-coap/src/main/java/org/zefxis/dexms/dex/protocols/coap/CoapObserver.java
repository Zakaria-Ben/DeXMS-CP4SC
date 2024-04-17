package org.zefxis.dexms.dex.protocols.coap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.Data.Context;
import org.zefxis.dexms.gmdl.utils.Data.MediaType;
import org.zefxis.dexms.gmdl.utils.enums.OperationType;

public class CoapObserver implements Runnable {

	private CoapClient client = null;
	JSONParser parser = new JSONParser();
	JSONObject jsonObject = null;
	private static String op_name = null;
	private static Operation op = null;
	private String coapUri = null;
	private MediatorCoapSubcomponent bcGmSubcomponent;
	private GmServiceRepresentation serviceRepresentation;

	public CoapObserver(MediatorGmSubcomponent bcGmSubcomponent, GmServiceRepresentation serviceRepresentation,
			MediatorConfiguration bcConfiguration) {
		this.bcGmSubcomponent = (MediatorCoapSubcomponent) bcGmSubcomponent;
		this.serviceRepresentation = serviceRepresentation;

		for (Entry<String, Operation> en : serviceRepresentation.getInterfaces().get(0).getOperations().entrySet()) {

			op_name = en.getKey();
			op = en.getValue();
		}
		coapUri = "coap://" + bcConfiguration.getSubcomponentAddress() + ":" + bcConfiguration.getSubcomponentPort()
				+ "/" + op_name;

	}

	public void run() {

		System.out.println("coapUri " + coapUri);
		CoapClient client = new CoapClient(coapUri).useCONs();
		client = client.useExecutor();
		CoapObserveRelation relation = client.observe(

				new CoapHandler() {
					@Override
					public void onLoad(CoapResponse response) {

						Context context = null;
						MediaType media = null;
						String receivedText = response.getResponseText();
						String message_id = "";
						String message = receivedText;
						if(receivedText.contains("-")) {
							
							if (receivedText.split("-").length > 1) {

								message_id = receivedText.split("-")[1];
								message = receivedText.split("-")[0];
							}
						}
						
						JSONParser parser = new JSONParser();
						JSONObject jsonObject = null;

						try {
							jsonObject = (JSONObject) parser.parse(message);
						} catch (ParseException e) {
							e.printStackTrace();
						}
						

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
						


						if (!message_id.equals("")) {

							Data d = new Data<String>("op_name", "String", true, op_name, context, media);
							datas.add(d);

							d = new Data<String>("message_id", "String", true, message_id, context, media);
							datas.add(d);
						}

						if (op.getOperationType() == OperationType.TWO_WAY_SYNC) {

							bcGmSubcomponent.mgetTwowaySync(op.getScope(), datas);
						} else if (op.getOperationType() == OperationType.ONE_WAY) {

							bcGmSubcomponent.mgetOneway(op.getScope(), datas);
						}

					}

					@Override
					public void onError() {
						System.err.println("OBSERVING FAILED (press enter to exit)");
					}
				});

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			br.readLine();
		} catch (IOException e) {
		}

	}

}
