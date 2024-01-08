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
package not.alexa.hermes.nlu;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.tts.Say;
import not.alexa.netobjects.BaseException;
import not.alexa.hermes.HermesApi.AsrToken;
import not.alexa.hermes.HermesApi.Intent;
import not.alexa.hermes.HermesApi.RequestAnswer;
import not.alexa.hermes.HermesApi.Slot;

public class NLUIntent implements HermesMessage<NLUIntent>, RequestAnswer {

	@JsonProperty(required = true) protected String input;
	@JsonProperty(required = true) protected Intent intent;
	@JsonProperty(required = true) protected Slot[] slots;
	@JsonProperty(defaultValue = "") protected String id;
	@JsonProperty(defaultValue = "default") protected String siteId;
	@JsonProperty(defaultValue = "") protected String sessionId;
	@JsonProperty(defaultValue = "") protected String customData;
	@JsonProperty protected AsrToken[][] asrTokens;
	@JsonProperty(defaultValue = "0.0") protected float asrConfidence=1f;
	
	
	public static Builder createBuilder(String input) {
		return new Builder().setInput(input);
	}
	
	protected NLUIntent() {
	}

	@Override
	public String getTopic() {
		return "hermes/intent/"+intent.getIntentName();
	}
	
	public float getConfidenceScore() {
		return intent.getConfidenceScore();
	}
	
	public String getIntent() {
		return intent.getIntentName();
	}
	
	public Slot getSlot(String name) {
		for(Slot slot:slots) {
			if(name.equals(slot.getEntity())) {
				return slot;
			}
		}
		return null;
	}

	public String getInput() {
		return input;
	}
	
	public Slot[] getSlots() {
		return slots;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getSiteId() {
		return siteId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getCustomData() {
		return customData;
	}
	
	public AsrToken[][] getAsrTokens() {
		return asrTokens==null?HermesApi.NO_ASR_TOKENS:asrTokens;
	}
	
	public float getAsrConfidence() {
		return asrConfidence;
	}
	
	public void reply(HermesApi api,String text) throws BaseException {
		Slot replyTo=getSlot("reply-to");
		if(replyTo!=null) {
			new Reply(api.getSiteId(),replyTo.getValue(),text).publish(api);
		} else {
			new Say(api.getSiteId(),text).publish(api);
		}
	}
	
	public static class Builder {
		NLUIntent answer=new NLUIntent();
		{
			answer.id="";
			answer.siteId="default";
			answer.sessionId="";
			answer.customData="";
		}

		private List<Slot> slots=new ArrayList<>();

		public Builder setInput(String input) {
			answer.input = input;
			return this;
		}
	
		public Builder setIntent(Intent intent) {
			answer.intent = intent;
			return this;
		}

		public Builder setIntent(String intent) {
			answer.intent = new Intent(intent,1f);
			return this;
		}

		public Builder addSlot(Slot slot) {
			for(int i=0;i<slots.size();i++) {
				if(slot.getEntity().equals(slots.get(i).getEntity())) {
					slots.set(i, slot);
					return this;
				}
			}
			slots.add(slot);
			return this;
		}
		public Builder addSlots(Slot[] slots) {
			if(slots!=null) for(Slot slot:slots) {
				addSlot(slot);
			}
			return this;
		}

		public Builder setId(String id) {
			answer.id = id;
			return this;
		}
	
		public Builder setSiteId(String siteId) {
			answer.siteId = siteId;
			return this;
		}
		
		public Builder setSessionId(String sessionId) {
			answer.sessionId = sessionId;
			return this;
		}

		public Builder setCustomData(String customData) {
			answer.customData = customData;
			return this;
		}

		public Builder setAsrConfidence(float asrConfidence) {
			answer.asrConfidence = asrConfidence;
			return this;
		}
		
		public Builder setAsrTokens(AsrToken[][] tokens) {
			answer.asrTokens=tokens;
			return this;
		}

		public NLUIntent build() {
			answer.slots=slots.toArray(HermesApi.NO_SLOTS);
			NLUIntent result=answer;
			try {
				answer=(NLUIntent)answer.clone();
			} catch(Throwable t) {
			}
			return result;
		}
	}
}
