/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArchUtils;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import jakarta.mail.Flags;
import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * This class handles all the required tasks to connect to cloud
 * - Publishes data to cloud
 * - Gets actuation data from cloud *
 */
public class CloudClientConnector implements ICloudClient, IConnectionListener
{

	// static
	
	private static final Logger _Logger = Logger.getLogger(CloudClientConnector.class.getName());
	
	// private var's
	
	private String topicPreFix = "";
	private MqttClientConnector mqttClient = null;
	private IDataMessageListener dataMessageListener = null;
	private IMqttMessageListener mqttMessageListener = null;
	// private IConnectionListener connListener = null;

	private int qosLevel = 1;

	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public CloudClientConnector()
	{
		super();
		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.topicPreFix = configUtil.getProperty(ConfigConst.CLOUD_GATEWAY_SERVICE, ConfigConst.BASE_TOPIC_KEY);

		if(topicPreFix == null){
			topicPreFix = "/";
		}
		else{
			if(!topicPreFix.endsWith("/")){
				topicPreFix += "/";
			}
		}
		
	}
	
	
	// public methods
	
	// Method to connect with cloud broker
	@Override
	public boolean connectClient()
	{
		if(this.mqttClient == null)
		{
			this.mqttClient = new MqttClientConnector(ConfigConst.CLOUD_GATEWAY_SERVICE);
			this.mqttClient.setConnectionListener(this);
		}

		return this.mqttClient.connectClient();
	}

	// Method to disconnect from cloud broker
	@Override
	public boolean disconnectClient()
	{
		if (this.mqttClient != null && this.mqttClient.isConnected()){
			return this.mqttClient.disconnectClient();
		}

		return false;
	}

	// Method to listen om incomming data
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if(listener != null)
		{
			this.dataMessageListener = listener;
			return true;
		}
		return false;
	}

	// Method to send edge(CDA) sensor data to cloud
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
	{
		if(resource != null && data != null)
		{
			String payload = DataUtil.getInstance().sensorDataToJson(data);
			return publishMessageToCloud(resource, data.getName(), payload);
		}
		return false;
	}

	// Method to send edge (GDA) system performance data to cloud
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
	{
		if(resource != null && data != null)
		{
			//Upstreaming CPU data
			SensorData cpuData = new SensorData();
			cpuData.updateData(data);
			cpuData.setName(ConfigConst.CPU_UTIL_NAME);
			cpuData.setValue(data.getCpuUtilization());
			_Logger.info("Updating CPU data to cloud: " + resource.getResourceName() + cpuData.getValue());
			boolean cpuDataUpStreamStatus = sendEdgeDataToCloud(resource, cpuData);
			
			if(!cpuDataUpStreamStatus){
				_Logger.warning("Failed to send CPU utilization data to cloud service");
			}

			// Upstreaming Memory data
			SensorData memData = new SensorData();
			memData.updateData(data);
			memData.setName(ConfigConst.MEM_UTIL_NAME);
			memData.setValue(data.getMemoryUtilization());
		
			boolean memDataUpStreamSuccess = sendEdgeDataToCloud(resource, memData);
			_Logger.info("Updating Memory data to cloud: " +resource.getResourceName()+ memData.getValue());
			
			if (! memDataUpStreamSuccess) {
				_Logger.warning("Failed to send memory utilization data to cloud service.");
			}

			// Upstreaming Disc Utilization
			SensorData diskData = new SensorData();
			diskData.updateData(diskData);
			diskData.setName(ConfigConst.DISK_UTIL_NAME);
			diskData.setValue(data.getDiskUtilization());

			boolean diskDataUpStreamtatus = sendEdgeDataToCloud(resource, diskData);
			_Logger.info("Updating Disk data to cloud: " +resource.getResourceName()+ diskData.getValue());

			if(!diskDataUpStreamtatus){
				_Logger.warning("Failed to send disk utilization data to cloud service");
			}

			return ((cpuDataUpStreamStatus && memDataUpStreamSuccess) && diskDataUpStreamtatus);
		}
		return false;
	}

	// Method to subscribe to cloud events
	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource)
	{
		boolean success = false;
		String topicName = null;

		if(this.mqttClient != null && this.mqttClient.isConnected()){
			topicName = createTopicName(resource);

			this.mqttClient.subscribeToTopic(topicName, qosLevel);
			success = true;
		}
		else{
			_Logger.warning("Subscription is only available for MQTT. No MQTT broker connected. Ignoring the topic: "+ topicName);
		}

		return success;
	}

	// Method to unsubscribe from cloud events
	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
	{
		boolean success = false;

		String topicName = null;

		if(this.mqttClient != null && this.mqttClient.isConnected())
		{
			topicName = createTopicName(resource);

			this.mqttClient.unsubscribeFromTopic(topicName);

			success = true;
		}
		else{
			_Logger.warning("Unsubscribe method is onpy available0for MQTT. No MQTT broker is connected. Ignoring topic: "+ topicName);
		}
		return success;
	}
	
	
	// private methods

	// Method to create a topic name for cloud
	private String createTopicName(ResourceNameEnum resource)
	{
		return createTopicName(resource.getDeviceName(), resource.getResourceType());
	}

	// Method to create topic to align with UBIDOTS
	private String createTopicName(ResourceNameEnum resource, String itemName)
	{
		return(createTopicName(resource) + "-" + itemName).toLowerCase();
	}
	// Method to create a topic name
	private String createTopicName(String deviceName, String resourceTypeName)
	{
		StringBuilder buf = new StringBuilder();
		if(deviceName != null && deviceName.trim().length() >  0){
			buf.append(topicPreFix).append(deviceName);
		}

		if(resourceTypeName != null && resourceTypeName.trim().length() > 0)
		{
			buf.append('/').append(resourceTypeName);
		}
		return buf.toString().toLowerCase();
	}

	
	// Method to publish message to cloud
	private boolean publishMessageToCloud(ResourceNameEnum resourceName, String itemName, String payload)
	{
		String topicName = createTopicName(resourceName) + "-" +itemName;
		return publishMessageToCloud(topicName, payload);
	}

	// Method to publish message to cloud
	private boolean publishMessageToCloud(String topicName, String payload)
	{
		try {
			_Logger.info("Publishing Messages to Cloud Service Provider" + topicName);
			this.mqttClient.publishMessage(topicName, payload.getBytes(), this.qosLevel);

			return true;
		}catch(Exception e){
			_Logger.warning("Failed to publish message to CSP" + topicName);
		}
		return false;
	}

	@Override
	public void onConnect()
	{
		_Logger.info("Handling Cloud Service Provider subscriptions and device topic provision");

		LedEnablementMessageListener ledListener = new LedEnablementMessageListener(this.dataMessageListener);

		ActuatorData ad = new ActuatorData();
		ad.setAsResponse();
		ad.setName(ConfigConst.LED_ACTUATOR_NAME);
		ad.setValue((float) -1.0);
		ResourceNameEnum leml = ledListener.getResource();

		String ledTopic = createTopicName(leml.getDeviceName() , ad.getName());
		String adJson = DataUtil.getInstance().actuatorDataToJson(ad);

		this.publishMessageToCloud(ledTopic, adJson);

		this.mqttClient.subscribeToTopic(ledTopic, this.qosLevel, ledListener);

	}

	@Override
	public void onDisconnect()
	{
		_Logger.info("MQTT client disconnected.Nothing to do.");
	}

	// Class to handle incomming LED actuation
	private class LedEnablementMessageListener implements IMqttMessageListener
	{
		private IDataMessageListener dataMessageListener = null;
		private ResourceNameEnum resource = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE;

		private int typeID = ConfigConst.LED_ACTUATOR_TYPE;
		private String itemName = ConfigConst.LED_ACTUATOR_NAME;

		LedEnablementMessageListener(IDataMessageListener dataMessageListener)
		{
			this.dataMessageListener = dataMessageListener;
		}

		public ResourceNameEnum getResource()
		{
			return this.resource;
		}

		// Method to handle message arrived
		@Override
		public void messageArrived(String topic, MqttMessage message)
		{
			try {
				_Logger.info("Messaged Reveived for LED Actuation in cloud client connector inner class: " + topic.trim());
				String jsonData = new String(message.getPayload());

				ActuatorData actuatorData = DataUtil.getInstance().jsonToActuatorData(jsonData);

				actuatorData.setLocationID("constraineddevice001");
				actuatorData.setTypeID(this.typeID);
				actuatorData.setName(this.itemName);

				int value = (int) actuatorData.getValue();


				switch (value) {
					case ConfigConst.ON_COMMAND:
						_Logger.info("Received LED enablement message [ON].");
						actuatorData.setStateData("LED switching ON");	
						actuatorData.setCommand(ConfigConst.ON_COMMAND);					
						break;
					
					case ConfigConst.OFF_COMMAND:
						_Logger.info("Received LED enablement message [OFF]");
						actuatorData.setStateData("LED switching OFF");
						actuatorData.setCommand(ConfigConst.ON_COMMAND);
						break;

					default:
						break;
				}

				if(this.dataMessageListener != null){
					jsonData = DataUtil.getInstance().actuatorDataToJson(actuatorData);

					this.dataMessageListener.handleIncomingMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, jsonData);
				}
			} catch(Exception e){
				_Logger.warning("Failed to convert message payload to actuator data");
			}
		}


	}
	
}
