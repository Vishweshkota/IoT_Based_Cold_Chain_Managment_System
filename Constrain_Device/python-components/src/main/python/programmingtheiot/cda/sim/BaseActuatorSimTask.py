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

from programmingtheiot.data.ActuatorData import ActuatorData

class BaseActuatorSimTask():
	"""
	This class is the base for all Actuator related activities.
	It sets the paramaters such as name, typeID, dataset, and gets the last known and updates present actuator values using 
	get response and update actuator response methods
	
	"""

	def __init__(self, name: str = ConfigConst.NOT_SET, typeID: int = ConfigConst.DEFAULT_ACTUATOR_TYPE, simpleName: str = "Actuator"):
		# pass
		self.latestActuatorResponse = ActuatorData(typeID = typeID, name = name)
		self.latestActuatorResponse.setAsResponse()

		self.name = name
		self.typeID = typeID
		self.simpleName = simpleName
		self.lastKnownCommand = ConfigConst.DEFAULT_COMMAND
		self.lastKnownValue = ConfigConst.DEFAULT_VAL
		
	def getLatestActuatorResponse(self) -> ActuatorData:
		"""
		This can return the current ActuatorData response instance or a copy.
		"""
		pass
	
	def getSimpleName(self) -> str:
		# pass
		return self.simpleName
	
	def updateActuator(self, data: ActuatorData) -> ActuatorData:
		
		"""
		NOTE: If 'data' is valid, the actuator-specific work can be delegated
		as follows:
		 - if command is ON: call self._activateActuator()
		 - if command is OFF: call self._deactivateActuator()
		
		Both of these methods will have a generic implementation (logging only) within
		this base class, although the sub-class may override if preferable.
		"""
		# pass

		# Validating if data is not null and type ID matches with data ID
		if data and self.typeID == data.getTypeID():
			statusCode = ConfigConst.DEFAULT_STATUS

			curCommand = data.getCommand()
			curVal = data.getValue()

			if curCommand == self.lastKnownCommand and curVal == self.lastKnownValue:
				logging.debug("New actuator command and value are repeated. Igonoring the command: %s and value %s",\
				  									str(curCommand), str(curVal))
				
			else:
				logging.debug("New actuator command and value to be applied:\tCommand: %s\tValue: %s", \
				  								str(curCommand), str(curVal))
				
				if curCommand == ConfigConst.COMMAND_ON:
					logging.info("Actuating actuator......")
					statusCode = self._activateActuator(val = data.getValue(), stateData = data.getStateData())
				
				
				elif curCommand == ConfigConst.COMMAND_OFF:
					logging.info("Deactivating Actuator......")
					statusCode = self._deactivateActuator(val = data.getValue(), stateData = data.getStateData())

				else:
					logging.warning("********UNKNOWN ACTUATION COMMAND*******\nIgnoring: %s",str(curCommand))
					statusCode = -1

				# Upating last known values with current values
				self.lastKnownCommand = curCommand
				self.lastKnownValue = curVal

				# Creating Actuator Data response from the original command
				actuatorResponse = ActuatorData()
				actuatorResponse.updateData(data)
				actuatorResponse.setStatusCode(statusCode)
				actuatorResponse.setAsResponse()

				self.latestActuatorResponse.updateData(actuatorResponse)

				return actuatorResponse
		
		return None




		
	def _activateActuator(self, val: float = ConfigConst.DEFAULT_VAL, stateData: str = None) -> int:
		"""
		Implement basic logging. Actuator-specific functionality should be implemented by sub-class.
		
		@param val The actuation activation value to process.
		@param stateData The string state data to use in processing the command.
		"""

		msg = "\n*******"
		msg = msg + "\n* 0 N *"
		msg = msg + "\n*******"
		msg = msg + "\n" + self.name + "Value -> " + str(val) + "\n======="

		logging.info("Activating %s actuator ON: %s", self.name, msg)

		return 0
		
	def _deactivateActuator(self, val: float = ConfigConst.DEFAULT_VAL, stateData: str = None) -> int:
		"""
		Implement basic logging. Actuator-specific functionality should be implemented by sub-class.
		
		@param val The actuation activation value to process.
		@param stateData The string state data to use in processing the command.
		"""
		
		msg = "\n*******"
		msg = msg + "\n* OFF *"
		msg = msg + "\n*******"
		logging.info("Simulating %s actuator OFF: %s", self.name, msg)

		return 0
		