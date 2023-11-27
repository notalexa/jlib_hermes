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
package not.alexa.hermes.features;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.HermesRequest;
import not.alexa.hermes.HermesMessage;

/**
 * Extension of the hermes API. The result of a features request are all features
 * available at the given (or any) site.
 * <br>It's possible that different features are located at different hosts for a given site. Therefore,
 * a features request can result in multiple answers.
 * 
 * 
 * @author notalexa
 *
 */
public class FeaturesRequest implements HermesMessage<FeaturesRequest>, HermesRequest {
	@JsonProperty String siteId;
	@JsonProperty String id;
	
	public FeaturesRequest() {
		this(null);
	}
	
	public FeaturesRequest(String siteId) {
		this(siteId,null);
	}
	
	public FeaturesRequest(String siteId,String id) {
		this.siteId=siteId;
		this.id=id;
	}

	@Override
	public String getTopic() {
		return "rhasspy/features/getFeatures";
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
	
	public Features createAnswer(String siteId,not.alexa.hermes.Feature[] features) {
		return new Features(features, siteId, id);
	}
}
