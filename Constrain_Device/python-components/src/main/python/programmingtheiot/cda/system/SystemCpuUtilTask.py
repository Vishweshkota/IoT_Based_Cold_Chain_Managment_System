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

class SystemCpuUtilTask(BaseSystemUtilTask):
	"""
	This class extends BaseSystemUtilTask and implements getTelemetryValue method to get the Cpu usage value
	
	"""

	def __init__(self):
		# pass
		super(SystemCpuUtilTask, self).__init__(name = ConfigConst.CPU_UTIL_NAME, typeID = ConfigConst.CPU_UTIL_TYPE)
	def getTelemetryValue(self) -> float:
		# pass
		return psutil.cpu_percent()		# Getting CPU utilization value using psutil
		