# IoT_Based_Cold_Chain_Managment_System
An end to end IoT based Cold/Supply chain management system
![System Architecture](https://github.com/user-attachments/assets/e2771a37-3079-4f87-a746-9d3143c49e0f)

This project contains 3 key compentents.
- Constrain Device App
- Gateway Device App
- Cloud storage and Visualization
# Constrain Device App
It is attached to the Physical elements of the system. Gets all telemetry data and enviromental data from the system and communicates it to Gateway device over MQTT/CoAP with TLS encryption. For this project CDA is attached to SenseHat emulator.
![SenseHat Emulator](https://github.com/user-attachments/assets/5da719f3-63f3-4d24-b7fa-e7e442b7b7f9)

# Gateway Device App
It is a gateway between CDA and GDA where data is processed and stored locally in Redis Database. It also transfers all the data received to cloud over an MQTT/CoAP with TLS encryption.
# Cloud
All the telemetry and encironmental data received are stored and visualized in a dashboard
![Cloud Dashboard](https://github.com/user-attachments/assets/0920d917-424f-486f-a602-0cf69af2ef6d)

