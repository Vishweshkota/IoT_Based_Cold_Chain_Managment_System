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

class ActuatorData(BaseIotData):
	"""
	This ActuatorData class extends BaseIot data.
	It gets a self object and returns the basic actuator data required.

	- command
	- state
	- value
	- respone
	It also sets the actuator parameters
	
	"""

	def __init__(self, typeID: int = ConfigConst.DEFAULT_ACTUATOR_TYPE, name = ConfigConst.NOT_SET, d = None):
		super(ActuatorData, self).__init__(name = name, typeID = typeID, d = d)
		# pass
		self.value = ConfigConst.DEFAULT_VAL
		self.command = ConfigConst.DEFAULT_COMMAND
		self.stateData = ""
		self.isResponse = False
	
	def getCommand(self) -> int:
		# pass
		return self.command
	
	def getStateData(self) -> str:
		# pass
		return self.stateData
	
	def getValue(self) -> float:
		# pass
		return self.value
	
	def isResponseFlagEnabled(self) -> bool:
		return self.isResponse
	
	def setCommand(self, command: int):
		# pass
		self.command = command
		self.updateTimeStamp()
	
	def setAsResponse(self):
		# pass
		self.isResponse = True
		self.updateTimeStamp()
		
	def setStateData(self, stateData: str):
		# pass
		if stateData:
			self.stateData = stateData
			self.updateTimeStamp()
	
	def setValue(self, val: float):
		# pass
		self.value = val
		self.updateTimeStamp()
		
	def _handleUpdateData(self, data):
		# pass
		if data and isinstance(data, ActuatorData):
			self.command = data.getCommand()
			self.stateData = data.getStateData()
			self.value = data.getValue()
			self.isResponse = data.isResponseFlagEnabled()
		