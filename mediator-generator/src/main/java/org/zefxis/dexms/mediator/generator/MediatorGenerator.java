package org.zefxis.dexms.mediator.generator;

//import org.zefxis.dexms.Mediator.generators.JarGenerator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zefxis.dexms.artifact.generators.Generator;
import org.zefxis.dexms.artifact.generators.JarGenerator;
import org.zefxis.dexms.artifact.generators.WarGenerator;
import org.zefxis.dexms.dex.protocols.coap.MediatorCoapSubcomponent;
import org.zefxis.dexms.dex.protocols.coaps.MediatorCoapsSubcomponent;
import org.zefxis.dexms.dex.protocols.dpws.MediatorDPWSSubcomponent;
import org.zefxis.dexms.dex.protocols.mqtt.MediatorMQTTSubcomponent;
import org.zefxis.dexms.dex.protocols.mqtts.MediatorMQTTSSubcomponent;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.dex.protocols.rest.MediatorRestSubcomponent;
import org.zefxis.dexms.dex.protocols.soap.MediatorSoapGenerator;
import org.zefxis.dexms.dex.protocols.soap.MediatorSoapSubcomponent;
import org.zefxis.dexms.dex.protocols.https.MediatorHttpsSubcomponent;
import org.zefxis.dexms.dex.protocols.websocket.MediatorWebsocketSubcomponent;
import org.zefxis.dexms.gmdl.utils.Constants;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.PathResolver;
import org.zefxis.dexms.gmdl.utils.enums.ProtocolType;
import org.zefxis.dexms.mediator.manager.MediatorManagerRestService;
import org.zefxis.dexms.mediator.manager.MediatorOutput;
import org.zefxis.dexms.tools.logger.GLog;
import org.zefxis.dexms.tools.logger.Logger;
import org.zefxis.dexms.tools.serviceparser.ServiceDescriptionParser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.reflect.Parameter;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;



public class MediatorGenerator{
	
	private boolean STARTING_FROM_JAR = false;
	private ProtocolType serviceProtocol = null;
	private ProtocolType busProtocol = null;
	private Logger logger = GLog.initLogger();
	private static HashMap<String, String> mapParameter = new HashMap<String, String>();

	public MediatorGenerator(){

		// Test if class is running from jar file or from classes files.
		if (getClass().getClassLoader().getResource("config.json").toString().startsWith("jar")){

			STARTING_FROM_JAR = true;
		}
	}

	/**********************************************
	 * Public method to call
	 ***********************************************/

	/**
	 * Returns MediatorOutput object which contains the generated 1 byte array of
	 * archive jar and 1 array bytes of wsdl file
	 * <p>
	 *
	 * @param interfaceDescriptionPath
	 *            string path of a gidl or gmdl file
	 * @param busProtocol
	 *            the protocol type to use (SOAP, REST, ...)
	 * @param service_name
	 *            the name of the service will be generated
	 * @return VsbOutput object
	 */

	public MediatorOutput generate(String interfaceDescriptionPath, ProtocolType busProtocol, String service_name) {

		service_name = deleteSpecialChar(service_name);
		Constants.service_name = service_name;
		System.out.println("The protocol type here is:" + busProtocol);
		MediatorOutput vsbOutput = generate(interfaceDescriptionPath, busProtocol);
		return vsbOutput;
	}
	
	/**********************************************
	 * Public method to call
	 ***********************************************/

	/**
	 * Returns MediatorOutput object which contains the generated 1 byte array of
	 * archive war and 1 array bytes of wsdl file
	 * <p>
	 *
	 * @param interfaceDescriptionPath
	 *            string path of a gidl or gmdl file
	 * @param busProtocol
	 *            the protocol type to use (SOAP, REST, ...)
	 * @param service_name
	 *            the name of the service will be generated
	 * @return VsbOutput object
	 */

	public MediatorOutput generateWar(String interfaceDescriptionPath, ProtocolType busProtocol, String service_name) {

		return generate(interfaceDescriptionPath,busProtocol, service_name);
	}

	/**
	 * Returns VsbOutput object which contains the generated 1 byte array of
	 * archive war and 1 array bytes of wsdl file
	 * <p>
	 *
	 * @param interfaceDescriptionByteArray
	 *            array of a gidl or gmdl file
	 * @param busProtocol
	 *            the protocole type to use (SOAP, REST, ...)
	 * @param service_name
	 *            the name of the service will be generated
	 * @return VsbOutput object
	 */

	public MediatorOutput generateWar(byte[] interfaceDescriptionByteArray, ProtocolType busProtocol, String service_name, String JKSPath) {

		service_name = deleteSpecialChar(service_name);
		Constants.service_name = service_name;
		String interfaceDescriptionPath = interfaceDescriptionBytesToFile(interfaceDescriptionByteArray);
		MediatorOutput vsbOutput = generate(interfaceDescriptionPath, busProtocol, JKSPath);

		File interfaceDescriptionFile = new File("interfaceDescription.gidl");
		if (interfaceDescriptionFile.exists()){

			interfaceDescriptionFile.delete();
		}
		return vsbOutput;
	}

	/**
	 * Returns boolean, it deletes all generated temporary files
	 * <p>
	 * 
	 * @return boolean
	 */
	

	public boolean deleteGeneratedFiles() {

		File directory = new File(Constants.generatedCodePath);
		// make sure directory exists
		if (!directory.exists()) {

			logger.e(this.getClass().getName(), "Delete request failed: Directory does not exist.");
			return false;
		}
		PathResolver.deleteTempDir(directory);
		return true;
	}

	public void setServiceEndpoint(String host, String port){
		
		mapParameter.put("host_service", host);
		mapParameter.put("port_service", port);
		
	}
	
	
    public void setBusEndpoint(String host, String port){
		
    	mapParameter.put("host_bus", host);
    	mapParameter.put("port_bus", port);
	}
    
    public void setSecurity(String path) {
    	mapParameter.put("JKSPath", path);
    }
	
	/******************************************************************************************************/

	/***************
	 * Private method called by public method
	 ************************************************/

	private void setConstants(String interfaceDescriptionPath){

		Random rand = new Random();
		Constants.service_port = String
				.valueOf(rand.nextInt(Constants.service_port_max_range - Constants.service_port_min_range + 1)
						+ Constants.service_port_min_range);
		Constants.setinvaddr_service_port = String.valueOf(rand
				.nextInt(Constants.setinvaddr_service_port_max_range - Constants.setinvaddr_service_port_min_range + 1)
				+ Constants.setinvaddr_service_port_min_range);
		Constants.service_bc_port = String
				.valueOf(rand.nextInt(Constants.service_bc_port_max_range - Constants.service_bc_port_min_range + 1)
						+ Constants.service_bc_port_min_range);
		Constants.bc_manager_servlet_port = String.valueOf(rand
				.nextInt(Constants.bc_manager_servlet_port_max_range - Constants.bc_manager_servlet_port_min_range + 1)
				+ Constants.bc_manager_servlet_port_min_range);
		
		Constants.subcomponent_port = String.valueOf(rand
				.nextInt(Constants.subcomponent_port_max_range - Constants.subcomponent_port_min_range + 1)
				+ Constants.subcomponent_port_min_range);
		
		
		Constants.printer_service_port = String.valueOf(rand
				.nextInt(Constants.printer_service_port_max_range - Constants.printer_service_port_min_range + 1)
				+ Constants.printer_service_port_min_range);
		
		Constants.generatedCodePath = PathResolver.createTempDir();

		Class[] classe = { MediatorManagerRestService.class };
		Constants.configFilePath = PathResolver.myFilePath(MediatorManagerRestService.class, "config.json");

		Constants.interfaceDescriptionFilePath = interfaceDescriptionPath;
		String artifact_generators_copy = Constants.generatedCodePath + File.separator + "artifact-generators";

		// Test if class is running from jar or from class file.
		if (STARTING_FROM_JAR){
			
			URL webapp_src_artifact = getClass().getClassLoader().getResource("webapp");
			if(webapp_src_artifact == null) {
				
				logger.e(this.getClass().getName(), "Could not find artifact-generators resource");
				return;
			}
			PathResolver.extractDirectoryFromJar(webapp_src_artifact, artifact_generators_copy);

		} else{

			PathResolver.copyFolder(new File(".." + File.separator + "artifact-generators" + File.separator + "src"+ File.separator +"main").getAbsolutePath(),
					artifact_generators_copy);
		}

		PathResolver.updatePortServiceSetInvAddr(Constants.setinvaddr_service_port);
		Constants.webapp_src_artifact = new File(artifact_generators_copy + File.separator +  File.separator + "webapp").getAbsolutePath();
		Constants.warName = "war" + Constants.service_name + "BC";
		Constants.wsdlName = "wsdl" + Constants.service_name + "BC";
		Constants.warDestination = new File(Constants.generatedCodePath + File.separator + Constants.warName + ".war")
				.getAbsolutePath();
		Constants.target_namespace = "org.zefxis.dexms.mediator.generated";
		Constants.target_namespace_path = Constants.target_namespace.replace(".", File.separator);
		Constants.soap_service_name = "BC" + Constants.service_name + "SoapEndpoint";
		Constants.dpws_service_name = "BC" + Constants.service_name + "DPWSEndpoint";
		Constants.wsdlDestination = new File(Constants.generatedCodePath).getAbsolutePath();
	}

	private MediatorOutput generate(String interfaceDescriptionPath, ProtocolType busProtocol) {

		if (!isInterfaceDescriptionFile(interfaceDescriptionPath)) {

			logger.e(this.getClass().getName(), "Interface Description file not found or not good file extension");
			throw new EmptyStackException();
		}

		setConstants(interfaceDescriptionPath);

		generateMediator(interfaceDescriptionPath, busProtocol);

		WarGenerator warGenerator = new WarGenerator();
		JarGenerator jarGenerator = new JarGenerator();

		warGenerator.addPackage(org.zefxis.dexms.mediator.generator.MediatorGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.tools.serviceparser.ServiceDescriptionParser.class.getPackage());


		warGenerator.addPackage(org.zefxis.dexms.tools.serviceparser.gidl.ParseGIDL.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.gmdl.utils.Operation.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.gmdl.utils.enums.OperationType.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.dpws.MediatorDPWSGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.mqtt.MediatorMQTTGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.mqtts.MediatorMQTTSGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.websocket.MediatorWebsocketGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.coap.MediatorCoapGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.coaps.MediatorCoapsGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.soap.MediatorSoapGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.rest.MediatorRestGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.https.MediatorHttpsGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.https.MediatorHttpsSubcomponent.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.mediator.manager.MediatorManagerRestService.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.artifact.war.RestServlet.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.artifact.generators.WarGenerator.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.mediator.manager.setinvaddrservice.BaseService.class.getPackage());
		warGenerator.addPackage(org.zefxis.dexms.dex.protocols.Manageable.class.getPackage());
		
		String gm_soap_pomxml;
		String gm_coap_pomxml;
		String gm_coaps_pomxml;
		String gm_dpws_pomxml;
		String gm_websocket_pomxl;
		String gm_mqtt_pomxl;
		String gm_mqtts_pomxl;
		String gm_rest_pomxl;
		String gm_soap_war_pomxml;
		String gm_https_pomxl;

		if (STARTING_FROM_JAR){

			gm_soap_pomxml = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-soap.xml");
			gm_coap_pomxml = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-coap.xml");
			gm_coaps_pomxml = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-coaps.xml");
			gm_dpws_pomxml = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-dpws.xml");
			gm_websocket_pomxl = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-websocket.xml");
			gm_rest_pomxl = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-rest.xml");
			gm_mqtt_pomxl = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-mqtt.xml");
			gm_mqtts_pomxl = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-mqtts.xml");
			gm_soap_war_pomxml = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-soap-war.xml");
			gm_https_pomxl = PathResolver.myFilePath(MediatorManagerRestService.class, "pom-gm-https.xml");
	

		}else{

			gm_soap_pomxml = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-soap.xml";

			gm_coap_pomxml = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-coap.xml";
			gm_coaps_pomxml = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-coaps.xml";
			gm_dpws_pomxml = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-dpws.xml";
			gm_websocket_pomxl = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-websocket.xml";
			gm_rest_pomxl = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-rest.xml";
			gm_mqtt_pomxl = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-mqtt.xml";
			gm_soap_war_pomxml = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-soap-war.xml";
			gm_https_pomxl = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-https.xml";
			gm_mqtts_pomxl = new File(".").getAbsolutePath() + File.separator + ".." +File.separator + "mediator-generator" + File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "pom-gm-mqtts.xml";
		}

		HashMap<String, String> hmapPomXml = new HashMap<String, String>();

		hmapPomXml.put("coap", gm_coap_pomxml);
		hmapPomXml.put("coaps", gm_coaps_pomxml);
		hmapPomXml.put("dpws", gm_dpws_pomxml);
		hmapPomXml.put("mqtt", gm_mqtt_pomxl);
		hmapPomXml.put("websocket", gm_websocket_pomxl);
		hmapPomXml.put("soap", gm_soap_pomxml);
		hmapPomXml.put("rest", gm_rest_pomxl);
		hmapPomXml.put("soapwar", gm_soap_war_pomxml);
		hmapPomXml.put("https", gm_https_pomxl);
		hmapPomXml.put("mqtts", gm_mqtts_pomxl);


		Generator generator = new Generator(busProtocol);
		MediatorOutput vsbOutput = new MediatorOutput();
		vsbOutput.generatedCodePath = Constants.generatedCodePath;
		vsbOutput.service_name = Constants.service_name;
		String path = mapParameter.get("JKSPath");
		vsbOutput.JKSPath = path;
		
		Class[] classesOptions = new Class[]{

				MediatorManagerRestService.class, MediatorGmSubcomponent.class, MediatorGmSubcomponent.class, MediatorWebsocketSubcomponent.class, MediatorRestSubcomponent.class,
				MediatorSoapSubcomponent.class, MediatorCoapSubcomponent.class, MediatorDPWSSubcomponent.class, MediatorMQTTSubcomponent.class,
				MediatorSoapSubcomponent.class, MediatorCoapSubcomponent.class, MediatorCoapsSubcomponent.class, MediatorDPWSSubcomponent.class, MediatorMQTTSubcomponent.class,
				ServiceDescriptionParser.class, MediatorConfiguration.class, MediatorSoapSubcomponent.class, MediatorHttpsSubcomponent.class, MediatorMQTTSSubcomponent.class, ObjectMapper.class,
				TypeFactory.class, Versioned.class, ResolvedType.class, JsonProperty.class 
		};
		
		if(busProtocol == ProtocolType.SOAP ){
			
			warGenerator.addDependencyFiles(hmapPomXml.get("soapwar"));
			generator.compileGeneratedClasses(classesOptions);
			warGenerator.setBusProtocol(this.busProtocol);
			warGenerator.setServiceProtocol(this.serviceProtocol);	
			vsbOutput = generator.generateWar(warGenerator, busProtocol);
			vsbOutput.service_bc_port = Constants.service_bc_port;
			vsbOutput.setinvaddr_service_port = Constants.setinvaddr_service_port;
			vsbOutput.service_port = Constants.service_port;
			vsbOutput.bc_manager_servlet_port = Constants.bc_manager_servlet_port;
			vsbOutput.printer_service_port = Constants.printer_service_port;
			vsbOutput.subcomponent_port = Constants.subcomponent_port;
			
		}else{
			
			if(busProtocol != ProtocolType.SOAP){
					
				vsbOutput.service_bc_port = Constants.service_bc_port;
				vsbOutput.setinvaddr_service_port = Constants.setinvaddr_service_port;
				vsbOutput.service_port = Constants.service_port;
				vsbOutput.bc_manager_servlet_port = Constants.bc_manager_servlet_port;
				vsbOutput.printer_service_port = Constants.printer_service_port;
				vsbOutput.subcomponent_port = Constants.subcomponent_port;
			}
			generator.compileGeneratedClasses(classesOptions);
			jarGenerator.setBusProtocol(this.busProtocol);
			jarGenerator.setServiceProtocol(this.serviceProtocol);
			jarGenerator.addDependencyFiles(hmapPomXml);
			vsbOutput.jar = generator.generateJar(jarGenerator);
		}
		return vsbOutput;
	}

	private void generateMediator(final String interfaceDescription, final ProtocolType busProtocol) {

		copyInterfaceDescription(interfaceDescription);

		MediatorConfiguration bcConfiguration = null;
		bcConfiguration = new MediatorConfiguration();

		bcConfiguration.setGeneratedCodePath(Constants.generatedCodePath);
		String path = mapParameter.get("JKSPath");
		bcConfiguration.setJKSPath(path);		

		GmServiceRepresentation gmServiceRepresentation = null;

		String interfaceDescFileExtension = "";
		String[] interfaceDescFileNameComponents = interfaceDescription.split("\\.");
		interfaceDescFileExtension = interfaceDescFileNameComponents[interfaceDescFileNameComponents.length - 1];

		switch (interfaceDescFileExtension) {
		case "gmdl":
			gmServiceRepresentation = ServiceDescriptionParser.getRepresentationFromGMDL(interfaceDescription);
			break;
		case "gidl":
			gmServiceRepresentation = ServiceDescriptionParser.getRepresentationFromGIDL(interfaceDescription);
			break;
		}

		this.busProtocol = busProtocol;
		this.serviceProtocol = gmServiceRepresentation.getProtocol();
		
		
		if (busProtocol == ProtocolType.SOAP){

			bcConfiguration.setTargetNamespace(Constants.target_namespace);
			bcConfiguration.setServiceName(Constants.soap_service_name);

			MediatorSoapGenerator soapGenerator = (MediatorSoapGenerator) new MediatorSoapGenerator(gmServiceRepresentation,
					bcConfiguration).setDebug(false);
			soapGenerator.generatePOJOAndEndpoint();
			soapGenerator.generateWSDL();

		}
		generateMediatorClass(gmServiceRepresentation, busProtocol);
		if(busProtocol != ProtocolType.SOAP) {
			
			generateMediatorMainClass();
		}
		
		
	}

	private void copyInterfaceDescription(String interfaceDescription){

		try {

			File input = new File(interfaceDescription);
			File output = new File(Constants.webapp_src_artifact + File.separator + "config" + File.separator
					+ "serviceDescription.gxdl");
			Scanner sc = new Scanner(input);
			PrintWriter printer = new PrintWriter(output);
			while (sc.hasNextLine()) {

				String s = sc.nextLine();
				printer.write(s);
			}
			sc.close();
			printer.close();
		} catch (FileNotFoundException e) {

			logger.e(this.getClass().getName(), "Interface file not found.");
		}
	}

	private static void generateMediatorClass(GmServiceRepresentation gmServiceRepresentation,ProtocolType busProtocol){

		String configTemplatePath = "";
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;

		configTemplatePath = Constants.configFilePath;

		try {
			jsonObject = (JSONObject) parser.parse(new FileReader(configTemplatePath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}

		/* Creating java code model classes */
		JCodeModel jCodeModel = new JCodeModel();
		/* Adding package here */
		JPackage jp = jCodeModel._package((String) jsonObject.get("target_namespace"));

		/* Giving Class Name to Generate */
		JDefinedClass jc = null;
		try {

			jc = jp._class("Mediator");

		} catch (JClassAlreadyExistsException e) {

			e.printStackTrace();
		}

		JClass BcGmSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent.class);
		JFieldVar ComponentsVar = jc.field(JMod.NONE, BcGmSubcomponentClass.array().array(), "subcomponent");

		JClass GmServiceRepresentationClass = jCodeModel
				.ref(org.zefxis.dexms.gmdl.utils.GmServiceRepresentation.class);
		JFieldVar GmServiceRepresentationVar = jc.field(JMod.NONE, GmServiceRepresentationClass,
				"gmServiceRepresentation", JExpr._null());

		JMethod jmCreate = jc.method(JMod.PUBLIC, void.class, "run");

		/* Adding method body */
		JBlock jBlock = jmCreate.body();

		JClass integerClass = jCodeModel.ref(java.lang.Integer.class);
		JClass StringClass = jCodeModel.ref(java.lang.String.class);

		JVar intFiveVar = jBlock.decl(integerClass, "intFive");
		jBlock.assign(JExpr.ref(intFiveVar.name()), jCodeModel.ref("Integer").staticInvoke("parseInt").arg("5"));

		JVar intOneVar = jBlock.decl(integerClass, "intOne");
		jBlock.assign(JExpr.ref(intOneVar.name()), jCodeModel.ref("Integer").staticInvoke("parseInt").arg("1"));

		JVar intNineVar = jBlock.decl(integerClass, "intNine");
		jBlock.assign(JExpr.ref(intNineVar.name()), jCodeModel.ref("Integer").staticInvoke("parseInt").arg("9"));

		JClass BcManagerClass = null;
		BcManagerClass = jCodeModel.ref(org.zefxis.dexms.mediator.manager.MediatorManagerRestService.class);
		JClass ConstantsClass = jCodeModel.ref(org.zefxis.dexms.gmdl.utils.Constants.class);

		JClass FileClass = jCodeModel.ref(java.io.File.class);
		JClass BcManagerRestServiceClass = jCodeModel.ref(org.zefxis.dexms.mediator.manager.MediatorManagerRestService.class);

		
		
		JClass pathResolverClass = null;
		pathResolverClass = jCodeModel.ref(org.zefxis.dexms.gmdl.utils.PathResolver.class);

		JVar pathResolverClassVar = jBlock.decl(pathResolverClass, "pathResolver").init(JExpr._new(pathResolverClass));

		JInvocation myFilePath = pathResolverClassVar.invoke("myFilePath").arg(jc.dotclass())
				.arg((JExpr._new(StringClass).arg("config")).plus(FileClass.staticRef("separator"))
						.plus(JExpr._new(StringClass).arg("serviceDescription.gxdl")));

		JVar interfaceDescriptionPathVar = null;
		interfaceDescriptionPathVar = jBlock.decl(StringClass, "interfaceDescFilePath");
		jBlock.assign(JExpr.ref(interfaceDescriptionPathVar.name()), myFilePath);

		JClass SystemClass = jCodeModel.ref(System.class);
		JInvocation printstmt = SystemClass.staticRef("out").invoke("println").arg(interfaceDescriptionPathVar);
		jBlock.add(printstmt);



		JClass serviceDescriptionClass = jCodeModel.ref(ServiceDescriptionParser.class);

		String interfaceDescriptionPath = Constants.interfaceDescriptionFilePath;
		String extension = "";
		String[] interfaceDescPieces = interfaceDescriptionPath.split("\\.");

		extension = interfaceDescPieces[interfaceDescPieces.length - 1];
		JInvocation getInterfaceRepresentation = null;

		switch (extension) {

		case "gmdl":
			getInterfaceRepresentation = serviceDescriptionClass.staticInvoke("getRepresentationFromGMDL")
					.arg(interfaceDescriptionPathVar);
			break;
		case "gidl":
			getInterfaceRepresentation = serviceDescriptionClass.staticInvoke("getRepresentationFromGIDL")
					.arg(interfaceDescriptionPathVar);
			break;
		}

		jBlock.assign(GmServiceRepresentationVar, getInterfaceRepresentation);

		jBlock.decl(jCodeModel.INT, "num_interfaces",
				GmServiceRepresentationVar.invoke("getInterfaces").invoke("size"));

		jBlock.assign(ComponentsVar, JExpr.ref("new MediatorGmSubcomponent[num_interfaces][2]"));

		JForLoop forLoop = jBlock._for();
		JVar ivar = forLoop.init(jCodeModel.INT, "i", JExpr.lit(0));
		forLoop.test(ivar.lt(GmServiceRepresentationVar.invoke("getInterfaces").invoke("size")));
		forLoop.update(ivar.assignPlus(JExpr.lit(1)));

		JBlock forBlock = forLoop.body();

		JClass InterfaceClass = jCodeModel.ref(org.zefxis.dexms.gmdl.utils.Interface.class);
		JVar InterfaceVar = forBlock.decl(InterfaceClass, "inter", JExpr._null());

		forBlock.assign(JExpr.ref(InterfaceVar.name()),
				GmServiceRepresentationVar.invoke("getInterfaces").invoke("get").arg(ivar));

		JClass RoleTypeClass = jCodeModel.ref(org.zefxis.dexms.gmdl.utils.enums.RoleType.class);
		JClass BcRestSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.rest.MediatorRestSubcomponent.class);
		JClass BcSoapSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.soap.MediatorSoapSubcomponent.class);
		JClass BcMQTTSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.mqtt.MediatorMQTTSubcomponent.class);
		JClass BcCoapSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.coap.MediatorCoapSubcomponent.class);
		JClass BcCoapsSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.coaps.MediatorCoapsSubcomponent.class);
		JClass BcDpwsSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.dpws.MediatorDPWSSubcomponent.class);
		JClass BcWebSocketSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.websocket.MediatorWebsocketSubcomponent.class);
		JClass BcHttpsSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.https.MediatorHttpsSubcomponent.class);
		JClass BcMQTTSSubcomponentClass = jCodeModel
				.ref(org.zefxis.dexms.dex.protocols.mqtts.MediatorMQTTSSubcomponent.class);

		JClass BcConfigurationClass = jCodeModel.ref(org.zefxis.dexms.gmdl.utils.MediatorConfiguration.class);
		

		JClass EnumClass = jCodeModel.ref("org.zefxis.dexms.gmdl.utils.enums.RoleType");

		JFieldRef RoleTypeServerEnum = null;
		JFieldRef RoleTypeClientEnum = null;

		RoleTypeServerEnum = EnumClass.staticRef("SERVER");
		RoleTypeClientEnum = EnumClass.staticRef("CLIENT");

		JVar RoleTypeClassVar = forBlock.decl(RoleTypeClass, "busRole");

		JConditional roleCondition = forBlock._if(InterfaceVar.invoke("getRole").eq(RoleTypeServerEnum));
		roleCondition._then().assign(JExpr.ref(RoleTypeClassVar.name()), RoleTypeClientEnum);
		roleCondition._else().assign(JExpr.ref(RoleTypeClassVar.name()), RoleTypeServerEnum);

		JVar bcConfig1Var = forBlock.decl(BcConfigurationClass, "bcConfiguration1", JExpr._new(BcConfigurationClass));
		JVar bcConfig2Var = forBlock.decl(BcConfigurationClass, "bcConfiguration2", JExpr._new(BcConfigurationClass));

		JInvocation setRole1 = bcConfig1Var.invoke("setSubcomponentRole").arg(InterfaceVar.invoke("getRole"));
		forBlock.add(setRole1);

		JInvocation setRole2 = bcConfig2Var.invoke("setSubcomponentRole").arg(RoleTypeClassVar);
		forBlock.add(setRole2);

		String packagePath = Constants.target_namespace;
		packagePath = packagePath.replace(".", File.separator);

		String generatedCodePath = Constants.generatedCodePath;
		generatedCodePath = generatedCodePath + File.separator;

		
		JInvocation myFilePath1 = pathResolverClassVar.invoke("myFilePath").arg(jc.dotclass())
				.arg((JExpr._new(StringClass).arg("config")).plus(FileClass.staticRef("separator"))
						.plus(JExpr._new(StringClass).arg("config_block1_interface_")).plus(jCodeModel
								.ref(java.lang.String.class).staticInvoke("valueOf").arg(ivar.plus(intOneVar))));

		JVar config_block1_interfacePathVar = forBlock.decl(StringClass, "config_block1_interfacePath");
		forBlock.assign(JExpr.ref(config_block1_interfacePathVar.name()), myFilePath1);

		JInvocation parseInvocation1 = bcConfig1Var.invoke("parseFromJSON").arg(GmServiceRepresentationVar)
				.arg(JExpr._new(StringClass).arg(config_block1_interfacePathVar));

	

		forBlock.add(parseInvocation1);

		JInvocation myFilePath2 = pathResolverClassVar.invoke("myFilePath").arg(jc.dotclass())
				.arg((JExpr._new(StringClass).arg("config")).plus(FileClass.staticRef("separator"))
						.plus(JExpr._new(StringClass).arg("config_block2_interface_")).plus(jCodeModel
								.ref(java.lang.String.class).staticInvoke("valueOf").arg(ivar.plus(intOneVar))));

		JVar config_block2_interfacePathVar = forBlock.decl(StringClass, "config_block2_interfacePath");
		forBlock.assign(JExpr.ref(config_block2_interfacePathVar.name()), myFilePath2);

		JInvocation parseInvocation2 = bcConfig2Var.invoke("parseFromJSON").arg(GmServiceRepresentationVar)
				.arg(JExpr._new(StringClass).arg(config_block2_interfacePathVar));


		forBlock.add(parseInvocation2);


		switch (busProtocol){
		case REST:

			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.REST, gmServiceRepresentation.getHostAddress(),Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcRestSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			// BcGmSubcomponentVar1.init(JExpr._new(BcRestSubcomponentClass).arg(bcConfig1Var));
			break;
		
		case SOAP:
			for(int i=1; i<=gmServiceRepresentation.getInterfaces().size(); i++)  
				createConfigFileBusProtocole(ProtocolType.SOAP, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"), JExpr._new(BcSoapSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			

			break;
		case MQTT:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.MQTT, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcMQTTSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			break;
		case COAP:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.COAP, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcCoapSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			break;
			
		case COAPS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.COAPS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcCoapsSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			break;

		case DPWS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.DPWS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcDpwsSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			break;
			
		case HTTPS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.HTTPS, gmServiceRepresentation.getHostAddress(),Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcHttpsSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			break;
			
		case MQTTS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.MQTTS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcMQTTSSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			break;
			
		case JMS:
			break;
		case PUB_NUB:
			break;
		case SEMI_SPACE:
			break;
		case WEB_SOCKETS:
			
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileBusProtocole(ProtocolType.WEB_SOCKETS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block1_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][0]"),
					JExpr._new(BcWebSocketSubcomponentClass).arg(bcConfig1Var).arg(GmServiceRepresentationVar));
			
			
			break;
		case ZERO_MQ:
			break;
		default:
			break;
		}

		
		switch(gmServiceRepresentation.getProtocol()){
		
		case REST:
			for(int i=1; i<=gmServiceRepresentation.getInterfaces().size(); i++)  
				createConfigFileGmServiceProtocole(ProtocolType.REST, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator + "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"), JExpr._new(BcRestSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			
			
			break;
		case SOAP:
			for(int i=1; i<=gmServiceRepresentation.getInterfaces().size(); i++)  
				createConfigFileGmServiceProtocole(ProtocolType.SOAP, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator + "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"), JExpr._new(BcSoapSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));

			
			break;
		case MQTT:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileGmServiceProtocole(ProtocolType.MQTT, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"),
					JExpr._new(BcMQTTSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			break;
		case COAP:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileGmServiceProtocole(ProtocolType.COAP, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"),
					JExpr._new(BcCoapSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			break;
			
		case COAPS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileGmServiceProtocole(ProtocolType.COAPS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"),
					JExpr._new(BcCoapsSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			break;

		case DPWS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileGmServiceProtocole(ProtocolType.DPWS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"),
					JExpr._new(BcDpwsSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			break;
			
		case WEB_SOCKETS:
				
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileGmServiceProtocole(ProtocolType.WEB_SOCKETS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"),
					JExpr._new(BcWebSocketSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			
			break;
		
		case HTTPS:
			for(int i=1; i<=gmServiceRepresentation.getInterfaces().size(); i++)  
				createConfigFileGmServiceProtocole(ProtocolType.HTTPS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator + "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"), JExpr._new(BcHttpsSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			
			break;
			
		case MQTTS:
			for (int i = 1; i <= gmServiceRepresentation.getInterfaces().size(); i++)
				createConfigFileGmServiceProtocole(ProtocolType.MQTTS, gmServiceRepresentation.getHostAddress(), Constants.webapp_src_artifact + File.separator
						+ "config" + File.separator + "config_block2_interface_" + String.valueOf(i));
			forBlock.assign(JExpr.ref("subcomponent[i][1]"),
					JExpr._new(BcMQTTSSubcomponentClass).arg(bcConfig2Var).arg(GmServiceRepresentationVar));
			break;
		case JMS:
			break;
		case PUB_NUB:
			break;
		case SEMI_SPACE:
			break;
		case ZERO_MQ:
			break;
		default:
			break;
		}

		JVar BcGmSubcomponentVar1 = forBlock.decl(BcGmSubcomponentClass, "block1Component", JExpr.ref("subcomponent[i][0]"));
		JVar BcGmSubcomponentVar2 = forBlock.decl(BcGmSubcomponentClass, "block2Component", JExpr.ref("subcomponent[i][1]"));

		forBlock.add(BcGmSubcomponentVar1.invoke("setNextComponent").arg(BcGmSubcomponentVar2));
		forBlock.add(BcGmSubcomponentVar2.invoke("setNextComponent").arg(BcGmSubcomponentVar1));

		forBlock.add(BcGmSubcomponentVar1.invoke("start"));
		forBlock.add(BcGmSubcomponentVar2.invoke("start"));

		JMethod jmCreatePause = jc.method(JMod.PUBLIC, void.class, "pause");
		JBlock jBlockPause = jmCreatePause.body();

		JForLoop forLoopPause = jBlockPause._for();
		JVar ivarPause = forLoopPause.init(jCodeModel.INT, "i", JExpr.lit(0));
		forLoopPause.test(ivarPause.lt(GmServiceRepresentationVar.invoke("getInterfaces").invoke("size")));
		forLoopPause.update(ivarPause.assignPlus(JExpr.lit(1)));

		JBlock forBlockPause = forLoopPause.body();

		JInvocation printstmtPause = SystemClass.staticRef("out").invoke("println").arg("genfac stop iteration");
		forBlockPause.add(printstmtPause);

		JVar BcGmSubcomponentVar1Pause = forBlockPause.decl(BcGmSubcomponentClass, "block1Component",
				JExpr.ref("subcomponent[i][0]"));
		JVar BcGmSubcomponentVar2Pause = forBlockPause.decl(BcGmSubcomponentClass, "block2Component",
				JExpr.ref("subcomponent[i][1]"));

		forBlockPause.add(BcGmSubcomponentVar1Pause.invoke("stop"));
		forBlockPause.add(BcGmSubcomponentVar2Pause.invoke("stop"));

		try {

			jCodeModel.build(new File(generatedCodePath));

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private static void generateMediatorMainClass(){

		String configTemplatePath = "";
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;

		configTemplatePath = Constants.configFilePath;

		try {
			jsonObject = (JSONObject) parser.parse(new FileReader(configTemplatePath));
		} catch (IOException | ParseException e){
			
			e.printStackTrace();
		}

		/* Creating java code model classes */
		JCodeModel jCodeModel = new JCodeModel();
		
		/* Adding package here */
		JPackage jp = jCodeModel._package((String) jsonObject.get("target_namespace"));

		/* Giving Class Name to Generate */
		JDefinedClass jc = null;
		try {

			jc = jp._class("MediatorMain");

		} catch (JClassAlreadyExistsException e) {

			e.printStackTrace();
		}

		JClass StringClass = jCodeModel.ref(String.class);
		JClass bindingComponent = jCodeModel.ref(Constants.target_namespace + ".Mediator");

		JMethod jmMain = jc.method(JMod.PUBLIC | JMod.STATIC, void.class, "main");
		jmMain.param(StringClass.array(), "args");
		JBlock jmMainBlock = jmMain.body();
		JVar bindingComponentVar = jmMainBlock.decl(bindingComponent, "mediator")
				.init(JExpr._new(bindingComponent));

		JInvocation run = bindingComponentVar.invoke("run");
		jmMainBlock.add(run);
		try{
			
			jCodeModel.build(new File(Constants.generatedCodePath + File.separator));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	private static void createConfigFileBusProtocole(ProtocolType protocol, String host,  String filename){
		
		

    	
    	
		String configPath = PathResolver.myFilePath(MediatorManagerRestService.class, "config.json");
		System.out.println("This is the config file path:" +configPath);
		System.out.println("This is the config file path:" +configPath);
		System.out.println("This is the config file path:" +configPath);
		System.out.println("This is the config file path:" +configPath);
		JSONParser configParser = new JSONParser();
		JSONObject configJsonObject = null;

		String configTemplatePath = PathResolver.myFilePath(MediatorManagerRestService.class, "template-config.json");

		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;

		try {
			configJsonObject = (JSONObject) configParser.parse(new FileReader(configPath));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}

		try {

			jsonObject = (JSONObject) parser.parse(new FileReader(configTemplatePath));

		} catch (IOException | ParseException e) {

			e.printStackTrace();
		}

		String host_bus = mapParameter.get("host_bus");
		String port = mapParameter.get("port_bus");
		String path = mapParameter.get("JKSPath");
		
		switch (protocol) {

		case REST:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);
			jsonObject.put("subcomponent_port", port);

			break;
		case SOAP:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", Constants.service_port);
			jsonObject.put("bc_rest_service_port", Constants.service_bc_port);
			jsonObject.put("bc_manager_servlet_port", Constants.bc_manager_servlet_port);
			jsonObject.put("subcomponent_address", "http://localhost");
			jsonObject.put("invocation_address", host_bus);

			break;

		case MQTT:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);

			break;
		case COAP:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);

			break;
			
		case COAPS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);

			break;

		case DPWS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);

			break;

		case WEB_SOCKETS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);

			break;
		
		case HTTPS:
			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);
			System.out.println("Cration of the configuration file:" + path);
			System.out.println("Cration of the configuration file:" + path);
			System.out.println("Cration of the configuration file:" + path);
			jsonObject.put("JKSPath", path);
			break;
			
		case MQTTS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_bus);
			jsonObject.put("invocation_address", host_bus);
			jsonObject.put("JKSPath", path);
			break;
		}
			

		// temporarily disabled
		try (FileWriter file = new FileWriter(filename)) {
			file.write(jsonObject.toJSONString());
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private static void createConfigFileGmServiceProtocole(ProtocolType protocol, String host, String filename) {

		String configPath = PathResolver.myFilePath(MediatorManagerRestService.class, "config.json");
		JSONParser configParser = new JSONParser();
		JSONObject configJsonObject = null;

		String configTemplatePath = PathResolver.myFilePath(MediatorManagerRestService.class, "template-config.json");
		;
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;

		try {

			configJsonObject = (JSONObject) configParser.parse(new FileReader(configPath));

		} catch (IOException | ParseException e){

			e.printStackTrace();
		}

		try {

			jsonObject = (JSONObject) parser.parse(new FileReader(configTemplatePath));

		} catch (IOException | ParseException e) {

			e.printStackTrace();
		}
		
		String host_service = mapParameter.get("host_service");
		String port = mapParameter.get("port_service");
		System.out.println("Port:" + mapParameter.get("subcomponent_port"));
		
		String JKSPath = mapParameter.get("JKSPath");
		
		switch (protocol) {

		case REST:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", "1112");
			jsonObject.put("service_port", "1113");
			jsonObject.put("subcomponent_address", host_service+":"+port);
			jsonObject.put("invocation_address", "http://"+host_service+":"+port+"/");


			break;
		case SOAP:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", Constants.service_port);
			jsonObject.put("bc_rest_service_port", Constants.service_bc_port);
			jsonObject.put("bc_manager_servlet_port", Constants.bc_manager_servlet_port);
			jsonObject.put("subcomponent_address", "http://localhost");
			jsonObject.put("invocation_address", host_service);

			break;

		case MQTT:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service);
			jsonObject.put("invocation_address", host_service);

			break;
			
		case MQTTS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service);
			jsonObject.put("invocation_address", host_service);
			jsonObject.put("JKSPath", JKSPath);

			break;
			
		case COAP:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service);
			jsonObject.put("invocation_address", host_service);

			break;
			
		case COAPS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service);
			jsonObject.put("invocation_address", host_service);

			break;

		case DPWS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service);
			jsonObject.put("invocation_address",host_service);

			break;

		case WEB_SOCKETS:

			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service);
			jsonObject.put("invocation_address", host_service);

			break;
		
		case HTTPS:
			jsonObject.put("target_namespace", Constants.target_namespace);
			jsonObject.put("service_name", Constants.soap_service_name);
			jsonObject.put("subcomponent_port", port);
			jsonObject.put("service_port", port);
			jsonObject.put("subcomponent_address", host_service+":"+port);
			jsonObject.put("invocation_address", "http://"+host_service+":"+port+"/");
			System.out.println("Cration of the configuration file:" + JKSPath);
			System.out.println("Cration of the configuration file:" + JKSPath);
			System.out.println("Cration of the configuration file:" + JKSPath);
			jsonObject.put("JKSPath", JKSPath);
			
			break;
		}

		// temporarily disabled
		try (FileWriter file = new FileWriter(filename)){

			file.write(jsonObject.toJSONString());

		} catch (IOException e) {System.out.println(e.getMessage());}
	}

	private static boolean isInterfaceDescriptionFile(String interfaceDescriptionPath) {

		boolean isGoodFile = false;
		File interfaceDescriptionFile = new File(interfaceDescriptionPath);

		if (!interfaceDescriptionFile.isFile()) {

			return isGoodFile;
		}
		String[] interfaceDescription = interfaceDescriptionFile.getName().split("\\.");
		String interfaceDescriptionExtension = interfaceDescription[interfaceDescription.length - 1];

		switch (interfaceDescriptionExtension){

		case "gmdl":

			isGoodFile = true;
			break;

		case "gidl":

			isGoodFile = true;
			break;
		}
		return isGoodFile;
	}

	private static String interfaceDescriptionBytesToFile(byte[] interfaceDescriptionByteArray) {

		File interfaceDescriptionFile = new File("interfaceDescription.gidl");
		FileOutputStream fos;
		try {

			fos = new FileOutputStream(interfaceDescriptionFile);
			fos.write(interfaceDescriptionByteArray);
			fos.flush();
			fos.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return interfaceDescriptionFile.getAbsolutePath();
	}

	private static String deleteSpecialChar(String s) {

		StringBuilder result = new StringBuilder();
		boolean firstChar = true;

		for (char ch : s.toCharArray()) {
			if (firstChar) {
				firstChar = false;
				if (Character.isJavaIdentifierStart(ch)) {
					result.append(ch);
				}
				continue;
			}
			if (Character.isJavaIdentifierPart(ch)) {
				result.append(ch);
			}
		}
		return result.toString();
	}
	/******************************************************************************************************/

}

