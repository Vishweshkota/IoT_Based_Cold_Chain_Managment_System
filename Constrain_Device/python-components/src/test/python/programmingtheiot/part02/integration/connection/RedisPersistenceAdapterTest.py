import logging
import unittest

import programmingtheiot.common.ConfigConst as ConfigConst
from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.data.DataUtil import DataUtil

from programmingtheiot.common.ResourceNameEnum import ResourceNameEnum

from programmingtheiot.data.SensorData import SensorData
from programmingtheiot.cda.system.SensorAdapterManager import SensorAdapterManager

from programmingtheiot.data.ActuatorData import ActuatorData

from programmingtheiot.data.SystemPerformanceData import SystemPerformanceData

from programmingtheiot.cda.connection.RedisPersistenceAdapter import RedisPersistenceAdapter


class RedisPersistenceAdapterTest(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        logging.basicConfig(format = '%(asctime)s:%(module)s:%(levelname)s:%(message)s', level = logging.DEBUG)
        logging.info("Running RedisPersistenceAdapterTest test cases...")

        encodeToUtf8 = False
        self.dataUtil = DataUtil(encodeToUtf8)

    def setUp(self):
        logging.info("*****Redis Persistence Adapter Test*****")
    
    def tearDown(self):
        pass

    # @unittest.skip("Ignore if not commented")
    def testConnectClient(self):

        redisObj = RedisPersistenceAdapter()
        redisObj.connectClient()

    # @unittest.skip("Ignore if not commented")
    def testStoreSensorData(self):

        redisObj = RedisPersistenceAdapter()
        sdObj = SensorData()
        sdObj.setName("Temperature")
        sdObj.setValue(22.5)

        redisObj.storeSensorData(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sdObj)

    #@unittest.skip("Ignore if not commented")
    def testDisconnectClient(self):

        redisObj = RedisPersistenceAdapter()
        redisObj.disconnetClient()


if __name__=="__main__":
    unittest.main()