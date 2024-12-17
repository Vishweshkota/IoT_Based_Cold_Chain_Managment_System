/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;


/**
 * This class extends BaseSystemUtilTask and implements the methods required to 
 * get and monitor cpu utilization
 * it overrides getTelemetry() method
 * 
 */
public class SystemCpuUtilTask extends BaseSystemUtilTask
{
		private static final Logger _Logger =
		Logger.getLogger(SystemCpuUtilTask.class.getName());
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SystemCpuUtilTask()
	{
		super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
	}
	
	
	// public methods

	/* Method to get  CPU utilization value using java.lang.managment*/	
	
	@Override
	public float getTelemetryValue()
	{
		OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
		double cpuUtil = mxBean.getSystemLoadAverage();
		return (float) cpuUtil*100;
	}
	
}
