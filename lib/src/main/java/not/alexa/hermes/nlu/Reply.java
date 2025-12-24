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
package not.alexa.hermes.nlu;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.audio.AudioPlayBytes;

/**
 * A reply for an intent. This is new and used to send text back to the caller.
 * <p>A client should
 * <ul>
 * <li>set an initial slot with entity {@code reply-to} and a unique id as value in the query
 * (using {@link Query.Builder#addInitialSlot(not.alexa.hermes.HermesApi.Slot)}) and
 * <li>define an overlay of {@link Reply} handling the answer by checking the {@link #getCallerId()}
 * and using the text (sending it to a TTS service for example).
 * </ul>
 * A {@link #senderSupportsAudio} value of <code>true</code> indicates, that the sender is able to play bytes using {@link AudioPlayBytes}.
 * In this case, the receiver of the reply <i>may</i> send an audio version of the text back to the sender of this message.
 * <br>
 * If the client doesn't set a {@code reply-to}, the base implementation in {@link NLUIntent#reply(not.alexa.hermes.HermesApi, String)}
 * sends the string to the tts service of the same site (assuming that this service is present).
 */
public class Reply implements HermesMessage<Reply> {
	@JsonProperty(defaultValue = "default") String siteId;
	@JsonProperty String callerId;
	@JsonProperty(defaultValue = "true") boolean senderSupportsAudio=true;
	@JsonProperty String text;
	protected Reply() {}
	public Reply(String siteId,String callerId,boolean senderSupportsAudio,String text) {
		this.text=text;
		this.callerId=callerId;
		this.senderSupportsAudio=senderSupportsAudio;
		this.siteId=siteId;
	}

	@Override
	public String getTopic() {
		return "rhasspy/handle/reply";
	}

	public String getCallerId() {
		return callerId;
	}

	public String getSiteId() {
		return siteId;
	}
	
	/**
	 * Indicates, that the sender of this reply supports binary messages of type {@link AudioPlayBytes}.
	 * @return {@code true} if the sender is able to play audio
	 */
	public boolean doesSenderSupportAudio() {
		return senderSupportsAudio;
	}

	public String getText() {
		return text;
	}
}
