/*
 * Copyright (C) 2023 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package not.alexa.hermes.audio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.Device;
import not.alexa.hermes.HermesApi.RequestAnswer;
import not.alexa.hermes.HermesMessage;

public class AudioDevices implements HermesMessage<AudioDevices>, RequestAnswer {

	@JsonProperty(required = true) Device[] devices;
	@JsonProperty String id;
	@JsonProperty(defaultValue="default") String siteId;
	
	@JsonCreator
	public AudioDevices(@JsonProperty("devices") Device[] devices,@JsonProperty("siteId") String siteId) {
		this(devices,siteId,null);
	}
	
	AudioDevices(Device[] devices,String siteId,String id) {
		this.devices=devices;
		this.siteId=siteId;
		this.id=id;
	}
	
	@Override
	public String getTopic() {
		return "rhasspy/audioServer/devices";
	}
	
	public Device[] getDevices() {
		return devices;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getSiteId() {
		return siteId;
	}
}
