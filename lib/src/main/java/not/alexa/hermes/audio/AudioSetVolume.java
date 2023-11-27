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

import not.alexa.hermes.HermesMessage;

public class AudioSetVolume implements HermesMessage<AudioSetVolume> {
	@JsonProperty(required = true) float volume;
	@JsonProperty(defaultValue = "default") String siteId;

	public AudioSetVolume(float volume) {
		this(volume,"default");
	}
	
	@JsonCreator
	public AudioSetVolume(@JsonProperty("volume") float volume,@JsonProperty("siteId") String siteId) {
		this.volume=volume;
		this.siteId=siteId;
	}

	@Override
	public String getTopic() {
		return "rhasspy/audioServer/setVolume";
	}

	public float getVolume() {
		return volume;
	}

	public String getSiteId() {
		return siteId;
	}
}
