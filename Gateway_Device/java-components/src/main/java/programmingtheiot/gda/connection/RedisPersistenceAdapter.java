/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class RedisPersistenceAdapter implements IPersistenceClient
{
	// static
	
	private static final Logger _Logger = Logger.getLogger(RedisPersistenceAdapter.class.getName());
	
	// private var's
	private String host = null;
	private int port;
	private Jedis redisClientConnector;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public RedisPersistenceAdapter()
	{
		super();
		
		initConfig();
	}
	
	
	// public methods
	
	// public methods
	
	/**
	 *
	 */
	@Override
	public boolean connectClient()
	{
		try {
			if(!this.redisClientConnector.isConnected())
			{
				_Logger.warning("Connecting to Redis");
				this.redisClientConnector.connect();
				return true;
			}

			else{
				_Logger.warning("Redis client already connected");
			}
		} catch (Exception e) {
			_Logger.info("Failed to Connect to Redis server: " + e);
		}
		return false;
	}

	/**
	 *
	 */
	@Override
	public boolean disconnectClient()
	{
		if(this.redisClientConnector.isConnected())
		{
			this.redisClientConnector.disconnect();
			_Logger.info("Successfully disconnectd from redis server");
			return true;
		}
		return false;
	}

	/**
	 *
	 */
	@Override
	public ActuatorData[] getActuatorData(String topic, Date startDate, Date endDate)
	{
		return null;
	}

	/**
	 *
	 */
	@Override
	public SensorData[] getSensorData(String topic, Date startDate, Date endDate)
	{
		return null;
	}

	/**
	 *
	 */
	@Override
	public void registerDataStorageListener(Class cType, IPersistenceListener listener, String... topics)
	{
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, ActuatorData data)
	{
		this.redisClientConnector.append(topic, "\n");
		return storeData(topic, data);
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, SensorData data)
	{
		this.redisClientConnector.append(topic, "\n");
		return storeData(topic, data);
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, SystemPerformanceData data)
	{
		this.redisClientConnector.append(topic, "\n");
		return storeData(topic, data);
	}


	
	
	// private methods
	
	/**
	 * Initialize configurations for redis client
	 */
	private void initConfig()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.host = configUtil.getProperty(ConfigConst.DATA_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		this.port = configUtil.getInteger(ConfigConst.DATA_GATEWAY_SERVICE, ConfigConst.PORT_KEY, 0);

		this.redisClientConnector = new Jedis(this.host, this.port);

	}

	private boolean storeData(String topic, SystemPerformanceData data)
	{
		String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(data);
		this.redisClientConnector.append(topic, jsonData);
		return true;
	}

	private boolean storeData(String topic, SensorData data)
	{
		String jsonData = DataUtil.getInstance().sensorDataToJson(data);
		this.redisClientConnector.append(topic, jsonData);
		return true;
	}

	private boolean storeData(String topic, ActuatorData data)
	{
		String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
		this.redisClientConnector.append(topic, jsonData);
		return true;
	}

}
