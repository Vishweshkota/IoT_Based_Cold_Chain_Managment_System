#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell to guide the student and assist with
# implementation for the Programming the Internet of Things exercises,
# and designed to be modified by the student as needed.
#

import logging

from apscheduler.schedulers.background import BackgroundScheduler

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.common.IDataMessageListener import IDataMessageListener

from programmingtheiot.cda.system.SystemCpuUtilTask import SystemCpuUtilTask
from programmingtheiot.cda.system.SystemMemUtilTask import SystemMemUtilTask

from programmingtheiot.data.SystemPerformanceData import SystemPerformanceData

class SystemPerformanceManager(object):
	"""
	This class is responsible for managing all the system performance related data. It primarly manager
	
	1) CpuUtilization
	2) MemoryUtilization

	It uses startManager and stopManager methods to run System Performance management in background

	It sets all the constants for system data and starts a background scheduler adding instances of SystemCpuUtilTask and SystemMemUtilTask

	It gets the telemetry values using handle telemetry method which invokes getTelemetryValues from the repective classes 
	
	"""

	def __init__(self):
		#pass
		configUtil = ConfigUtil()

		"""Setting All the class scope variables \
			-> pollRate
			-> locationID
		"""
		self.pollRate = \
		configUtil.getInteger(section = ConfigConst.CONSTRAINED_DEVICE, key = ConfigConst.POLL_CYCLES_KEY, defaultVal = ConfigConst.DEFAULT_POLL_CYCLES)
	
		self.locationID = \
		configUtil.getProperty(section = ConfigConst.CONSTRAINED_DEVICE, key = ConfigConst.DEVICE_LOCATION_ID_KEY, defaultVal = ConfigConst.NOT_SET)
	
		if self.pollRate <= 0:
			self.pollRate = ConfigConst.DEFAULT_POLL_CYCLES
		
		self.dataMsgListener = None

		'''*****Connecting SystemCpuUtilTask and SystemMemUtilTask to SystemPerformanceManager*****'''
		
		self.scheduler = BackgroundScheduler()
		self.scheduler.add_job(self.handleTelemetry, 'interval', seconds = self.pollRate)

		self.cpuUtilTask = SystemCpuUtilTask()
		self.memUtilTask = SystemMemUtilTask()


	def handleTelemetry(self):
		# pass

		'''Getting CPU and MEMORY Telemetry values'''
		self.cpuUtilPct = self.cpuUtilTask.getTelemetryValue()
		self.memUtilPct = self.memUtilTask.getTelemetryValue()

		logging.debug('\nCPU utilization is %s percentage\nMemory utilization is %s percentage',str(self.cpuUtilPct),str(self.memUtilPct))

		sysPrefData = SystemPerformanceData()
		sysPrefData.setLocationID(self.locationID)
		sysPrefData.setCpuUtilization(self.cpuUtilPct)
		sysPrefData.setMemoryUtilization(self.memUtilPct)

		if self.dataMsgListener:
			self.dataMsgListener.handleSystemPerformanceMessage(data = sysPrefData)

		
	def setDataMessageListener(self, listener: IDataMessageListener) -> bool:
		if listener:
			self.dataMsgListener = listener
	
	def startManager(self):
		#pass
		logging.info("Starting SystemPerformanceManager...")
		# Checking if scheduler is running 
		if not self.scheduler.running:
			self.scheduler.start()
			logging.info("Started SystemPerformanceManager.")
		else:
			logging.warning("SystemPerformanceManager scheduler already started. Ignoring....")
		
	def stopManager(self):
		#pass
		logging.info("Stopping SystemPerformanceManager...")
		
		try:
			self.scheduler.shutdown()
			logging.info("Stopped SystemPerformanceManager.")
		except:
			logging.warning("SystemPerformanceManager schedular already stopped. Ignoring...")
