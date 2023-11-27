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

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;

public class StartSession implements HermesMessage<StartSession> {
	@JsonProperty Init init;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String customData;
	
	protected StartSession() {
	}
	
	public StartSession(Init init,String siteId,String customData) {
		this.init=init;
		this.siteId=siteId;
		this.customData=customData;
	}

	@Override
	public String getTopic() {
		return "hermes/dialogueManager/startSession";
	}

	public Init getInit() {
		return init;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getCustomData() {
		return customData;
	}
	
	public SessionStarted createStarted(String sessionId) {
		return new SessionStarted(sessionId,siteId,customData);
	}
	
	public SessionQueued createQueued(String sessionId) {
		return new SessionQueued(sessionId,siteId,customData);
	}

	public static class Init {
		@JsonProperty(required = true) String type;
		@JsonProperty(defaultValue="false") boolean canBeEnqueued;
		@JsonProperty String text;
		@JsonProperty String[] intentFilter;
		@JsonProperty(defaultValue="false") boolean sendIntentNotRecognized;
		protected Init() {
		}
		public Init(String notification) {
			type="notification";
			text=notification;
		}
		public Init(boolean canBeEnqueued,String msg) {
			this(canBeEnqueued,msg,null,false);
		}
		public Init(Boolean canBeEnqueued, String text, String[] intentFilter, boolean sendIntentNotRecognized) {
			super();
			type="action";
			this.canBeEnqueued = canBeEnqueued;
			this.text = text;
			this.intentFilter = intentFilter;
			this.sendIntentNotRecognized = sendIntentNotRecognized;
		}
		public String getType() {
			return type;
		}
		public boolean getCanBeEnqueued() {
			return canBeEnqueued;
		}
		public String getText() {
			return text;
		}
		public String[] getIntentFilter() {
			return intentFilter;
		}
		public boolean isSendIntentNotRecognized() {
			return sendIntentNotRecognized;
		}
	}
}
