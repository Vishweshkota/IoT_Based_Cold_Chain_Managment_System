"""
    This is a test class to generate and test all 14 MQTT 3.1.1 Control Packets.
    It implements all the test cases required for connected devices class.
"""

import logging
import unittest

from time import sleep

import programmingtheiot.common.ConfigConst as ConfigConst

from programmingtheiot.common.ConfigUtil import ConfigUtil
from programmingtheiot.common.ResourceNameEnum import ResourceNameEnum
from programmingtheiot.common.DefaultDataMessageListener import DefaultDataMessageListener

from programmingtheiot.cda.connection.MqttClientConnector import MqttClientConnector

from programmingtheiot.data.ActuatorData import ActuatorData
from programmingtheiot.data.SensorData import SensorData
from programmingtheiot.data.SystemPerformanceData import SystemPerformanceData
from programmingtheiot.data.DataUtil import DataUtil

class MqttClientControPacketTest(unittest.TestCase):
    
    @classmethod
    def setUpClass(self):
        logging.basicConfig(format = '%(asctime)s:%(module)s:%(levelname)s:%(message)s', level = logging.DEBUG)
        logging.info("Executing the MqttClientControlPacketTest class....")

        self.cfg = ConfigUtil()

        # Creating a Mqtt client
        self.mcc = MqttClientConnector(clientID = "MyMqttTestClient")

    def setUp(self):
        pass
    
    def tearDown(self):
        pass
    
    # Method to test connect and disconnect for MQTT
    def testConnectAndDisconnect(self):
        delay = self.cfg.getInteger(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE)

        self.mcc.connectClient()

        sleep( delay + 5)

        self.mcc.disconnectClient()

    def testServerPing(self):
        pass

    def testPubSub(self):
        pass
