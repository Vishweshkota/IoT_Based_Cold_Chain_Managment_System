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

from programmingtheiot.data.ActuatorData import ActuatorData
from programmingtheiot.cda.sim.BaseActuatorSimTask import BaseActuatorSimTask

import programmingtheiot.common.ConfigConst as ConfigConst

class HvacActuatorSimTask(BaseActuatorSimTask):
	"""
	This is a simple wrapper for an Actuator abstraction - it provides
	a container for the actuator's state, value, name, and status. A
	command variable is also provided to instruct the actuator to
	perform a specific function (in addition to setting a new value
	via the 'val' parameter.	
	"""

	def __init__(self):
		# pass
		super(HvacActuatorSimTask, self).__init__( \
						name = ConfigConst.HVAC_ACTUATOR_NAME, \
						typeID = ConfigConst.HVAC_ACTUATOR_TYPE, \
						simpleName = "HVAC")
		