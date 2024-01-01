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
package not.alexa.hermes.nlu;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;

public class IntentNotRecognized implements HermesMessage<IntentNotRecognized> {
	@JsonProperty(required = true) String input;
	@JsonProperty String id;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	
	protected IntentNotRecognized() {
	}

	public IntentNotRecognized(String input,String id,String siteId,String sessionId) {
		this.input=input;
		this.id=id;
		this.siteId=siteId;
		this.sessionId=sessionId;
	}

	@Override
	public String getTopic() {
		return "hermes/nlu/intentNotRecognized";
	}

	public String getInput() {
		return input;
	}

	public String getId() {
		return id;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}

}
