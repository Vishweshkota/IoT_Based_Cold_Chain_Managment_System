#####
# 
# This class is part of the Programming the Internet of Things
# project, and is available via the MIT License, which can be
# found in the LICENSE file at the top level of this repository.
# 
# You may find it more helpful to your design to adjust the
# functionality, constants and interfaces (if there are any)
# provided within in order to meet the needs of your specific
# Programming the Internet of Things project.
# 

import logging

from time import sleep

"""Importing System performance module"""
# from programmingtheiot.cda.system.SystemPerformanceManager import SystemPerformanceManager

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.cda.app.DeviceDataManager import DeviceDataManager


logging.basicConfig(format = '%(asctime)s:%(name)s:%(levelname)s:%(message)s', level = logging.DEBUG)

class ConstrainedDeviceApp():
	"""
	Definition of the ConstrainedDeviceApp class.
	
	"""
	
	def __init__(self):
		"""
		Initialization of class.
		
		@param path The name of the resource to apply to the URI.
		"""
		logging.info("Initializing CDA...")

		# self.sysPerfMgr = SystemPerformanceManager()		# CLASS SCOPE OF INSTANCE OF SYSTEMPERFORMANCE MANAGER
		self.dataMgr = DeviceDataManager()

		# TODO: implementation here
	
	def startApp(self):
		"""
		Start the CDA. Calls startManager() on the device data manager instance.
		
		"""
		logging.info("Starting CDA...")
		
		# TODO: implementation here
		#sleep(65)
		self.dataMgr.startManager()		#STARTING SYSTEM PERFORMANCE MANAGER
		
		logging.info("CDA started.")

	def stopApp(self, code: int):
		"""
		Stop the CDA. Calls stopManager() on the device data manager instance.
		
		"""
		logging.info("CDA stopping...")
		
		# TODO: implementation here
		self.dataMgr.stopManager()
		
		logging.info("CDA stopped with exit code %s.", str(code))
		
	def parseArgs(self, args):
		"""
		Parse command line args.
		
		@param args The arguments to parse.
		"""
		logging.info("Parsing command line args...")



def main():
	"""
	Main function definition for running client as application.
	
	Current implementation runs for 35 seconds then exits.
	"""
	cda = ConstrainedDeviceApp()
	cda.startApp()

	runForever = ConfigUtil().getBoolean(ConfigConst.CONSTRAINED_DEVICE, ConfigConst.RUN_FOREVER_KEY)

	if runForever:
		while(True):
			sleep(5)
	else:
	
						# run for 65 seconds - this can be changed as needed
		sleep(65)		# Creates a time delay of 65 seconds between start and stop app 
	
		# optionally stop the app - this can be removed if needed
		cda.stopApp(0)

if __name__ == '__main__':
	"""
	Attribute definition for when invoking as app via command line
	
	"""
	main()
	