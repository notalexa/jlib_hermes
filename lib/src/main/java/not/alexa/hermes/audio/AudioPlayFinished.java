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
package not.alexa.hermes.audio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.IllegalTopicException;

public class AudioPlayFinished implements HermesMessage<AudioPlayFinished> {
	@JsonProperty protected String siteId;
	@JsonProperty protected String requestId;
	
	@JsonCreator
	public AudioPlayFinished(@JsonProperty("requestId") String requestId) {
		this("default",requestId);
	}
	
	public AudioPlayFinished(String siteId,String requestId) {
		this.siteId=siteId;
		this.requestId=requestId;
	}

	@Override
	public String getTopic() {
		return "hermes/audioServer/"+siteId+"/playFinished";
	}

	public String getSiteId() {
		return siteId;
	}

	public String getRequestId() {
		return requestId;
	}
	
	public AudioPlayFinished forSite(String siteId) {
		try {
			AudioPlayFinished clone=(AudioPlayFinished)clone();
			clone.siteId=siteId;
			return clone;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public AudioPlayFinished forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("hermes/audioServer/")&&topic.endsWith("/playFinished")) {
			String siteId=topic.substring("hermes/audioServer/".length(),topic.length()-"/playFinished".length());
			if(siteId.indexOf('/')<0) {
				return forSite(siteId);
			}
		}
		throw new IllegalTopicException(topic);
	}
}
