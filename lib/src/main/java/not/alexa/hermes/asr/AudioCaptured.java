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

import not.alexa.hermes.HermesApi.Binary;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.IllegalTopicException;
import not.alexa.netobjects.api.ResolvableBy;

@ResolvableBy("jackson")
public class AudioCaptured extends Binary<AudioCaptured> implements HermesMessage<AudioCaptured> {
	protected String siteId;
	protected String sessionId;
	
	protected AudioCaptured() {
		super(null);
	}
	
	public AudioCaptured(String siteId,String sessionId,byte[] data) {
		super(data);
		this.siteId=siteId;
		this.sessionId=sessionId;
	}

	@Override
	public String getTopic() {
		return "hermes/asr/"+siteId+"/"+sessionId+"/audioCaptured";
	}
	
	public String getSiteId() {
		return siteId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public AudioCaptured forIds(String siteId,String sessionId) {
		try {
			AudioCaptured cloned=(AudioCaptured)clone();
			cloned.sessionId=sessionId;
			cloned.siteId=siteId;
			return cloned;
		} catch(Throwable t) {
			return null;
		}
	}
		
	public AudioCaptured forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("hermes/asr/")&&topic.endsWith("/audioCaptured")) {
			String siteId=topic.substring("hermes/asr/".length(),topic.length()-"/audioCaptured".length());
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
