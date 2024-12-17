#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import programmingtheiot.common.ConfigConst as ConfigConst

class BaseSystemUtilTask():
	"""
	This is the base class to handle system performance data.

	It provides Name, TypeID and an abstract methods for Cpu Util Task and Memory Util Task to get telemetry values. 
	
	"""
	
	def __init__(self, name = ConfigConst.NOT_SET, typeID = ConfigConst.DEFAULT_SENSOR_TYPE):
		
		self.name = name
		self.typeID = typeID
	
	def getName(self) -> str:
		
		return self.name
	
	def getTypeID(self) -> int:
		
		return self.typeID
	
	def getTelemetryValue(self) -> float:
		pass
	