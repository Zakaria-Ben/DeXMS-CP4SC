package org.zefxis.dexms.service;

import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.zefxis.dexms.gmdl.utils.enums.ProtocolType;
import org.zefxis.dexms.mediator.generator.MediatorGenerator;
import org.zefxis.dexms.mediator.manager.MediatorOutput;

public class DexmsMediatorResource extends ServerResource {

	public DexmsMediatorResource() {

	}

	@Post
	public Representation generateMediator(Representation entity) throws ResourceException {
		String receivedText = null;
		try {

			receivedText = entity.getText();

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		System.out.println("received : " + receivedText);
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		String protocol = null;
		String interfaceService = null;
		String service_name = null;
		String JKS = null;

		try {

			jsonObject = (JSONObject) parser.parse(receivedText);
			protocol = (String) jsonObject.get("protocol");
			interfaceService = (String) jsonObject.get("interface");
			service_name = (String) jsonObject.get("service_name");

		} catch (ParseException e) {
			e.printStackTrace();
		}

		byte[] byteArray = stringToByteArray(interfaceService);

		ProtocolType busProtocol = null;
		switch (protocol.toUpperCase()) {
		case "REST":
			busProtocol = ProtocolType.REST;
			break;
		case "SOAP":
			busProtocol = ProtocolType.SOAP;
			break;
		case "MQTT":
			busProtocol = ProtocolType.MQTT;
			break;
		case "WEBSOCKETS":
			busProtocol = ProtocolType.WEB_SOCKETS;
			break;
		case "SEMI_SPACE":
			busProtocol = ProtocolType.SEMI_SPACE;
			break;
		case "JMS":
			busProtocol = ProtocolType.JMS;
			break;
		case "PUBNUB":
			busProtocol = ProtocolType.PUB_NUB;
			break;
		case "COAP":
			busProtocol = ProtocolType.COAP;
			break;
		case "ZERO_MQ":
			busProtocol = ProtocolType.ZERO_MQ;
			break;
		case "DPWS":

			busProtocol = ProtocolType.DPWS;
			break;
		default:

			busProtocol = null;
			break;
		}

		if (busProtocol == null) {

			MediatorOutput mediatorOutput = new MediatorOutput();
			return new ObjectRepresentation<MediatorOutput>(mediatorOutput);
		}
		MediatorGenerator mediator = new MediatorGenerator();
		MediatorOutput mediatorOutput = mediator.generateWar(byteArray, busProtocol, service_name, JKS);
		return new ObjectRepresentation<MediatorOutput>(mediatorOutput);
	}

	private byte[] stringToByteArray(String string) {

		byte[] array = new byte[string.length()];
		for (int i = 0; i < string.length(); i++) {
			array[i] = (byte) string.charAt(i);
		}
		return array;
	}
}
