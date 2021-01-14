/*
 *  Virtual Momentary Switch
 * 
 *  Copyright 2021 Tim Grimley
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
 *	Author: Tim Grimley
 *	Date: 01/14/2021
 *
 *	Changelog:
 *
 *  0.10 (09/22/2020) - Initial Release
 *	
 * Note - this device handler creates a virtual switch that shuts off automatically after a user set amount of seconds
 *
 */


import groovy.transform.Field
@Field final Integer RESET_TIME = 15 // Reset time in seconds


metadata {
	
  definition (name: "Momentary Switch", namespace: "mwav3", author: "Tim Grimley") {
        capability "Actuator"
        capability "Switch" 
        capability "Health Check"
    }
preferences {
    input "resetTime", "number", title: "Time after which to reset the switch off", required: false, displayDuringSetup: false, range: "1..120"
}
}

def installed() {
    // Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "lan", hubHardwareId: device.hub?.hardwareID, offlinePingable: "1"])
    off() // Reset it
}

def updated() {
    // Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "lan", hubHardwareId: device.hub?.hardwareID, offlinePingable: "1"])
    off() // Reset it
}

def ping() {
    sendEvent(name: "switch", value: device.currentValue("switch"))
}

def on() {
    log.trace "$device.displayName Switch on, and resetting in ${resetTime ?: RESET_TIME} seconds"
	sendEvent(name: "switch", value: "on")
    runIn(resetTime ?: RESET_TIME, off) 
}

def off() {
	log.trace "$device.displayName Resetting Switch to off"
	sendEvent(name: "switch", value: "off")
}

// THIS IS THE END OF THE FILE
