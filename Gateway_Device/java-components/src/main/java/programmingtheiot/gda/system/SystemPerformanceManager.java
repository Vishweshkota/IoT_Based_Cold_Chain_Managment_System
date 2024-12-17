/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.system;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPersistenceListener;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemPerformanceData;


/* Importing Logging framework */
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class creates the instances of all system performance classes.
 * It sets all the required paramaters and calls respective get telemetry methods
 * 
 */
public class SystemPerformanceManager
{
	// private var's
	/* Adding private class scope variables */
	private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());
	private int pollRate = ConfigConst.DEFAULT_POLL_CYCLES;

	private ScheduledExecutorService schedExeSvc = null;
	private SystemCpuUtilTask sysCpuUtilTask = null;
	private SystemMemUtilTask sysMemUtilTask = null;
	private SystemDiskUtilTask sysDiskUtilTask = null;

	private Runnable taskRunner = null;
	private boolean isStarted = false;

	private String locationID = ConfigConst.NOT_SET;
	private IDataMessageListener dataMsgListener = null;
	private IPersistenceClient redIPersistenceClient = null;

	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SystemPerformanceManager()
	{
		this.pollRate = ConfigUtil.getInstance().getInteger(ConfigConst.GATEWAY_DEVICE, ConfigConst.POLL_CYCLES_KEY, ConfigConst.DEFAULT_POLL_CYCLES);
		if (this.pollRate <=0){
			this.pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
		}

		this.locationID = ConfigUtil.getInstance().getProperty(ConfigConst.GATEWAY_DEVICE, ConfigConst.LOCATION_ID_PROP, ConfigConst.NOT_SET);

		this.schedExeSvc = Executors.newScheduledThreadPool(1);

		this.sysCpuUtilTask = new SystemCpuUtilTask();
		this.sysMemUtilTask = new SystemMemUtilTask();
		this.sysDiskUtilTask = new SystemDiskUtilTask();

		this.taskRunner = ()->{
			this.handleTelemetry();
		};
	}
	
	
	// public methods

	/* Getting CPU utilization and Memory Utilization values from classes
	 * -> SystemCpuUtilTask()
	 * -> SystemMemoryUtilTask()
	 * -> SystemDiskUtilTask()
	 */
	
	public void handleTelemetry()
	{
		float cpuUtil = this.sysCpuUtilTask.getTelemetryValue();
		float memUtil = this.sysMemUtilTask.getTelemetryValue();
		float diskUtil = this.sysDiskUtilTask.getTelemetryValue();

		RedisPersistenceAdapter redisClient = new RedisPersistenceAdapter();


		_Logger.info("CPU utiliztion: "+cpuUtil + "\nMemory Utilization: " + memUtil +"\nDisk Utilization: "+diskUtil);

		SystemPerformanceData spd = new SystemPerformanceData();

		spd.setLocationID(this.locationID);
		spd.setCpuUtilization(cpuUtil);
		spd.setMemoryUtilization(memUtil);
		spd.setDiskUtilization(diskUtil);

		redisClient.storeData(ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName(), ConfigConst.DEFAULT_QOS, spd);
		
		if (this.dataMsgListener != null)
		{
			this.dataMsgListener.handleSystemPerformanceMessage(ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE, spd);

		}
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null)
		{
			this.dataMsgListener = listener;
		}
	}


/* Adding Start manager and stop manager for logging info */
	public boolean startManager()
	{
		if(! this.isStarted){
			_Logger.info("SystemPerformanceManager is Starting...");

			ScheduledFuture<?> futureTask = this.schedExeSvc.scheduleAtFixedRate(this.taskRunner, 1L, this.pollRate, TimeUnit.SECONDS);
			
			this.isStarted = true;
		}
		else{
			_Logger.info("SystemPerformanceManager is already started.");
		}

		return this.isStarted;
	}

	public boolean stopManager()
	{
		this.schedExeSvc.shutdown();
		this.isStarted = false;

		_Logger.info("SystemPerformanceManager is stopped.");

		return true;
	}
	
}
