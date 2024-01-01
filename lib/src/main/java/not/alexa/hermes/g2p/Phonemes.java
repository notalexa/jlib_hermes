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
package not.alexa.hermes.g2p;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.RequestAnswer;
import not.alexa.hermes.HermesMessage;

public class Phonemes implements HermesMessage<Phonemes>, RequestAnswer {
	@JsonProperty Map<String,Result[]> wordPhonemes;
	@JsonProperty String id;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	
	protected Phonemes() {
	}

	public Phonemes(Map<String, Result[]> wordPhonemes, String id, String siteId, String sessionId) {
		this.wordPhonemes = wordPhonemes;
		this.id = id;
		this.siteId = siteId;
		this.sessionId = sessionId;
	}

	@Override
	public String getTopic() {
		return "rhasspy/g2p/phonemes";
	}
	
	public Map<String, Result[]> getWordPhonemes() {
		return wordPhonemes;
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

	public static class Result {
		@JsonProperty String[] phonemes;
		@JsonProperty Boolean guessed;
		protected Result() {
		}
		
		public Result(String[] phonemes,Boolean guessed) {
			this.phonemes=phonemes;
			this.guessed=guessed;
		}
	}

}
