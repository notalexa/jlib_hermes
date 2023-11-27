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

public class StopListening implements HermesMessage<StopListening> {
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	
	public StopListening() {
		this("default");
	}

	@JsonCreator
	public StopListening(@JsonProperty("siteId") String siteId) {
		this(siteId,null);
	}

	public StopListening(String siteId,String sessionId) {
		this.siteId=siteId;
		this.sessionId=sessionId;
	}

	@Override
	public String getTopic() {
		return "hermes/asr/stopListening";
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}
}
