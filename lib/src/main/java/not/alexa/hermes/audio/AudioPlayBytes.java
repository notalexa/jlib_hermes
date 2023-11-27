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
public class AudioPlayBytes extends Binary<AudioPlayBytes> implements HermesMessage<AudioPlayBytes> {
	protected String siteId;
	protected String requestId;
	
	AudioPlayBytes() {
		super(null);
	}
	public AudioPlayBytes(String siteId,String requestId,byte[] data) {
		super(data);
		this.siteId=siteId;
		this.requestId=requestId;
	}

	@Override
	public String getTopic() {
		return "hermes/audioServer/"+siteId+"/playBytes/"+requestId;
	}
	
	public String getSiteId() {
		return siteId;
	}

	public String getRequestId() {
		return requestId;
	}
	
	public AudioPlayFinished createAnswer() {
		return new AudioPlayFinished(siteId,requestId);
	}

	public AudioPlayBytes forIds(String siteId,String requestId) {
		try {
			AudioPlayBytes clone=(AudioPlayBytes)clone();
			clone.siteId=siteId;
			clone.requestId=requestId;
			return clone;
		} catch(Throwable t) {
			return null;
		}	
	}
	
	public AudioPlayBytes forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("hermes/audioServer/")&&topic.indexOf("/playBytes/")>0) {
			String siteId=topic.substring("hermes/audioServer/".length(),topic.indexOf("/playBytes/"));
			if(siteId.indexOf('/')<0) {
				String requestId=topic.substring("hermes/audioServer/".length()+siteId.length()+"/playBytes/".length());
				if(requestId.indexOf('/')<0) {
					return forIds(siteId, requestId);
				}
			}
		}
		throw new IllegalTopicException(topic);
	}
}
