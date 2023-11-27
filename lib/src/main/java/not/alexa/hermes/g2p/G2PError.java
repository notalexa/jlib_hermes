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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.Error;
import not.alexa.hermes.HermesMessage;

public class G2PError extends Error implements HermesMessage<G2PError> {

	public G2PError(String error) {
		this(error,"default");
	}
	
	@JsonCreator
	public G2PError(@JsonProperty("error") String error,@JsonProperty("siteId") String siteId) {
		this(error,null,siteId,null);
	}
	
	public G2PError(String error, String context, String siteId, String sessionId) {
		super(error, context, siteId, sessionId);
	}

	@Override
	public String getTopic() {
		return "hermes/error/g2p";
	}

}
