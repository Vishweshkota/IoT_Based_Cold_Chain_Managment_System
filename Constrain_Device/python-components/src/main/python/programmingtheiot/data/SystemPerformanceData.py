#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.data.BaseIotData import BaseIotData

class SystemPerformanceData(BaseIotData):
	"""
	This class implements all the data methods required for system performance.
	- gets and sets cpu utilization
	- gets and sets memory utilization
	
	"""
	DEFAULT_VAL = 0.0
	
	def __init__(self, d = None):
		super(SystemPerformanceData, self).__init__(name = ConfigConst.SYSTEM_PERF_MSG, typeID = ConfigConst.SYSTEM_PERF_TYPE, d = d)
		# pass
		self.cpuUtil = ConfigConst.DEFAULT_VAL
		self.memUtil = ConfigConst.DEFAULT_VAL 
	
	def getCpuUtilization(self):
		# pass
		return self.cpuUtil
	
	def getDiskUtilization(self):
		pass
	
	def getMemoryUtilization(self):
		# pass
		return self.memUtil
	
	def setCpuUtilization(self, cpuUtil):
		# pass
		self.cpuUtil = cpuUtil
		self.updateTimeStamp()
	
	def setDiskUtilization(self, diskUtil):
		pass
	
	def setMemoryUtilization(self, memUtil):
		# pass
		self.memUtil = memUtil
		self.updateTimeStamp()
	
	def _handleUpdateData(self, data):
		# pass
		if data and isinstance(data, SystemPerformanceData):
			self.cpuUtil = data.getCpuUtilization()
			self.memUtil = data.getMemoryUtilization()

		
