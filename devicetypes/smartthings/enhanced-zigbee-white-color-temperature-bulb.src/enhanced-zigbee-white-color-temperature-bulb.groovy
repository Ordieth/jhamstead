/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Enhanced ZigBee White Color Temperature Bulb
 *
 *  2/2/17 - Added ability to set On/Off Transition Time Attribute
 *
 *  Author: SmartThings
 *  Date: 2015-09-22
 */

metadata {
    definition (name: "Enhanced ZigBee White Color Temperature Bulb", namespace: "smartthings", author: "jhamstead") {

        capability "Actuator"
        capability "Color Temperature"
        capability "Configuration"
        capability "Health Check"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"

        attribute "colorName", "string"
        command "setGenericName"

        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300", outClusters: "0019"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04", outClusters: "0019"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY BR Tunable White", deviceJoinName: "SYLVANIA Smart BR30 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY RT Tunable White", deviceJoinName: "SYLVANIA Smart RT5/6 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "Classic A60 TW", deviceJoinName: "OSRAM LIGHTIFY LED Classic A60 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "LIGHTIFY A19 Tunable White", deviceJoinName: "SYLVANIA Smart A19 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B04, FC0F", outClusters: "0019", manufacturer: "OSRAM", model: "Classic B40 TW - LIGHTIFY", deviceJoinName: "OSRAM LIGHTIFY Classic B40 Tunable White"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0702, 0B05", outClusters: "0019", manufacturer: "sengled", model: "Z01-A19NAE26", deviceJoinName: "Sengled Element plus"
    }

    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2700..6500)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }
        valueTile("colorName", "device.colorName", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "colorName", label: '${currentValue}'
        }
        valueTile("onOffTransitionTime", "device.onOffTransitionTime", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "transitionTimeValue", label: 'Fade: ${currentValue} seconds'
        }

        main(["switch"])
        details(["switch", "colorTempSliderControl", "colorName", "onOffTransitionTime", "refresh"])
    }
    preferences {
        section ("Switch Properties"){
            input "transitionTime", "number", title: "On/Off Transition Time (0-30 Seconds)", description: true, defaultValue: 0, required: false, range: "0..30"
        }
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "description is $description"
    def event = zigbee.getEvent(description)
    if (event) {
        if (event.name=="level" && event.value==0) {}
        else {
            if (event.name=="colorTemperature") {
                setGenericName(event.value)
            }
            sendEvent(event)
        }
    }
    else {
        def cluster = zigbee.parse(description)
        //Map descMap = zigbee.parseDescriptionAsMap(description)
        if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07) {
            if (cluster.data[0] == 0x00) {
                log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
                sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
            }
            else {
                log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
            }
        }
        else if (cluster.clusterInt == 0x0008 && cluster.attrInt == 0x0010 && cluster.value) {
            def value = (Integer.parseInt(descMap.value, 16) / 10)
            return [ name: "onOffTransitionTime", descriptionText: "On/Off Transition Time: ${value}", value: value, displayed: true, isStateChange: true ]
        }
        else {
            log.warn "DID NOT PARSE MESSAGE for description : $description"
            log.debug "${cluster}"
        }
    }
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def setLevel(value) {
    zigbee.setLevel(value)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return zigbee.onOffRefresh()
}

def updated() {
    def onOffTransitionTime = (settings.transitionTime) ? settings.transitionTime : 0
    return zigbee.writeAttribute(0x0008, 0x0010, 0x21, zigbee.convertToHexString((onOffTransitionTime * 10),4)) + zigbee.readAttribute(0x0008, 0x0010)
}

def refresh() {
    zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh() + zigbee.onOffConfig(0, 300) + zigbee.levelConfig() + zigbee.colorTemperatureConfig()
    return zigbee.readAttribute(0x0008, 0x0010)
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
    // enrolls with default periodic reporting until newer 5 min interval is confirmed
    sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

    // OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
    refresh()
}

def setColorTemperature(value) {
    setGenericName(value)
    zigbee.setColorTemperature(value)
}

//Naming based on the wiki article here: http://en.wikipedia.org/wiki/Color_temperature
def setGenericName(value){
    if (value != null) {
        def genericName = "White"
        if (value < 3300) {
            genericName = "Soft White"
        } else if (value < 4150) {
            genericName = "Moonlight"
        } else if (value <= 5000) {
            genericName = "Cool White"
        } else if (value >= 5000) {
            genericName = "Daylight"
        }
        sendEvent(name: "colorName", value: genericName)
    }
}