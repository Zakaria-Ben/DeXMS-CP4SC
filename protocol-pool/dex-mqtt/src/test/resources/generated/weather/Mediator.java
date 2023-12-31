import java.util.ArrayList;
import java.util.List;
import javax.jws.WebService;
import org.zefxis.dexms.dex.protocols.mqtt.MQTTResponseBuilder;
import org.zefxis.dexms.dex.protocols.primitives.MediatorGmSubcomponent;
import org.zefxis.dexms.gmdl.utils.Data;


/**
 * This class was generated by the CHOReVOLUTION BindingComponent Generator using com.sun.codemodel 2.6
 * 
 */
@WebService(serviceName = "Mediator", targetNamespace = "")
public class Mediator {

    private final MediatorGmSubcomponent apiRef;

    public Mediator(MediatorGmSubcomponent apiRef) {
        this.apiRef = apiRef;
    }

    @javax.jws.WebMethod
    public RootClass getMeteoInfoByArea(Integer period, Double latitude, Double longitude, Double radius) {
        List<Data<?>> datas = new ArrayList<Data<?>>();
        datas.add(new Data<Integer>("period", "Integer", true, period, "PATH"));
        datas.add(new Data<Double>("latitude", "Double", true, latitude, "PATH"));
        datas.add(new Data<Double>("longitude", "Double", true, longitude, "PATH"));
        datas.add(new Data<Double>("radius", "Double", true, radius, "PATH"));
        String serializedrootClass = this.apiRef.mgetTwowaySync("/mes/get_metadata_in_area?collection=weather&{period}&{lat}&{lon}&{radius}", datas);
        return MQTTResponseBuilder.unmarshalObject("application/json", serializedrootClass, RootClass.class);
    }

    @javax.jws.WebMethod
    public RootClass getMeteoInfo(Integer period) {
        List<Data<?>> datas = new ArrayList<Data<?>>();
        datas.add(new Data<Integer>("period", "Integer", true, period, "PATH"));
        String serializedrootClass = this.apiRef.mgetTwowaySync("/mes/get_metadata_in_area?collection=weather&{period}", datas);
        return MQTTResponseBuilder.unmarshalObject("application/json", serializedrootClass, RootClass.class);
    }

}
