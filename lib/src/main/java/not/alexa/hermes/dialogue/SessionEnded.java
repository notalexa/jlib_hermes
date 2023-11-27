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
package not.alexa.hermes.dialogue;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;

public class SessionEnded implements HermesMessage<SessionEnded> {
	@JsonProperty(required = true) String termination;
	@JsonProperty(required = true) String sessionId;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String customData;
	
	protected SessionEnded() {
	}
	
	public SessionEnded(String termination,String sessionId,String siteId,String customData) {
		this.termination=termination;
		this.sessionId=sessionId;
		this.siteId=siteId;
		this.customData=customData;
	}

	@Override
	public String getTopic() {
		return "hermes/dialogueManager/sessionEnded";
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getCustomData() {
		return customData;
	}

	public String getTermination() {
		return termination;
	}

}
