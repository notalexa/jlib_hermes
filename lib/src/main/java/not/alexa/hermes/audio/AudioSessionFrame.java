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
public class AudioSessionFrame extends Binary<AudioSessionFrame> implements HermesMessage<AudioSessionFrame> {
	protected String siteId;
	protected String sessionId;
	
	protected AudioSessionFrame() {
		super(null);
	}
	
	public AudioSessionFrame(String siteId,String sessionId,byte[] data) {
		super(data);
		this.siteId=siteId;
		this.sessionId=sessionId;
	}

	@Override
	public String getTopic() {
		return "hermes/audioServer/"+siteId+"/"+sessionId+"/audioSessionFrame";
	}
	
	public String getSiteId() {
		return siteId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public AudioSessionFrame forIds(String siteId,String sessionId) {
		try {
			AudioSessionFrame clone=(AudioSessionFrame)clone();
			clone.siteId=siteId;
			clone.sessionId=sessionId;
			return clone;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public AudioSessionFrame forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("hermes/audioServer/")&&topic.endsWith("/audioSessionFrame")) {
			String siteId=topic.substring("hermes/audioServer/".length(),topic.length()-"/audioSessionFrame".length());
			if(siteId.indexOf('/')>0) {
				String sessionId=siteId.substring(siteId.indexOf('/')+1);
				siteId=siteId.substring(0,sessionId.length()-2);
				if(sessionId.indexOf('/')<0) {
					return forIds(siteId,sessionId);
				}
			}
		}
		throw new IllegalTopicException(topic);
	}
}
