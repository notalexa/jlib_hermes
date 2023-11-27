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
package not.alexa.hermes.asr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.IllegalTopicException;

public class TrainSuccess implements HermesMessage<TrainSuccess> {
	@JsonProperty String id;
	@JsonIgnore String siteId;
	protected TrainSuccess() {
	}

	TrainSuccess(String siteId,String id) {
		this.siteId=siteId;
		this.id=id;
	}

	@Override
	public String getTopic() {
		return "rhasspy/asr/"+siteId+"/trainSuccess";
	}

	public String getId() {
		return id;
	}

	public String getSiteId() {
		return siteId;
	}

	public TrainSuccess forSite(String siteId) {
		try {
			TrainSuccess cloned=(TrainSuccess)clone();
			cloned.siteId=siteId;
			return cloned;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public TrainSuccess forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("rhasspy/asr/")&&topic.endsWith("/trainSuccess")) {
			String siteId=topic.substring("rhasspy/asr/".length(),topic.length()-"/trainSuccess".length());
			if(siteId.indexOf('/')<0) {
				return forSite(siteId);
			}
		}
		throw new IllegalTopicException(topic);
	}
}
