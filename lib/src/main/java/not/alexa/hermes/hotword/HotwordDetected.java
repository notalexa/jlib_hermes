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
package not.alexa.hermes.hotword;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.IllegalTopicException;

public class HotwordDetected implements HermesMessage<HotwordDetected> {
	@JsonIgnore String wakewordId;
	@JsonProperty(required = true) String modelId;
	@JsonProperty(defaultValue = "\0") String modelVersion;
	@JsonProperty(defaultValue = "personal") String modelType;
	@JsonProperty(defaultValue = "1") float currentSensitivity;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	@JsonProperty Boolean sendAudioCaptured;
	
 	protected HotwordDetected() {
	}
 	
 	public HotwordDetected(String wakewordId,String modelId) {
 		this(wakewordId,modelId,"","personal",1f,"default",null,null);
 	}

	public HotwordDetected(String wakewordId, String modelId, String modelVersion, String modelType,
			float currentSensitivity, String siteId, String sessionId, Boolean sendAudioCaptured) {
		super();
		this.wakewordId = wakewordId;
		this.modelId = modelId;
		this.modelVersion = modelVersion;
		this.modelType = modelType;
		this.currentSensitivity = currentSensitivity;
		this.siteId = siteId;
		this.sessionId = sessionId;
		this.sendAudioCaptured = sendAudioCaptured;
	}

	@Override
	public String getTopic() {
		return "hermes/hotword/"+wakewordId+"/detected";
	}

	public String getWakewordId() {
		return wakewordId;
	}

	public String getModelId() {
		return modelId;
	}

	public String getModelVersion() {
		return modelVersion;
	}

	public String getModelType() {
		return modelType;
	}

	public float getCurrentSensitivity() {
		return currentSensitivity;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Boolean getSendAudioCaptured() {
		return sendAudioCaptured;
	}
	
	public HotwordDetected forWakewordId(String id) {
		try {
			HotwordDetected cloned=(HotwordDetected)clone();
			cloned.wakewordId=id;
			return cloned;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public HotwordDetected forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("hermes/hotword/")&&topic.endsWith("/detected")) {
			String wakewordId=topic.substring("hermes/hotword/".length(),topic.length()-"/detected".length());
			if(wakewordId.indexOf('/')<0) {
				return forWakewordId(wakewordId);
			}
		}
		throw new IllegalTopicException(topic);
	}
}
