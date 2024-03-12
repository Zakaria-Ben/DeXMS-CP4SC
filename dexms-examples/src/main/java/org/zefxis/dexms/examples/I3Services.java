package org.zefxis.dexms.examples;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.zefxis.dexms.gmdl.utils.enums.ProtocolType;
import org.zefxis.dexms.mediator.generator.MediatorGenerator;
import org.zefxis.dexms.mediator.manager.MediatorOutput;

public class I3Services {
		public static void main(String[] args) {
			// TODO Auto-generated method stub
						
			MediatorGenerator mediator = new MediatorGenerator();

			// Corresponds to the IP address and port number of the MQTT broker
			mediator.setServiceEndpoint("localhost", "2032");

			// Corresponds to the IP address and port number of the CoAP endpoint
			mediator.setBusEndpoint("localhost", "3333");
			
			//mediator.setSecurity("/Users/zbenomar/eclipse-workspace/Testjetty2/src/main/java/testjetty2/mykeystore.jks");
			mediator.setBusEndpointSecurity("/Users/zbenomar/Desktop/CP4SC-Demo/DeXMS-CP4SC/dexms-examples/keystore.jks");

			mediator.setServiceEndpointSecurity("/Users/zbenomar/Desktop/CP4SC-Demo/DeXMS-CP4SC/dexms-examples/keystore.jks");
			
			
			String gidlFile = "src/main/java/org/zefxis/dexms/examples/randomValue.gidl";
			MediatorOutput output = mediator.generate(gidlFile, ProtocolType.HTTPS, "MQTTS_TO_HTTPS");
			try {
				FileUtils.writeByteArrayToFile(new File("MQTTS_TO_HTTPS.jar"), output.jar);
			} 
			catch (IOException e) {e.printStackTrace();}
			
		}
	}