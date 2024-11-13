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
package not.alexa.hermes;

import java.util.List;
import java.util.Map;

import not.alexa.hermes.mqtt.HermesServer;
import not.alexa.netobjects.Context;

/**
 * Implementors of hermes features typically implement this interface to register resources and overlays.
 * <br>Configuration is done in the {@link HermesServer} setup which creates a working server at startup.

 * @author notalexa
 *
 */
public interface HermesComponent {
	
	/**
	 * Register extensions, resources and overlays needed for this component.
	 * 
	 * @param extensions the extensions table
	 * @param resources the resources added to the context
	 * @param overlays the overlays of the generated type loader
	 */
	public default void configure(Map<String,Class<? extends HermesMessage<?>>> extensions,Map<Class<?>,Object> resources,List<Class<? extends HermesMessage<?>>> overlays) {
	}
	
	/**
	 * Called on startup
	 * 
	 * @param api the api
	 * @param context the startup context 
	 */
	public default void startup(HermesApi api,Context context) {
	}
	
	/**
	 * Called on shutdown
	 * 
	 * @param api the api
	 * @param context the shutdown context
	 */
	public default void shutdown(HermesApi api,Context context) {
	}

}
