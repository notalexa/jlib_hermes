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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.net.httpserver.*;

import not.alexa.hermes.http.HttpHandler.HttpRequestHandler;

/**
 * A request handler serving data from directories. No {@code ETag} is provided and
 * no caching is done.
 * An example for configuration is
 * <pre>
 * - class: not.alexa.hermes.http.DirectoryHandler
 *   path: /docs/
 *   dir: ${user.dir}/www
 * </pre>
 * resolving content from the base directory {@code www} below the users
 * directory using the complete path.
 * @author notalexa
 */
public class DirectoryHandler implements HttpRequestHandler {
	@JsonProperty(required = true) String path;
	@JsonProperty(required = true) String dir;
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try(InputStream in=new FileInputStream(dir+exchange.getRequestURI().getPath())) {
			exchange.sendResponseHeaders(200,0);
			try(OutputStream out=exchange.getResponseBody()) {
				byte[] buffer=new byte[2048];
				int n;
				while((n=in.read(buffer))>=0) {
					out.write(buffer,0,n);
				}
			}
		} catch(Throwable t) {
			exchange.sendResponseHeaders(404,-1);
		}
	}
	
	@Override
	public String getPath() {
		return path;
	}
}