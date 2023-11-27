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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.IllegalTopicException;

public class Train implements HermesMessage<Train> {
	@JsonProperty(required = true) String graph_path;
	@JsonProperty String id;
	String siteId;
	
	@JsonCreator
	public Train(@JsonProperty("graph_path") String graph_path) {
		this(graph_path,"default",null);
	}
	
	public Train(String graph_path,String siteId,String id) {
		this.graph_path=graph_path;
		this.siteId=siteId;
		this.id=id;
	}
	
	@Override
	public String getTopic() {
		return "rhasspy/nlu/"+siteId+"/train";
	}

	public String getGraph_path() {
		return graph_path;
	}

	public String getId() {
		return id;
	}

	public String getSiteId() {
		return siteId;
	}
	
	public TrainSuccess createSuccess() {
		return new TrainSuccess(siteId, id);
	}
	
	public NLUError createError(String msg) {
		return new NLUError(msg,siteId);
	}

	public Train forSite(String siteId) {
		try {
			Train cloned=(Train)clone();
			cloned.siteId=siteId;
			return cloned;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public Train forTopic(String topic) throws IllegalTopicException {
		if(topic.startsWith("rhasspy/nlu/")&&topic.endsWith("/train")) {
			String siteId=topic.substring("rhasspy/nlu/".length(),topic.length()-"/train".length());
			if(siteId.indexOf('/')<0) {
				return forSite(siteId);
			}
		}
		throw new IllegalTopicException(topic);
	}

}
