/**
 *  Photon-Powered Accent Light Strips
 *
 *  Copyright 2015 Jeffrey Neong
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
 */
 
 
preferences {
    input("deviceId", "text", title: "Device ID")
    input("token", "text", title: "Access Token")
}

metadata {
	definition (name: "Photon-Powered Accent Light Strips", namespace: "jeffneong", author: "Jeffrey Neong") {
		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Sensor"
		capability "Signal Strength"
		capability "Switch"
		capability "Switch Level"
        
        command "discoOn"
        command "discoOff"

        attribute "brightness", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}
    
	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.Lighting.light11", backgroundColor:"#79b821", nextState:"off"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.Lighting.light13", backgroundColor:"#ffffff", nextState:"on"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 6, range:"(0..100)") {
            state "level", action:"switch level.setLevel", name:"Brightness"
        }
        standardTile("disco", "device.disco", decoration: "flat", width: 2, height: 2) {
            state "discoOff", defaultState:true, label:"Disco!", action:"discoOn", backgroundColor:"#ffffff"
            state "discoOn", label:"Disco!", action:"discoOff", backgroundColor:"#53a7c0"
        }
	}

	main(["switch"])
	details(["switch", "levelSliderControl", "disco"])
    
}


// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'hue' attribute
	// TODO: handle 'saturation' attribute
	// TODO: handle 'color' attribute
	// TODO: handle 'colorTemperature' attribute
	// TODO: handle 'lqi' attribute
	// TODO: handle 'rssi' attribute
	// TODO: handle 'switch' attribute
	// TODO: handle 'level' attribute
    
    //MubAction web call parsing
    def msg = parseLanMessage(description)

    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)
}


def discoOn() {
	log.debug "Executing 'discoOn'"
}

def discoOff() {
	log.debug "Executing 'discoOff'"
}

// handle commands
def setHue() {
	log.debug "Executing 'setHue'"
}

def setSaturation() {
	log.debug "Executing 'setSaturation'"
}


def setColor(value) {
	//log.debug "Executing 'setColor' with map " + value
    def packedColor = "${value.red}".padLeft(3,"0") + "${value.green}".padLeft(3,"0") + "${value.blue}".padLeft(3,"0")
    log.debug "Executing 'setColor' with packedColor: " + packedColor
    put (packedColor,"color")

    if (value.hue) { sendEvent(name: "hue", value: value.hue)}
	if (value.saturation) { sendEvent(name: "saturation", value: value.saturation)}
	if (value.hex) { sendEvent(name: "color", value: value.hex)}
	if (value.level) { sendEvent(name: "level", value: value.level)}
	if (value.switch) { sendEvent(name: "switch", value: value.switch)}
}


def setColorTemperature() {
	log.debug "Executing 'setColorTemperature'"
}


def on() {
	//Signal the photon to turn on, and signal the UI of the
    //previous brightness value, which is the return value
    //of the photon's "On" function
	sendEvent(name: "level", value: "${put ('1',"ledstate")}")
    sendEvent(name: "switch", value: "on")
}

def off() {
	//Signal the photon to turn off the LEDs
	put ('0',"ledstate")
    //Update the UIs to reflect the "Off" state
    sendEvent(name: "level", value: "0")
    sendEvent(name: "switch", value: "off")
}


private put(command,operation) {
    //Spark Core API Call
	httpPost(
		uri: "https://api.spark.io/v1/devices/${deviceId}/${operation}",
        body: [access_token: token, command: command],  
	) { response -> 
    	log.debug (response.data)
        return response.data.return_value
    	}
}


def setLevel(brightness) {
	log.debug "Executing 'setLevel' with brightness "+ brightness
    sendEvent(name: "level", value: brightness)
    put(brightness,"brightness")

    //Brightness switch on
    if(brightness != 0) {
    	//Update UI for switch to indicate its on
    	sendEvent(name: "switch", value: "on")
    }
    else if(brightness == 0) {
    	//Update UI for switch tile showing brightness == 0, which is "off"
    	sendEvent(name: "switch", value: "off")
    }
}




// HELPER METHODS: to get the address of the device
private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}