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
			mediator.setServiceEndpoint("localhost", "3000");

			// Corresponds to the IP address and port number of the CoAP endpoint
			mediator.setBusEndpoint("localhost", "4000");
			
			//mediator.setSecurity("/Users/zbenomar/eclipse-workspace/Testjetty2/src/main/java/testjetty2/mykeystore.jks");
			mediator.setSecurity("/Users/zbenomar/Desktop/CP4SC-Demo/DeXMS-CP4SC/dexms-examples/keystore.jks");

			
			String Path = "/Users/zbenomar/Desktop/CP4SC-Demo/DeXMS-CP4SC/dexms-examples/keystore.jks";
			
			String gidlFile = "src/main/java/org/zefxis/dexms/examples/randomValue.gidl";
			MediatorOutput output = mediator.generate(gidlFile, ProtocolType.HTTPS, "HTTP_TO_HTTPS");
			try {
				FileUtils.writeByteArrayToFile(new File("HTTP_TO_HTTPS.jar"), output.jar);
			} 
			catch (IOException e) {e.printStackTrace();}
			
		}
	}