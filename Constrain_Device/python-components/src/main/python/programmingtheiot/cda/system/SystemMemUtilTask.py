#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import logging
import psutil

import programmingtheiot.common.ConfigConst as ConfigConst
from programmingtheiot.cda.system.BaseSystemUtilTask import BaseSystemUtilTask

class SystemMemUtilTask(BaseSystemUtilTask):
	"""
	This class extends BaseSystemUtilTask and implements getTelemetryValue method to get the memory usage value
	
	"""

	def __init__(self):
		# pass
		super(SystemMemUtilTask,self).__init__(name = ConfigConst.MEM_UTIL_NAME, typeID = ConfigConst.MEM_UTIL_TYPE)
	
	def getTelemetryValue(self) -> float:
		# pass
		return psutil.virtual_memory().percent		# Getting Memory utilization Value using psutil
		