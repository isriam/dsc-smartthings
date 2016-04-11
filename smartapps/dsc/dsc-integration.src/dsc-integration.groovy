/*
 *  DSC Alarm Panel integration via REST API callbacks
 *
 *  Author: Kent Holloway <drizit@gmail.com>
 *  Modified by: Matt Martz <matt.martz@gmail.com>
 *  Modified by: Jordan <jordan@xeron.cc>
 *  Modified by: Mike Maat <mmaat@ualberta.ca> on 2016-04-08
 *		Changes:	Added Flood Sensor Type (define a flood sensor (DSC WS4985) in alarmserver.cfg as 'flood'
 *					Fixed line identation
 *					Customized alarm push messages to be more understandable for end users
 *					Added a setting to show/not show partition number in notification messages
 *					Fixed "notification" spelling error on line 46
 */

definition(
	name: 'DSC Integration',
	namespace: 'DSC',
	author: 'Jordan <jordan@xeron.cc>',
	description: 'DSC Integration App',
	category: 'My Apps',
	iconUrl: 'https://dl.dropboxusercontent.com/u/2760581/dscpanel_small.png',
	iconX2Url: 'https://dl.dropboxusercontent.com/u/2760581/dscpanel_large.png',
	oauth: true,
	singleInstance: true
)

import groovy.json.JsonBuilder

preferences {
	section('Alarmserver Setup:') {
		input('ip', 'text', title: 'IP', description: 'The IP of your alarmserver (required)', required: false)
		input('port', 'text', title: 'Port', description: 'The port (required)', required: false)
		input 'shmSync', 'enum', title: 'Smart Home Monitor Sync', required: false,
  			metadata: [
				values: ['Yes','No']
			]
		input 'shmBypass', 'enum', title: 'SHM Stay/Away Bypass', required: false,
			metadata: [
				values: ['Yes','No']
			]
  	}
  	section('XBMC Notifications (optional):') {
		// TODO: put inputs here
		input 'xbmcserver', 'text', title: 'XBMC IP', description: 'IP Address', required: false
		input 'xbmcport', 'number', title: 'XBMC Port', description: 'Port', required: false
  	}
  	section('Notifications (optional)') {
		input 'sendPush', 'enum', title: 'Push Notification', required: false,
			metadata: [
				values: ['Yes','No']
			]
		input 'phone1', 'phone', title: 'Phone Number', required: false
  	}
  	section('Notification events (optional):') {
		input 'notifyEvents', 'enum', title: 'Which Events', description: 'Events to notify on', required: false, multiple: true,
			options: [
				'all', 'partition alarm', 'partition armed', 'partition away', 'partition disarm', 'partition duress',
				'partition entrydelay', 'partition exitdelay', 'partition forceready', 'partition instantaway',
				'partition instantstay', 'partition notready', 'partition ready', 'partition restore', 'partition stay',
				'partition trouble', 'partition keyfirealarm', 'partition keyfirerestore', 'partition keyauxalarm',
				'partition keyauxrestore', 'partition keypanicalarm', 'partition keypanicrestore', 'led backlight on',
				'led backlight off', 'led fire on', 'led fire off', 'led program on', 'led program off', 'led trouble on',
				'led trouble off', 'led bypass on', 'led bypass off', 'led memory on', 'led memory off', 'led armed on',
				'led armed off', 'led ready on', 'led ready off', 'zone alarm', 'zone clear', 'zone closed', 'zone fault',
				'zone open', 'zone restore', 'zone smoke', 'zone tamper'
			]
		input 'includePartition', 'enum', title: 'Include Partition # in Notification', required: false,
			metadata: [
				values: ['Yes','No']
			]
  	}
}

mappings {
	path('/update')            { action: [POST: 'update'] }
  	path('/installzones')      { action: [POST: 'installzones'] }
  	path('/installpartitions') { action: [POST: 'installpartitions'] }
}

def initialize() {
	
    	if (settings.shmSync == 'Yes') {
    		subscribe(location, 'alarmSystemStatus', shmHandler)
	}
}

def shmHandler(evt) {
  
	if (settings.shmSync == 'Yes') {

		log.debug "shmHandler: shm changed state to: ${evt.value}"
		def children = getChildDevices()
		def child = children.find { item -> item.device.deviceNetworkId in ['dscstay1', 'dscaway1'] }
		if (child != null) {
			log.debug "shmHandler: using panel: ${child.device.deviceNetworkId} state: ${child.currentStatus}"
			//map DSC states to simplified values for comparison
			def dscMap = [
				'alarm': 'on',
				'away':'away',
				'entrydelay': 'on',
				'exitdelay': 'on',
				'forceready':'off',
				'instantaway':'away',
				'instantstay':'stay',
				'ready':'off',
				'stay':'stay',
			]

			if (dscMap[child.currentStatus] && evt.value != dscMap[child.currentStatus]) {
				if (evt.value == 'off' && dscMap[child.currentStatus] in ['stay', 'away', 'on'] ) {
					sendUrl('disarm')
					log.debug "shmHandler: ${evt.value} is valid action for ${child.currentStatus}, disarm sent"
				} 
				else if (evt.value == 'away' && dscMap[child.currentStatus] in ['stay', 'off'] ) {
					sendUrl('arm')
					log.debug "shmHandler: ${evt.value} is valid action for ${child.currentStatus}, arm sent"
				} 
				else if (evt.value == 'stay' && dscMap[child.currentStatus] in ['away', 'off'] ) {
					sendUrl('stayarm')
					log.debug "shmHandler: ${evt.value} is valid action for ${child.currentStatus}, stayarm sent"
				}
			}
		}
  	}
}

def installzones() {
	
	def children = getChildDevices()
	def zones = request.JSON

	def zoneMap = [
		'contact':'DSC Zone Contact',
		'motion':'DSC Zone Motion',
		'smoke':'DSC Zone Smoke',
		'co':'DSC Zone CO',
		'flood':'DSC Zone Flood',
	]

	log.debug "children are ${children}"
	for (zone in zones) {
		def id = zone.key
		def type = zone.value.'type'
		def device = zoneMap."${type}"
		def name = zone.value.'name'
		def networkId = "dsczone${id}"
		def zoneDevice = children.find { item -> item.device.deviceNetworkId == networkId }

		if (zoneDevice == null) {
			log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
			zoneDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
		} 
		else {
			log.debug "zone device was ${zoneDevice}"
			try {
				log.debug "trying name update for ${networkId}"
				zoneDevice.name = "${name}"
				log.debug "trying label update for ${networkId}"
				zoneDevice.label = "${name}"
			} 
			catch(IllegalArgumentException e) {
				log.debug "excepted for ${networkId}"
				if ("${e}".contains('identifier required')) {
					log.debug "Attempted update but device didn't exist. Creating ${networkId}"
					zoneDevice = addChildDevice("dsc", "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
				} 
				else {
					log.error "${e}"
				}
			}
		}
  	}

	for (child in children) {
		if (child.device.deviceNetworkId.contains('dsczone')) {
			def zone = child.device.deviceNetworkId.minus('dsczone')
			def jsonZone = zones.find { x -> "${x.key}" == "${zone}"}
      		
			if (jsonZone == null) {
				try {
					log.debug "Deleting device ${child.device.deviceNetworkId} ${child.device.name} as it was not in the config"
					deleteChildDevice(child.device.deviceNetworkId)
				} 
				catch(MissingMethodException e) {
					if ("${e}".contains('types: (null) values: [null]')) {
						log.debug "Device ${child.device.deviceNetworkId} was empty, likely deleted already."
					} 
					else {
						log.error e
					}
				}
			}
		}
	}
}

def installpartitions() {
 	
	def children = getChildDevices()
	def partitions = request.JSON

	def partMap = [
		'stay':'DSC Stay Panel',
		'away':'DSC Away Panel',
	]

	log.debug "children are ${children}"
	for (part in partitions) {
		def id = part.key

		for (p in part.value) {
			def type = p.key
			def name = p.value
			def networkId = "dsc${type}${id}"
			def partDevice = children.find { item -> item.device.deviceNetworkId == networkId }
			def device = partMap."${type}"

			if (partDevice == null) {
				log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
				partDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
			} 
			else {
				log.debug "part device was ${partDevice}"
				try {
					log.debug "trying name update for ${networkId}"
					partDevice.name = "${name}"
					log.debug "trying label update for ${networkId}"
					partDevice.label = "${name}"
				} 
				catch(IllegalArgumentException e) {
					log.debug "excepted for ${networkId}"
					if ("${e}".contains('identifier required')) {
						log.debug "Attempted update but device didn't exist. Creating ${networkId}"
						partDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
					} 
					else {
             					log.error "${e}"
           				}
				}
			}
		}
	}

	for (child in children) {
		for (p in ['stay', 'away']) {
			if (child.device.deviceNetworkId.contains("dsc${p}")) {
				def part = child.device.deviceNetworkId.minus("dsc${p}")
				def jsonPart = partitions.find { x -> x.value."${p}" }
				if (jsonPart== null) {
					try {
						log.debug "Deleting device ${child.device.deviceNetworkId} ${child.device.name} as it was not in the config"
						deleteChildDevice(child.device.deviceNetworkId)
					} 
					catch(MissingMethodException e) {
						if ("${e}".contains('types: (null) values: [null]')) {
							log.debug "Device ${child.device.deviceNetworkId} was empty, likely deleted already."
						} 
						else {
							log.error e
						}
					}
				}
			}
		}
	}
}

def sendUrl(url) {
	
	log.debug "sendUrl(" + url + ")"
	def result = new physicalgraph.device.HubAction(
		method: 'GET',
		path: "/api/alarm/${url}",
		headers: [
			HOST: "${settings.ip}:${settings.port}"
		]
	)
	sendHubCommand(result)
	log.debug 'response' : "Request to send url: ${url} received"
    
	return result
}


def installed() {
	log.debug 'Installed!'
}

def testFunction() {
	log.debug "Test Function"
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
	log.debug 'Updated!'
}

private update() {

	def update = request.JSON

	//If the update contains parameters (LED on Panel Update)
	if (update.'parameters') {
		for (p in update.'parameters') {
			if (notifyEvents && (notifyEvents.contains('all') || notifyEvents.contains("led ${p.key} ${p.value}".toString()))) {
				def messageBody = "Keypad LED ${p.key} in ${p.value} state"
				
				if (includePartition == 'Yes')
					messageBody += " (Partition: ${update.'value'})"
                    
				sendMessage(messageBody)
			}
		}
	} 
  
	//If the update does not contain parameters
	else {
		if (notifyEvents && (notifyEvents.contains('all') || notifyEvents.contains("${update.'type'} ${update.'status'}".toString()))) {
      		
			if ("${update.'type'}" == 'partition') {
            
				def messageBody = "Alarm in ${update.'status'} state"
                
				if ("${update.'status'}" == 'alarm')
					messageBody = "Alarm is active!"
				else if ("${update.'status'}" == 'armed')
					messageBody = "Alarm is armed"
				else if ("${update.'status'}" == 'away')
					messageBody = "Alarm is armed/away"
				else if ("${update.'status'}" == 'disarm')
					messageBody = "Alarm is disarmed"
				else if ("${update.'status'}" == 'duress')
					messageBody = "Alarm is in a state of duress"
				else if ("${update.'status'}" == 'entrydelay')
					messageBody = "Entry delay in progress"
				else if ("${update.'status'}" == 'exitdelay')
					messageBody = "Exit delay in progress" 
				else if ("${update.'status'}" == 'forceready')
					messageBody = "Alarm has been forced ready"
				else if ("${update.'status'}" == 'instantaway')
					messageBody = "Alarm has been set to instant away"
				else if ("${update.'status'}" == 'instantstay')
					messageBody = "Alarm has been set to instant stay"
				else if ("${update.'status'}" == 'notready')
					messageBody = "Alarm is not ready"
				else if ("${update.'status'}" == 'ready')
					messageBody = "Alarm is ready"
				else if ("${update.'status'}" == 'restore')
					messageBody = "Alarm has been restored"
				else if ("${update.'status'}" == 'stay')
					messageBody = "Alarm is armed/stay"
				else if ("${update.'status'}" == 'trouble')
					messageBody = "Alarm has a trouble status"
				else if ("${update.'status'}" == 'keyfirealarm')
					messageBody = "Fire alarm key pressed on alarm"
				else if ("${update.'status'}" == 'keyfirerestore')
					messageBody = "Fire alarm key restored on alarm"
				else if ("${update.'status'}" == 'keyauxalarm')
					messageBody = "Auxiliary alarm key pressed on alarm"
				else if ("${update.'status'}" == 'keyauxrestore')
					messageBody = "Auxiliary alarm key restored on alarm"
				else if ("${update.'status'}" == 'keypanicalarm')
					messageBody = "Panic key pressed on alarm"
				else if ("${update.'status'}" == 'keypanicrestore')
					messageBody = "Panic key restored on alarm"
                
				if (includePartition == 'Yes')
					messageBody += " (Partition: ${update.'value'})"
                    
				sendMessage(messageBody)
			}
            
			else if ("${update.'type'}" == 'zone') {
            
				if ("${update.'status'}" == 'clear')
					sendMessage("Zone ${update.'value'} is clear")
				else if ("${update.'status'}" == 'closed')
					sendMessage("Zone ${update.'value'} is closed")
				else if ("${update.'status'}" == 'fault')
					sendMessage("Zone ${update.'value'} is in fault")
				else if ("${update.'status'}" == 'open')
					sendMessage("Zone ${update.'value'} is open")
				else if ("${update.'status'}" == 'restore')
					sendMessage("Zone ${update.'value'} has been restored")
				else if ("${update.'status'}" == 'smoke')
					sendMessage("Smoke detected in zone ${update.'value'}")
				else if ("${update.'status'}" == 'tamper')
					sendMessage("Zone ${update.'value'} has been tampered with")
				else
					sendMessage("Zone ${update.'value'} in ${update.'status'} state")
			}
		}
	}
  
	if ("${update.'type'}" == 'zone') {
		updateZoneDevices(update.'value', update.'status')
	}
  
	else if ("${update.'type'}" == 'partition') {
		if (settings.shmSync == 'Yes') {
			// Map DSC states to SHM modes, only using absolute states for now, no exit/entry delay
			def shmMap = [
				'away':'away',
				'forceready':'off',
				'instantaway':'away',
				'instantstay':'stay',
				'notready':'off',
				'ready':'off',
				'stay':'stay',
			]

			if (shmMap[update.'status']) {
				if (settings.shmBypass != 'Yes' || !(location.currentState("alarmSystemStatus")?.value in ['away','stay'] && shmMap[update.'status'] in ['away','stay'])) {
					log.debug "sending smart home monitor: ${shmMap[update.'status']} for status: ${update.'status'}"
					sendLocationEvent(name: "alarmSystemStatus", value: shmMap[update.'status'])
				}
			}
		}
		updatePartitions(update.'value', update.'status', update.'parameters')
	}
}

private updateZoneDevices(zonenum,zonestatus) {
	
	def children = getChildDevices()
	log.debug "zone: ${zonenum} is ${zonestatus}"
	// log.debug "zonedevices.id are $zonedevices.id"
	// log.debug "zonedevices.displayName are $zonedevices.displayName"
	// log.debug "zonedevices.deviceNetworkId are $zonedevices.deviceNetworkId"
	def zonedevice = children.find { item -> item.device.deviceNetworkId == "dsczone${zonenum}"}
	//def zonedevice = zonedevices.find { it.deviceNetworkId == "dsczone${zonenum}" }
  	
	if (zonedevice) {
		log.debug "Was True... Zone Device: $zonedevice.displayName at $zonedevice.deviceNetworkId is ${zonestatus}"
		//Was True... Zone Device: Front Door Sensor at zone1 is closed
		zonedevice.zone("${zonestatus}")
    
		if ("${settings.xbmcserver}" != "") {  //Note: I haven't tested this if statement, but it looks like it would work.
			def lanaddress = "${settings.xbmcserver}:${settings.xbmcport}"
			def deviceNetworkId = "1234"
			def json = new JsonBuilder()
			def messagetitle = "$zonedevice.displayName".replaceAll(' ','%20')
			log.debug "$messagetitle"
			json.call("jsonrpc":"2.0","method":"GUI.ShowNotification","params":[title: "$messagetitle",message: "${zonestatus}"],"id":1)
			def xbmcmessage = "/jsonrpc?request="+json.toString()
			def result = new physicalgraph.device.HubAction("""GET $xbmcmessage HTTP/1.1\r\nHOST: $lanaddress\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
			sendHubCommand(result)
		}
	}
}

private updatePartitions(partitionnum, partitionstatus, partitionparams) {
	
	def children = getChildDevices()
	log.debug "partition: ${partitionnum} is ${partitionstatus}"
  	
	def awaypanel = children.find { item -> item.device.deviceNetworkId == "dscaway${partitionnum}"}
	if (awaypanel) {
		log.debug "Was True... Away Switch device: $awaypanel.displayName at $awaypanel.deviceNetworkId is ${partitionstatus}"
		//Was True... Zone Device: Front Door Sensor at zone1 is closed
		awaypanel.partition("${partitionstatus}", "${partitionnum}", partitionparams)
	}
  	
	def staypanel = children.find { item -> item.device.deviceNetworkId == "dscstay${partitionnum}"}
	if (staypanel) {
		log.debug "Was True... Stay Switch device: $staypanel.displayName at $staypanel.deviceNetworkId is ${partitionstatus}"
		//Was True... Zone Device: Front Door Sensor at zone1 is closed
		staypanel.partition("${partitionstatus}", "${partitionnum}", partitionparams)
	}
}

private sendMessage(msg) {
	
	def newMsg = "Alarm Notification: $msg"
  	
	if (phone1) {
		sendSms(phone1, newMsg)
	}
  	
	if (sendPush == 'Yes') {
		sendPush(newMsg)
	}
}
