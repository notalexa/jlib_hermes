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
package not.alexa.hermes.tts;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesMessage;

public class Say implements HermesMessage<Say> {
	@JsonProperty(required = true) String text;
	@JsonProperty String lang;
	@JsonProperty String id;
	@JsonProperty(defaultValue="-1") float volume;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	@JsonProperty String voiceId;
	
	protected Say() {
	}
	
	public Say(String text) {
		this("default",text);
	}

	public Say(String siteId,String text) {
		this(text,null,HermesApi.createId(),-1f,siteId,null,null);
	}

	public Say(String text,String lang,String id,float volume,String siteId,String sessionId,String voiceId) {
		this.text=text;
		this.lang=lang;
		this.id=id;
		this.volume=volume;
		this.siteId=siteId;
		this.sessionId=sessionId;
	}

	@Override
	public String getTopic() {
		return "hermes/tts/say";
	}

	public String getText() {
		return text;
	}

	public String getLang() {
		return lang;
	}

	public String getId() {
		return id;
	}

	public float getVolume() {
		return volume;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public SayFinished createFinished() {
		return new SayFinished(siteId, id);
	}
	
	public TTSError createError(String msg) {
		return new TTSError(msg,siteId);
	}
}
