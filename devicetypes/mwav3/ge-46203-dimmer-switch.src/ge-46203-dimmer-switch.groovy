/**
 *  GE/Jasco 46203 Z-Wave Plus Dimmer Switch
 *
 *  Contains code from https://github.com/nuttytree/Nutty-SmartThings/blob/master/devicetypes/nuttytree/ge-jasco-zwave-plus-dimmer-switch.src/ge-jasco-zwave-plus-dimmer-switch.groovy
 *
 *  Copyright 2020 Chris Nussbaum, Tim Grimley
 *  Thanks Chris for the original copy of this great code!
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
 *	Date: 9/15/2020
 *
 *	Changelog:
 *
 *  0.10 (09/15/2020) -	Initial 0.1 Beta. NOTE - This is a beta release, michicago confirmed functional though (Thanks!)
 *  
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pressed
 *   Double-Tap Down   2        pressed
 *
 */

import groovy.transform.Field
import groovy.json.JsonOutput

metadata {
	definition (name: "GE 46203 Dimmer Switch", namespace: "mwav3", author: "Tim Grimley") {
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

		attribute "onoffordimmer", "enum", ["onoff", "notonoff"]
        attribute "allowaltexclusion", "enum", ["normalexc", "extrapress"]
        attribute "rampfastslow", "enum", ["slowramp", "fastramp"]
        attribute "mindim", "number"
        attribute "maxbright", "number"
        attribute "defaultbright", "number"
       
        
        command "doubleUp"
        command "doubleDown"
        command "onoff"
        command "notonoff"
        command "levelUp"
        command "levelDown"
        command "normalexc"
        command "extrapress"
        command "slowramp"
        command "fastramp"
        command "setmaxbright"
        command "setmindim"
      
        
        // These include version because there are older firmwares that don't support double-tap or the extra association groups
        fingerprint mfr:"0063", prod:"4944", model:"3235", ver: "5.54", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
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
        input "ledIndicator", "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When Switch On", "off": "When Switch Off", "never": "Never On", "always": "Always On"], defaultValue: "off"
        input "onoffswitch", "bool", title: "Make Dimmer On/Off Only", description: "On/Off Only? ", required: false
        input "altexclusion", "bool", title: "Prevent Accidental Exclusion", description: "Prevent accidental exclusion? ", required: false
        input "ramplevel", "bool", title: "Ramp Level When Setting", description: "Dim up/down slowly by command? ", required: false
    	input "mindim", "number", title: "Minimum Dimmer Threshold (1-99) Default 1", description: "Minimum Dimmer ", required: false, range: "1..99"
		input "maxbright", "number", title: "Maximum Brightness Threshold (1-99) Default 99", description: "Maximum Brightness ", required: false, range: "1..99"
        input "defaultbright", "number", title: "Default Brightness at Switch- 0 for previous", description: "Default Level ", required: false, range: "0..99"
		

       
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
        sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["pushed"]), displayed:false)
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
    	createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    state.group3 = "1,2"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	createEvent(name: "numberOfButtons", value: 2, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    // not sure if this section will work right or needs revision
    
    log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
	def name = ""
    def value = ""
    def reportValue = cmd.scaledConfigurationValue
    switch (cmd.parameterNumber) {
        case 3:
            name = "indicatorStatus"
            value = reportValue == 1 ? "when on"  : reportValue == 2 ? "never" : reportValue == 3 ? "always" : "when off"
            break
        case 16:
            name = "onoffswitch"
            value = reportValue == 1 ? "true" : "false"
            break
        case 19:
            name = "altexclusion"
            value = reportValue == 1 ? "true" : "false"
            break
        case 6:
            name = "ramplevel"
            value = reportValue == 1 ? "true" : "false"
            break
        case 30:
            name = "mindim"
            value = reportValue
            break
        case 31:
            name = "maxbright"
            value = reportValue
            break
        case 32:
            name = "defaultbright"
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
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 6).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 19).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 30).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 31).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 32).format()
   
    
    // Add the hub to association group 3 to get double-tap notifications
    // not a good way to do double tap on this switch but leaving for now
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
    
    delayBetween(cmds,500)
}

def updated() {

    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def nodes = []
    def cmds = []
    
    if (settings.mindim != null) {
    
    def mindim = Math.max(Math.min(mindim, 99), 1)
   	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: mindim, parameterNumber: 30, size: 1).format()))
    sendEvent(name: "mindim", value: mindim, displayed: false)
  	    
    }
    
      
    if (settings.maxbright != null) {
    
    def maxbright = Math.max(Math.min(maxbright, 99), 1)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: maxbright, parameterNumber: 31, size: 1).format()))
    sendEvent(name: "maxbright", value: maxbright, displayed: false)
    
    }
    
    if (settings.defaultbright != null) {
    
    def defaultbright = Math.max(Math.min(defaultbright, 99), 0)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: defaultbright, parameterNumber: 32, size: 1).format()))
    sendEvent(name: "defaultbright", value: defaultbright, displayed: false)
    
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
        case "always":
			indicatorAlways()
			break
		default:
			indicatorWhenOff()
			break
	}
    
    switch (onoffswitch) {
    	case "false":
        	notonoffswitch()
            break
        case "true":
        	onoffswitch()
            break
        default:
        	notonoffswitch()
	    break
	}      
 
 switch (altexclusion) {
    	case "false":
        	allowAccexclusion()
            break
        case "true":
        	preventAccexclusion()
            break
        default:
        	allowAccexclusion()
	    break
	}   
    
    switch (ramplevel) {
    	case "false":
        	fastramp()
            break
        case "true":
        	slowramp()
            break
        default:
        	fastramp()
	    break
	}  
 
	sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
   
   
   }

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
    if (cmd.configurationValue[0] == 3) {value = "always"}
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

void indicatorAlways() {
	sendEvent(name: "indicatorStatus", value: "always", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [3], parameterNumber: 3, size: 1).format()))
}

 void onoffswitch() {
	sendEvent(name: "onoffordimmer", value: "onoff", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 16, size: 1).format()))
	}

 void notonoffswitch() {
	sendEvent(name: "onoffordimmer", value: "notonoff", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 16, size: 1).format()))
 }

 void allowAccexclusion() {
 	sendEvent(name: "allowaltexclusion", value: "normalexc", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 19, size: 1).format()))
 }

 void preventAccexclusion() {
	sendEvent(name: "allowaltexclusion", value: "extrapress", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 19, size: 1).format()))
 }

void fastramp() {
 	sendEvent(name: "rampfastslow", value: "fastramp", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 6, size: 1).format()))
 }
 
 void slowramp() {
 	sendEvent(name: "rampfastslow", value: "slowramp", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 6, size: 1).format()))
 }

def doubleUp() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "digital")
}

def setmindim(mindim) {
	mindim = Math.max(Math.min(mindim, 99), 1)
	sendEvent(name: "mindim", value: dim, displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: mindim, parameterNumber: 30, size: 1).format()))
}

def setmaxbright(maxbright) {
	maxbright = Math.max(Math.min(maxbright, 99), 1)
    sendEvent(name: "maxbright", value: maxbright, displayed: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: maxbright, parameterNumber: 31, size: 1).format()))
}

def setdefaultbright(defaultbright) {
    defaultbright = Math.max(Math.min(defaultbright, 99), 0)
    sendEvent(name: "defaultbright", value: defaultbright, displayed: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: defaultbright, parameterNumber: 32, size: 1).format()))
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
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 6).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 19).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 30).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 31).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 32).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,600)
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	], 5000)
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	], 5000)
}

def setLevel(value) {
	log.debug "setLevel >> value: $value"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	if (level > 0) {
		sendEvent(name: "switch", value: "on")
	} else {
		sendEvent(name: "switch", value: "off")
	}
	delayBetween([zwave.basicV1.basicSet(value: level).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def setLevel(value, duration) {
	log.debug "setLevel >> value: $value, duration: $duration"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	def getStatusDelay = duration < 128 ? (duration * 1000) + 2000 : (Math.round(duration / 60) * 60 * 1000) + 2000
	delayBetween([zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
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
