/**
 *  GE/Jasco Z-Wave Plus On/Off Switch 46201 or 46562
 * 
 *  Contains code from https://github.com/nuttytree/Nutty-SmartThings/blob/master/devicetypes/nuttytree/ge-jasco-zwave-plus-on-off-switch.src/ge-jasco-zwave-plus-on-off-switch.groovy
 *
 *  Copyright 2020 Chris Nussbaum, Tim Grimley
 *  Contributors - Bradlee_S
 *  Thanks Chris for the original copy of this great code!
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
 *	Date: 12/03/2020
 *
 *	Changelog:
 *
 *  0.11 (08/31/2020) - Initial Release 
 *  
 *
 *   Button Mappings  NOTE - THIS IS A BREAKING CHANGE from any other DTH and uses a single button.  
 *                    ALL prior automations will need to be re-programmed or updated when updating this DTH from other versions:
 *
 *   ACTION             BUTTON#    BUTTON ACTION
 *   Single-Tap Up        1        up
 *   Single-Tap Down      1        down  
 *   Double-Tap Up        1        up_2x
 *   Double-Tap Down      1        down_2x  
 *   Triple-Tap Up        1        up_3x
 *   Triple-Tap Down      1        down_3x
 *   Hold Up              1        up_hold
 *   Hold Down            1        down_hold
 *   Release Hold	      1        holdrelease
 * 
 *   If options do not change, go to preferences and toggle "force settings update/refresh"
 */

import groovy.transform.Field
import groovy.json.JsonOutput

metadata {
	definition (name: "GE 46201 On Off Switch", namespace: "mwav3", author: "Tim Grimley") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Indicator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"

        command "inverted"
        command "notInverted"
        
        // These include version because there are older firmwares that don't support double-tap or the extra association groups
		fingerprint mfr:"0063", prod:"4952", model: "3137", ver: "5.53", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3135", ver: "5.53", deviceJoinName: "Jasco Z-Wave Plus Wall Switch"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
	}
    
    preferences {
        
        input "ledIndicator", "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When Switch On", "off": "When Switch Off", "never": "Never On", "always": "Always On"], defaultValue: "off"
        input "altexclusion", "bool", title: "Prevent Accidental Exclusion", description: "Prevent accidental exclusion? ", required: false
        input "forceupdate", "bool", title: "Force Settings Update/Refresh?", description: "Toggle to force settings update", required: false
        
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
				attributeState "on", label: '${name}', action: "switch.off", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png", backgroundColor: "#00a0dc", nextState:"turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png", backgroundColor: "#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:"Turning On", action:"switch.off", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:"Turning Off", action:"switch.on", icon:"https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png", backgroundColor:"#ffffff", nextState:"turningOn"
			}
		}
        
		standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
			state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
			state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
		}
        
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch"])
        details(["switch", "indicator", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
    log.debug "description: $description"
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    
    if (!device.currentValue("supportedButtonValues")) {
        sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up","down","up_hold","down_hold","up_2x","down_2x","up_3x","down_3x"]), displayed:false)
    }
    
    result    
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	log.debug("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")
	def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		log.debug("zwaveEvent(): Could not extract command from ${cmd}")
	} else {
		return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    log.debug "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
	createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    
    log.debug "---Central Scene Command--- ${device.displayName} sent ${cmd}"
    def upordown = []
    
    // scene number is 1 for up 2 for down
    upordown = (cmd.sceneNumber) as Integer

  // single taps
     if(cmd.keyAttributes == 0){
    	if(upordown == 1) {
        
    	createEvent(name: "button", value: "up", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was single tapped", isStateChange: true)
    	}
    	else if(upordown == 2) {
        
        createEvent(name: "button", value: "down", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was single tapped", isStateChange: true)
    	}     
      }   
      
  // button release
     else if(cmd.keyAttributes == 1){
    	createEvent(name: "button", value: "holdRelease", data: [buttonNumber: 1], descriptionText: "$device.displayName button was released", isStateChange: true)    
      }  

  // single tap hold
    else if(cmd.keyAttributes == 2){
  		if(upordown == 1) {
        
    	createEvent(name: "button", value: "up_hold", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was held", isStateChange: true)
    	}
        
    	else if(upordown == 2) {
        
        createEvent(name: "button", value: "down_hold", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was held", isStateChange: true)
    	} 
      }
    

    // double taps
    else if(cmd.keyAttributes == 3){
    
    	if(upordown == 1) {
        
    	createEvent(name: "button", value: "up_2x", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was double tapped", isStateChange: true)
    	}
    	else if(upordown == 2) {
        
        createEvent(name: "button", value: "down_2x", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was double tapped", isStateChange: true)
    	}
    }
    
    // triple taps 
    else if(cmd.keyAttributes == 4){
    	if(upordown == 1) {
        
    	createEvent(name: "button", value: "up_3x", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was triple tapped", isStateChange: true)
    	}
    	else if(upordown == 2) {
        
        createEvent(name: "button", value: "down_3x", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was triple tapped", isStateChange: true)
    	}  
     }
   
    else {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
    }
 
}


def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
	def name = ""
    def value = ""
    def reportValue = cmd.configurationValue[0]
    switch (cmd.parameterNumber) {
        case 3:
            name = "indicatorStatus"
            value = reportValue == 1 ? "when on"  : reportValue == 2 ? "never" : reportValue == 3 ? "always" : "when off"
            break
        case 19:
            name = "altexclusion"
            value = reportValue == 1 ? "true" : "false"
            break
        default:
            break
    }
	createEvent([name: name, value: value, displayed: false])
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
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
    createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
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
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 19).format()
       
    delayBetween(cmds,500)
}

def updated() {
    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def nodes = []
    def cmds = []
    
          
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
    
  
	sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up","down","up_hold","down_hold","up_2x","down_2x","up_3x","down_3x"]), displayed:false)
  
	
	sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
   
   log.debug "---Preferences Updated--- ${device.displayName} sent ${cmds}"
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

void allowAccexclusion() {
 	sendEvent(name: "allowaltexclusion", value: "normalexc", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 19, size: 1).format()))
 }

 void preventAccexclusion() {
	sendEvent(name: "allowaltexclusion", value: "extrapress", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 19, size: 1).format()))
 }

def poll() {
	def cmds = []
    cmds << zwave.switchBinaryV1.switchBinaryGet().format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,500)
}

def refresh() {
	def cmds = []
	cmds << zwave.switchBinaryV1.switchBinaryGet().format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,500)
     
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

def initialize() {
	sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["up","down","up_hold","down_hold","up_2x","down_2x","up_3x","down_3x"]), displayed:false)
  
      
}


// Private Methods

private parseAssocGroupList(list, group) {
   def nodes = []
    if (list) {
        def nodeList = list.split(',')
        def max = 5
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would conflict with scene control)."
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
