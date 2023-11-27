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

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.AsrToken;
import not.alexa.hermes.HermesMessage;

public class TextCaptured implements HermesMessage<TextCaptured> {
	@JsonProperty(required = true) String text;
	@JsonProperty(required = true) float likelihood;
	@JsonProperty(required = true) float seconds;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	@JsonProperty String wakewordId;
	@JsonProperty AsrToken[][] asrTokens;
	
	protected TextCaptured() {
	}

	public TextCaptured(String text, float likelihood, float seconds, String siteId, String sessionId,
			String wakewordId, AsrToken[][] asrTokens) {
		super();
		this.text = text;
		this.likelihood = likelihood;
		this.seconds = seconds;
		this.siteId = siteId;
		this.sessionId = sessionId;
		this.wakewordId = wakewordId;
		this.asrTokens = asrTokens;
	}

	@Override
	public String getTopic() {
		return "hermes/asr/textCaptured";
	}

	public String getText() {
		return text;
	}

	public float getLikelihood() {
		return likelihood;
	}

	public float getSeconds() {
		return seconds;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getWakewordId() {
		return wakewordId;
	}

	public AsrToken[][] getAsrTokens() {
		return asrTokens;
	}
}
