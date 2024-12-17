#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import logging

import programmingtheiot.common.ConfigConst as ConfigConst
from programmingtheiot.cda.sim.BaseSensorSimTask import BaseSensorSimTask
from programmingtheiot.cda.sim.SensorDataGenerator import SensorDataGenerator

from programmingtheiot.data.SensorData import SensorData

class TemperatureSensorSimTask(BaseSensorSimTask):
	"""
	This is a simple wrapper for an Sensor abstraction - it provides
	a container for the sensors's state, value, name, and status. A
	command variable is also provided to instruct the sensor to
	perform a specific function (in addition to setting a new value
	via the 'val' parameter.
	
	"""

	def __init__(self, dataSet = None):
		super(TemperatureSensorSimTask, self).__init__( \
										name = ConfigConst.TEMP_SENSOR_NAME, \
										typeID = ConfigConst.TEMP_SENSOR_TYPE, \
										dataSet = dataSet, \
										minVal = SensorDataGenerator.LOW_NORMAL_INDOOR_TEMP, \
										maxVal =  SensorDataGenerator.HI_NORMAL_INDOOR_TEMP)
		# pass