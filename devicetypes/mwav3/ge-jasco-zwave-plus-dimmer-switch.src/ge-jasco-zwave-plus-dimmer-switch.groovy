/**
 *  GE/Jasco Z-Wave Plus Dimmer Switch
 *
 *  Contains code from https://github.com/nuttytree/Nutty-SmartThings/blob/master/devicetypes/nuttytree/ge-jasco-zwave-plus-dimmer-switch.src/ge-jasco-zwave-plus-dimmer-switch.groovy
 *
 *  Copyright 2020 Chris Nussbaum, Tim Grimley
 *  Contributors - Evan Hunter, Bradlee_S
 *  Thanks Chris for the original copy of this great code!
 *  Thanks Evan for additional parameter configs for on/off and minimum dimmer
 *  Thanks Bradlee for the button programming to get this working in the new app's automations section
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
 *	Date: 8/31/2020
 *
 *	Changelog:
 *
 *  0.22 (11/17/2020) - Changed to support one button with different values (see mapping note below)
 *	0.21 (10/08/2020) - Added setting to ramp when setting level, added light capability
 *  0.20 (09/26/2020) - Added settings to force button value updates, configuration for on/off only, minimum dim level
 *  0.19 (08/29/2020) - Additional rewrite moved config settings to preferences to work with new app various changes
 *  0.18 (08/19/2020) - Initial Release Old handler Updated to add compatibility to new Smart lighting app 
 *  
 *
 *   Button Mappings  NOTE - THIS IS A BREAKING CHANGE from prior versions and uses a single button.  
 *                    ALL prior automations will need to be re-programmed or updated when updating this DTH from old versions:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        up_2x
 *   Double-Tap Down   1        down_2x
 *
 */

import groovy.transform.Field
import groovy.json.JsonOutput

metadata {
	definition (name: "GE/Jasco Z-Wave Plus Dimmer Switch", namespace: "mwav3", author: "Tim Grimley") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Health Check"
		capability "Indicator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
        capability "Light"

		attribute "inverted", "enum", ["inverted", "not inverted"]
        attribute "switchMode", "enum", ["modeDimmer", "modeSwitch"]
        attribute "ramp", "enum", ["instant", "ramp"]
		attribute "minimumDim", "number"
        attribute "zwaveSteps", "number"
        attribute "zwaveDelay", "number"
        attribute "manualSteps", "number"
        attribute "manualDelay", "number"
        attribute "allSteps", "number"
        attribute "allDelay", "number"
        
        command "doubleUp"
        command "doubleDown"
        command "inverted"
        command "notInverted"
        command "modeDimmer"
		command "modeSwitch"
		command "setMinimumDim"
        command "levelUp"
        command "levelDown"
        command "setZwaveSteps"
        command "setZwaveDelay"
        command "setManualSteps"
        command "setManualDelay"
        command "setAllSteps"
        command "setAllDelay"
        
        // These include version because there are older firmwares that don't support double-tap or the extra association groups
        fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.26", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
	    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.27", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
	    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.28", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
	    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.29", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3039", ver: "5.19", deviceJoinName: "GE Z-Wave Plus 1000W Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3130", ver: "5.21", deviceJoinName: "GE Z-Wave Plus Toggle Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3135", ver: "5.26", deviceJoinName: "Jasco Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3136", ver: "5.21", deviceJoinName: "Jasco Z-Wave Plus 1000W Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3137", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Dimmer"
	}


	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		status "09%": "command: 2003, payload: 09"
		status "10%": "command: 2003, payload: 0A"
		status "33%": "command: 2003, payload: 21"
		status "66%": "command: 2003, payload: 42"
		status "99%": "command: 2003, payload: 63"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}
    
    preferences {
       
       input "ledIndicator", "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When On", "off": "When Off", "never": "Never"], defaultValue: "off"
        input "invertSwitch", "bool", title: "Invert Switch", description: "Invert switch? ", required: false
        input "switchMode", "bool", title: "Make Dimmer On/Off Only", description: "On/Off Only? ", required: false
        input "ramp", "bool", title: "Ramp on set level (True, False) Default False", description: "Ramp Set Level", required: false
        input "forceupdate", "bool", title: "Force Settings Update/Refresh?", description: "Toggle to force settings update", required: false
		input "zwaveSteps", "number", title: "Z-Wave Dim Steps (1-99) Default 1", description: "Z-Wave Dim Steps ", required: false, range: "1..99"
		input "zwaveDelay", "number", title: "Z-Wave Dim Delay (10ms Increments, 3-255) Default 3", description: "Z-Wave Dim Delay (10ms Increments) ", required: false, range: "3..255"
		input "manualSteps", "number", title: "Manual Dim Steps (1-99) Default 1", description: "Manual Dim Steps ", required: false, range: "1..99"
		input "manualDelay", "number", title: "Manual Dim Delay (10ms Increments, 1-255) Default 3", description: "Manual Dim Delay (10ms Increments) ", required: false, range: "1..255"
		input "minimumDim", "number", title: "Minimim Dim Level (1-99) Default 1", description: "%", required: false, range: "1..99"		
		
        // No one uses these
        // input "allonSteps", "number", title: "All-On/All-Off Dim Steps (1-99)", description: "All-On/All-Off Dim Steps ", required: false, range: "1..99"
		// input "allonDelay", "number", title: "All-On/All-Off Dim Delay (10ms Increments, 1-255)", description: "All-On/All-Off Dim Delay (10ms Increments) ", required: false, range: "1..255"

       
       input (
            type: "paragraph",
            element: "paragraph",
            title: "Configure Association Groups:",
            description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
                         "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
                         "Devices are entered as a comma delimited list of IDs in hexadecimal format."
        )

        input (
            name: "requestedGroup2",
            title: "Association Group 2 Members (Max of 5):",
            type: "text",
            required: false
        )

        input (
            name: "requestedGroup3",
            title: "Association Group 3 Members (Max of 4):",
            type: "text",
            required: false
        )
    }

	tiles(scale:2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState:"turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:"Turning On", action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:"Turning Off", action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action:"levelUp"
				attributeState "VALUE_DOWN", action:"levelDown"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
        
         	standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		 }
         
          standardTile("doubleUp", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Tap ▲▲", backgroundColor: "#ffffff", action: "doubleUp", icon: "st.switches.switch.on"
		 }     
 
         standardTile("doubleDown", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Tap ▼▼", backgroundColor: "#ffffff", action: "doubleDown", icon: "st.switches.switch.off"
		 } 

        
        	// These are removed since they do not appear in new app
        
       
                      
		// standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
		//	state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
		//	state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
		//	state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
		// }
        
		// standardTile("inverted", "device.inverted", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
		//	state "not inverted", label: "Not Inverted", action:"inverted", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchNotInverted.png", backgroundColor: "#ffffff"
		//	state "inverted", label: "Inverted", action:"notInverted", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchInverted.png", backgroundColor: "#ffffff"
		// }

		// standardTile("zwaveStepsLabel", "device.zwaveSteps",  width: 2, height: 1, inactiveLabel: false) {
        //	state "default", label:'Z-Wave Dim Steps: ${currentValue}'
        // }
        
        // controlTile("zwaveSteps", "device.zwaveSteps", "slider", width: 4, height: 1, range:"(1..99)", inactiveLabel: false) {
		//	state "default", action:"setZwaveSteps"
		// }

		// standardTile("zwaveDelayLabel", "device.zwaveDelay",  width: 2, height: 1, inactiveLabel: false) {
        // 	state "default", label:'Z-Wave Dim Delay: ${currentValue}0ms'
        // }
        // controlTile("zwaveDelay", "device.zwaveDelay", "slider", width: 4, height: 1, range:"(1..255)", inactiveLabel: false) {
		//	state "default", action:"setZwaveDelay"
		// }

		// standardTile("manualStepsLabel", "device.manualSteps",  width: 2, height: 1, inactiveLabel: false) {
        //	state "default", label:'Manual Dim Steps: ${currentValue}'
        // }
        // controlTile("manualSteps", "device.manualSteps", "slider", width: 4, height: 1, range:"(1..99)", inactiveLabel: false) {
		//	state "default", action:"setManualSteps"
		// }

		// standardTile("manualDelayLabel", "device.manualDelay",  width: 2, height: 1, inactiveLabel: false) {
        //	state "default", label:'Manual Dim Delay: ${currentValue}0ms'
        // }
        // controlTile("manualDelay", "device.manualDelay", "slider", width: 4, height: 1, range:"(1..255)", inactiveLabel: false) {
		//	state "default", action:"setManualDelay"
		// }

		// standardTile("allStepsLabel", "device.allSteps",  width: 2, height: 1, inactiveLabel: false) {
        // 	state "default", label:'All On/Off Dim Steps: ${currentValue}'
        // }
        // controlTile("allSteps", "device.allSteps", "slider", width: 4, height: 1, range:"(1..99)", inactiveLabel: false) {
		//	state "default", action:"setAllSteps"
		// }

		// standardTile("allDelayLabel", "device.allDelay",  width: 2, height: 1, inactiveLabel: false) {
        //	state "default", label:'All On/Off Dim Delay: ${currentValue}0ms'
        //   }
        // controlTile("allDelay", "device.allDelay", "slider", width: 4, height: 1, range:"(1..255)", inactiveLabel: false) {
		//	state "default", action:"setAllDelay"
		// }

		main "switch"
        details(["switch", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
    log.debug "description: $description"
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x26: 3, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    if (!device.currentValue("supportedButtonValues")) {
        sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up_2x","down_2x"]), displayed:false)
    }
    result    
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	log.debug("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")
	def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		log.warn("zwaveEvent(): Could not extract command from ${cmd}")
	} else {
		log.debug("zwaveEvent(): Extracted command ${encapsulatedCommand}")
        return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	dimmerEvents(cmd)
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	def result = [createEvent(name: "switch", value: value, type: "physical")]
	if (cmd.value && cmd.value <= 100) {
		result << createEvent(name: "level", value: cmd.value, unit: "%", type: "physical")
	}
	return result
}


def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	if (cmd.value == 255) {
    	createEvent(name: "button", value: "up_2x", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1 up_2x) on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "button", value: "down_2x", data: [buttonNumber: 1], descriptionText: "Double-tap down (button 1 down_2x) on $device.displayName", isStateChange: true, type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    state.group3 = "1,2"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	createEvent(name: "numberOfButtons", value: 1, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
	def name = ""
    def value = ""
    def reportValue = cmd.scaledConfigurationValue
    switch (cmd.parameterNumber) {
        case 3:
            name = "indicatorStatus"
            value = reportValue == 1 ? "when on" : reportValue == 2 ? "never" : "when off"
            break
        case 4:
            name = "inverted"
            value = reportValue == 1 ? "true" : "false"
            break
        case 6:
        	name = "ramp"
            value = reportValue == 1 ? "true" : "false"
            break
        case 7:
            name = "zwaveSteps"
            value = reportValue
            break
        case 8:
            name = "zwaveDelay"
            value = reportValue
            break
        case 9:
            name = "manualSteps"
            value = reportValue
            break
        case 10:
            name = "manualDelay"
            value = reportValue
            break
        case 11:
            name = "allSteps"
            value = reportValue
            break
        case 12:
            name = "allDelay"
            value = reportValue
            break
        case 16:
            name = "switchMode"
            value = reportValue == 1 ? "true" : "false"
            break
        case 20:
            name = "minimumDim"
            value = reportValue
            break
        default:
            break
    }
	createEvent([name: name, value: value, displayed: false])
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    log.debug "---MANUFACTURER SPECIFIC REPORT V2---"
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
    state.manufacturer=cmd.manufacturerName
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)	
    sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	log.debug "---VERSION REPORT V1--- ${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

// handle commands
def configure() {
    def cmds = []
    // Get current config parameter values
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 20).format()
    
    // Add the hub to association group 3 to get double-tap notifications
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
    
    delayBetween(cmds,500)
}

def updated() {
    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def nodes = []
    def cmds = []
    
    if (settings.zwaveSteps != null) {
    
    def zwaveSteps = Math.max(Math.min(zwaveSteps, 99), 1)
   	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: zwaveSteps, parameterNumber: 7, size: 1).format()))
    sendEvent(name: "zwaveSteps", value: zwaveSteps, displayed: false)
  	    
    }
    
    if (settings.zwaveDelay != null) {
    
    def zwaveDelay = Math.max(Math.min(zwaveDelay, 255), 3)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: zwaveDelay, parameterNumber: 8, size: 2).format()))
    sendEvent(name: "zwaveDelay", value: zwaveDelay, displayed: false)
    
    }
    
    if (settings.manualSteps != null) {
    
    def manualSteps = Math.max(Math.min(manualSteps, 99), 1)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: manualSteps, parameterNumber: 9, size: 1).format()))
    sendEvent(name: "manualSteps", value: manualSteps, displayed: false)
    
    }
    
    if (settings.manualDelay != null) {
    
    def manualDelay = Math.max(Math.min(manualDelay, 255), 1)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: manualDelay, parameterNumber: 10, size: 2).format()))
    sendEvent(name: "manualDelay", value: manualDelay, displayed: false)
    
    }
    
    if (settings.minimumDim != null) {
	def minimumDim = Math.max(Math.min(minimumDim, 99), 1)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: minimumDim, parameterNumber: 20, size: 1).format()))
	sendEvent(name: "minimumDim", value: minimumDim, displayed: false)  
    }
    
         
	if (settings.requestedGroup2 != state.currentGroup2) {
        nodes = parseAssocGroupList(settings.requestedGroup2, 2)
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: [])
        cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes)
        cmds << zwave.associationV2.associationGet(groupingIdentifier: 2)
        state.currentGroup2 = settings.requestedGroup2
    }

    if (settings.requestedGroup3 != state.currentGroup3) {
        nodes = parseAssocGroupList(settings.requestedGroup3, 3)
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: [])
        cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes)
        cmds << zwave.associationV2.associationGet(groupingIdentifier: 3)
        state.currentGroup3 = settings.requestedGroup3
    }
  
    switch (ledIndicator) {
		case "on":
			indicatorWhenOn()
			break
		case "off":
			indicatorWhenOff()
			break
		case "never":
			indicatorNever()
			break
		default:
			indicatorWhenOff()
			break
	}
    
    switch (invertSwitch) {
    	case "false":
        	notInverted()
            break
        case "true":
        	inverted()
            break
        default:
        	notInverted()
            break
	}      
    
    switch (switchMode) {
    	case "false":
        	modeDimmer()
            break
        case "true":
        	modeSwitch()
            break
        default:
        	modeDimmer()
            break
	}    
    
    switch (ramp) {
		case "false":
			setNoRamp()
			break
		case "true":
			setRamp()
			break
		default:
			setNoRamp()
			break
	}
 
	sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up_2x","down_2x"]), displayed:false) 
	
	sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
   
   log.debug "---Preferences Updated--- ${device.displayName} sent ${cmds}"
   
   }

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
	[name: "indicatorStatus", value: value, displayed: false]
}

void indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
}

void indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
}

void indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
}

void inverted() {
	sendEvent(name: "inverted", value: "inverted", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()))
	}

void notInverted() {
	sendEvent(name: "inverted", value: "not inverted", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()))
}

void modeDimmer() {
	sendEvent(name: "switchMode", value: "modeDimmer", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 16, size: 1).format()))
}

void modeSwitch() {
	sendEvent(name: "switchMode", value: "modeSwitch", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 16, size: 1).format()))
}

void setNoRamp() {
	sendEvent(name: "ramp", value: "instant", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 6, size: 1).format()))
}

void setRamp() {
	sendEvent(name: "ramp", value: "ramp", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 6, size: 1).format()))
}

def doubleUp() {
	sendEvent(name: "button", value: "up_2x", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1 up_2x) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "down_2x", data: [buttonNumber: 1], descriptionText: "Double-tap down (button 1 down_2x) on $device.displayName", isStateChange: true, type: "digital")
}

def set(steps) {
	steps = Math.max(Math.min(steps, 99), 1)
	sendEvent(name: "zwaveSteps", value: steps, displayed: false)	
	zwave.configurationV2.configurationSet(scaledConfigurationValue: steps, parameterNumber: 7, size: 1).format()
}

def setZwaveDelay(delay) {
	delay = Math.max(Math.min(delay, 255), 3)
	sendEvent(name: "zwaveDelay", value: delay, displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: delay, parameterNumber: 8, size: 2).format()))
}

def setManualSteps(steps) {
	steps = Math.max(Math.min(steps, 99), 1)
	sendEvent(name: "manualSteps", value: steps, displayed: false)	
	zwave.configurationV2.configurationSet(scaledConfigurationValue: steps, parameterNumber: 9, size: 1).format()
}

def setManualDelay(delay) {
	delay = Math.max(Math.min(delay, 255), 1)
	sendEvent(name: "manualDelay", value: delay, displayed: false)
	zwave.configurationV2.configurationSet(scaledConfigurationValue: delay, parameterNumber: 10, size: 2).format()
}

def setAllSteps(steps) {
	steps = Math.max(Math.min(steps, 99), 1)
	sendEvent(name: "allSteps", value: steps, displayed: false)	
	zwave.configurationV2.configurationSet(scaledConfigurationValue: steps, parameterNumber: 11, size: 1).format()
}

def setAllDelay(delay) {
	delay = Math.max(Math.min(delay, 255), 1)
	sendEvent(name: "allDelay", value: delay, displayed: false)
	zwave.configurationV2.configurationSet(scaledConfigurationValue: delay, parameterNumber: 12, size: 2).format()
}

def poll() {
	def cmds = []
    cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,500)
}

def ping() {
	refresh()
}

def refresh() {
	def cmds = []
	cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 20).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,600)
}

def on() {
	def cmds = []
    cmds << zwave.basicV1.basicSet(value: 0xFF).format()
   	cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
    def numberofzsteps = (100 / (device.currentValue("zwaveSteps")))
    def calcdelay = (numberofzsteps * (10*(device.currentValue("zwaveDelay")))).longValue()
    def delay = Math.max(Math.min(calcdelay, 10000), 1000)
    delayBetween(cmds, delay)
}

def off() {
	def cmds = []
    cmds << zwave.basicV1.basicSet(value: 0x00).format()
   	cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
    def numberofzsteps = (100 / (device.currentValue("zwaveSteps")))
    def calcdelay = (numberofzsteps * (10*(device.currentValue("zwaveDelay")))).longValue()
    def delay = Math.max(Math.min(calcdelay, 10000), 1000)
    delayBetween(cmds, delay)
}

def setLevel(value) {
	//revised to account for minimumdim
	
    def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
    def curmindim = (device.currentValue("minimumDim"))
    
    if (curmindim == null) {
    	curmindim = 1
    }
    
    // adjust levels below minimum dim to the minimum dim level except for 0
    if ((level < curmindim) && (level != 0)) {
    	log.debug "${device.displayName} - level set command of $level below minimum dim of $curmindim, adjusting level to minimum dim"
        level = curmindim       
    }
    
    if (level > 0) {
	    sendEvent(name: "switch", value: "on")
	 } else {
		sendEvent(name: "switch", value: "off")
	 }
  
     def numberofzsteps = (100 / (device.currentValue("zwaveSteps")))
     def calcdelay = (numberofzsteps * (level / 100 ) * (10*(device.currentValue("zwaveDelay")))).longValue()
     def delay = Math.max(Math.min(calcdelay, 10000), 5000)
     
     if (device.currentValue("ramp") == "instant") {
        delay = 1000       
    }
     
     delayBetween([zwave.basicV1.basicSet(value: level).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], delay)
 
}

def setLevel(value, duration) {
	log.debug "setLevel >> value: $value, duration: $duration"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000
	delayBetween ([zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
				   zwave.switchMultilevelV1.switchMultilevelGet().format()], getStatusDelay)
}

def levelUp() {
    int nextLevel = device.currentValue("level") + 10
    if( nextLevel > 100) {
    	nextLevel = 100
    }
    setLevel(nextLevel)
}
	
def levelDown() {
    int nextLevel = device.currentValue("level") - 10
    if( nextLevel < 0) {
    	nextLevel = 0
    }
    if (nextLevel == 0) {
    	off()
    }
    else {
	    setLevel(nextLevel)
    }
}

// Private Methods

private parseAssocGroupList(list, group) {
    def nodes = group == 2 ? [] : [zwaveHubNodeId]
    if (list) {
        def nodeList = list.split(',')
        def max = group == 2 ? 5 : 4
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
                }
                else if ( (nodeId > 0) & (nodeId < 256) ) {
                    nodes << nodeId
                    count++
                }
                else {
                    log.warn "Association Group ${group}: Invalid member: ${node}"
                }
            }
            else {
                log.warn "Association Group ${group}: Invalid member: ${node}"
            }
        }
    }
    
    return nodes
}
