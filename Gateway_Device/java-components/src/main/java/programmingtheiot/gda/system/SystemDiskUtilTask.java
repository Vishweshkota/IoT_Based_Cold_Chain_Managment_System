package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import java.io.File;

public class SystemDiskUtilTask extends BaseSystemUtilTask
{
    //private
    private static final Logger _Logger = Logger.getLogger(SystemDiskUtilTask.class.getName());

    // Constructor

    public SystemDiskUtilTask()
	{
		super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
	}
	
	
	// public methods
	
	/* Method to get  Disk utilization value using java.lang.managment*/	

	@Override
	public float getTelemetryValue()
	{
		File diskUsage = new File("/home");
        double totalSpace = diskUsage.getTotalSpace();
		double freeSpace = diskUsage.getFreeSpace();
        double usedSpace = diskUsage.getUsableSpace();

		_Logger.fine("Disk Size: " + totalSpace + "; Avaialable space: " + freeSpace + "Used space: "+ (totalSpace-freeSpace));

		double diskUtil = ((totalSpace-freeSpace) / totalSpace) * 100.0d;
		return (float)diskUtil;
	}
	
}
