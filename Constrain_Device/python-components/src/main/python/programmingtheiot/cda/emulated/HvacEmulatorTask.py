#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import logging

from time import sleep

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.cda.sim.BaseActuatorSimTask import BaseActuatorSimTask

from pisense import SenseHAT

class HvacEmulatorTask(BaseActuatorSimTask):
	"""
	This class extends Base Actuator simulator Task and provides wrapper methods to activate and deactivate the Hvac 
	emulated on SenseHat Emulator. It displaces the type of actuator and the value on the screen.
	
	"""

	def __init__(self):
		# pass
		super(HvacEmulatorTask, self).__init__(name = ConfigConst.HVAC_ACTUATOR_NAME, \
										 		typeID = ConfigConst.HVAC_ACTUATOR_TYPE, \
												simpleName = "HVAC")
		
		enableEmulation = ConfigUtil().getBoolean(ConfigConst.CONSTRAINED_DEVICE, ConfigConst.ENABLE_EMULATOR_KEY)

		self.sh = SenseHAT(emulate = enableEmulation)

	def _activateActuator(self, val: float = ConfigConst.DEFAULT_VAL, stateData: str = None) -> int:
		# pass
		if self.sh.screen:
			msg = self.getSimpleName() + "ON: " + str(val) + "C"
			self.sh.screen.scroll_text(msg)
			return 0
		
		else:
			logging.warning("No SenseHAT LED screen instance to write.")
			return -1

	def _deactivateActuator(self, val: float = ConfigConst.DEFAULT_VAL, stateData: str = None) -> int:
		# pass
		if self.sh.screen:
			msg = self.getSimpleName() + "OFF"
			self.sh.screen.scroll_text(msg)

			sleep(5)

			self.sh.screen.clear()
			return 0
		
		else:
			logging.warning("No SenseHAT LED screen instance to clear / close.")
			return -1
	