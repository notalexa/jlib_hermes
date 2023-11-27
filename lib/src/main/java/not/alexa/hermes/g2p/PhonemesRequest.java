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

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.HermesRequest;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.g2p.Phonemes.Result;
import not.alexa.netobjects.BaseException;

public class PhonemesRequest implements HermesMessage<PhonemesRequest>, HermesRequest {
	@JsonProperty(required = true) String[] words;
	@JsonProperty String id;
	@JsonProperty(defaultValue = "5") int numGuesses;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String sessionId;
	
	protected PhonemesRequest() {
	}
	
	public PhonemesRequest(String[] words) {
		this(words,null,5,"default",null);
	}
	
	public PhonemesRequest(String[] words,String id,int numGuesses,String siteId,String sessionId) {
		this.words=words;
		this.id=id;
		this.numGuesses=numGuesses;
		this.siteId=siteId;
		this.sessionId=sessionId;
	}

	@Override
	public String getTopic() {
		return "rhasspy/g2p/pronounce";
	}

	public String[] getWords() {
		return words;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public void setId(String id) {
		this.id=id;
	}

	public int getNumGuesses() {
		return numGuesses;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Phonemes createAnswer(Map<String,Result[]> result) {
		return new Phonemes(result, id, siteId, sessionId);
	}
	
	public Phonemes getPhonemes(HermesApi api) throws BaseException {
		return api.publishForAnswer(this, Phonemes.class);
	}
}
