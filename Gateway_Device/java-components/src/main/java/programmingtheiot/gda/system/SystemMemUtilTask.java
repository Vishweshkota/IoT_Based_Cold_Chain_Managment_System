/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;

/**
 * This class extends BaseSystemUtilTask and implements the methods required to 
 * get and monitor memory utilization
 * it overrides getTelemetry() method
 * 
 */
public class SystemMemUtilTask extends BaseSystemUtilTask
{
		private static final Logger _Logger =
		Logger.getLogger(SystemMemUtilTask.class.getName());
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SystemMemUtilTask()
	{
		super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
	}
	
	
	// public methods
	
	/* Method to get  Memory utilization value using java.lang.managment*/	

	@Override
	public float getTelemetryValue()
	{
		MemoryUsage memUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		double memUsed = (double) memUsage.getUsed();
		double memMax = (double) memUsage.getMax();

		_Logger.fine("Mem Used: " + memUsed + "; Mem Max: " + memMax);

		double memUtil = (memUsed / memMax) * 100.0d;
		return (float)memUtil*100;
	}
	
}
