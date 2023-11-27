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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.HermesRequest;
import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.hotword.Hotwords.Model;
import not.alexa.netobjects.BaseException;

public class HotwordsRequest implements HermesMessage<HotwordsRequest>, HermesRequest {
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String id;
	
	public HotwordsRequest() {
		this("default");
	}
	
	@JsonCreator
	public HotwordsRequest(@JsonProperty("siteId") String siteId) {
		this(siteId,null);
	}
	
	public HotwordsRequest(String siteId,String id) {
		this.siteId=siteId;
		this.id=id;
	}

	@Override
	public String getTopic() {
		return "rhasspy/hotword/getHotwords";
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
	
	public Hotwords createAnswer(Model[] voices) {
		return new Hotwords(voices, siteId, id);
	}
	
	public Hotwords getHotwords(HermesApi api) throws BaseException {
		return api.publishForAnswer(this, Hotwords.class);
	}
}
