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

import programmingtheiot.common.ConfigConst as ConfigConst
from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.common.IDataMessageListener import IDataMessageListener

from programmingtheiot.data.ActuatorData import ActuatorData

from programmingtheiot.cda.sim.HvacActuatorSimTask import HvacActuatorSimTask
from programmingtheiot.cda.sim.HumidifierActuatorSimTask import HumidifierActuatorSimTask

class ActuatorAdapterManager(object):
	"""
	This class is responsible for managing all the Actuator functionality.

	It initializes the sensor paramaters such as 
	1) pollRate, locationID, enableEmulator

	It also schedules the Actuator tasks in background using apscheduler
	It implements handle telemetry which calls generate telemetry to get the readouts from Actuators attached(Either simulated or emulated)
	
	"""
	
	def __init__(self, dataMsgListener: IDataMessageListener = None):
		# pass
		self.dataMsgListener = dataMsgListener

		self.configUtil = ConfigUtil()

		self.useSimulator = self.configUtil.getBoolean(section = ConfigConst.CONSTRAINED_DEVICE, \
												 		key = ConfigConst.ENABLE_SIMULATOR_KEY)
		
		self.useEmulator = self.configUtil.getBoolean(section = ConfigConst.CONSTRAINED_DEVICE, \
														key = ConfigConst.ENABLE_EMULATOR_KEY)
		
		self.deviceID = self.configUtil.getProperty(section = ConfigConst.CONSTRAINED_DEVICE, \
											  		key = ConfigConst.DEVICE_LOCATION_ID_KEY, \
													defaultVal = ConfigConst.NOT_SET)
		
		self.locationID = self.configUtil.getProperty(section = ConfigConst.CONSTRAINED_DEVICE, \
														key = ConfigConst.DEVICE_LOCATION_ID_KEY, \
														defaultVal = ConfigConst.NOT_SET)
		
		self.humidifierActuator = None
		self.hvacActuator 		= None
		self.ledDisplayActuator = None

		self._initEnvironmentalActuationTasks()
	
	def _initEnvironmentalActuationTasks(self):
		# pass
		if not self.useEmulator:
			# loading simulated actuation tasks
			self.humidifierActuator = HumidifierActuatorSimTask()
			self.hvacActuator       = HvacActuatorSimTask()
		
		else:
			'''Loading Humidifier actuation Task'''
			humidfModule = import_module('programmingtheiot.cda.emulated.HumidifierEmulatorTask', 'HumidifierEmulatorTask')
			humidfClass = getattr(humidfModule, 'HumidifierEmulatorTask')
			self.humidifierActuator = humidfClass()

			'''Loading HVAC actuation Task'''
			hvacModule = import_module('programmingtheiot.cda.emulated.HvacEmulatorTask', 'HvacEmulatorTask')
			hvacClass = getattr(hvacModule, 'HvacEmulatorTask')
			self.hvacActuator = hvacClass()			
			
			'''Loading LED actuation Task'''
			ledDisplayModule = import_module('programmingtheiot.cda.emulated.LedDisplayEmulatorTask', 'LedDisplayEmulatorTask')
			ledClass = getattr(ledDisplayModule, 'LedDisplayEmulatorTask')
			self.ledDisplayActuator = ledClass()	


	def sendActuatorCommand(self, data: ActuatorData) -> ActuatorData:

		if data and not data.isResponseFlagEnabled():
			if self.locationID == data.getLocationID():
				logging.info("Actuator Command received for the location ID's %s. Processing...", str(data.getLocationID()))

				aType = data.getTypeID()
				responseData = None

				# Check for type of actuator invoced
				if aType == ConfigConst.HUMIDIFIER_ACTUATOR_TYPE and self.humidifierActuator:
					responseData = self.humidifierActuator.updateActuator(data)
				
				elif aType == ConfigConst.HVAC_ACTUATOR_TYPE and self.hvacActuator:
					responseData = self.hvacActuator.updateActuator(data)
				
				elif aType == ConfigConst.LED_DISPLAY_ACTUATOR_TYPE and self.ledDisplayActuator:
					responseData = self.ledDisplayActuator.updateActuator(data)
				
				else:
					logging.warning("No Valid actuator type. Ignoring the actuation type %s", str(data.getTypeID()))

				logging.info(responseData)
				return responseData
			
			else:
				logging.warning("Location ID doesn't match. Ignoring actuation MYID: %s != YourID %s", str(self.locationID, data.getLocationID()))
		
		else:
			logging.warning("Actuation message received. Message is empty or response. Ignoring....")
	
		return None



	
	def setDataMessageListener(self, listener: IDataMessageListener) -> bool:
		# pass
		if listener:
			self.dataMsgListener = listener
