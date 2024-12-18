/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.app;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils.Null;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.BaseIotData;
import programmingtheiot.data.SystemPerformanceData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * This class manages all the data and connections of GateWay Device App
 *
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger = Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's
	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = false;
	private boolean enableCloudClient = true;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private ICloudClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager sysPerfMgr = null;
	private RedisPersistenceAdapter redisClientConnector = null;

	private ActuatorData latestHumidifierActuatorData = null;
	private ActuatorData latestLedData = null;
	private ActuatorData latestHumidifierActuatorResponse = null;

	private SensorData latestHumiditySensorData = null;
	private SensorData latestTemperatureSensorData = null;
	private SensorData latestPressureSensorData = null;

	private OffsetDateTime latestHumiditySensorTimeStamp = null;

	private boolean handleHumidityChangeOnDevice = false; // optional
	private int     lastKnownHumidifierCommand   = ConfigConst.OFF_COMMAND;

	
	private long    humidityMaxTimePastThreshold = 10; // seconds
	private float   nominalHumiditySetting   = 40.0f;
	private float   triggerHumidifierFloor   = 30.0f;
	private float   triggerHumidifierCeiling = 50.0f;
	
	// constructors
	
	public DeviceDataManager()
	{
		super();

		ConfigUtil configUtil = ConfigUtil.getInstance();

		// Getting all the configurations to able from Piotconfig.props
		this.enableMqttClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		this.enableCoapServer = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		this.enableCloudClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
		this.enablePersistenceClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		this.enableSystemPerf = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
		this.enablePersistenceClient = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);

		// Getting paramaters to handle humidity values from Piotconfig.props
		this.handleHumidityChangeOnDevice =configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");
		this.humidityMaxTimePastThreshold =configUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
		this.nominalHumiditySetting =configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
		this.triggerHumidifierFloor =configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
		this.triggerHumidifierCeiling =configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");

		if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
			this.humidityMaxTimePastThreshold = 30;
		}

		initManager();
		
		initConnections();
	}
	
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapClient,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();
		
		initConnections();
	}
	

	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null)
		{
			_Logger.info("Handling actuator response: "+data.getName()+" "+data.getValue());

			if(data.hasError()){
				_Logger.warning("Error flag set for the Actuation data instance");
			}
			return true;
		}
		else{
			return false;
		}
	} 

	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if(data != null)
		{
			_Logger.info("Handling Actuator request received" + data.getCommand());
			_Logger.log(Level.FINE, "Actuator request received: {0}.Message: {1}",
			new Object[]{resourceName.getResourceName(), Integer.valueOf((data.getCommand()))});

			if(data.hasError()){
				_Logger.warning("Error flag is set for Actuator Data Instance");
			}
			int qos = ConfigConst.DEFAULT_QOS;

			this.sendActuatorCommandtoCda(resourceName, data);
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if(resourceName != null && msg != null)
		{
			try {
				if(resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE){
					_Logger.info("Handiling incomming general Message: " + msg);

					ActuatorData ad = DataUtil.getInstance().jsonToActuatorData(msg);
					String jsonData = DataUtil.getInstance().actuatorDataToJson(ad);
					
						if(this.mqttClient != null){
							_Logger.info("Publishing data to MQTT broker: " + jsonData);
							return this.mqttClient.publishMessage(resourceName, jsonData, 0);
						}

					}
					else{
						_Logger.warning("Failed to parse incomming message. Unknown type: " + msg);
						return false;
					}

				}catch(Exception e){
					_Logger.log(Level.WARNING, "Failed to process incomming message for resource: " + resourceName, e);
				}
		}
		else{
			_Logger.warning("Incomming message has no data. Ignoring for resource: " + resourceName);
		}
		return false;
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		
		if(data != null)
		{
			_Logger.info("Handling sensor message: " + data.getName());

			if(data.hasError())
			{
				_Logger.warning("Error flag set for the SensorData instance");
			}

			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
			_Logger.info("JSON [SensorData] -> " + jsonData);

			int qos = ConfigConst.DEFAULT_QOS;

			if(this.enablePersistenceClient && this.persistenceClient != null)
			{
				this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
			}

			this.handleIncomingDataAnalysis(resourceName, data);
			this.handleUpstreamTransmission(resourceName, jsonData, qos);

			return true;
		}
		else{
			return false;
		}
	}

	// Method to handle system performance data
	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if(data != null)
		{
			_Logger.info("Handling System Performance message: " + data.getName());

			if (data.hasError())
			{
				_Logger.warning("Error flag set for SystemPerformanceData instance");
			}

			String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(data);
			
			int qos = ConfigConst.DEFAULT_QOS;

			this.handleUpstreamTransmission(resourceName, jsonData, qos);

			return true;
		}
		else{
			return false;
		}
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if(listener != null){
			this.actuatorDataListener = listener;
		}
	}
	
	public void startManager()
	{
		_Logger.info("*******Device Data Manager Started Successfully*******");

		// Adding MQTT client and subscribing to a topic
		if (this.mqttClient != null) 
		{
			if (this.mqttClient.connectClient()) 
			{
				_Logger.info("Successfully connected MQTT client to broker.");
			}
			else 
			{
				_Logger.severe("Failed to connect MQTT client to broker.");
			}
		}

		if(this.cloudClient != null)
		{
			if(this.cloudClient.connectClient())
			{
				_Logger.info("Successfully connected to Cloud Service Provider");
			}
			else{
				_Logger.severe("Failed to connect to Cloud Service Provider");
			}
		}

		if(this.redisClientConnector != null)
		{
			if(this.redisClientConnector.connectClient())
			{
				_Logger.info("Successfully connected to Redis Data Base");
			}
			else{
				_Logger.severe("Failed to connet to Redis Data Base");
			}
		}
		if(this.enableCoapServer && this.coapServer != null){
			if(this.coapServer.startServer()){
				_Logger.info("CoAP server started.");
			}else{
				_Logger.severe("Failed to start CoAP server. Check log file for details.");
			}
		}

		if(this.sysPerfMgr != null)
		{
			_Logger.info("*******System Performance Manager Started Successfully*******");
			this.sysPerfMgr.startManager();
		}
	}

	
	public void stopManager()
	{
		if (this.mqttClient != null)
		{
			// Unsubscribing and disconnecting MQTT client
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);

			this.mqttClient.disconnectClient();
			
			if (this.mqttClient.disconnectClient())
			{
				_Logger.info("Successfully disconnected MQTT client from broker.");
			} 
			else
			{
				_Logger.severe("Failed to disconnect MQTT client from broker.");	
			}
		}

		if (this.cloudClient != null)
		{
			this.cloudClient.unsubscribeFromCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE);
			this.cloudClient.disconnectClient();
		}
		if(this.cloudClient.disconnectClient()){
			_Logger.info("Success disconnected from Cloud service provider");
		}else{
			_Logger.severe("Failed to disconnect from Cloud Service Provider");
		}

		if(this.enableCoapServer && this.coapServer != null){
			if(this.coapServer.stopServer()){
				_Logger.info("CoAP server stopped.");
			}else{
				_Logger.severe("Failed to stop CoAP server. Check log file for details.");
			}
		}

		if(this.sysPerfMgr != null)
		{
			this.sysPerfMgr.stopManager();
			_Logger.info("******Syatem performance Manager Stopped Successfully******");
		}
	}

	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */

	private void initConnections()
	{
	}


	private void initManager()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.enableSystemPerf = configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE , ConfigConst.ENABLE_SYSTEM_PERF_KEY);

		if(this.enableSystemPerf){
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}

		if(this.enableMqttClient){
			this.mqttClient = new MqttClientConnector();
			this.mqttClient.setDataMessageListener(this);
		}

		if(this.enableCoapServer){
			this.coapServer = new CoapServerGateway(this);
		}

		if(this.enableCloudClient){
			this.cloudClient = new CloudClientConnector();
			this.cloudClient.setDataMessageListener(this);
		}

		if(this.enablePersistenceClient){
			this.redisClientConnector = new RedisPersistenceAdapter();
			// this.redisClientConnector.setPersistanceListener(this);
		}
	}
	

	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.info("Analysing incoming actuator data: " + data.getName());

		if(data.isResponseFlagEnabled()){

		}else{
			if(this.actuatorDataListener != null)
			{
				this.actuatorDataListener.onActuatorDataUpdate(data);
			}
		}
	}

	private boolean handleIncomingDataAnalysis(ResourceNameEnum resourceName, SensorData data)
	{
		if(data != null)
		{
			_Logger.info("Analysing incoming SensorData");
			if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE)
			{
				handleHumiditySensorAnalysis(resourceName, data);
			}
			return true;
		}
		else{
			return false;
		}
	}

	private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data)
	{
		//
		// NOTE: INCOMPLETE and VERY BASIC CODE SAMPLE. Not intended to provide a solution.
		//
		_Logger.fine("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());
	
		boolean isLow  = data.getValue() < this.triggerHumidifierFloor;
		boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;
	
		if (isLow || isHigh) {
			_Logger.fine("Humidity data from CDA exceeds nominal range.");
		
			if (this.latestHumiditySensorData == null) {
				// set properties then exit - nothing more to do until the next sample
				this.latestHumiditySensorData = data;
				this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);
			
				_Logger.info("Starting humidity nominal exception timer. Waiting for seconds: " + this.humidityMaxTimePastThreshold);
			
				return;
			} else {
				OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);
			
				long diffSeconds = ChronoUnit.SECONDS.between(this.latestHumiditySensorTimeStamp, curHumiditySensorTimeStamp);
			
				_Logger.info("Checking Humidity value exception time delta: " + diffSeconds);
			
				if (diffSeconds >= this.humidityMaxTimePastThreshold) 
				{
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
					ad.setValue(this.nominalHumiditySetting);
				
					if (isLow) 
					{
						ad.setCommand(ConfigConst.ON_COMMAND);
					}
					else if (isHigh)
					{
						ad.setCommand(ConfigConst.OFF_COMMAND);
					}
				
					_Logger.info("Humidity exceptional value reached. Sending actuation event to CDA: " + ad);
				
					this.lastKnownHumidifierCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
				
					// set ActuatorData and reset SensorData (and timestamp)
					this.latestHumidifierActuatorData = ad;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				}
			}
		} else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
			// check if we need to turn off the humidifier
			if (this.latestHumidifierActuatorData != null) {
				// check the value - if the humidifier is on, but not yet at nominal, keep it on
				if (this.latestHumidifierActuatorData.getValue() >= this.nominalHumiditySetting) {
					this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);
				
					_Logger.info("Humidity nominal value reached. Sending OFF actuation event to CDA: " + this.latestHumidifierActuatorData);
				
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);
				
					// reset ActuatorData and SensorData (and timestamp)
					this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
					this.latestHumidifierActuatorData = null;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				} else {
					_Logger.info("Humidifier is still on. Not yet at nominal levels (OK).");
				}
			} else {
				// shouldn't happen, unless some other logic
				// nullifies the class-scoped ActuatorData instance
				_Logger.warning("ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");
			}
		}
	}

	private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
	{
		// NOTE: This is how an ActuatorData command will get passed to the CDA
		// when the GDA is providing the CoAP server and hosting the appropriate
		// ActuatorData resource. It will typically be used when the OBSERVE
		// client (the CDA, assuming the GDA is the server and CDA is the client)
		// has sent an OBSERVE GET request to the ActuatorData resource.

		if (this.actuatorDataListener != null) {
			this.actuatorDataListener.onActuatorDataUpdate(data);
		}
		
		// NOTE: This is how an ActuatorData command will get passed to the CDA
		// when using MQTT to communicate between the GDA and CDA
		if (this.enableMqttClient && this.mqttClient != null) {
			String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
			
			if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
				_Logger.info("Published ActuatorData command from GDA to CDA: " + data.getCommand());
			} else {
				_Logger.warning("Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
			}
		}
	}

	private OffsetDateTime getDateTimeFromData(BaseIotData data)
	{
		OffsetDateTime odt = null;
	
		try {
			odt = OffsetDateTime.parse(data.getTimeStamp());
		} catch (Exception e) {
			_Logger.warning("Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");
		
			// TODO: this won't be accurate, but should be reasonably close, as the CDA will
			// most likely have recently sent the data to the GDA
			odt = OffsetDateTime.now();
		}
	
		return odt;
	}

	private boolean handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if(data != null)
		{
			_Logger.info("Analysing incoming SystemPerformanceData");

			return true;
		}
		else{
			return false;
		}
	}

	// Method to send data to cloud
	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
	{
		
		if (jsonData != null)
		{
			_Logger.info("Sending JSON Data to cloud service: " + resourceName);
			
			if(this.cloudClient != null)
			{
				if(resourceName == ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE)
				{
					SensorData data = DataUtil.getInstance().jsonToSensorData(jsonData);
					this.cloudClient.sendEdgeDataToCloud(resourceName, data);
					return true;
				}
				else if (resourceName == ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE)
				{
					SystemPerformanceData data = DataUtil.getInstance().jsonToSystemPerformanceData(jsonData);
					this.cloudClient.sendEdgeDataToCloud(resourceName, data);
					return true;
				}
				else if (resourceName == ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE)
				{
					SystemPerformanceData data = DataUtil.getInstance().jsonToSystemPerformanceData(jsonData);
					this.cloudClient.sendEdgeDataToCloud(resourceName, data);
					return true;
				}
				else
				{
					_Logger.warning("Resource Match Not Found " + resourceName);
					return false;
				}
			}
			else{
				_Logger.info("Cloud client no connected to broker: ");
				return false;
			}
		}
		else{
			_Logger.warning("No JSON data received. Failed to up stream data: " + resourceName);
			return false;
		}
	}

}
