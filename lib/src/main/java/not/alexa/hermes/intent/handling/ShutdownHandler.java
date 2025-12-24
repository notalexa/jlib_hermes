/*
 * Copyright (C) 2025 Not Alexa
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

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.nlu.NLUIntent;

/**
 * Handler shutting down the server. Together with restart scripts, this handler can be used to restart the server.
 * <br>
 * The process is devided into two steps: At first call, the handler sends a reply to confirm (if not empty). At second call within 30sec, the handler replies with
 * a bye bye message (if not empty) and shotdown the server after the configured delay.
 * 
 * @author notalexa
 */
public class ShutdownHandler implements IntentHandler {
	@JsonProperty(defaultValue = "0") int exitCode=0;
	@JsonProperty(defaultValue = "4000") int shutdownDelay=4000;
	@JsonProperty(defaultValue = "Sag das noch mal") String confirmationText="Sag das noch mal";
	@JsonProperty(defaultValue = "Mach ich. Bis gleich.") String byebyeText="Mach ich. Bis gleich.";
	private long lastCall;

	public ShutdownHandler() {
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		try {
			if(intent.getIntent().contentEquals("shutdown")) {
				api.getContext().getLogger().info("Shutdown request received. Exit...");
				if(System.currentTimeMillis()-lastCall<30000||(confirmationText==null||confirmationText.isEmpty())) {
					api.getContext().getLogger().info("Really shutdown.");
					if(byebyeText!=null&&!byebyeText.isEmpty()) {
						intent.reply(api,byebyeText);
					}
					new Thread(() -> {
						try {
							Thread.sleep(shutdownDelay);
							System.exit(exitCode);
						} catch(Throwable t) {
						}
					}).start();
				} else {
					api.getContext().getLogger().info("Confirm shutdown request.");
					lastCall=System.currentTimeMillis();
					intent.reply(api,confirmationText);
				}
				return true;
			}
		} catch(Throwable t) {
			return true;
		}
		return false;	
	}
}
