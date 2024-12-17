package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;

public class GetActuatorCommandResourceHandler extends CoapResource implements IActuatorDataListener
{
    private static final Logger _Logger = 
        Logger.getLogger(GetActuatorCommandResourceHandler.class.getName());

    private ActuatorData actuatorData = null;

    public GetActuatorCommandResourceHandler(String resourceName)
    {
    	super(resourceName);
	    // set the resource to be observable
	    super.setObservable(true);
    }

    public boolean onActuatorDataUpdate(ActuatorData data)
    {
	    if (data != null && this.actuatorData != null) {
		    this.actuatorData.updateData(data);
		
		    // notify all connected clients
		    super.changed();
		
		    _Logger.info("Actuator data updated for URI: " + super.getURI() + ": Data value = " + this.actuatorData.getValue());
		
		    return true;
	    }
	
	    return false;
    }

    @Override
    public void handleGET(CoapExchange context)
    {
        //Accepting the request
        context.accept();

        //Converting actuator data into json object
        String jsonData = DataUtil.getInstance().actuatorDataToJson(this.actuatorData);

        //Responding 
        context.respond(ResponseCode.CONTENT, jsonData);
    }
}
