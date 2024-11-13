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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.Device.Mode;
import not.alexa.hermes.asr.AudioCaptured;
import not.alexa.hermes.asr.TextCaptured;
import not.alexa.hermes.audio.AudioDevices;
import not.alexa.hermes.audio.AudioDevicesRequest;
import not.alexa.hermes.audio.AudioFrame;
import not.alexa.hermes.audio.AudioPlayBytes;
import not.alexa.hermes.audio.AudioPlayFinished;
import not.alexa.hermes.audio.AudioSessionFrame;
import not.alexa.hermes.audio.AudioToggleOff;
import not.alexa.hermes.audio.AudioToggleOn;
import not.alexa.hermes.dialogue.Configure;
import not.alexa.hermes.dialogue.SessionStarted;
import not.alexa.hermes.dialogue.StartSession;
import not.alexa.hermes.features.Features;
import not.alexa.hermes.features.FeaturesRequest;
import not.alexa.hermes.g2p.Phonemes;
import not.alexa.hermes.g2p.PhonemesRequest;
import not.alexa.hermes.hotword.HotwordDetected;
import not.alexa.hermes.hotword.Hotwords;
import not.alexa.hermes.hotword.HotwordsRequest;
import not.alexa.hermes.intent.handling.ToggleOff;
import not.alexa.hermes.intent.handling.ToggleOn;
import not.alexa.hermes.mqtt.HermesMqtt;
import not.alexa.hermes.nlu.IntentNotRecognized;
import not.alexa.hermes.nlu.NLUError;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.hermes.nlu.Query;
import not.alexa.hermes.nlu.Reply;
import not.alexa.hermes.nlu.Train;
import not.alexa.hermes.nlu.TrainSuccess;
import not.alexa.hermes.tts.Say;
import not.alexa.hermes.tts.SayFinished;
import not.alexa.hermes.tts.Voices;
import not.alexa.hermes.tts.VoicesRequest;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.TypeLoader;

/**
 * Implementation of the hermes API including all rhasspy extensions. For a documentation of the different messages, see <a href="https://rhasspy.readthedocs.io/en/latest/reference/#mqtt-api">the Rhasspy reference</a>.
 * The implementation includes another extension {@link FeaturesRequest} which can be used to obtain the currently available features in the
 * network (including the site ids). For a list of features see {@link Feature}.
 * <br>This class doesn't provide a mechanism of attaching to an MQTT instance. Instead, published messages are handled locally. To attach to a
 * MQTT instance, use {@link HermesMqtt}.
 * <p><b>Usage</b> follows the typical network object paradigm: Overlay the messages you want to implement and override the {@link HermesMessage#received(HermesApi)}
 * method. Define a type loader with the given overlays and create a context for this loader. In the context, register the resources, the overlays need.
 * Create an api instance with the context.
 * <br>This API defines one overlay {@link FeaturesRequestHandler} of {@link FeaturesRequest} which can be used to implement the {@link Feature#Features}
 * feature. An example for this would be
 * <pre>
 * Context context=new DefaultTypeLoader().overlay(FeaturesRequestHandler.class).createContext();
 * HermesApi api=new HermesMqtt(context,"default");
 * </pre>
 * (Note that implementing features <b>always requires a site id</b> while using a site doesn't.)
 * Another example can be found in the TTS implementation.
 * <p>
 * 
 * @author notalexa
 *
 */
public class HermesApi {
	public static final Slot[] NO_SLOTS=new Slot[0];
	public static final AsrToken[][] NO_ASR_TOKENS=new AsrToken[0][0];
	public static final Device[] NO_DEVICE=new Device[0];

	private YamlCodingScheme prototype=JsonCodingScheme.RESTRICTED_SCHEME.newBuilder().setIndent("","").build();
	private Map<Class<?>,CodingScheme> schemes=new HashMap<>();
	private Context context;
	protected String siteId;
	private Feature[] features;
	protected String[] topics;
	Map<String,Class<? extends HermesMessage<?>>> classMap=new HashMap<>();
	private Map<String,PendingAnswer<?>> pendingAnswers=new HashMap<>();
	private Set<Class<?>> loadedClasses=new HashSet<>();
	private Map<Class<? extends HermesMessage<?>>,Binary<?>> binaryPrototypes=new HashMap<>();
	private HermesComponent[] loadedComponents;
	protected Map<String,Class<? extends HermesMessage<?>>> extensions=new HashMap<>();
	private List<TopicMatcher> topicMatchers=new ArrayList<>();
	protected Map<String,Subscriber> subscribers=new HashMap<>();
	/**
	 * Create a context without a site id (typically a client).
	 * 
	 * @param parent the base context to use
	 * @param components the hermes components to use to handle messages
	 */
	public HermesApi(Context parent,HermesComponent...components) {
		this(parent,null,components);
	}

	/**
	 * Create a context with a site id (typically a server).
	 * 
	 * @param parent the context to use
	 * @param siteId the site id of this instance
	 * @param components the components to use to handle messages
	 */
	public HermesApi(Context parent,String siteId,HermesComponent...components) {
		context=makeContext(parent,components);
		this.siteId=siteId;
		context.putAdapter(HermesApi.class, this);
		init(context,components);
	}
	
	/**
	 * 
	 * @return a unique id
	 */
	public static String createId() {
		return UUID.randomUUID().toString();
	}
	
	private Context makeContext(Context parent,HermesComponent...components) {
		if(components.length>0) {
			Map<Class<?>,Object> resources=new HashMap<>();
			List<Class<? extends HermesMessage<?>>> overlays=new ArrayList<>();
			for(HermesComponent component:components) {
				component.configure(extensions,resources, overlays);
			}
			TypeLoader loader=parent.getTypeLoader().overlay(overlays.toArray(new Class[0]));
			Context context=new Context.Default(parent) {

				@Override
				public TypeLoader getTypeLoader() {
					return loader;
				}
			};
			for(Map.Entry<Class<?>,Object> entry:resources.entrySet()) {
				context.putAdapter(entry.getKey(),entry.getValue());
			}
			return context;
		} else {
			return parent;
		}
	}
	
	/**
	 * 
	 * @return the site id
	 */
	public String getSiteId() {
		return siteId;
	}

	/**
	 * 
	 * @return the context of this api
	 */
	public Context getContext() {
		return context;
	}

	
	protected void init(Context context,HermesComponent[] components) {
		loadedComponents=components;
		Set<Feature> features=new HashSet<>();
		Set<String> topics=new HashSet<>();
		TypeLoader loader=context.getTypeLoader();
		for(Map.Entry<String,Class<? extends HermesMessage<?>>> extension:extensions.entrySet()) {
			if(loader.hasOverlays(extension.getValue())) {
				topics.add(extension.getKey());
				if(extension.getKey().indexOf('+')<0&&extension.getKey().indexOf('#')<0) {
					classMap.put(extension.getKey(), extension.getValue());
				} else {
					topicMatchers.add(new TopicMatcher(extension.getKey(),extension.getValue()));
				}
			}
		}
		if(siteId!=null&&loader.hasOverlays(FeaturesRequest.class)) {
			features.add(Feature.Features.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(not.alexa.hermes.features.Features.class)) {
			Feature.Features.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(VoicesRequest.class)||loader.hasOverlays(Say.class)) {
			features.add(Feature.TTS.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(SayFinished.class)||loader.hasOverlays(Voices.class)) {
			Feature.TTS.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(ToggleOn.class)&&loader.hasOverlays(ToggleOff.class)) {
			features.add(Feature.IntentHandling.initServer(siteId,topics, classMap));
		}
		if(siteId!=null&&loader.hasOverlays(not.alexa.hermes.hotword.ToggleOn.class)&&loader.hasOverlays(not.alexa.hermes.hotword.ToggleOff.class)) {
			features.add(Feature.Hotword.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(HotwordDetected.class)) {
			Feature.Hotword.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(StartSession.class)||loader.hasOverlays(Configure.class)) {
			features.add(Feature.DialogueManager.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(SessionStarted.class)) {
			Feature.DialogueManager.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(PhonemesRequest.class)) {
			features.add(Feature.G2P.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(Phonemes.class)) {
			Feature.G2P.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(Query.class)) {
			features.add(Feature.NLU.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(NLUIntent.class)||loader.hasOverlays(IntentNotRecognized.class)) {
			Feature.NLU.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(Train.class)) {
			features.add(Feature.NLUTrain.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(TrainSuccess.class)) {
			Feature.NLUTrain.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(not.alexa.hermes.asr.ToggleOn.class)&&loader.hasOverlays(not.alexa.hermes.asr.ToggleOff.class)) {
			features.add(Feature.ASR.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(Reply.class)) {
			Feature.Reply.initClient(siteId,topics, classMap);
		}
		if(loader.hasOverlays(TextCaptured.class)||loader.hasOverlays(AudioCaptured.class)) {
			Feature.ASR.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(not.alexa.hermes.asr.Train.class)) {
			features.add(Feature.ASRTrain.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(not.alexa.hermes.asr.TrainSuccess.class)) {
			Feature.ASRTrain.initClient(siteId,topics, classMap);
		}
		if(loader.hasOverlays(AudioFrame.class)||loader.hasOverlays(AudioSessionFrame.class)) {
			Feature.AudioInput.initClient(siteId,topics, classMap);
		}
		if(loader.hasOverlays(AudioPlayFinished.class)) {
			Feature.AudioOutput.initClient(siteId,topics, classMap);
		}
		if(siteId!=null&&loader.hasOverlays(AudioToggleOn.class)&&loader.hasOverlays(AudioToggleOff.class)) {
			features.add(Feature.AudioOutput.initServer(siteId,topics, classMap));
		}
		if(siteId!=null&&loader.hasOverlays(AudioDevicesRequest.class)) {
			features.add(Feature.AudioInfo.initServer(siteId,topics, classMap));
		}
		if(loader.hasOverlays(AudioDevices.class)) {
			Feature.AudioInfo.initClient(siteId,topics, classMap);
		}
		features.remove(Feature.NoFeature);
		this.features=features.toArray(new Feature[features.size()]);
		this.topics=topics.toArray(new String[topics.size()]);
		this.loadedClasses.addAll(classMap.values());
		for(HermesComponent component:components) {
			component.startup(this,context);
		}
	}
	
	protected void received(String topic,byte[] data) throws BaseException, IllegalTopicException {
		Class<? extends HermesMessage<?>> hermesClass=classMap.get(topic);
		HermesMessage<?> msg=null;
		if(hermesClass==null) {
			if(topic.startsWith("hermes/intent/")) {
				hermesClass=NLUIntent.class;
			} else if(topic.startsWith("hermes/hotword/")&&topic.endsWith("/detected")) {
				hermesClass=HotwordDetected.class;
			} else if(topic.startsWith("rhasspy/nlu/")&&topic.endsWith("/trainSuccess")) {
				hermesClass=TrainSuccess.class;
			} else if(topic.startsWith("rhasspy/asr/")&&topic.endsWith("/trainSuccess")) {
				hermesClass=not.alexa.hermes.asr.TrainSuccess.class;
			} else if(topic.startsWith("hermes/asr/")&&topic.endsWith("/audioCaptured")) {
				receivedBinary(topic,AudioCaptured.class,data);
			} else if(topic.startsWith("hermes/audioServer")) {
				if(topic.endsWith("/playFinished")) {
					hermesClass=AudioPlayFinished.class;
				} else if(topic.indexOf("/playBytes/")>0) {
					receivedBinary(topic, AudioPlayBytes.class, data);
				} else if(topic.endsWith("/audioFrame")) {
					receivedBinary(topic, AudioFrame.class, data);
				} else if(topic.endsWith("/audioSessionFrame")) {
					receivedBinary(topic, AudioSessionFrame.class, data);
				}
			} else for(TopicMatcher matcher:topicMatchers) {
				if(matcher.matches(topic)) {
					hermesClass=matcher.clazz;
					break;
				}
			}
		}
		if(hermesClass!=null) {
			msg=getScheme(hermesClass).createDecoder(context, data).decode(hermesClass);
		}
		if(msg!=null) {
			received(msg.forTopic(topic));
		}
	}
	
	/**
	 * Check if the provided site id matches this site id.
	 * 
	 * @param siteId the provided site id
	 * @return {@code true} if the id matches (that is either the provided id is {@code null} or equals this id)
	 */
	public boolean matches(String siteId) {
		return siteId==null||siteId.equals(this.siteId);
	}
	
	private void receivedBinary(String topic, Class<? extends HermesMessage<?>> clazz, byte[] data) throws BaseException,IllegalTopicException {
		Binary<?> prototype=binaryPrototypes.computeIfAbsent(clazz,(c)->{
			try {
				return getScheme(clazz).createDecoder(context,"{}".getBytes()).decode(Binary.class);
			} catch(Throwable t) {
				return null;
			}
		});
		if(prototype!=null) {
			prototype=prototype.forData(data);
			received(prototype.forData(data).forTopic(topic));
		}
	}

	protected void received(HermesMessage<?> msg) throws BaseException {
		if(msg instanceof RequestAnswer) {
			String id=((RequestAnswer)msg).getId();
			PendingAnswer<?> answer=pendingAnswers.get(id);
			if(answer!=null) {
				answer.set(msg);
			}
		}
		msg.received(this);
	}
	
	private synchronized CodingScheme getScheme(@SuppressWarnings("rawtypes") Class<? extends HermesMessage> clazz) {
		CodingScheme scheme=schemes.get(clazz);
		if(scheme==null) {
			scheme=prototype.newBuilder().setRootType(clazz).build();
			schemes.put(clazz, scheme);
		}
		return scheme;
	}
	
	/**
	 * Encode a message (into JSON if JSON or return the payload if binary).
	 * 
	 * @param msg the message to encode
	 * @return the encoded message
	 * @throws BaseException if an error occurs
	 */
	public byte[] encode(HermesMessage<?> msg) throws BaseException {
		if(msg instanceof Binary) {
			return ((Binary<?>)msg).encoded();
		}
		return getScheme(msg.getClass()).createEncoder(context).encode(msg).asBytes();
	}
	
	/**
	 * Publish a message. The default implemenation publishes the message to this instance providing a loop back.
	 * 
	 * @param msg the message to publish
	 * @throws BaseException if an error occurs
	 */
	public void publish(HermesMessage<?> msg) throws BaseException {
		publish(msg.getTopic(),encode(msg));
	}

	/**
	 * Publish a message to a specific topic.
	 * 
	 * @param topic the topic
	 * @param msg  the message to publish
	 * @throws BaseException if an error occurs
	 */
	public void publish(String topic, byte[] msg) throws BaseException  {
		try {
			received(topic,msg);
		} catch(IllegalTopicException e) {
			BaseException.throwException(e);
		}
	}

	/**
	 * Convenience method to query an NLU for the given input on the default site.
	 * 
	 * @param input the input
	 * @throws BaseException if an error occurs
	 */
	public void query(String input) throws BaseException {
		query(input,"default");
	}

	/**
	 * Convenience method to query an NLU for the given input on the given site.
	 * 
	 * @param input the input
	 * @param siteId the site id
	 * @throws BaseException if an error occurs
	 */
	public void query(String input,String siteId) throws BaseException {
		Query.createBuilder(input).setSiteId(siteId).build().publish(this);
	}

	/**
	 * Convenience method to query the voices at the default site.
	 * 
	 * @return the voices on the site
	 * @throws BaseException if an error occurs
	 */
	public Voices getVoices() throws BaseException {
		return getVoices("default");
	}

	/**
	 * Convenience method to query the voices at the given site.
	 * 
	 * @param siteId the site id
	 * @return the voices on the site
	 * @throws BaseException if an error occurs
	 */
	public Voices getVoices(String siteId) throws BaseException {
		return new VoicesRequest(siteId).getVoices(this);
	}

	/**
	 * Convenience method to query the audio devices at the default site (both input and output).
	 * 
	 * @return the audiodevices on the site
	 * @throws BaseException if an error occurs
	 */
	public AudioDevices getDevices() throws BaseException {
		return getDevices("default");
	}

	/**
	 * Convenience method to query the audio devices at the given site (both input and output).
	 * 
	 * @param siteId the site id
	 * @return the audiodevices on the site
	 * @throws BaseException if an error occurs
	 */
	public AudioDevices getDevices(String siteId) throws BaseException {
		return new AudioDevicesRequest(Mode.values(),siteId,false).getDevices(this);
	}

	/**
	 * Convenience method to query the hotwords at the default site.
	 * 
	 * @return the hotwords on the site
	 * @throws BaseException if an error occurs
	 */
	public Hotwords getHotwords() throws BaseException {
		return getHotwords("default");
	}

	/**
	 * Convenience method to query the hotwords at the default site.
	 * 
	 * @param siteId the site id
	 * @return the hotwords on the site
	 * @throws BaseException if an error occurs
	 */
	public Hotwords getHotwords(String siteId) throws BaseException {
		return new HotwordsRequest(siteId).getHotwords(this);
	}

	/**
	 * Convenience method to query the phonemes of one word at the default site.
	 * 
	 * @param word the word
	 * @return the phonemes of the word
	 * @throws BaseException if an error occurs
	 */
	public Phonemes getPhonemes(String word) throws BaseException {
		return getPhonemes(new String[] {word});
	}

	/**
	 * Convenience method to query the phonemes of words at the default site.
	 * 
	 * @param words the words
	 * @return the phonemes of the words
	 * @throws BaseException if an error occurs
	 */
	public Phonemes getPhonemes(String[] words) throws BaseException {
		return getPhonemes("default",words);
	}

	/**
	 * Convenience method to query the phonemes of words at the given site.
	 * 
	 * @param siteId the site id
	 * @param words the words
	 * @return the phonemes of the words
	 * @throws BaseException if an error occurs
	 */
	public Phonemes getPhonemes(String siteId,String[] words) throws BaseException {
		return new PhonemesRequest(words,null,5,siteId,null).getPhonemes(this);
	}

	protected void addTopic(String topic) {
		String[] tmp=Arrays.copyOf(topics,topics.length+1);
		tmp[topics.length]=topic;
		topics=tmp;
	}
	
	/**
	 * Add a topic with the given subscriber
	 * 
	 * @param topic the topic to add
	 * @param subscriber the subscriber for this topic
	 */
	public void addTopic(String topic,Subscriber subscriber) {
		if(!subscribers.containsKey(topic)) {
			subscribers.put(topic, subscriber);
			addTopic(topic);
		}
	}

	/**
	 * Method to publish a hermes message expecting an answer. A use case can be a {@link FeaturesRequest} with an expected {@link Features}
	 * answer. This is not provided in this API since features for a site can be implemented on different servers implying multiple answers to one
	 * request.
	 * 
	 * @param <R> the type of the request
	 * @param <T> the type of the answer
	 * @param msg the request
	 * @param answerType the class of the answer
	 * @return the answer to the request
	 * @throws BaseException if an error occurs (especially a timeout error after 0.5 seconds).
	 * 
	 */
	public <R extends HermesRequest&HermesMessage<?>, T extends RequestAnswer&HermesMessage<?>> T publishForAnswer(R msg,Class<T> answerType) throws BaseException {
		String id=createId();
		((HermesRequest)msg).setId(id);
		if(!loadedClasses.contains(answerType)) {
			String topic=Feature.findTopic(answerType);
			if(topic!=null) {
				classMap.put(topic, answerType);
				loadedClasses.add(answerType);
				addTopic(topic);
			} else {
				throw new BaseException(BaseException.BAD_REQUEST,answerType.getName()+" has no registered topic");
			}
		}
		PendingAnswer<T> answer;
		pendingAnswers.put(id, answer=new PendingAnswer<T>(msg));
		publish(msg);
		return answer.await();
	}

	/**
	 * Class used in the NLU section
	 * 
	 * @author notalexa
	 *
	 */
	public static class Slot {
		@JsonProperty(required = true) protected String entity;
		@JsonProperty(required = true) protected String slotName;
		@JsonProperty(required = true) protected String rawValue;
		@JsonProperty(required = true) protected Value value;
		@JsonProperty protected Range range;
		
		Slot() {}
		
		public Slot(String name,String value) {
			this.entity=this.slotName=name;
			this.value=new Value(this.rawValue=value);
		}
		
		public Slot(String entity,String slotName,String rawValue,String value,Range range) {
			this.entity=entity;
			this.slotName=slotName;
			this.rawValue=rawValue;
			this.value=new Value(value);
			this.range=range;
		}
		
		public String getEntity() {
			return entity;
		}

		public String getSlotName() {
			return slotName;
		}

		public String getRawValue() {
			return rawValue;
		}

		public String getValue() {
			return value.getValue();
		}

		public Range getRange() {
			return range;
		}

		public static class Value {
			@JsonProperty(required = true) protected String value;
			Value() {}
			public Value(String value) {
				this.value=value;
			}
			
			public String getValue() {
				return value;
			}
		}
		
		public static class Range {
			@JsonProperty(required = true) protected int start;
			@JsonProperty(required = true) protected int end;
			Range() {}
			public Range(int start,int end) {
				this.start=start;
				this.end=end;
			}
			public int getStart() {
				return start;
			}
			public int getEnd() {
				return end;
			}
		}
	}
	
	/**
	 * Class used in the NLU section
	 * 
	 * @author notalexa
	 *
	 */
	public static class Intent {
		@JsonProperty(required = true) protected String intentName;
		@JsonProperty(required = true) protected float confidenceScore=1f;
		Intent() {}
		public Intent(String intentName,float confidenceScore) {
			this.intentName=intentName;
			this.confidenceScore=confidenceScore;
		}
		public String getIntentName() {
			return intentName;
		}
		public float getConfidenceScore() {
			return confidenceScore;
		}
	}
	
	/**
	 * Class used in the NLU section
	 * 
	 * @author notalexa
	 *
	 */
	public static class AsrToken {
		@JsonProperty(required = true) String value;
		@JsonProperty(required = true) float confidence;
		@JsonProperty(value="range_start",required = true) int start;
		@JsonProperty(value="range_end",required = true) int end;

		@JsonCreator
		public AsrToken(@JsonProperty("value") String value,@JsonProperty("confidence") float confidence,@JsonProperty("range_start") int start,@JsonProperty("range_end") int end) {
			this.value=value;
			this.confidence=confidence;
			this.start=start;
			this.end=end;
		}
		
		public String getValue() {
			return value;
		}
		
		public float getConfidence() {
			return confidence;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}
	}
	
	/**
	 * Class representing binary data (for example {@link AudioFrame}.
	 * 
	 * @author notalexa
	 *
	 * @param <T> the type of the message
	 */
	public abstract static class Binary<T extends HermesMessage<T>> implements HermesMessage<T> {
		protected byte[] data;
		public Binary(byte[] data) {
			this.data=data;
		}
		
		public byte[] encoded() {
			return data;
		}
		
		public Binary<T> forData(byte[] data) {
			try {
				@SuppressWarnings("unchecked")
				Binary<T> cloned=(Binary<T>)clone();
				cloned.data=data;
				return cloned;
			} catch(Throwable t) {
				return null;
			}
		}

	}
	
	/**
	 * Base class for (rhasspy) errors (see {@link NLUError}.
	 * @author notalexa
	 *
	 */
	public static class Error {
		@JsonProperty(required = true) String error;
		@JsonProperty String context;
		@JsonProperty(defaultValue = "default") String siteId;
		@JsonProperty String sessionId;
		
		protected Error(String error,String context,String siteId,String sessionId) {
			this.error=error;
			this.context=context;
			this.siteId=siteId;
			this.sessionId=sessionId;
		}

		public String getError() {
			return error;
		}

		public String getContext() {
			return context;
		}

		public String getSiteId() {
			return siteId;
		}

		public String getSessionId() {
			return sessionId;
		}
	}
	
	/**
	 * Class used in the audio section
	 * 
	 * @author notalexa
	 *
	 */
	public static class Device {
		@JsonProperty(required = true) Mode mode;

		@JsonProperty(required = true) String id;
		@JsonProperty String name;
		@JsonProperty String description;
		@JsonProperty Boolean working;

		@JsonCreator
		public Device(@JsonProperty("mode") Mode mode, @JsonProperty("id") String id) {
			this(mode,id,null,null,null);
		}

		public Device(Mode mode, String id, String name, String description, Boolean working) {
			super();
			this.mode = mode;
			this.id = id;
			this.name = name;
			this.description = description;
			this.working = working;
		}
		
		public Mode getMode() {
			return mode;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}
		
		public boolean isTested() {
			return working!=null;
		}
		
		public boolean isWorking() {
			return working!=null&&working;
		}
		
		public enum Mode {
			input,output;
		}
	}
	
	/**
	 * Overlay implementing the features feature ({@link Feature#Features}).
	 * 
	 * @author notalexa
	 *
	 */
	@Overlay
	public class FeaturesRequestHandler extends FeaturesRequest {

		@Override
		public void received(HermesApi api) throws BaseException {
			if(HermesApi.this.siteId!=null&&matches(getSiteId())) {
				createAnswer(HermesApi.this.siteId,features).publish(api);
			}
		}		
	}
	
	/**
	 * Interface indicating that the hermes message provides a hermes request
	 * 
	 * @author notalexa
	 *
	 */
	public interface HermesRequest {
		void setId(String id);
	}
	
	/**
	 * Interface indicating that the hermes message is an answer to a hermes request.
	 * 
	 * @author notalexa
	 *
	 */
	public interface RequestAnswer {
		String getId();
	}
	
	private class PendingAnswer<T> extends WeakReference<Object> {
		CountDownLatch latch;
		T result;
		
		public PendingAnswer(Object referent) {
			super(referent);
			latch=new CountDownLatch(1);
		}
		
		@SuppressWarnings("unchecked")
		private void set(Object result) {
			this.result=(T)result;
			latch.countDown();
		}
		private T await() throws BaseException {
			try {
				if(!latch.await(1000,TimeUnit.MILLISECONDS)) {
					throw new BaseException(BaseException.GENERAL, "Timeout");
				}
			} catch(InterruptedException t) {
				throw new BaseException(BaseException.GENERAL, "Timeout");
			}
			return result;
		}
	}

	static class TopicMatcher {
		String[] components;
		boolean hasMultiLevelWildcard;
		boolean hasEndWildcard;
		Class<? extends HermesMessage<?>> clazz;
		public TopicMatcher(String key, Class<? extends HermesMessage<?>> clazz) {
			this.clazz=clazz;
			if(key.endsWith("#")) {
				hasEndWildcard=hasMultiLevelWildcard=true;
				key=key.substring(0,key.length()-1);
			} else {
				hasEndWildcard=key.endsWith("+");
			}
			components=key.split("\\+");
		}
		
		public boolean matches(String topic) {
			int offset=0;
			int c=0;
			for(String component:components) {
				if(topic.regionMatches(offset, component, 0,component.length())) {
					c++;
					int l=offset+component.length();
					offset=topic.indexOf('/',offset+component.length());
					if(offset<0) {
						return c==components.length&&(hasEndWildcard||l==topic.length());
					}
				} else {
					return false;
				}
			}
			return hasMultiLevelWildcard;
		}		
	}
	
	/**
	 * Subscriberinterface for a given topic (or class of topics).
	 * 
	 * @author notalexa
	 * @see HermesApi#addTopic(String, Subscriber)
	 */
	public interface Subscriber {
		/**
		 * Called whenever a matching topic is received.
		 * 
		 * @param api the hermes api
		 * @param topic the topic of the received message
		 * @param msg the message itself
		 */
		public void received(HermesApi api, String topic, byte[] msg);
	}
}
