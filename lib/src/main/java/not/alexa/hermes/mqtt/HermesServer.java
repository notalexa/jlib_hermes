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
package not.alexa.hermes.mqtt;

import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.MqttClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.Feature;
import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesComponent;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.utils.BaseUtils;

/**
 * Base class to use for serves providing hermes services. On
 * {@link #startup(Context)}, the server connects to a mqtt broker and
 * installs the provided components.
 * <br>The {@link #main(String[])} method expects a yaml (or json) file as startup configuration.
 * A configuration may look like
 * <pre>
 * --- @expand
 * uri: tcp://${MQTT_HOST}:1883
 * components:
 * - class: not.alexa.hermes.intent.handling.Stack
 *   handlers: 
 *   - class: not.alexa.hermes.intent.handling.LogHandler
 *   - class: not.alexa.hermes.media.HermesPlayer
 *     player:
 *       class: not.alexa.hermes.media.players.AudioPlayers
 *       players:
 *       - class: not.alexa.hermes.media.players.JukeBox
 *         name: jukebox
 *         baseDir: ${user.home}/Music
 *       - class: not.alexa.hermes.media.players.Tuner
 *   - class: not.alexa.hermes.intent.handling.DateTimeHandler
 *   - class: not.alexa.hermes.media.pulseaudio.PulseAudioHandler
 *     channels:
 *     - key: ${DEFAULT_SINK:}
 *       value: defaultchannel 
 *   - class: not.alexa.hermes.http.HttpHandler
 *     port: 8080
 *     handlers:
 *     - class: not.alexa.hermes.http.DirectoryHandler
 *       path: /profile/
 *       dir: ${user.dir}/www
 * - class: not.alexa.hermes.service.tts.TTS
 *   defaultLanguage: ${TTS_LOCALE:de-DE}
 *   languages:
 *   - en-US
 *   - en-GB
 *   - de-DE
 *   - es-ES
 *   - fr-FR
 *   - it-IT
 * </pre>
 * with environment variables {@code MQTT_HOST}, {@code DEFAULT_SINK}, {@code TTS_LOCALE}.
 * (and an tts script {@code tts.sh}
 * The various parameters are documents in the corresponding classes.
 * 
 */
public class HermesServer implements AutoCloseable {
	@JsonProperty String uri;
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty HermesComponent[] components;
	
	private MqttClient client;
	private HermesApi api;
	
	protected HermesServer() {
	}
	
	/**
	 * Instantiate the server
	 * @param uri the mqtt uri
	 * @param siteId the site id of this server
	 * @param components the components to install
	 */
	public HermesServer(String uri,String siteId,HermesComponent...components) {
		this.uri=uri;
		this.siteId=siteId;
		this.components=components;
	}
	
	/**
	 * Startup this server.
	 * 
	 * @param context the base context to use
	 * @throws BaseException if an error occurs
	 */
	public void startup(Context context) throws BaseException {
		client=HermesMqtt.createClient(uri);
		HermesComponent[] components=Arrays.copyOf(this.components,this.components.length+1);
		components[components.length-1]=Feature.getFeatureComponent();
		HermesMqtt api=new HermesMqtt(context, siteId, components);
		try {
			client.connect();
			api.subscribeTo(client);
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
		this.api=api;
	}
	
	/**
	 * Shutdown this server.
	 * 
	 * @throws BaseException if an error occurs
	 */
	public void shutdown() throws BaseException {
		try {
			close();
		} catch(Exception e) {
			BaseException.throwException(e);
		}
	}

	
	@Override
	public void close() throws Exception {
		if(client!=null) try {
			client.disconnect();
			client.close();
		} finally {
			client=null;
		}
		api=null;
	}

	/**
	 * Instantiate and startup the server. The method expects one argument:
	 * A file or URL pointing to a (yaml or json) startup configuration
	 * 
	 * @param args the arguments for startup
	 * @throws Exception if an error occurs
	 */
	public static void main(String[] args) throws Exception {
		Context context=Context.createRootContext();
		try(HermesServer instance=BaseUtils.resolveStartupInstance(context, HermesServer.class, args)) {
			instance.startup(context);
			Thread.sleep(Long.MAX_VALUE);
		}
	}
}
