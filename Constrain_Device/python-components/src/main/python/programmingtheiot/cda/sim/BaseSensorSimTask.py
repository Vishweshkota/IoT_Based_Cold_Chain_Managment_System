#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import logging
import random

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.data.SensorData import SensorData
from programmingtheiot.cda.sim.SensorDataGenerator import SensorDataGenerator

class BaseSensorSimTask():
	"""
	This class is the base for all sensor related activities.
	It sets the paramaters such as name, typeID, dataset, and gets the sensor telemetry values using generate telemetry and sensor data generator classes.
	
	"""

	DEFAULT_MIN_VAL = ConfigConst.DEFAULT_VAL
	DEFAULT_MAX_VAL = 100.0
	
	def __init__(self, name = ConfigConst.NOT_SET, typeID: int = ConfigConst.DEFAULT_SENSOR_TYPE, dataSet = None, minVal: float = DEFAULT_MIN_VAL, maxVal: float = DEFAULT_MAX_VAL):
		# pass
		self.dataSet = dataSet
		self.name = name
		self.typeID = typeID
		self.dataSetIndex = 0
		self.useRandomizer = False

		self.latestSensorData = None

		if not self.dataSet:
			self.useRandomizer = True
			self.minVal = minVal
			self.maxVal = maxVal
	
	def generateTelemetry(self) -> SensorData:
		"""
		Implement basic logging and SensorData creation. Sensor-specific functionality
		should be implemented by sub-class.
		
		A local reference to SensorData can be contained in this base class.
		"""
		
		sensorData = SensorData(typeID = self.getTypeID(), name = self.getName())
		sensorVal = ConfigConst.DEFAULT_VAL

		if self.useRandomizer:
			sensorVal = random.uniform(self.minVal, self.maxVal)
		
		else:
			sensorVal = self.dataSet.getDataEntry(index = self.dataSetIndex)
			self.dataSetIndex += 1

			if self.dataSetIndex >= self.dataSet.getDataEntryCount()-1:
				self.dataSetIndex = 0
		
		sensorData.setValue(sensorVal)

		self.latestSensorData = sensorData
		
		return sensorData
	
	def getTelemetryValue(self) -> float:
		"""
		If a local reference to SensorData is not None, simply return its current value.
		If SensorData hasn't yet been created, call self.generateTelemetry(), then return
		its current value.
		"""
		# pass
		if not self.latestSensorData:
			self.generateTelemetry()

		return self.latestSensorData.getValue()
	
	def getLatestTelemetry(self) -> SensorData:
		"""
		This can return the current SensorData instance or a copy.
		"""
		pass
	
	def getName(self) -> str:
		# pass
		return self.name
	
	def getTypeID(self) -> int:
		# pass
		return self.typeID
	