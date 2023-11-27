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
package not.alexa.hermes.asr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;

public class ToggleOff implements HermesMessage<ToggleOff> {
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty(defaultValue = "\0") String reason;
	
	public ToggleOff() {
		this("default","");
	}
	
	@JsonCreator
	public ToggleOff(@JsonProperty("siteId") String siteId,@JsonProperty("reason") String reason) {
		this.siteId=siteId;
		this.reason=reason;
	}

	@Override
	public String getTopic() {
		return "hermes/asr/toggleOff";
	}

	public String getSiteId() {
		return siteId;
	}

	public String getReason() {
		return reason;
	}
}
