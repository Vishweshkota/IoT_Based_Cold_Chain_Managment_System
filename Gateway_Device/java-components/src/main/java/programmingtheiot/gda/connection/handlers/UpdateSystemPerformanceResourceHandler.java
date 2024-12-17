package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.DeviceDataManager;


public class UpdateSystemPerformanceResourceHandler extends CoapResource
{
    private IDataMessageListener dataMsgListener = null;
  	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());

    public UpdateSystemPerformanceResourceHandler(String resourceName)
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

                SystemPerformanceData sysPerfData = DataUtil.getInstance().jsonToSystemPerformanceData(jsonData);
                this.dataMsgListener.handleSystemPerformanceMessage(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData);
                code = ResponseCode.CHANGED;
            }catch(Exception e){
                _Logger.warning("Failed to handle PUT request. Message: " + e.getMessage());
                code = ResponseCode.BAD_REQUEST;
            }
        }else {
            _Logger.info("No callabck listener for request. Ignoring PUT.");
            code = ResponseCode.CONTINUE;
        }
        String msg = "Update System Perf Data Request Handled: " + super.getName();

        context.respond(code, msg);
    }
}
