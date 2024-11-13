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
import not.alexa.netobjects.Context;

/**
 * Interface for intent handling.
 * 
 */
public interface IntentHandler {
	
	/**
	 * Called if an intent is received.
	 * 
	 * @param api the api which received the intent
	 * @param intent the intent itself
	 * @return {@code true} if the intent was consumed and should be removed from the stack
	 * or {@code false} otherwise
	 */
	public default boolean onIntentReceived(HermesApi api,NLUIntent intent) {
		return false;
	}
	
	/**
	 * Called on startup. Defaults to a no operation.
	 * 
	 * @param context the context to use for startup
	 */
	public default void startup(HermesApi api,Context context) {
	}
	
	/**
	 * Called on shutdown. Defaults to a no operation.
	 * @param context the context on shutdown
	 */
	public default void shutdown(HermesApi api,Context context) {
	}
}
