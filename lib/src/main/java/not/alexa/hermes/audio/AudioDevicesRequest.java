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
import not.alexa.hermes.HermesApi.Device.Mode;
import not.alexa.netobjects.BaseException;
import not.alexa.hermes.HermesApi.HermesRequest;
import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesMessage;

public class AudioDevicesRequest implements HermesMessage<AudioDevicesRequest>, HermesRequest {
	@JsonProperty Mode[] modes;
	@JsonProperty String id;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty(defaultValue = "false") boolean test;
	
	@JsonCreator
	public AudioDevicesRequest(@JsonProperty("modes") Mode[] modes,@JsonProperty("siteId") String siteId,@JsonProperty("test") boolean test) {
		this(modes,null,siteId,test);
	}
	
	public AudioDevicesRequest(Mode[] modes,String id,String siteId,boolean test) {
		this.modes=modes;
		this.id=id;
		this.siteId=siteId;
		this.test=test;
	}

	@Override
	public String getTopic() {
		return "rhasspy/audioServer/getDevices";
	}

	public Mode[] getModes() {
		return modes;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public void setId(String id) {
		this.id=id;
	}

	public String getSiteId() {
		return siteId;
	}

	public boolean isTest() {
		return test;
	}
	
	public AudioDevices createAnswer(Device[] devices) {
		return new AudioDevices(devices, siteId, id);
	}
	
	public AudioDevices getDevices(HermesApi api) throws BaseException {
		return api.publishForAnswer(this, AudioDevices.class);
	}
}
