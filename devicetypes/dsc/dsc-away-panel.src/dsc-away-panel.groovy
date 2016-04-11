/**
 *  DSC Away Panel
 *
 *  Author: Jordan <jordan@xeron.cc
 *  Original Code By: Rob Fisher <robfish@att.net>, Carlos Santiago <carloss66@gmail.com>, JTT <aesystems@gmail.com>
 *  Cosmetically Tweaked By: Mike Maat <mmaat@ualberta.ca> on 2016-04-08
 *  Date: 2016-02-03
 */
 // for the UI

metadata {
    // Automatically generated. Make future change here.
    definition (name: "DSC Away Panel", author: "Jordan <jordan@xeron.cc>", namespace: 'DSC') {
        capability "Switch"
        capability "Refresh"

        command "away"
        command "bypassoff"
        command "disarm"
        command "instant"
        command "night"
        command "nokey"
        command "partition"
        command "key"
        command "keyfire"
        command "keyaux"
        command "keypanic"
        command "reset"
        command "stay"
        command "togglechime"
    }

    // simulator metadata
    simulator {
    }

    // UI tile definitions
    tiles(scale: 2) {
    	multiAttributeTile(name:"status", type: "general", width: 6, height: 4){
     	   tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
				attributeState "alarm", label:'Alarming', icon:"st.security.alarm.alarm", backgroundColor:"#ff0000"
				attributeState "away", label:'Armed Away', icon:"st.security.alarm.on", backgroundColor:"#6666FF"
				attributeState "disarm", label:'Disarm', icon:"st.security.alarm.off", backgroundColor:"#79b821"
				attributeState "duress", label:'Duress', icon:"st.security.alarm.alarm", backgroundColor:"#ff0000"
				attributeState "entrydelay", label:'Entry Delay', icon:"st.security.alarm.on", backgroundColor:"#ff9900"
                attributeState "exitdelay", label:'Exit Delay', icon:"st.security.alarm.on", backgroundColor:"#ff9900"
                attributeState "notready", label:'Not Ready', icon:"st.security.alarm.off", backgroundColor:"#ffcc00"
                attributeState "ready", label:'Ready', icon:"st.security.alarm.off", backgroundColor:"#79b821"
                attributeState "forceready", label:'Ready', icon:"st.security.alarm.off", backgroundColor:"#79b821"
                attributeState "stay", label:'Armed Stay', icon:"st.security.alarm.on", backgroundColor:"#00A0DC"
                attributeState "instantaway", label:'Armed Away', icon:"st.security.alarm.on", backgroundColor:"#6666FF"
                attributeState "instantstay", label:'Armed Stay', icon:"st.security.alarm.on", backgroundColor:"#00A0DC"
           }
		}
        
        // This title is just a hidden title for use in the "Things" lists
        standardTile ("statusHidden", "device.status", width: 4, height: 4, title: "Status") {
        	state "alarm", label:'Alarming', action: 'disarm', icon:"st.security.alarm.alarm", backgroundColor:"#ff0000"
        	state "away", label:'Armed Away', action: 'disarm', icon:"st.security.alarm.on", backgroundColor:"#6666FF"
        	state "disarm", label:'Disarm', icon:"st.security.alarm.off", backgroundColor:"#79b821"
        	state "duress", label:'Duress', action: 'disarm', icon:"st.security.alarm.alarm", backgroundColor:"#ff0000"
        	state "entrydelay", label:'Entry Delay', action: 'disarm', icon:"st.security.alarm.on", backgroundColor:"#ff9900"
        	state "exitdelay", label:'Exit Delay', action: 'disarm', icon:"st.security.alarm.on", backgroundColor:"#ff9900"
        	state "notready", label:'Not Ready', icon:"st.security.alarm.off", backgroundColor:"#ffcc00"
        	state "ready", label:'Ready', action: 'away', icon:"st.security.alarm.off", backgroundColor:"#79b821"
        	state "forceready", label:'Ready', action: 'away', icon:"st.security.alarm.off", backgroundColor:"#79b821"
        	state "stay", label:'Armed Stay', action: 'disarm', icon:"st.security.alarm.on", backgroundColor:"#00A0DC"
        	state "instantaway", label:'Armed Away', action: 'disarm', icon:"st.security.alarm.on", backgroundColor:"#6666FF"
        	state "instantstay", label:'Armed Stay', action: 'disarm', icon:"st.security.alarm.on", backgroundColor:"#00A0DC"
       }
      standardTile("away", "capability.momentary", width: 2, height: 2, title: "Away", decoration: "flat"){
        state "away", label: 'Away', action: "away", icon: "st.presence.car.car", backgroundColor: "#6666FF"
      }
      standardTile("stay", "capability.momentary", width: 2, height: 2, title: "Stay", decoration: "flat"){
        state "stay", label: 'Stay', action: "stay", icon: "st.presence.house.secured", backgroundColor: "#00A0DC"
      }
      standardTile("disarm", "capability.momentary", width: 2, height: 2, title: "Disarm", decoration: "flat"){
        state "disarm", label: 'Disarm', action: "disarm", icon: "st.presence.house.unlocked", backgroundColor: "#79b821"
      }
      standardTile("trouble", "device.trouble", width: 2, height: 2, title: "Trouble") {
        state "detected", label: 'Trouble', icon: "st.security.alarm.alarm", backgroundColor: "#ffa81e"
        state "clear", label: 'No\u00A0Trouble', icon: "st.security.alarm.clear"
      }
      standardTile("chime", "device.chime", width: 2, height: 2, title: "Chime", decoration: "flat"){
        state "togglechime", label: 'Toggling\u00A0Chime', action: "togglechime", icon: "st.alarm.beep.beep"//, backgroundColor: "#fbd48a"
        state "chime", label: 'Chime', action: "togglechime", icon: "st.alarm.beep.beep"//, backgroundColor: "#EE9D00"
        state "nochime", label: 'No\u00A0Chime', action: "togglechime", icon: "st.alarm.beep.beep"//, backgroundColor: "#796338"
      }
      standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
        state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
      }

      main "statusHidden"
      details(["status", "away", "stay", "disarm", "trouble", "chime", "refresh"])
    }
}

def partition(String state, String partition, Map parameters) {
  // state will be a valid state for the panel (ready, notready, armed, etc)
  // partition will be a partition number, for most users this will always be 1

  log.debug "Partition: ${state} for partition: ${partition}"

  def onList = ['alarm','away','entrydelay','exitdelay','instantaway']

  def chimeList = ['chime','nochime']

  def troubleMap = [
    'trouble':"detected",
    'restore':"clear",
  ]

  if (onList.contains(state)) {
    sendEvent (name: "switch", value: "on")
  } else if (!(chimeList.contains(state) || troubleMap[state] || state.startsWith('led') || state.startsWith('key'))) {
    sendEvent (name: "switch", value: "off")
  }

  if (troubleMap[state]) {
    def troubleState = troubleMap."${state}"
    // Send trouble event
    sendEvent (name: "trouble", value: "${troubleState}")
  } else if (chimeList.contains(state)) {
    // Send chime event
    sendEvent (name: "chime", value: "${state}")
  } else if (state.startsWith('led')) {
    def flash = (state.startsWith('ledflash')) ? 'flash ' : ''
    for (p in parameters) {
      sendEvent (name: "led${p.key}", value: "${flash}${p.value}")
    }
  } else if (state.startsWith('key')) {
    def name = state.minus('alarm').minus('restore')
    def value = state.replaceAll(/.*(alarm|restore)/, '$1')
    sendEvent (name: "${name}", value: "${value}")
  } else {
    // Send final event
    sendEvent (name: "status", value: "${state}")
  }
}

def away() {
  parent.sendUrl('arm')
}

def bypassoff() {
  parent.sendUrl("bypass?zone=0")
}

def disarm() {
  parent.sendUrl('disarm')
}

def instant() {
  parent.sendUrl('toggleinstant')
}

def night() {
  parent.sendUrl('togglenight')
}

def nokey() {
  sendEvent (name: "key", value: "nokey")
}

def on() {
  away()
}

def off() {
  disarm()
}

def key() {
  sendEvent (name: "key", value: "key")
}

def keyfire() {
  if ("${device.currentValue("key")}" == 'key') {
    parent.sendUrl('panic?type=1')
  }
}

def keyaux() {
  if ("${device.currentValue("key")}" == 'key') {
    parent.sendUrl('panic?type=2')
  }
}

def keypanic() {
  if ("${device.currentValue("key")}" == 'key') {
    parent.sendUrl('panic?type=3')
  }
}

def refresh() {
  parent.sendUrl('refresh')
}

def reset() {
  parent.sendUrl('reset')
}

def stay() {
  parent.sendUrl('stayarm')
}

def togglechime() {
  parent.sendUrl('togglechime')
}
