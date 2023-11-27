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

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.RequestAnswer;
import not.alexa.hermes.HermesMessage;

public class Hotwords implements HermesMessage<Hotwords>, RequestAnswer {
	@JsonProperty Model[] models;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String id;
	
	protected Hotwords() {
	}
	
	Hotwords(Model[] models,String siteId,String id) {
		this.models=models;
		this.siteId=siteId;
		this.id=id;
	}

	@Override
	public String getTopic() {
		return "rhasspy/hotword/hotwords";
	}

	public Model[] getModels() {
		return models;
	}

	public String getSiteId() {
		return siteId;
	}

	@Override
	public String getId() {
		return id;
	}

	public static class Model {
		@JsonProperty(required = true) String modelId;
		@JsonProperty(required = true) String[] words;
		@JsonProperty String modelVersion;
		@JsonProperty(defaultValue = "personal") String modelType;
		
		Model() {
		}
		
		public Model(String modelId,String[] words,String modelVersion,String modelType) {
			this.modelId=modelId;
			this.words=words;
			this.modelVersion=modelVersion;
			this.modelType=modelType;
		}

		public String getModelId() {
			return modelId;
		}

		public String[] getWords() {
			return words;
		}

		public String getModelVersion() {
			return modelVersion;
		}

		public String getModelType() {
			return modelType;
		}
	}
}
