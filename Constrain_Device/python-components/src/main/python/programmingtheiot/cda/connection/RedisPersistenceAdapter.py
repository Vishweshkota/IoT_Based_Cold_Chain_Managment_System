#####
# 
# This class is part of the Programming the Internet of Things project.
# 
# It is provided as a simple shell implementation for the Programming 
# the Internet of Things exercises,and designed to be modified by the student as needed.
#

import logging
import redis

from programmingtheiot.common.ConfigUtil import ConfigUtil
import programmingtheiot.common.ConfigConst as  ConfigConst

from programmingtheiot.common.ResourceNameEnum import ResourceNameEnum
from programmingtheiot.data.SensorData import SensorData
from programmingtheiot.data.ActuatorData import ActuatorData
from programmingtheiot.data.SystemPerformanceData import SystemPerformanceData

"""
    Shell Implementation of RedisPersistenceAdpter it manages 
    client connections and status of clients and data stored

    - connectClient()
    - disconnectClient()
    - storeData()
"""
class RedisPersistenceAdapter():

    """
        Constructor
    """
    def __init__(self) -> None:
        self.configUtil = ConfigUtil()

        self.redisObj = None

        """Getting host name and port number from config const file"""
        self.hostName = self.configUtil.getProperty(section = ConfigConst.DATA_GATEWAY_SERVICE, \
                                              key = ConfigConst.HOST_KEY, defaultVal = ConfigConst.DEFAULT_HOST)
        
        self.port = self.configUtil.getInteger(section = ConfigConst.DATA_GATEWAY_SERVICE, \
                                          key = ConfigConst.PORT_KEY, defaultVal = ConfigConst.DEFAULT_REDIS_PORT)
        
        
        self.redisObj = redis.Redis(self.hostName, self.port)


    """ Method to connect to redis server """
    def connectClient(self) -> bool:

        if self.redisObj.ping:
            logging.warning("*****Redis client is already connected*****")
            return True
        
        else:
            logging.info("Connecting to Redis")
            self.redisObj = redis.Redis(host = self.hostName, port = self.port)
            return True 


    """Method to disconnect from redis server"""
    def disconnetClient(self) -> bool:
        logging.info("***Disconnecting Redis Client***")
    
        if self.redisObj.close():
            logging.info("***Redis Client disconnected Successfully")
            return True
        else:
            logging.warning("*****Failed to disconnect Redis Client")

    """Method to store data on the redis server"""
    def storeSensorData(self, resource: ResourceNameEnum, data: SensorData) -> bool:
       self.redisObj.mset(data)
 