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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.nlu.NLUIntent;

/**
 * Handler for script execution. Intent and command can be set. Additionally, a list of slots
 * can be specified. In this case, the value of the slot (or an empty string if not set) is
 * added to the commandline.
 * <br>
 * The handler supports to special log lines:
 * <ul>
 * <li> {@code ###reply(:|[true]|[false|)} sends the end of the line as a reply to the sender of the
 * intent. In the case  {@code ###reply[true]}, the {@code senderSupportsAudio} flag is set to {@code true},
 * otherwise to {@code false}.
 * <li> {@code ###terminate} forces the handler to destroy the process immediately.
 * </ul>
 *  
 * @author notalexa
 */
public class ExecHandler implements IntentHandler {
	private static Logger LOGGER=LoggerFactory.getLogger(ExecHandler.class);
	@JsonProperty(required = true) String intent;
	@JsonProperty(required = true) String cmd;
	@JsonProperty String[] slots;

	public ExecHandler() {
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		if(intent.getIntent().equals(this.intent)) {
			String[] cmdLine=new String[1+(slots==null?0:slots.length)];
			cmdLine[0]=cmd;
			int index=1;
			if(slots!=null) for(String slot:slots) {
				Slot s=intent.getSlot(slot);
				cmdLine[index++]=s==null?"":s.getValue();
			}
			List<String> reply=new ArrayList<String>();
			AtomicBoolean senderSupportsAudio=new AtomicBoolean(false);
			if(execute(cmdLine,reply,senderSupportsAudio)) {
				if(reply.size()>0) try {
					intent.reply(api, senderSupportsAudio.get(),String.join(" ", reply));
				} catch(Throwable t) {
					LOGGER.error("Execution of {} failed.",cmd,t);
				}
			}
			return true;
		}
		return false;
	}

	protected boolean execute(String[] cmdLine,List<String> reply,AtomicBoolean senderSupportsAudio) {
		try {
			reply.clear();
			LOGGER.info("Execute {}",cmd);
			Process proc=Runtime.getRuntime().exec(cmd);
			try(BufferedReader out=new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
				new Thread("stdout") {
					{
						setDaemon(true);
					}
					@Override
					public void run() {
						try {
							String line;
							while((line=out.readLine())!=null) {
								if(line.startsWith("###reply")) {
									line=line.substring("###reply".length());
									if(line.startsWith(":")) {
										line=line.substring(1);
									} else if(line.startsWith("[false]")) {
										line=line.substring("[false]".length());
									} else if(line.startsWith("[true]")) {
										line=line.substring("[true]".length());
										senderSupportsAudio.set(true);
									}
									reply.add(line.trim());
								} else if(line.trim().equals("###terminate")) {
									LOGGER.info("Termination of {} forced.",cmd);
									proc.destroyForcibly();
								}
							}
						} catch(Throwable t) {
						}
					}
				}.start();
				proc.waitFor();
				Thread.sleep(250);
			}
			LOGGER.info("Process {} terminated",cmd);
			if(proc.exitValue()==0) {
				return true;
			} else {
				LOGGER.warn("Excecution of {} failed with return code {}.",cmd,proc.exitValue());
			}
		} catch(Throwable t) {
			return false;
		}
		return false;
	}

}
