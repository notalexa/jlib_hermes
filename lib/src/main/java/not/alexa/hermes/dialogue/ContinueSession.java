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

public class ContinueSession implements HermesMessage<ContinueSession> {
	@JsonProperty(required = true) String sessionId;
	@JsonProperty String customData;
	@JsonProperty String text;
	@JsonProperty String[] intentFilter;
	@JsonProperty(defaultValue = "false") boolean sendIntentNotRecognized;
	
	protected ContinueSession() {
	}
	
	public ContinueSession(String sessionId,String customData,String text,String[] intentFilter,boolean sendIntentNotRecognized) {
		this.sessionId=sessionId;
		this.customData=customData;
		this.text=text;
		this.intentFilter=intentFilter;
		this.sendIntentNotRecognized=sendIntentNotRecognized;
	}

	@Override
	public String getTopic() {
		return "hermes/dialogueManager/continueSession";
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getCustomData() {
		return customData;
	}

	public String getText() {
		return text;
	}

	public String[] getIntentFilter() {
		return intentFilter;
	}

	public boolean isSendIntentNotRecognized() {
		return sendIntentNotRecognized;
	}

}
