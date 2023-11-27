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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.HermesRequest;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.tts.Voices.Voice;
import not.alexa.netobjects.BaseException;

public class VoicesRequest implements HermesMessage<VoicesRequest>, HermesRequest {
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String id;
	
	public VoicesRequest() {
		this("default");
	}
	
	@JsonCreator
	public VoicesRequest(@JsonProperty("siteId") String siteId) {
		this(siteId,null);
	}
	
	public VoicesRequest(String siteId,String id) {
		this.siteId=siteId;
		this.id=id;
	}

	@Override
	public String getTopic() {
		return "rhasspy/tts/getVoices";
	}

	public String getSiteId() {
		return siteId;
	}

	public String getId() {
		return id;
	}
	
	@Override
	public void setId(String id) {
		this.id=id;
	}

	public Voices createAnswer(Voice[] voices) {
		return new Voices(voices, siteId, id);
	}
	
	public Voices getVoices(HermesApi api) throws BaseException {
		return api.publishForAnswer(this, Voices.class);
	}

}
