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
import java.io.File;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.common.SimpleCertManagementUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;

/*
	This class implements a simple pubsub protocol using MQTT to receive sensor data 
	from CDA and send Actuator commands. It implements simple callbacks for all paho mqtt messages.
	This class is instanced in device data manager to manage connections.
	
*/
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	//Class Scoped Variables

	private boolean useAsyncClient  = true;

	private MqttAsyncClient       mqttClient      = null;
	private MqttConnectOptions    connOpts        = null;
	private MemoryPersistence     persistence     = null;
	private IDataMessageListener  dataMsgListener = null;

	private String pemFileName = null;
	private boolean enableEncryption = false;
	private boolean useCleanSession = false;
	private boolean enableAutoReconnect = true;

	private String  clientID        = null;
	private String  brokerAddr      = null;
	private String  host            = ConfigConst.DEFAULT_HOST;
	private String  protocol        = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int     port            = ConfigConst.DEFAULT_MQTT_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;

	// Cloud Connections
	private IConnectionListener connListener = null;
	private boolean useCloudGatewayConfig = false;
	
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// constructors
	
	/*
	 * Default.
	 */
	public MqttClientConnector()
	{
		super();
		initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
	}
	
	// Cloud Constructor
	public MqttClientConnector(boolean useCloudGatewayConfig)
	{
		this(useCloudGatewayConfig ? ConfigConst.CLOUD_GATEWAY_SERVICE : null);
	}

	public MqttClientConnector(String cloudGatewayConfigSectionName)
	{
		super();

		if (cloudGatewayConfigSectionName != null && cloudGatewayConfigSectionName.trim().length() > 0) {
			this.useCloudGatewayConfig = true;
			
			initClientParameters(cloudGatewayConfigSectionName);
		} else {
			this.useCloudGatewayConfig = false;
			
			// NOTE: This next method call should have already been created
			// in Lab Module 10. It is simply a delegate to handle parsing
			// of the appropriate configuration file section
			initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
		}
	}
	
	// public methods
	
	// Method to connect to mqtt broker
	@Override
	public boolean connectClient()
	{
		try{
			
			if(this.mqttClient == null) {
				// this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient = new MqttAsyncClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}

			if(!this.mqttClient.isConnected()){
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOpts);
				return true;
			}
			else{
				_Logger.warning("MQTT client already connected to broker: " + this.brokerAddr);
			}
		} 
		catch(MqttException e){
			_Logger.log(Level.SEVERE, "Failed to connect MQTT client to broker.", e);
		}

		return false;
	}

	// Method to disconnect from Mqtt broker
	@Override
	public boolean disconnectClient()
	{
		try {
				if(this.mqttClient != null){
					if(this.mqttClient.isConnected()){
						_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
						this.mqttClient.disconnect();
						return true;
					}
					else{
						_Logger.warning("MQTT client not connected to broker: " + this.brokerAddr);
					}
				}
		}
		catch(Exception e){
			_Logger.log(Level.SEVERE, "Failed to disconnet MQTT client from broker: " + this.brokerAddr, e);
		}

		return false;
	}

	public boolean isConnected()
	{
		return (this.mqttClient != null && this.mqttClient.isConnected());
	}
	
	// Method to publish message
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos)
	{
		if(topicName == null)
		{
			_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}

		if(msg == null || msg.length() == 0)
		{
			_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}

		return publishMessage(topicName.getResourceName(), msg.getBytes(), qos);
	}

	// Method to subscribe to a topic
	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
	{
		if (topicName == null)
		{
			_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
			return false;
		}
		return subscribeToTopic(topicName.getResourceName(), qos);
	}

	// Method to unsubscribe from a topic
	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
			return false;
		}
		return unsubscribeFromTopic(topicName.getResourceName());
	}

	// Method to listen on incomming connection
	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		if(listener != null)
		{
			_Logger.info("Setting connection listener");
			this.connListener = listener;
			return true;
		}
		else{
			_Logger.warning("No connection listener specified");
		}
		return false;
	}
	
	// Method to listen on incomming data
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null){
			this.dataMsgListener = listener;
			return true;
		}

		return false;
	}
	
	// callbackmethods

	// Method called when MQTT connection is successfully established	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " +serverURI);

		int qos = ConfigConst.DEFAULT_QOS;

		this.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
		this.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
		this.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);

		try {
			if(!this.useCloudGatewayConfig){
				_Logger.info("Subscribing to topic: " + ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE.getResourceName());

				this.mqttClient.subscribe(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE.getResourceName(), qos);
			}
		} catch (Exception e) {
			_Logger.warning("Failed to connect to CDA Actuator response topic");
		}

		if(this.connListener != null)
		{
			this.connListener.onConnect();
		}
	}

	// Method called when MQTT connection is lost
	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.log(Level.WARNING, "Lost connection to MQTT broker: " + this.brokerAddr, t);
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		_Logger.info("Delivered MQTT message with ID: " + token.getMessageId());
	}
	
	// Method called when a message is arrived on MQTT connection
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		_Logger.info("MQTT message arrived on topic: " + topic + "\n'Message : " + msg);

		try
		{
			RedisPersistenceAdapter redisClientAdapter = new RedisPersistenceAdapter();

			if(topic.trim().equals(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceName()))
			{
				_Logger.info("Handling Sensor data received form CDA: " + topic.trim());
				SensorData sensorData = DataUtil.getInstance().jsonToSensorData(new String(msg.getPayload()));
				this.dataMsgListener.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);

				redisClientAdapter.storeData(topic, ConfigConst.DEFAULT_QOS, sensorData);
				 
			}

			if (topic.trim().equals(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName())) 
			{
				_Logger.info("Handling System performance data received form CDA: " + topic.trim());
				SystemPerformanceData sysPerfData = DataUtil.getInstance().jsonToSystemPerformanceData(new String(msg.getPayload()));
				this.dataMsgListener.handleSystemPerformanceMessage(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData);

				redisClientAdapter.storeData(topic, ConfigConst.DEFAULT_QOS, sysPerfData);
			}

			if (topic.trim().equals(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE.getResourceName()))
			{
				_Logger.info("Handling Actuator Response data received form CDA: " + topic.trim());
				ActuatorData actuatorData = DataUtil.getInstance().jsonToActuatorData(new String(msg.getPayload()));
				this.dataMsgListener.handleActuatorCommandResponse(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, actuatorData);

				redisClientAdapter.storeData(topic, ConfigConst.DEFAULT_QOS, actuatorData);
			}
			
		}catch(Exception e)
		{
			_Logger.warning("Failed to convert data and incomming handler");
		}
		

	}

	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
	
		this.host = configUtil.getProperty(configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);

		this.port =configUtil.getInteger(configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
		
		this.brokerKeepAlive =configUtil.getInteger(configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		
		this.enableEncryption =configUtil.getBoolean(configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		
		this.pemFileName =configUtil.getProperty(configSectionName, ConfigConst.CERT_FILE_KEY);
		
		// This next config file boolean property is optional; it can be
		// set within the [Mqtt.GatewayService] and [Cloud.GatewayService]
		// sections of PiotConfig.props. You can use it to create a logical
		// flow within this class to determine whether to use MqttClient
		// or MqttAsyncClient, or simply choose one of the two classes based
		// on your usage needs. Generally speaking, MqttAsyncClient will
		// be necessary when running the GDA as an application, as it will
		// need to handle incoming and outgoing messages using MQTT
		// simultaneously. For GDA-only testing using the test cases
		// specified in this lab module and others, it's generally best -
		// and likely required - to use MqttClient.
		// 
		// IMPORTANT: If you're using an older version of ConfigConst.java,
		// you'll need to add the following line of code to ConfigConst.java:
		// public static final String USE_ASYNC_CLIENT_KEY = "useAsyncClient";
		this.useAsyncClient = configUtil.getBoolean(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.USE_ASYNC_CLIENT_KEY);
	
		// NOTE: updated from Lab Module 07 - attempt to load clientID from configuration file
		this.clientID = configUtil.getProperty(ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY, MqttClient.generateClientId());
		
		// these are specific to the MQTT connection which will be used during connect
		this.persistence = new MemoryPersistence();
		this.connOpts    = new MqttConnectOptions();
		
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(this.useCleanSession);
		this.connOpts.setAutomaticReconnect(this.enableAutoReconnect);
		
		// if encryption is enabled, try to load and apply the cert(s)
		if (this.enableEncryption) {
			initSecureConnectionParameters(configSectionName);
		}
		
		// if there's a credential file, try to load and apply them
		if (configUtil.hasProperty(configSectionName, ConfigConst.CRED_FILE_KEY)) {
			initCredentialConnectionParameters(configSectionName);
		}
		
		// NOTE: URL does not have a protocol handler for "tcp" or "ssl",
		// so construct the URL manually
		this.brokerAddr  = this.protocol + "://" + this.host + ":" + this.port;
		
		_Logger.info("Using URL for broker conn: " + this.brokerAddr);
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();

		try{
			_Logger.info("Checking if credentials file exists and is loadable");

			Properties props = configUtil.getCredentials(configSectionName);

			if(props != null)
			{
				// this.connOpts.setUserName(props.getProperty(ConfigConst.USER_NAME_TOKEN_KEY,""));
				this.connOpts.setUserName("BBUS-s3QpAu8AtPBfo5JxbFOyY0xCRcTd97");
				this.connOpts.setPassword(props.getProperty(ConfigConst.USER_AUTH_TOKEN_KEY, "").toCharArray());

				_Logger.info("Credentials now set");
			}else{
				_Logger.warning("No credentials are to set");
			}
		}
		catch (Exception e){
			_Logger.log(Level.WARNING, "Credential file non-existence. Disabling auth requirements", e);
		}
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		try {
			_Logger.info("Configuring TLS...");
			
			if (this.pemFileName != null) {
				File file = new File(this.pemFileName);
				
				if (file.exists()) {
					_Logger.info("PEM file valid. Using secure connection: " + this.pemFileName);
				} else {
					this.enableEncryption = false;
					
					_Logger.log(Level.WARNING, "PEM file invalid. Using insecure connection: " + this.pemFileName, new Exception());
					
					return;
				}
			}
			
			SSLSocketFactory sslFactory = SimpleCertManagementUtil.getInstance().loadCertificate(this.pemFileName);
			
			this.connOpts.setSocketFactory(sslFactory);
			
			// override current config parameters
			this.port =configUtil.getInteger(configSectionName, ConfigConst.SECURE_PORT_KEY, ConfigConst.DEFAULT_MQTT_SECURE_PORT);
			
			this.protocol = ConfigConst.DEFAULT_MQTT_SECURE_PROTOCOL;
			
			_Logger.info("TLS enabled.");

		} 
		catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to initialize secure MQTT connection. Using insecure connection.", e);
			
			this.enableEncryption = false;
		}
	}

	// Protected Methods

	// Method to Publish a message
	protected boolean publishMessage(String topicName, byte[] payload, int qos)
	{
		if(topicName == null)
		{
			_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		if(payload == null || payload.length == 0)
		{
			_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		if(qos < 0 || qos > 2)
		{
			_Logger.warning("Invalid QOS level. Using default QOS value");
			qos = ConfigConst.DEFAULT_QOS;
		}
		try{
			MqttMessage mqttMsg = new MqttMessage();
			mqttMsg.setQos(qos);
			mqttMsg.setPayload(payload);
			this.mqttClient.publish(topicName, mqttMsg);
			
			return true;
		}catch(Exception e){
			_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topicName, e);
		}

		return false;
	}

	// Method to Subscribe to a specified topic
	protected boolean subscribeToTopic(String topicName, int qos)
	{
		return subscribeToTopic(topicName, qos, null);
	}

	protected boolean subscribeToTopic(String topicName, int qos, IMqttMessageListener listener)
	{
		if(topicName == null)
		{
			_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
			return false;
		}
		if(qos < 0 || qos > 2)
		{
			_Logger.warning("Invalid QOS level. Configuring to default QOS level");
			qos = ConfigConst.DEFAULT_QOS;
		}

		try {
			if(listener != null)
			{
				this.mqttClient.subscribe(topicName, qos, listener);
				_Logger.info("Successfully subscribed to topic: " + topicName);
			}
			else{
				this.mqttClient.subscribe(topicName, qos);
				_Logger.info("Subscribed to topic: " + topicName);
			}
			return true;

		}catch (Exception e){
			_Logger.log(Level.SEVERE, "Failed to subsribe to topic: " + topicName, e);
		}
		return false;
	}

	// Method to unsubscribe from a specified topic
	protected boolean unsubscribeFromTopic(String topicName)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
			
			return false;
		}
		
		try {
			this.mqttClient.unsubscribe(topicName);
			
			_Logger.info("Successfully unsubscribed from topic: " + topicName);
			
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic: " + topicName, e);
		}
		
		return false;
	}



}
