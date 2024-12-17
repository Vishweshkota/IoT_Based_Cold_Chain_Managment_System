package programmingtheiot.gda.connection.handlers;

import org.eclipse.californium.core.CoapResource;


import java.util.logging.Logger;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.DeviceDataManager;

public class UpdateTelemetryResourceHandler extends CoapResource 
{
    private IDataMessageListener dataMsgListener = null;
    private static final Logger _Logger =
        Logger.getLogger(DeviceDataManager.class.getName());

    public UpdateTelemetryResourceHandler(String resourceName)
    {
        super(resourceName);
    }

    public void setDataMessageListener(IDataMessageListener listener)
    {
	    if (listener != null) {
		    this.dataMsgListener = listener;
	    }
    }

    @Override
    public void handlePUT(CoapExchange context)
    {
        ResponseCode code = ResponseCode.NOT_ACCEPTABLE;

        context.accept();

        if(this.dataMsgListener != null){
            try{
                String jsonData = new String(context.getRequestPayload());

                SensorData sensorData = DataUtil.getInstance().jsonToSensorData(jsonData);
                this.dataMsgListener.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);
                code = ResponseCode.CHANGED;
            }catch(Exception e){
                _Logger.warning("Failed to handle PUT request. Message: " + e.getMessage());
                code = ResponseCode.BAD_REQUEST;
            }
        }else {
            _Logger.info("No callabck listener for request. Ignoring PUT.");
            code = ResponseCode.CONTINUE;
        }
        String msg = "Update Telemetry Data Request Handled: " + super.getName();

        context.respond(code, msg);
    }
}
