/*
 *  DSC 2-Wire Smoke Device Zone (UNTESTED, Probably broken)
 *
 *  Original Device Author: Matt Martz <matt.martz@gmail.com>
 *  Smoke Alarm Additions Author: Dan S <coke12oz@hotmail.com>
 *  Modified by Jordan <jordan@xeron.cc>
 *  Date: 2016-02-04
 *  Cosmetically Tweaked By: Mike Maat <mmaat@ualberta.ca> on 2016-04-08
 */

// for the UI
metadata {
	definition (name: "DSC Zone Smoke 2w", author: "jordan@xeron.cc", namespace: 'DSC') {
		// Change or define capabilities here as needed
		capability "Smoke Detector"
		capability "Sensor"
		capability "Momentary"

		// Add commands as needed
		command "zone"
		command "bypass"
	}

	tiles {
		// Main Row
		multiAttributeTile(name:"zone", type: "generic", width: 6, height: 4){
			tileAttribute ("device.smoke", key: "PRIMARY_CONTROL") {
				attributeState "clear", label:'Clear', icon:"st.alarm.smoke.clear", backgroundColor:"#ffffff"
				attributeState "smoke", label:'Smoke', icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13"
				attributeState "tested", label:'Tested', icon:"st.alarm.smoke.test", backgroundColor:"#e86d13"
			}
		}

		// This tile will be the tile that is displayed on the Hub page.
		main "zone"

		// These tiles will be displayed when clicked on the device, in the order listed here.
		details "zone"
	}
}

// handle commands
def zone(String state) {
	def text = null
	def results = []
	// state will be a valid state for a zone (open, closed)
	// zone will be a number for the zone
	log.debug "Zone: ${state}"
	sendEvent (name: "smoke", value: "${state}")
}
