/*
 *  GoControl Wireless Remote Switch
 * 
 *  Contains code from https://github.com/nuttytree/Nutty-SmartThings/blob/master/devicetypes/nuttytree/ge-jasco-zwave-plus-on-off-switch.src/ge-jasco-zwave-plus-on-off-switch.groovy
 *  Contains code from https://github.com/ajpri/ST-GoContWirelessSwitch/blob/master/devicetypes/ajpri/gocontrol-wireless-remote-switch.src/gocontrol-wireless-remote-switch.groovy
 *
 *  Copyright 2020 Chris Nussbaum, Austin Pritchett, and Tim Grimley
 *  Thanks Austin for the original copy of this code, which was used as a template to make these updates.
 *  Thanks Chris for association programming and double tap.
 *  Thanks Bradlee S for pointing out new supported button values
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
 *	Date: 09/22/2020
 *
 *	Changelog:
 *
 *  0.12 (11/18/2020) - Redid code for different actions to work with native automations section of new app. Added new preferences.
 *  0.11 (09/22/2020) - Initial Release Updated to Work with New Smartthings App, support double tap and associations
 *	
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Single-Tap Up        1        up
 *   Single-Tap Down      1        down
 *   Hold Up              1        up_hold
 *   Hold Down            1        down_hold
 *   Double-Tap Up        1        up_2x
 *   Double-Tap Down      1        down_2x  
 *   Double-Tap Up Hold   1        up_3x
 *   Double-Tap Down Hold 1        down_3x
 *
 *  Note - Central scene control, aka single tap, still triggers on any double tap due to firmware of the switch.  
 *         If you don't want single and double tap actions to both trigger at the same time, a toggle option has been added 
 *         to preferences to create a delay that will disregard the single tap on a double tap.  
 *         However, this option will always create a delay of about 2 seconds when clicking the single tap while 
 *         the handler checks for a double tap.  If not using double tap set this preference to false to eliminate delay.
 *
 */

import groovy.transform.Field
import groovy.json.JsonOutput

metadata {
	definition (name: "GoControl Wireless Remote Switch", namespace: "mwav3", author: "Tim Grimley", ocfDeviceType: "x.com.st.d.remotecontroller") {
    	capability "Actuator"
		capability "Button"
        capability "Holdable Button"
		capability "Configuration"
        capability "Battery"
        capability "Sensor"
        capability "Refresh"
        
        attribute "inverted", "enum", ["inverted", "not inverted"]
        attribute "controlled", "enum", ["both", "scene", "association"]
        
        
        command "inverted"
        command "notInverted"
        command "controlBoth"
        command "controlScene"
        command "controlAssociation"
        command "pressup"
        command "pressdown"
        command "holdup"
        command "holddown"


		fingerprint deviceId:"0x1801", inClusters:"0x5E, 0x86, 0x72, 0x5B, 0x85, 0x59, 0x73, 0x70, 0x80, 0x84, 0x5A, 0x7A", outClusters:"0x5B, 0x20"        
	}

	simulator {
		// TODO: define status and reply messages here
        status "button 1 pushed":  "command: 5B03, payload: 40 00 01"
	}

	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
		}
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
        main "button"
		details(["button", "battery"])
	}
    
    preferences {
       input (
            type: "paragraph",
            element: "paragraph",
            title: "Instructions:",
            description: "Switch must be awake prior to setting. Press any button on switch prior to setting any option on this page. Press any button again once or twice after changing for them to program to the switch."
       )
       
        input  "invertSwitch", "bool", title: "Invert Switch", description: "Invert switch? ", required: false 
      	input  "controlMode", "enum", title: "Control Mode (Default Both)", description: "Associaton and scene control... ", required: false, options:["both": "Send Both Scenes and Associations", "scene": "Send Scenes Only", "association": "Send Associations Only" ], defaultValue: "both" 
      	input "delayTime", "bool", title: "Create delay for separate double tap?", description: "Set to true to stop single tap from also triggering with double tap ", required: false
      	input "forceupdate", "bool", title: "Force Settings Update/Refresh?", description: "Toggle to force settings update", required: false
      
      input (
            type: "paragraph",
            element: "paragraph",
            title: "Configure Association Groups:",
            description: "Devices in association group 2 will receive commands directly from the controller.  Press up or down for on off, hold up or hold down for brighten or dim. Use this to control another device as if it was connected to this switch.\n\n" +
                         "Devices in association group 3 will receive commands directly from the controller when it is double tapped.  Press up or down twice for on off, press up twice and hold for brighten or down twice and hold to dim.\n\n" +
                         "Devices are entered as a comma delimited list of IDs in hexadecimal format.  These will not work with send scenes only option, and will only work on other Zwave devices."
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
}

// parse events into attributes
def parse(String description) {
	//log.debug "Parsing '${description}'"
    
    def results = []
	if (description.startsWith("Err")) {
	    results = createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description)

		if(cmd) results += zwaveEvent(cmd)
		if(!results) results = [ descriptionText: cmd, displayed: true ]
	}
	return results
     
     if (!device.currentValue("supportedButtonValues")) {
        sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["down","up","up_hold","down_hold","up_2x","down_2x","up_3x","down_3x"]), displayed:false)
    }

}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    state.lastPress = now()
    log.debug "---Double Tap--- ${device.displayName} sent ${cmd}"
	if (cmd.value == 255) {
    	createEvent(name: "button", value: "up_2x", data: [buttonNumber: 1], descriptionText: "Double-tap up on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "button", value: "down_2x", data: [buttonNumber: 1], descriptionText: "Double-tap down on $device.displayName", isStateChange: true, type: "physical")
    }
 }

 def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd) {
	state.lastPress = now()
    log.debug "---Double Tap and Hold--- ${device.displayName} sent ${cmd}"
	if (cmd.startLevel == 0) {
    	createEvent(name: "button", value: "up_3x", data: [buttonNumber: 1], descriptionText: "Double-tap up and hold up on $device.displayName", isStateChange: true, type: "physical")     }
	else if (cmd.startLevel == 255) {
    	createEvent(name: "button", value: "down_3x", data: [buttonNumber: 1], descriptionText: "Double-tap down and hold down on $device.displayName", isStateChange: true, type: "physical")
    }
 }


def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    log.debug "---Central Scene Command--- ${device.displayName} sent ${cmd}"
      	
    if(cmd.keyAttributes == 0){
    	
         if(cmd.sceneNumber == 1) {
          
          	if(delayTime == true) {
          		runIn(2, pressup) }
		 	else  {
            createEvent(name: "button", value: "up", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was pushed", isStateChange: true) }
        }
         else if(cmd.sceneNumber == 2) {
         	if(delayTime == true) {
            	runIn(2, pressdown) }
            else  {
          createEvent(name: "button", value: "down", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was pushed", isStateChange: true) } 
        } 
        
    } else if(cmd.keyAttributes == 1){    
		createEvent(name: "button", value: "holdRelease", data: [buttonNumber: 1], descriptionText: "$device.displayName button was released", isStateChange: true)
    }
    
     else if(cmd.keyAttributes == 2){
		 if(cmd.sceneNumber == 1) {
          
          	if(delayTime == true) {
          		runIn(2, holdup) }
		 	else  {
            createEvent(name: "button", value: "up_hold", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was held", isStateChange: true) }
        }
         else if(cmd.sceneNumber == 2) {
         	if(delayTime == true) {
            	runIn(2, holddown) }
            else  {
          createEvent(name: "button", value: "down_hold", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was held", isStateChange: true) } 
        } 
	}
}

void pressup() {
	if (state.lastPress && now() <= state.lastPress + 5000) {
    log.debug "$device.displayName - Double tap detected disregard single tap"
    return }
    state.lastPress = now()
	sendEvent(name: "button", value: "up", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was pushed", isStateChange: true) 
}

void pressdown() {
	if (state.lastPress && now() <= state.lastPress + 5000) {
    log.debug "$device.displayName - Double tap detected disregard single tap"
    return }
    state.lastPress = now()
	sendEvent(name: "button", value: "down", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was pushed", isStateChange: true)
}

void holdup() {
	if (state.lastPress && now() <= state.lastPress + 5000) {
    log.debug "$device.displayName - Double tap hold detected disregard single hold"
    return }
    state.lastPress = now()
	sendEvent(name: "button", value: "up_hold", data: [buttonNumber: 1], descriptionText: "$device.displayName button up was held", isStateChange: true) 
}

void holddown() {
	if (state.lastPress && now() <= state.lastPress + 5000) {
    log.debug "$device.displayName - Double tap hold detected disregard single hold"
    return }
    state.lastPress = now()
	sendEvent(name: "button", value: "down_hold", data: [buttonNumber: 1], descriptionText: "$device.displayName button down was held", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]

	// Only ask for battery if we haven't had a BatteryReport in a while
	if (!state.lastbatt || (new Date().time) - state.lastbatt > 24*60*60*1000) {
		result << response(zwave.batteryV1.batteryGet())
		result << response("delay 1200")  // leave time for device to respond to batteryGet
	}
    
	result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
    result
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	
        }
        else {
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        	        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {    
    def result = []
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} battery is low"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbatt = now()
	result << createEvent(map)

	result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

def configure() {
    def cmds = []
    // Get current config parameter values
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 2).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    
    
    // Add the hub to association group 3 to get double-tap notifications
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
    
    delayBetween(cmds,500)
    
    log.debug "-${device.displayName} --Configured--- sent following cmds ${cmds} to device"
}

def installed() {
	initialize()
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
    
    switch (controlMode) {
		case "both":
			controlBoth()
			break
		case "association":
			controlAssociation()
			break
		case "scene":
			controlScene()
			break
		default:
			controlBoth()
			break
	}
    
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["down","up","up_hold","down_hold","up_2x","down_2x","up_3x","down_3x"]), displayed:false)
    
    sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
    
    configure()
    
    log.debug "---Preferences Updated--- ${device.displayName} sent ${cmds}"
    
}

void controlBoth() {
	sendEvent(name: "controlled", value: "both", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 2, size: 1).format()))
}

void controlScene() {
	sendEvent(name: "controlled", value: "scene", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 2, size: 1).format()))
}

void controlAssociation() {
	sendEvent(name: "controlled", value: "association", displayed: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 2, size: 1).format()))
}

void inverted() {
	sendEvent(name: "inverted", value: "inverted", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()))
	}

void notInverted() {
	sendEvent(name: "inverted", value: "not inverted", display: false)
    sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()))
}

def initialize() {
	sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "button", value: "up", data: [buttonNumber: 1], displayed: false)
    sendEvent(name: "supportedButtonValues", value:JsonOutput.toJson(["down","up","up_hold","down_hold","up_2x","down_2x","up_3x","down_3x"]), displayed:false) 
    configure()
    
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
