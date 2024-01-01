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

import not.alexa.hermes.HermesApi.Binary;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.IllegalTopicException;
import not.alexa.netobjects.api.ResolvableBy;

@ResolvableBy("jackson")
public class AudioFrame extends Binary<AudioFrame> implements HermesMessage<AudioFrame> {
	protected String siteId;
	
	protected AudioFrame() {
		super(null);
	}
	
	public AudioFrame(String siteId,byte[] data) {
		super(data);
		this.siteId=siteId;
	}

	@Override
	public String getTopic() {
		return "hermes/audioServer/"+siteId+"/audioFrame";
	}
	
	public String getSiteId() {
		return siteId;
	}
	
	public AudioFrame forSite(String siteId) {
		try {
			AudioFrame clone=(AudioFrame)clone();
			clone.siteId=siteId;
			return clone;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public AudioFrame forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("hermes/audioServer/")&&topic.endsWith("/audioFrame")) {
			String siteId=topic.substring("hermes/audioServer/".length(),topic.length()-"/audioFrame".length());
			if(siteId.indexOf('/')<0) {
				return forSite(siteId);
			}
		}
		throw new IllegalTopicException(topic);
	}
}
