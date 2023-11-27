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
package not.alexa.hermes.dialogue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;

public class Configure implements HermesMessage<Configure> {
	@JsonProperty IntentConfig[] intents;
	@JsonProperty(defaultValue = "default") String siteId;

	protected Configure() {
	}
	
	public Configure(IntentConfig[] intents) {
		this(intents,"default");
	}
	
	public Configure(IntentConfig[] intents,String siteId) {
		this.intents=intents;
		this.siteId=siteId;
	}

	@Override
	public String getTopic() {
		return "hermes/dialogueManager/configure";
	}
	
	public IntentConfig[] getIntents() {
		return intents;
	}

	public String getSiteId() {
		return siteId;
	}

	public static class IntentConfig {
		@JsonProperty(required = true) String intentId;
		@JsonProperty(required = true) boolean enable;
		@JsonCreator
		public IntentConfig(@JsonProperty("intentId") String intentId,@JsonProperty("enable") boolean enable) {
			this.intentId=intentId;
			this.enable=enable;
		}
		public String getIntentId() {
			return intentId;
		}
		public boolean isEnable() {
			return enable;
		}		
	}

}
