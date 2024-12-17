#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import logging

from importlib import import_module

from apscheduler.schedulers.background import BackgroundScheduler

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.common.IDataMessageListener import IDataMessageListener

from programmingtheiot.cda.sim.SensorDataGenerator import SensorDataGenerator
from programmingtheiot.cda.sim.HumiditySensorSimTask import HumiditySensorSimTask
from programmingtheiot.cda.sim.TemperatureSensorSimTask import TemperatureSensorSimTask
from programmingtheiot.cda.sim.PressureSensorSimTask import PressureSensorSimTask

class SensorAdapterManager(object):
	"""

	This class is responsible for managing all the sensor functionality.
	It initializes the sensor paramaters such as 
	1) pollRate, locationID, enableEmulator
	2) Sets floor and ceiling value of each sensor attached

	It also schedules the sensor tasks in background using apscheduler
	It implements handle telemetry which calls generate telemetry to get the readouts from sensors(Either simulated or emulated)
	"""

	def __init__(self):
		# pass
		self.configUtil = ConfigUtil()

		self.pollRate = self.configUtil.getInteger(section = ConfigConst.CONSTRAINED_DEVICE,\
											 		key = ConfigConst.POLL_CYCLES_KEY, \
													defaultVal = ConfigConst.DEFAULT_POLL_CYCLES)
		
		self.useEmulator = self.configUtil.getBoolean(section = ConfigConst.CONSTRAINED_DEVICE, \
														key = ConfigConst.ENABLE_EMULATOR_KEY)
		
		self.locationID = self.configUtil.getProperty(section = ConfigConst.CONSTRAINED_DEVICE, \
														key = ConfigConst.DEVICE_LOCATION_ID_KEY, \
														defaultVal = ConfigConst.NOT_SET)
		
		if self.pollRate <= 0:
			self.pollRate = ConfigConst.DEFAULT_POLL_CYCLES

		self.scheduler = BackgroundScheduler()
		self.scheduler.add_job(self.handleTelemetry, 'interval', seconds = self.pollRate, \
								max_instances = 2, coalesce = True, misfire_grace_time = 15)

		""" Defining the Sensor adaptor to be used"""	
		self.dataMsgListner  = None
		self.humidityAdapter = None
		self.pressureAdaptor = None
		self.tempAdaptor     = None

		self._initEnvironmentalSensorTasks()

	def _initEnvironmentalSensorTasks(self):
		
		humidityFloor = self.configUtil.getFloat(section = ConfigConst.CONSTRAINED_DEVICE, \
										   		key = ConfigConst.HUMIDITY_SIM_FLOOR_KEY, \
												defaultVal = SensorDataGenerator.LOW_NORMAL_ENV_HUMIDITY)
		
		humidityCeiling = self.configUtil.getFloat(section = ConfigConst.CONSTRAINED_DEVICE, \
											 		key = ConfigConst.HUMIDITY_SIM_CEILING_KEY, \
													defaultVal = SensorDataGenerator.HI_NORMAL_ENV_HUMIDITY)
		
		pressureFloor = self.configUtil.getFloat(section = ConfigConst.CONSTRAINED_DEVICE, \
										   		key = ConfigConst.PRESSURE_SIM_FLOOR_KEY, \
												defaultVal = SensorDataGenerator.LOW_NORMAL_ENV_PRESSURE)
		
		pressureCeiling = self.configUtil.getFloat(section = ConfigConst.CONSTRAINED_DEVICE, \
											 		key = ConfigConst.PRESSURE_SIM_CEILING_KEY, \
													defaultVal = SensorDataGenerator.HI_NORMAL_ENV_PRESSURE)
		
		tempFloor = self.configUtil.getFloat(section = ConfigConst.CONSTRAINED_DEVICE, \
										   		key = ConfigConst.TEMP_SIM_FLOOR_KEY, \
												defaultVal = SensorDataGenerator.LOW_NORMAL_INDOOR_TEMP)
		
		tempCeiling = self.configUtil.getFloat(section = ConfigConst.CONSTRAINED_DEVICE, \
											 		key = ConfigConst.TEMP_SIM_CEILING_KEY, \
													defaultVal = SensorDataGenerator.HI_NORMAL_INDOOR_TEMP)
		
		if not self.useEmulator:
			self.dataGenerator = SensorDataGenerator()

			humidityData = self.dataGenerator.generateDailyEnvironmentHumidityDataSet(\
					minValue = humidityFloor, maxValue = humidityCeiling, useSeconds = False)
			
			pressureData = self.dataGenerator.generateDailyEnvironmentPressureDataSet(\
					minValue = pressureFloor, maxValue = pressureCeiling, useSeconds = False)
			
			tempData = self.dataGenerator.generateDailyIndoorTemperatureDataSet(\
					minValue = tempFloor, maxValue = tempCeiling, useSeconds = False)
			
			self.humidityAdapter = HumiditySensorSimTask(dataSet    = humidityData)
			self.pressureAdaptor = PressureSensorSimTask(dataSet    = pressureData)
			self.tempAdaptor     = TemperatureSensorSimTask(dataSet = tempData)

		else:
			''' Importing humidity emulator task module'''

	

			humidModule = import_module('programmingtheiot.cda.emulated.HumiditySensorEmulatorTask', 'HumiditySensorEmulatorTask')
			humidClass = getattr(humidModule, 'HumiditySensorEmulatorTask')
			self.humidityAdapter = humidClass()

			'''Importing pressure emulator task module'''

			pressModule = import_module('programmingtheiot.cda.emulated.PressureSensorEmulatorTask', 'PressureSensorEmulatorTask')
			pressClass = getattr(pressModule, 'PressureSensorEmulatorTask')
			self.pressureAdaptor = pressClass()

			'''Importing temperature emulator task module'''
			
			tempModule = import_module('programmingtheiot.cda.emulated.TemperatureSensorEmulatorTask', 'TemperatureSensorEmulatorTask')
			tempClass = getattr(tempModule, 'TemperatureSensorEmulatorTask')
			self.tempAdaptor = tempClass()

	
	def handleTelemetry(self):
		# pass

		humidityData = self.humidityAdapter.generateTelemetry()
		pressureData = self.pressureAdaptor.generateTelemetry()
		tempData     = self.tempAdaptor.generateTelemetry()
		
		humidityData.setLocationID(self.locationID)
		pressureData.setLocationID(self.locationID)
		tempData.setLocationID(self.locationID)

		logging.debug("Generated Humidity Data: "+ str(humidityData.value) + "\n")
		logging.debug("Generated Pressure Data: "+ str(pressureData.value)+ "\n")
		logging.debug("Generated Temperature Data: "+ str(tempData.value) + "\n")

		if self.dataMsgListner:
			self.dataMsgListner.handleSensorMessage(humidityData)
			self.dataMsgListner.handleSensorMessage(pressureData)
			self.dataMsgListner.handleSensorMessage(tempData)

		
	def setDataMessageListener(self, listener: IDataMessageListener) -> bool:
		# pass
		if listener:
			self.dataMsgListner = listener
			return True
		
		else:
			logging.info("Listener doesn't exist. Returing false")
			return False
	
	def startManager(self) -> bool:
		# pass
		logging.info("*****Starting Sensor Adaptor Manager*****")

		if not self.scheduler.running:
			self.scheduler.start()
			return True
		
		else:
			logging.info("Sensor adaptor manager already started. Ignoring new start")
			return False
		
	def stopManager(self) -> bool:
		# pass
		logging.info("*****Stopping Sensor Adaptor Manager*****")

		try:
			self.scheduler.shutdown()
			return True
		except:
			logging.info("Sensor adaptor manager already stopped. Igmoring")
			return False
