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
import java.util.Set;

import not.alexa.hermes.HermesApi.FeaturesRequestHandler;
import not.alexa.hermes.asr.ASRError;
import not.alexa.hermes.asr.AudioCaptured;
import not.alexa.hermes.asr.TextCaptured;
import not.alexa.hermes.audio.AudioDevices;
import not.alexa.hermes.audio.AudioDevicesRequest;
import not.alexa.hermes.audio.AudioPlayError;
import not.alexa.hermes.audio.AudioRecordError;
import not.alexa.hermes.audio.AudioSetVolume;
import not.alexa.hermes.audio.AudioToggleOff;
import not.alexa.hermes.audio.AudioToggleOn;
import not.alexa.hermes.dialogue.Configure;
import not.alexa.hermes.dialogue.ContinueSession;
import not.alexa.hermes.dialogue.DialogueError;
import not.alexa.hermes.dialogue.EndSession;
import not.alexa.hermes.dialogue.IntentNotRecognized;
import not.alexa.hermes.dialogue.SessionEnded;
import not.alexa.hermes.dialogue.SessionQueued;
import not.alexa.hermes.dialogue.SessionStarted;
import not.alexa.hermes.dialogue.StartSession;
import not.alexa.hermes.features.FeaturesRequest;
import not.alexa.hermes.g2p.G2PError;
import not.alexa.hermes.g2p.Phonemes;
import not.alexa.hermes.g2p.PhonemesRequest;
import not.alexa.hermes.hotword.HotwordError;
import not.alexa.hermes.hotword.Hotwords;
import not.alexa.hermes.hotword.HotwordsRequest;
import not.alexa.hermes.intent.handling.ToggleOff;
import not.alexa.hermes.intent.handling.ToggleOn;
import not.alexa.hermes.nlu.NLUError;
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
import not.alexa.hermes.nlu.Query;
import not.alexa.hermes.nlu.Train;
import not.alexa.hermes.tts.Say;
import not.alexa.hermes.tts.SayFinished;
import not.alexa.hermes.tts.TTSError;
import not.alexa.hermes.tts.Voices;
import not.alexa.hermes.tts.VoicesRequest;

public enum Feature {
	NoFeature(null,null),
	Features(new Entry[] {
		new Entry("rhasspy/features/features",not.alexa.hermes.features.Features.class)
	},new Entry[] {
		new Entry("rhasspy/features/getFeatures",FeaturesRequest.class)
	}),
	AudioInfo(new Entry[] {
		new Entry("rhasspy/audioServer/devices",AudioDevices.class)
	},new Entry[] {
		new Entry("rhasspy/audioServer/getDevices",AudioDevicesRequest.class)	
	}),
	AudioInput(new Entry[] {
		new Entry("hermes/audioServer/<siteId>/audioFrame",null),
		new Entry("hermes/audioServer/<siteId>/+/audioSessionFrame",null),			
		new Entry("hermes/error/audioServer/record",AudioRecordError.class),
	},new Entry[] {
	}),
	AudioOutput(new Entry[] {
		new Entry("hermes/audioServer/<siteId>/playFinished",null),
		new Entry("hermes/error/audioServer/play",AudioPlayError.class),
	},new Entry[] {
		new Entry("hermes/audioServer/toggleOn",AudioToggleOn.class),
		new Entry("hermes/audioServer/toggleOff",AudioToggleOff.class),
		new Entry("hermes/audioServer/setVolume",AudioSetVolume.class),
		new Entry("hermes/audioServer/<siteId>/playBytes/+",null),
		new Entry("hermes/error/audioServer/play",AudioPlayError.class),
	}),
	ASR(new Entry[] {
		new Entry("hermes/asr/textCaptured",TextCaptured.class),
		new Entry("hermes/asr/<siteId>/+/audioCaptured",AudioCaptured.class),
			
	},new Entry[] {
		new Entry("hermes/asr/toggleOn",not.alexa.hermes.asr.ToggleOn.class),
		new Entry("hermes/asr/toggleOff",not.alexa.hermes.asr.ToggleOff.class),
		new Entry("hermes/asr/startListening",not.alexa.hermes.asr.StartListening.class),
		new Entry("hermes/asr/stopListening",not.alexa.hermes.asr.StopListening.class),
	}),
	ASRTrain(new Entry[] {
		new Entry("rhasspy/asr/<siteId>/trainSuccess",null),
		new Entry("hermes/error/asr",ASRError.class),
	},new Entry[] {
		new Entry("rhasspy/asr/<siteId>/train",not.alexa.hermes.asr.Train.class),
		new Entry("hermes/error/asr",ASRError.class),
	}),
	NLU(new Entry[] {
		new Entry("hermes/intent/#",null),
		new Entry("hermes/nlu/intentNotRecognized",not.alexa.hermes.nlu.IntentNotRecognized.class),
	},new Entry[] {
		new Entry("hermes/nlu/query",Query.class),
	}),
	Reply(new Entry[] {
			new Entry("rhasspy/handle/reply",not.alexa.hermes.nlu.Reply.class),
		},new Entry[] {
		}),
	NLUTrain(new Entry[] {
		new Entry("rhasspy/nlu/<siteId>/trainSuccess",null),
		new Entry("hermes/error/nlu",NLUError.class),
	},new Entry[] {
		new Entry("rhasspy/nlu/<siteId>/train",Train.class),
		new Entry("hermes/error/nlu",NLUError.class),
	}),
	G2P(new Entry[] {
		new Entry("rhasspy/g2p/phonemes",Phonemes.class),
		new Entry("hermes/error/g2p",G2PError.class),
	},new Entry[] {
		new Entry("rhasspy/g2p/pronounce",PhonemesRequest.class),
		new Entry("hermes/error/g2p",G2PError.class),
	}),
	DialogueManager(new Entry[] {
		new Entry("hermes/dialogueManager/sessionStarted",SessionStarted.class),
		new Entry("hermes/dialogueManager/sessionQueued",SessionQueued.class),
		new Entry("hermes/dialogueManager/sessionEnded",SessionEnded.class),
		new Entry("hermes/dialogueManager/intentNotRecognized",IntentNotRecognized.class),
		new Entry("hermes/error/dialogueManager",DialogueError.class),
	},new Entry[] {
		new Entry("hermes/dialogueManager/startSession",StartSession.class),
		new Entry("hermes/dialogueManager/continueSession",ContinueSession.class),
		new Entry("hermes/dialogueManager/endSession",EndSession.class),
		new Entry("hermes/dialogueManager/configure",Configure.class),
		new Entry("hermes/error/dialogueManager",DialogueError.class),
	}),
	Hotword(new Entry[] {
		new Entry("hermes/hotword/+/detected",null),
		new Entry("hermes/error/hotword",HotwordError.class),
		new Entry("rhasspy/hotword/hotwords",Hotwords.class)
	},new Entry[] {
		new Entry("hermes/hotword/toggleOn",not.alexa.hermes.hotword.ToggleOn.class),
		new Entry("hermes/hotword/toggleOff",not.alexa.hermes.hotword.ToggleOff.class),
		new Entry("rhasspy/hotword/getHotwords",HotwordsRequest.class),
		new Entry("hermes/error/hotword",HotwordError.class),
	}),
	IntentHandling(new Entry[] {	
	},new Entry[] {
		new Entry("rhasspy/handle/toggleOn",ToggleOn.class),
		new Entry("rhasspy/handle/toggleOff",ToggleOff.class),
		new Entry("hermes/intent/#",null),
	}),
	TTS(new Entry[] {
		new Entry("hermes/tts/sayFinished",SayFinished.class),
		new Entry("rhasspy/tts/voices",Voices.class),
		new Entry("hermes/error/tts",TTSError.class)
	},new Entry[] {
		new Entry("hermes/tts/say",Say.class),
		new Entry("rhasspy/tts/getVoices",VoicesRequest.class),
		new Entry("hermes/error/tts",TTSError.class)
	});
	private Entry[] clientTopics;
	private Entry[] serverTopics;
	
	private Feature(Entry[] clientTopics,Entry[] serverTopics) {
		this.clientTopics=clientTopics;
		this.serverTopics=serverTopics;
	}
	
	public Feature initClient(String siteId,Set<String> topics,Map<String,Class<? extends HermesMessage<?>>> classMap) {
		return init(siteId,clientTopics,topics,classMap);
	}

	public Feature initServer(String siteId,Set<String> topics,Map<String,Class<? extends HermesMessage<?>>> classMap) {
		return siteId==null?NoFeature:init(siteId,serverTopics,topics,classMap);
	}
	
	public Feature init(String siteId,Entry[] featureTopics,Set<String> topics,Map<String,Class<? extends HermesMessage<?>>> classMap) {
		for(Entry topic:featureTopics) {
			String t=topic.topic.replace("<siteId>", siteId==null?"+":siteId);
			topics.add(t);
			if(topic.clazz!=null&&t.indexOf('+')<0&&t.indexOf('#')<0) {
				classMap.put(topic.topic,topic.clazz);
			}
		}
		return this;
	}
	
	private String findTopicInternal(Class<?> clazz) {
		if(clientTopics!=null) for(Entry entry:clientTopics) {
			if(clazz.equals(entry.clazz)) {
				return entry.topic;
			}
		}
		return null;
	}
	
	private static class Entry {
		String topic;
		Class<? extends HermesMessage<?>> clazz;
		private Entry(String topic,Class<? extends HermesMessage<?>> clazz) {
			this.topic=topic;
			this.clazz=clazz;
		}
	}
	
	public static String findTopic(Class<?> clazz) {
		for(Feature feature:values()) {
			String topic=feature.findTopicInternal(clazz);
			if(topic!=null) {
				return topic;
			}
		}
		return null;
	}

	/**
	 * 
	 * @return a hermes component representing handling feature requests
	 * @see FeaturesRequestHandler.class
	 */
	public static HermesComponent getFeatureComponent() {
		return new HermesComponent() {
			
			@Override
			public void configure(Map<String, Class<? extends HermesMessage<?>>> extensions, Map<Class<?>, Object> resources, List<Class<? extends HermesMessage<?>>> overlays) {
				overlays.add(FeaturesRequestHandler.class);
			}
		};
	}
}
