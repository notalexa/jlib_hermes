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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesComponent;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;

/**
 * Class implementing {@link HermesComponent} for handling intents.
 * The stack keeps a list of {@link IntentHandler}s calling one after
 * the other until the handler returns {@code true} if active and ignores
 * all messages if not active
 * 
 * @author notalexa
 *
 */
public class Stack implements HermesComponent {
	@JsonProperty(defaultValue = "true") boolean active;
	@JsonProperty(defaultValue = "true") boolean acceptWildcard;
	@JsonProperty IntentHandler[] handlers;
	
	protected Stack() {
	}

	/**
	 * Constructor for the given handler, initially active.
	 * 
	 * @param handler the handler
	 */
	public Stack(IntentHandler handler) {
		this(new IntentHandler[] { handler });
	}
	
	/**
	 * Constructor for the given handlers, initially active.
	 * 
	 * @param handlers the handlers
	 */
	public Stack(IntentHandler...handlers) {
		this(true,handlers);
	}

	/**
	 * General constructor.
	 * 
	 * @param active is this instance active?
	 * @param handlers the handlers
	 */
	public Stack(boolean active,IntentHandler...handlers) {
		this.active=active;
		this.handlers=handlers;
	}


	@Override
	public void configure(Map<String,Class<? extends HermesMessage<?>>> extensions,Map<Class<?>, Object> resources, List<Class<? extends HermesMessage<?>>> overlays) {
		if(handlers!=null) for(IntentHandler handler:handlers) {
			if(handler instanceof HermesComponent) {
				((HermesComponent)handler).configure(extensions,resources, overlays);
			}
		}
		resources.put(Stack.class,this);
		overlays.add(ToggleOnHandler.class);
		overlays.add(ToggleOffHandler.class);
		overlays.add(NLUIntentHandler.class);
	}
	

	@Override
	public void startup(HermesApi api,Context context) {
		if(handlers!=null) for(IntentHandler handler:handlers) {
			handler.startup(api,context);
		}
	}

	@Override
	public void shutdown(HermesApi api,Context context) {
		if(handlers!=null) for(IntentHandler handler:handlers) {
			handler.shutdown(api,context);
		}
	}


	/**
	 * Handle incoming intents by delegating to the listeners (if active).
	 * 
	 * @param api the hermes API
	 * @param intent the intent to handle
	 */
	protected void onIntentReceived(HermesApi api,NLUIntent intent) {
		if(active&&handlers!=null) for(IntentHandler handler:handlers) {
			if(handler.onIntentReceived(api,intent)) {
				break;
			}
		}
	}
	
	/**
	 * Handler for incoming {@link ToggleOn} requests.
	 * 
	 * @author notalexa
	 *
	 */
	@Overlay
	public class ToggleOnHandler extends ToggleOn {

		protected ToggleOnHandler(@JsonProperty("siteId") String siteId) {
			super(siteId);
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			if(api.matches(getSiteId())) {
				active=true;
			}
		}
	}

	/**
	 * Handler for incoming {@link ToggleOff} requests.
	 * 
	 * @author notalexa
	 *
	 */
	@Overlay
	public class ToggleOffHandler extends ToggleOff {

		protected ToggleOffHandler(@JsonProperty("siteId") String siteId) {
			super(siteId);
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			if(api.matches(getSiteId())) {
				active=false;
			}
		}
	}
	
	/**
	 * Handler for incoming {@link NLUIntent} requests.
	 * This handler delegates to {@link Stack#onIntentReceived(HermesApi, NLUIntent)} if the siteId matches or the siteId is <code>*</code> and
	 * wildcards are accepted and the messagetimestamp is not older than a minute. 
	 * 
	 * @author notalexa
	 *
	 */
	@Overlay
	public class NLUIntentHandler extends NLUIntent {

		public NLUIntentHandler() {
			super();
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			if((api.matches(getSiteId())||(acceptWildcard&&"*".equals(getSiteId())))&&timestamp>System.currentTimeMillis()-60000) {
				onIntentReceived(api,this);
			}
		}
	}
}
