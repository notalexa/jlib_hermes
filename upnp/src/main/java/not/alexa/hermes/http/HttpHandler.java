/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.hermes.http;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.net.httpserver.*;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.intent.handling.IntentHandler;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.netobjects.Context;

/**
 * Http support for intent handling. This (optional) server is intended to deliver additional
 * resources if necessary.
 * Configuration is done specifying the port and a list of handlers of type {@link HttpRequestHandler}. Currently the {@link DirectoryHandler}
 * is provided. A possible configuration can be
 * <pre>
 * - class: not.alexa.hermes.http.HttpHandler
 *   port: 8080
 *   handlers:
 *   - class: not.alexa.hermes.http.DirectoryHandler
 *     path: /docs/
 *     dir: ${user.dir}/www
 * </pre>
 * which configures a http server resolving content from the base directory {@code www} below the users
 * directory using the complete path.
 * @author notalexa 
 */
public class HttpHandler implements IntentHandler {
	@JsonProperty(required = true) int port;
	@JsonProperty List<HttpRequestHandler> handlers;
	
	private HttpServer server;
	
	public HttpHandler() {
	}

	/**
	 * Interface for defining request handlers. This interface is a direct extendsion of 
	 * {@linkplain com.sun.net.httpserver.HttpHandler} and additionaly contains the
	 * path of the handler
	 */
	public interface HttpRequestHandler extends com.sun.net.httpserver.HttpHandler {
		/**
		 * 
		 * @return the path of this handler
		 */
		public String getPath();
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		return false;
	}

	@Override
	public void startup(Context context) {
		if(handlers!=null&&handlers.size()>0) try {
			server=HttpServer.create(new InetSocketAddress(port),10);
			server.setExecutor(Executors.newCachedThreadPool());
			for(HttpRequestHandler handler:handlers) {
				server.createContext(handler.getPath(),handler);
			}
			server.start();				
		} catch(Throwable t) {
			context.getLogger().error("Starting HTTP failed.",t);
		}
	}

	@Override
	public void shutdown(Context context) {
		if(server!=null) try {
			server.stop(0);
		} catch(Throwable t) {
			context.getLogger().warn("Stopping HTTP failed.", t);
		}
	}
}
