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
package not.alexa.hermes.media.pulseaudio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.intent.handling.IntentHandler;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.netobjects.Context;

/**
 * Intent handler for {@code mediaplayer/output} which allows to change a pulse audio sink. Bluetooth devices providing an {@code a2dp} profile are automatically added
 * if the device name is not found in the current list and the bluetooth device is paired and trusted. All other devices must be declared statically in the configuration.
 * <br>Devices are selected from the value of the {@code device} slot. On a raspberry pi with a hifiberry DAC+, we configured statically the hifiberry card with value {@code default} 
 * and key {@code alsa_output.platform-soc_sound.stereo-fallback}.
 * A (paired and trusted) Teufel ROCKSTER Cross blaster can be selected as {@code teufel rockster cross}, a Qudelix 5K bluetooth headphone amplifier as {@¢ode qudelix-5k}.
 * <br>If the bluetooth device is not yet connected, the implementation tries to connect for the next minute. Typically, turning the device on initiates the
 * connection.
 * 
 * @author notalexa
 *  
 */
public class PulseAudioHandler implements IntentHandler {
	private static final Logger LOGGER=LoggerFactory.getLogger(PulseAudioHandler.class);
	@JsonProperty private Map<String,String> sinks;
	private Map<String,String> resolvedSinks=new HashMap<>();
	private Semaphore selectSemaphore=new Semaphore(1);
	private AtomicInteger connectId=new AtomicInteger();

	public PulseAudioHandler() {
	}
	
	public PulseAudioHandler(Map<String,String> sinks) {
		this.sinks=sinks;
	}
	
	protected void addBluetoothDevices(Map<String,String> channels) {
		List<String> output=new ArrayList<>();
		if(execute("bluetoothctl devices",output)) for(String line:output) {
			StringTokenizer tokenizer=new StringTokenizer(line);
			if(tokenizer.countTokens()>=3) {
				tokenizer.nextToken();
				String ip=tokenizer.nextToken();
				if(isAdmissable(ip)) {
					String descr=tokenizer.nextToken();
					while(tokenizer.hasMoreTokens()) {
						descr+=" "+tokenizer.nextToken();
					}
					channels.put("bluez_sink."+ip.replace(':','_')+".a2dp_sink",descr);
				}
			}
		}
	}
	
	private String ip(String name) {
		if(name.startsWith("bluez_sink.")) {
			return name.substring("bluez_sink.".length(),name.lastIndexOf('.')).replace('_',':');
		}
		return null;
	}
	
	protected boolean isConnected(String name) {
		String ip=ip(name);
		List<String> output=new ArrayList<>();
		if(ip!=null) {
			if(execute("bluetoothctl info "+ip,output)) for(String s:output) {
				if(s.indexOf("Connected: yes")>=0) {
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	protected boolean connect(String name) {
		String ip=ip(name);
		if(ip!=null) {
			int id=connectId.incrementAndGet();
			int retry=0;
			boolean connected=false;
			while(id==connectId.get()&&retry<30&&!(connected=isConnected(name))) try {
				retry++;
				if(id==connectId.get()) {
					execute("bluetoothctl connect "+ip,new ArrayList<>());
				}
				Thread.sleep(2000);
			} catch(Throwable t) {
				return false;
			}
			if(connected&&retry>0) try {
				LOGGER.info("Connect succeeded after retry. Wait 10sec to settle...");
				Thread.sleep(10000);
			} catch(Throwable t) {
			}
			return connected;
		}
		return true;		
	}
	
	public boolean select(String name) {
		try {
			if(selectSemaphore.tryAcquire(30,TimeUnit.SECONDS)) try {
				if(connect(name)) {
					return execute("pactl set-default-sink "+name,new ArrayList<>());
				}
			} finally {
				selectSemaphore.release();
			} else {
				LOGGER.warn("Failed to acquire access to set {}",name);
			}
		} catch(Throwable t) {
		}
		return false;
	}
	
	public PulseAudioHandler update() {
		Map<String,String> newSinks=sinks==null?new HashMap<>():new HashMap<>(sinks);
		addBluetoothDevices(newSinks);
		resolvedSinks=newSinks;
		return this;
	}
	
	protected boolean isAdmissable(String ip) {
		List<String> output=new ArrayList<>();
		boolean paired=false;
		boolean trusted=false;
		boolean profile=false;
		if(execute("bluetoothctl info "+ip,output)) for(String s:output) {
			paired|=s.indexOf("Paired: yes")>=0;
			trusted|=s.indexOf("Trusted: yes")>=0;
			profile|=s.indexOf("0000110b-0000-1000-8000-00805f9b34fb")>=0;
		}
		return paired&&trusted&&profile;
	}
	
	protected boolean execute(String cmd,List<String> output) {
		try {
			output.clear();
			Process proc=Runtime.getRuntime().exec(cmd);
			BufferedReader out=new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while((line=out.readLine())!=null) {
				output.add(line);
			}
			proc.waitFor();
			if(proc.exitValue()==0) {
				return true;
			} else {
				LOGGER.warn("Excecution of {} failed with return code {}.",cmd,proc.exitValue());
				for(String s:output) {
					System.out.println(s);
				}
			}
		} catch(Throwable t) {
			return false;
		}
		return false;
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		if("mediaplayer/output".equals(intent.getIntent())) {
			Slot slot=intent.getSlot("device");
			if(slot!=null) {
				new Thread(() -> {
					connectId.incrementAndGet();
					for(int i=0;i<2;i++) {
						for(Map.Entry<String,String> entry:resolvedSinks.entrySet()) {
							if(entry.getValue().equalsIgnoreCase(slot.getValue())) {
								LOGGER.info("Changed to {}: {}",entry.getValue(),select(entry.getKey()));
								return;
							}
						}
						if(i==0) {
							update();
						} else {
							LOGGER.warn("Output {} cannot resolved to a sink.",slot.getValue());
						}
					}
				}).start();
			} else {
				LOGGER.warn("Device slot not set.");
			}
			return true;
		}
		return false;
	}

	@Override
	public void startup(HermesApi api,Context context) {
		IntentHandler.super.startup(api,context);
	}

	@Override
	public void shutdown(HermesApi api,Context context) {
		IntentHandler.super.shutdown(api,context);
	}
}
