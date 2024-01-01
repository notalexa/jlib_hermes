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
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.HermesRequest;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.HermesMessage;

public class Query implements HermesMessage<Query>, HermesRequest {
	static final Query[] NO_ALTERNATIVES=new Query[0];

	@JsonProperty protected String input;
	@JsonProperty protected String[] intentFilter;
	@JsonProperty(required = false) protected String id;
	@JsonProperty(defaultValue = "default") protected String siteId;
	@JsonProperty(required = false) protected String sessionId;
	@JsonProperty protected Slot[] initialSlots;
	@JsonProperty(defaultValue="0") protected float asrConfidence;
	@JsonProperty protected Query[] alternatives;

	public static Builder createBuilder(String input) {
		return new Builder().setInput(input);
	}
	
	protected Query() {
	}

	@Override
	public String getTopic() {
		return "hermes/nlu/query";
	}

	public String getInput() {
		return input;
	}

	public String[] getIntentFilter() {
		return intentFilter;
	}

	public String getId() {
		return id;
	}
	
	public String getSiteId() {
		return siteId;
	}
	

	@Override
	public void setId(String id) {
		this.id=id;
	}


	public String getSessionId() {
		return sessionId;
	}

	public float getAsrConfidence() {
		return asrConfidence;
	}

	public Query[] getAlternatives() {
		return alternatives==null?NO_ALTERNATIVES:alternatives;
	}
	
	public NLUIntent.Builder createAnswer() {
		return NLUIntent.createBuilder(input)
				.addSlots(initialSlots)
				.setId(id)
				.setSessionId(sessionId)
				.setSiteId(siteId)
				.setAsrConfidence(asrConfidence);
	}
	
	public IntentNotRecognized createError() {
		return new IntentNotRecognized(input, id, siteId, sessionId);
	}
	
	public static class Builder {
		Query query=new Query();
		{
			query.siteId="default";
		}
		private List<Slot> slots=new ArrayList<>();
		public Builder setInput(String input) {
			query.input = input;
			return this;
		}
	
		public Builder setIntentFilter(String[] intentFilter) {
			query.intentFilter = intentFilter;
			return this;
		}
		
		public Builder addInitialSlot(Slot slot) {
			for(int i=0;i<slots.size();i++) {
				if(slot.getEntity().equals(slots.get(i).getEntity())) {
					slots.set(i, slot);
					return this;
				}
			}
			slots.add(slot);
			return this;
		}
	
		public Builder setId(String id) {
			query.id = id;
			return this;
		}
	
		public Builder setSiteId(String siteId) {
			query.siteId = siteId;
			return this;
		}

		public Builder setSessionId(String sessionId) {
			query.sessionId = sessionId;
			return this;
		}

		public Builder setAsrConfidence(float asrConfidence) {
			query.asrConfidence = asrConfidence;
			return this;
		}

		public Builder setAlternatives(Query[] alternatives) {
			query.alternatives=alternatives.clone();
			return this;
		}

		public Builder setAlternatives(Collection<Query> alternatives) {
			return setAlternatives(alternatives.toArray(NO_ALTERNATIVES));
		}

		public Query build() {
			if(slots.size()>0) {
				query.initialSlots=slots.toArray(HermesApi.NO_SLOTS);
			}
			Query result=query;
			try {
				query=(Query)query.clone();
			} catch(Throwable t) {
			}
			return result;
		}
	}
}
