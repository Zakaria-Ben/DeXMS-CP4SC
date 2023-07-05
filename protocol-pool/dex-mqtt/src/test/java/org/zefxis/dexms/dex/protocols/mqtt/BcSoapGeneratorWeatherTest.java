package org.zefxis.dexms.dex.protocols.mqtt;

import org.junit.Before;
import org.junit.Test;
import org.zefxis.dexms.dex.protocols.mqtt.MediatorMQTTGenerator;

import pl.ncdc.differentia.DifferentiaAssert;
import org.zefxis.dexms.gmdl.utils.MediatorConfiguration;
import org.zefxis.dexms.gmdl.utils.GmServiceRepresentation;
import org.zefxis.dexms.gmdl.utils.Operation;
import org.zefxis.dexms.gmdl.utils.Scope;
import org.zefxis.dexms.dex.protocols.generators.MediatorSubcomponentGenerator;
import org.zefxis.dexms.gmdl.utils.Data;
import org.zefxis.dexms.gmdl.utils.Data.Context;
import org.zefxis.dexms.gmdl.utils.Data.MediaType;
import org.zefxis.dexms.gmdl.utils.enums.OperationType;
import org.zefxis.dexms.gmdl.utils.enums.ProtocolType;
import org.zefxis.dexms.gmdl.utils.enums.QosType;
import org.zefxis.dexms.gmdl.utils.enums.RoleType;
import org.zefxis.dexms.gmdl.utils.enums.Verb;

/**
 * @author Georgios Bouloukakis (boulouk@gmail.com)
 *
 * BcSoapGeneratorWeatherTest.java Created: 1 févr. 2016
 * Description:
 */
public class BcSoapGeneratorWeatherTest {

	private MediatorSubcomponentGenerator soapGenerator;

	@Before
	public void initGenerator() {
		MediatorConfiguration compConfServer = new MediatorConfiguration();
		compConfServer.setSubcomponentRole(RoleType.SERVER);
		compConfServer.setServiceAddress("http://93.62.202.227");
		compConfServer.setGeneratedCodePath("src/test/resources/generated/weather");
		compConfServer.setTargetNamespace("");

		GmServiceRepresentation serviceDefinition = new GmServiceRepresentation();
		serviceDefinition.setProtocol(ProtocolType.REST);

		/* Types Definitions */
		Data<?> rootClass = new Data<>("rootClass", "RootClass", false, MediaType.JSON, Context.BODY, true);

		Data<?> status = new Data<>("status", "String", true, MediaType.JSON, Context.PATH, true);

		// TODO fix the lists
		// Data<?> docs = new Data<>("docs", "List<WeatherType>", false, MediaType.JSON, Context.BODY, true);
		Data<?> docs = new Data<>("docs", "WeatherType", false, MediaType.JSON, Context.BODY, true);

		Data<?> latitude = new Data<>("latitude", "Double", true, MediaType.JSON, Context.PATH, true);
		Data<?> longitude = new Data<>("longitude", "Double", true, MediaType.JSON, Context.PATH, true);
		Data<?> temperature = new Data<>("temperature", "Double", true, MediaType.JSON, Context.PATH, true);
		Data<?> pressure = new Data<>("pressure", "Double", true, MediaType.JSON, Context.PATH, true);
		Data<?> observation_time = new Data<>("observation_time", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> tag = new Data<>("tag", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> __CLASS__ = new Data<>("__CLASS__", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> collection = new Data<>("collection", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> id = new Data<>("id", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> _id = new Data<>("_id", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> location = new Data<>("location", "Location", false, MediaType.JSON, Context.BODY, true);
		Data<?> status_double = new Data<>("status", "Double", true, MediaType.JSON, Context.PATH, true);
		Data<?> humidity = new Data<>("humidity", "Integer", true, MediaType.JSON, Context.PATH, true);
		Data<?> creation_date = new Data<>("creation_date", "String", true, MediaType.JSON, Context.PATH, true);
		Data<?> elevation = new Data<>("elevation", "Double", true, MediaType.JSON, Context.PATH, true);
		Data<?> rain = new Data<>("rain", "String", true, MediaType.JSON, Context.PATH, true);

		Data<?> type = new Data<>("type", "String", true, MediaType.JSON, Context.PATH, true);

		// TODO fix the lists
		// Data<?> coordinates = new Data<>("coordinates", "List<Double>", true, MediaType.JSON, Context.PATH, true);
		Data<?> coordinates = new Data<>("coordinates", "Double", true, MediaType.JSON, Context.PATH, true);

		docs.addAttribute(latitude);
		docs.addAttribute(longitude);
		docs.addAttribute(temperature);
		docs.addAttribute(pressure);
		docs.addAttribute(observation_time);
		docs.addAttribute(tag);
		docs.addAttribute(__CLASS__);
		docs.addAttribute(collection);
		docs.addAttribute(id);
		docs.addAttribute(_id);
		docs.addAttribute(location);
		docs.addAttribute(status_double);
		docs.addAttribute(humidity);
		docs.addAttribute(creation_date);
		docs.addAttribute(elevation);
		docs.addAttribute(rain);

		location.addAttribute(type);
		location.addAttribute(coordinates);

		rootClass.addAttribute(status);
		rootClass.addAttribute(docs);

		serviceDefinition.addTypeDefinition(rootClass);

		// Data for the requests
		Data<?> period = new Data<>("period", "Integer", true, MediaType.JSON, Context.PATH, true);
		Data<?> radius = new Data<>("radius", "Double", true, MediaType.JSON, Context.PATH, true);

		/* -- TWOWAY OPERATION 1 -- */

		Scope scope1 = new Scope();
		scope1.setName("getMeteoInfo");
		scope1.setVerb(Verb.GET);
		scope1.setUri("/mes/get_metadata_in_area?collection=weather&{period}");

		Operation twoWayOperation1 = new Operation("operation_1", OperationType.TWO_WAY_SYNC, QosType.RELIABLE, scope1);
		twoWayOperation1.addGetData(period);
		twoWayOperation1.setPostData(rootClass);

		serviceDefinition.addOperation(twoWayOperation1);

		/* ------------------------ */

		/* -- TWOWAY OPERATION 2 -- */

		Scope scope2 = new Scope();
		scope2.setName("getMeteoInfoByArea");
		scope2.setVerb(Verb.GET);
		scope2.setUri("/mes/get_metadata_in_area?collection=weather&{period}&{lat}&{lon}&{radius}");

		Operation twoWayOperation2 = new Operation("operation_2", OperationType.TWO_WAY_SYNC, QosType.RELIABLE, scope2);
		twoWayOperation2.addGetData(period);
		twoWayOperation2.addGetData(latitude);
		twoWayOperation2.addGetData(longitude);
		twoWayOperation2.addGetData(radius);
		twoWayOperation2.setPostData(rootClass);

		serviceDefinition.addOperation(twoWayOperation2);

		/* ------------------------ */

		this.soapGenerator = new MediatorMQTTGenerator(serviceDefinition, compConfServer).setDebug(true);
	}

	@Test
	public void testEndpointGeneration() {
		this.soapGenerator.generatePOJOAndEndpoint();
		// DifferentiaAssert.assertSourcesEqual("src/test/resources/expected/BindingComponent.java",
		// "src/test/resources/generated/BindingComponent.java");
	}
	//
	// @Test
	// public void testPojoGeneration() {
	// this.soapGenerator.generateBc();
	// DifferentiaAssert.assertSourcesEqual("src/test/resources/expected/TrafficLight.java",
	// "src/test/resources/generated/TrafficLight.java");
	// }
}