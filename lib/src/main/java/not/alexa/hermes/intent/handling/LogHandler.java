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
package not.alexa.hermes.intent.handling;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.api.ResolvableBy;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.json.JsonCodingScheme;

/**
 * Simple handler logging the incoming intent (and returning {@code false} for
 * further handling. This handler should be the first in the stack to log
 * all incoming intents.
 * 
 */
@ResolvableBy("jackson")
public class LogHandler implements IntentHandler {
	private CodingScheme scheme=JsonCodingScheme.RESTRICTED_SCHEME.newBuilder().setIndent("  ","\n").build();
	public LogHandler() {
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		if(intent.getSiteId().equals(api.getSiteId())) try(ByteEncoder encoder=scheme.createEncoder(api.getContext())) {
			api.getContext().getLogger().info(intent.getTopic()+"\n"+new String(encoder.encode(intent).asBytes()));
		} catch(BaseException e) {
		}
		return false;
	}
}
