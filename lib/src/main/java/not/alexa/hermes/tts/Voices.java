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

import not.alexa.hermes.HermesApi.RequestAnswer;
import not.alexa.hermes.HermesMessage;

public class Voices implements HermesMessage<Voices>, RequestAnswer {
	@JsonProperty Voice[] voices;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String id;
	
	protected Voices() {
	}
	
	Voices(Voice[] voices,String siteId,String id) {
		this.voices=voices;
		this.siteId=siteId;
		this.id=id;
	}

	@Override
	public String getTopic() {
		return "rhasspy/tts/voices";
	}

	public Voice[] getVoices() {
		return voices;
	}

	public String getSiteId() {
		return siteId;
	}

	@Override
	public String getId() {
		return id;
	}

	public static class Voice {
		@JsonProperty(required = true) String voiceId;
		@JsonProperty String lang;
		@JsonProperty String description;
		Voice() {
		}
		
		public Voice(String voiceId,String lang,String description) {
			this.voiceId=voiceId;
			this.lang=lang;
			this.description=description;
		}

		public String getVoiceId() {
			return voiceId;
		}

		public String getLang() {
			return lang;
		}

		public String getDescription() {
			return description;
		}
	}
}
