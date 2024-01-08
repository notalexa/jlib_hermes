package not.alexa.hermes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import not.alexa.hermes.HermesApi.AsrToken;
import not.alexa.hermes.HermesApi.Device.Mode;
import not.alexa.hermes.HermesApi.Intent;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.HermesApi.Slot.Range;
import not.alexa.hermes.asr.ASRError;
import not.alexa.hermes.asr.AudioCaptured;
import not.alexa.hermes.asr.StartListening;
import not.alexa.hermes.asr.StopListening;
import not.alexa.hermes.asr.TextCaptured;
import not.alexa.hermes.asr.Train;
import not.alexa.hermes.asr.TrainSuccess;
import not.alexa.hermes.audio.AudioDevices;
import not.alexa.hermes.audio.AudioDevicesRequest;
import not.alexa.hermes.audio.AudioFrame;
import not.alexa.hermes.audio.AudioPlayBytes;
import not.alexa.hermes.audio.AudioPlayError;
import not.alexa.hermes.audio.AudioPlayFinished;
import not.alexa.hermes.audio.AudioRecordError;
import not.alexa.hermes.audio.AudioSessionFrame;
import not.alexa.hermes.audio.AudioSetVolume;
import not.alexa.hermes.dialogue.Configure;
import not.alexa.hermes.dialogue.Configure.IntentConfig;
import not.alexa.hermes.dialogue.ContinueSession;
import not.alexa.hermes.dialogue.DialogueError;
import not.alexa.hermes.dialogue.EndSession;
import not.alexa.hermes.dialogue.IntentNotRecognized;
import not.alexa.hermes.dialogue.SessionEnded;
import not.alexa.hermes.dialogue.SessionQueued;
import not.alexa.hermes.dialogue.SessionStarted;
import not.alexa.hermes.dialogue.StartSession;
import not.alexa.hermes.dialogue.StartSession.Init;
import not.alexa.hermes.features.Features;
import not.alexa.hermes.features.FeaturesRequest;
import not.alexa.hermes.g2p.G2PError;
import not.alexa.hermes.g2p.Phonemes;
import not.alexa.hermes.g2p.Phonemes.Result;
import not.alexa.hermes.g2p.PhonemesRequest;
import not.alexa.hermes.hotword.HotwordDetected;
import not.alexa.hermes.hotword.HotwordError;
import not.alexa.hermes.hotword.Hotwords;
import not.alexa.hermes.hotword.Hotwords.Model;
import not.alexa.hermes.hotword.HotwordsRequest;
import not.alexa.hermes.intent.handling.ToggleOff;
import not.alexa.hermes.intent.handling.ToggleOn;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.hermes.nlu.NLUError;
import not.alexa.hermes.nlu.Query;
import not.alexa.hermes.tts.Say;
import not.alexa.hermes.tts.SayFinished;
import not.alexa.hermes.tts.TTSError;
import not.alexa.hermes.tts.Voices;
import not.alexa.hermes.tts.Voices.Voice;
import not.alexa.hermes.tts.VoicesRequest;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;


public class HermesMessageTest {

	public HermesMessageTest() {
	}
	
	public static Stream<TestData<?>> testData() {
		return Stream.of(
				new TestData<FeaturesRequest>(new FeaturesRequest(),msg->{
					assertNull(msg.getSiteId());
					assertNull(msg.getId());
					assertEquals("rhasspy/features/getFeatures", msg.getTopic());
				}),
				new TestData<FeaturesRequest>(new FeaturesRequest("default","id"),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("id", msg.getId());
					assertEquals("rhasspy/features/getFeatures", msg.getTopic());
				}),
				new TestData<Features>(new FeaturesRequest().createAnswer("siteId",Feature.values()),msg->{
					assertEquals("siteId", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals("rhasspy/features/features", msg.getTopic());
					assertArrayEquals(Feature.values(), msg.getFeatures());
				}),
				new TestData<Features>(new FeaturesRequest("default","id").createAnswer("default", new Feature[] { Feature.Features}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("id", msg.getId());
					assertEquals("rhasspy/features/features", msg.getTopic());
					assertArrayEquals(new Feature[] { Feature.Features}, msg.getFeatures());
				}),
				new TestData<ToggleOn>(new ToggleOn(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("rhasspy/handle/toggleOn", msg.getTopic());
				}),
				new TestData<ToggleOff>(new ToggleOff(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("rhasspy/handle/toggleOff", msg.getTopic());
				}),
				new TestData<PhonemesRequest>(new PhonemesRequest(new String[] { "example"}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(5, msg.getNumGuesses());
					assertNull(msg.getSessionId());
					assertNull(msg.getId());
					assertEquals("rhasspy/g2p/pronounce", msg.getTopic());
					assertArrayEquals(new String[] { "example"}, msg.getWords());
				}),
				new TestData<Phonemes>(new PhonemesRequest(new String[] { "example"}).createAnswer(Collections.singletonMap("example", new Result[] {new Result(new String[] { "e" },false)})),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(1, msg.getWordPhonemes().size());
					assertNull(msg.getSessionId());
					assertNull(msg.getId());
					assertEquals("rhasspy/g2p/phonemes", msg.getTopic());
				}),
				new TestData<G2PError>(new G2PError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/g2p", msg.getTopic());
				}),
				new TestData<TTSError>(new TTSError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/tts", msg.getTopic());
				}),
				new TestData<Say>(new Say("Hello world"),msg->{
					assertEquals("Hello world",msg.getText());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNotNull(msg.getId());
					assertNull(msg.getLang());
					assertEquals(-1,msg.getVolume());
					assertEquals("hermes/tts/say", msg.getTopic());
				}),
				new TestData<TTSError>(new Say("Hello world").createError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/tts", msg.getTopic());
				}),
				new TestData<SayFinished>(new Say("Hello world").createFinished(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNotNull(msg.getId());
					assertEquals("hermes/tts/sayFinished", msg.getTopic());
				}),
				new TestData<VoicesRequest>(new VoicesRequest(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals("rhasspy/tts/getVoices", msg.getTopic());
				}),
				new TestData<Voices>(new VoicesRequest().createAnswer(new Voice[] { new Voice("id","de","voice")}),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals(1,msg.getVoices().length);
					assertEquals("id", msg.getVoices()[0].getVoiceId());
					assertEquals("de", msg.getVoices()[0].getLang());
					assertEquals("voice", msg.getVoices()[0].getDescription());
					assertEquals("rhasspy/tts/voices", msg.getTopic());
				}),
				new TestData<HotwordError>(new HotwordError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/hotword", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.hotword.ToggleOn>(new not.alexa.hermes.hotword.ToggleOn(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("", msg.getReason());
					assertEquals("hermes/hotword/toggleOn", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.hotword.ToggleOff>(new not.alexa.hermes.hotword.ToggleOff(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("", msg.getReason());
					assertEquals("hermes/hotword/toggleOff", msg.getTopic());
				}),
				new TestData<HotwordsRequest>(new HotwordsRequest(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals("rhasspy/hotword/getHotwords", msg.getTopic());
				}),
				new TestData<Hotwords>(new HotwordsRequest().createAnswer(new Model[] { new Model("id",new String[] { "wakeup"},"version","personal")}),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals(1,msg.getModels().length);
					assertEquals("id", msg.getModels()[0].getModelId());
					assertEquals("version", msg.getModels()[0].getModelVersion());
					assertEquals("personal", msg.getModels()[0].getModelType());
					assertEquals(1, msg.getModels()[0].getWords().length);
					assertEquals("wakeup", msg.getModels()[0].getWords()[0]);
					assertEquals("rhasspy/hotword/hotwords", msg.getTopic());
				}),
				new TestData<HotwordDetected>(new HotwordDetected("wakewordId","modelId"),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("wakewordId",msg.getWakewordId());
					assertEquals("", msg.getModelVersion());
					assertEquals("personal", msg.getModelType());
					assertEquals(1f, msg.getCurrentSensitivity());
					assertEquals("modelId", msg.getModelId());
					assertNull(msg.getSessionId());
					assertNull(msg.getSendAudioCaptured());
					assertEquals("hermes/hotword/wakewordId/detected", msg.getTopic());
				}),
				new TestData<DialogueError>(new DialogueError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/dialogueManager", msg.getTopic());
				}),
				new TestData<StartSession>(new StartSession(new Init(false,"Hello"),"default","custom"),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("action",msg.getInit().getType());
					assertEquals("Hello",msg.getInit().getText());
					assertEquals(false,msg.getInit().isSendIntentNotRecognized());
					assertEquals(false, msg.getInit().getCanBeEnqueued());
					assertNull(msg.getInit().getIntentFilter());
					assertEquals("hermes/dialogueManager/startSession", msg.getTopic());
				}),
				new TestData<SessionStarted>(new StartSession(new Init(false,"Hello"),"default","custom").createStarted("sessionId"),msg->{
					assertEquals("sessionId", msg.getSessionId());
					assertEquals("default", msg.getSiteId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("hermes/dialogueManager/sessionStarted", msg.getTopic());
				}),
				new TestData<SessionQueued>(new StartSession(new Init(false,"Hello"),"default","custom").createQueued("sessionId"),msg->{
					assertEquals("sessionId", msg.getSessionId());
					assertEquals("default", msg.getSiteId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("hermes/dialogueManager/sessionQueued", msg.getTopic());
				}),
				new TestData<StartSession>(new StartSession(new Init("Bye"),"default","custom"),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("notification",msg.getInit().getType());
					assertEquals("Bye",msg.getInit().getText());
					assertEquals(false,msg.getInit().isSendIntentNotRecognized());
					assertEquals(false, msg.getInit().getCanBeEnqueued());
					assertNull(msg.getInit().getIntentFilter());
					assertEquals("hermes/dialogueManager/startSession", msg.getTopic());
				}),
				new TestData<ContinueSession>(new ContinueSession("sessionId","custom","text",null,false),msg->{
					assertEquals("sessionId", msg.getSessionId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("text",msg.getText());
					assertEquals(false,msg.isSendIntentNotRecognized());
					assertNull(msg.getIntentFilter());
					assertEquals("hermes/dialogueManager/continueSession", msg.getTopic());
				}),
				new TestData<EndSession>(new EndSession("sessionId","text","custom"),msg->{
					assertEquals("sessionId", msg.getSessionId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("text",msg.getText());
					assertEquals("hermes/dialogueManager/endSession", msg.getTopic());
				}),
				new TestData<SessionEnded>(new EndSession("sessionId","text","custom").createAnswer("default"),msg->{
					assertEquals("nominal", msg.getTermination());
					assertEquals("sessionId", msg.getSessionId());
					assertEquals("default", msg.getSiteId());
					assertEquals("custom",msg.getCustomData());
					assertEquals("hermes/dialogueManager/sessionEnded", msg.getTopic());
				}),
				new TestData<Configure>(new Configure(new IntentConfig[] { new IntentConfig("intent",false) } ),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(1, msg.getIntents().length);
					assertEquals("intent", msg.getIntents()[0].getIntentId());
					assertEquals(false, msg.getIntents()[0].isEnable());
					assertEquals("hermes/dialogueManager/configure", msg.getTopic());
				}),
				new TestData<IntentNotRecognized>(new IntentNotRecognized("sessionId","input words","default","custom"),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("sessionId", msg.getSessionId());
					assertEquals("input words", msg.getInput());
					assertEquals("custom", msg.getCustomData());
					assertEquals("hermes/dialogueManager/intentNotRecognized", msg.getTopic());
				}),
				new TestData<ASRError>(new ASRError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/asr", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.asr.ToggleOn>(new not.alexa.hermes.asr.ToggleOn(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("", msg.getReason());
					assertEquals("hermes/asr/toggleOn", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.asr.ToggleOff>(new not.alexa.hermes.asr.ToggleOff(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("", msg.getReason());
					assertEquals("hermes/asr/toggleOff", msg.getTopic());
				}),
				new TestData<StartListening>(new StartListening(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getWakewordId());
					assertEquals(true,msg.isStopOnSilence());
					assertEquals(false,msg.isSendAudioCaptured());
					assertEquals("hermes/asr/startListening", msg.getTopic());
				}),
				new TestData<StopListening>(new StopListening(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertEquals("hermes/asr/stopListening", msg.getTopic());
				}),
				new TestData<TextCaptured>(new TextCaptured("input words",1f,2.1f,"default",null,null,null),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getAsrTokens());
					assertNull(msg.getWakewordId());
					assertEquals("input words", msg.getText());
					assertEquals(1f, msg.getLikelihood());
					assertEquals(2.1f, msg.getSeconds());
					assertEquals("hermes/asr/textCaptured", msg.getTopic());
				}),
				new TestData<AudioCaptured>(new AudioCaptured("default","sessionId",new byte[] { 0}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("sessionId", msg.getSessionId());
					assertEquals(1,msg.encoded().length);
					assertEquals("hermes/asr/default/sessionId/audioCaptured", msg.getTopic());
				}),
				new TestData<Train>(new Train("graph_path","default",null),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("graph_path", msg.getGraph_path());
					assertNull(msg.getId());
					assertEquals("rhasspy/asr/default/train", msg.getTopic());
				}),
				new TestData<TrainSuccess>(new Train("graph_path","default",null).createSuccess(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals("rhasspy/asr/default/trainSuccess", msg.getTopic());
				}),
				new TestData<ASRError>(new Train("graph_path","default",null).createError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/asr", msg.getTopic());
				}),
				new TestData<AudioRecordError>(new AudioRecordError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/audioServer/record", msg.getTopic());
				}),
				new TestData<AudioPlayError>(new AudioPlayError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/audioServer/play", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.audio.AudioToggleOn>(new not.alexa.hermes.audio.AudioToggleOn(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("hermes/audioServer/toggleOn", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.audio.AudioToggleOff>(new not.alexa.hermes.audio.AudioToggleOff(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("hermes/audioServer/toggleOff", msg.getTopic());
				}),
				new TestData<AudioSetVolume>(new AudioSetVolume(0.5f),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(0.5f, msg.getVolume());
					assertEquals("rhasspy/audioServer/setVolume", msg.getTopic());
				}),
				new TestData<AudioDevicesRequest>(new AudioDevicesRequest(new Mode[] { Mode.output},"default",false),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(false,msg.isTest());
					assertNull(msg.getId());
					assertEquals(1,msg.getModes().length);
					assertEquals(Mode.output,msg.getModes()[0]);
					assertEquals("rhasspy/audioServer/getDevices", msg.getTopic());
				}),
				new TestData<AudioDevices>(new AudioDevicesRequest(new Mode[] { Mode.output},"default",false)
						.createAnswer(new HermesApi.Device[] {new HermesApi.Device(Mode.input, "deviceId1"),
								new HermesApi.Device(Mode.input, "deviceId2",null,null,false),
								new HermesApi.Device(Mode.output, "deviceId3",null,null,true)}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(3,msg.getDevices().length);
					assertEquals(Mode.input, msg.getDevices()[0].getMode());
					assertEquals("deviceId1", msg.getDevices()[0].getId());
					assertEquals(false, msg.getDevices()[0].isTested());
					assertEquals(false, msg.getDevices()[0].isWorking());
					assertEquals(true, msg.getDevices()[1].isTested());
					assertEquals(false, msg.getDevices()[1].isWorking());
					assertEquals(true, msg.getDevices()[2].isTested());
					assertEquals(true, msg.getDevices()[2].isWorking());
					assertNull(msg.getDevices()[0].getName());
					assertNull(msg.getId());
					assertEquals("rhasspy/audioServer/devices", msg.getTopic());
				}),
				new TestData<AudioFrame>(new AudioFrame("default",new byte[] { 0}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals(1,msg.encoded().length);
					assertEquals("hermes/audioServer/default/audioFrame", msg.getTopic());
				}),
				new TestData<AudioSessionFrame>(new AudioSessionFrame("default","sessionId",new byte[] { 0}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("sessionId", msg.getSessionId());
					assertEquals(1,msg.encoded().length);
					assertEquals("hermes/audioServer/default/sessionId/audioSessionFrame", msg.getTopic());
				}),
				new TestData<AudioPlayBytes>(new AudioPlayBytes("default","requestId",new byte[] { 0}),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("requestId", msg.getRequestId());
					assertEquals(1,msg.encoded().length);
					assertEquals("hermes/audioServer/default/playBytes/requestId", msg.getTopic());
				}),
				new TestData<AudioPlayFinished>(new AudioPlayBytes("default","requestId",new byte[] { 0}).createAnswer(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("requestId", msg.getRequestId());
					assertEquals("hermes/audioServer/default/playFinished", msg.getTopic());
				}),
				new TestData<NLUError>(new NLUError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/nlu", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.nlu.Train>(new not.alexa.hermes.nlu.Train("graph_path","default",null),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("graph_path", msg.getGraph_path());
					assertNull(msg.getId());
					assertEquals("rhasspy/nlu/default/train", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.nlu.TrainSuccess>(new not.alexa.hermes.nlu.Train("graph_path","default",null).createSuccess(),msg->{
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getId());
					assertEquals("rhasspy/nlu/default/trainSuccess", msg.getTopic());
				}),
				new TestData<NLUError>(new not.alexa.hermes.nlu.Train("graph_path","default",null).createError("error"),msg->{
					assertEquals("error",msg.getError());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getContext());
					assertEquals("hermes/error/nlu", msg.getTopic());
				}),
				new TestData<Query>(Query.createBuilder("input words")
						.addInitialSlot(new Slot("slotName","v"))
						.addInitialSlot(new Slot("slotName2","value"))
						.addInitialSlot(new Slot("slotName","value"))
						.setId(null)
						.setSiteId("default")
						.setSessionId("sessionId")
						.setAsrConfidence(0.1f)
						.setIntentFilter(new String[] {"intent"})
						.setAlternatives(Collections.singleton(
								Query.createBuilder("input words2").build()))
						.build(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("input words", msg.getInput());
					assertEquals(1, msg.getIntentFilter().length);
					assertEquals("intent", msg.getIntentFilter()[0]);
					assertEquals(1, msg.getAlternatives().length);
					assertEquals("sessionId", msg.getSessionId());
					assertEquals(0.1f, msg.getAsrConfidence());
					assertEquals("input words2", msg.getAlternatives()[0].getInput());
					assertNull(msg.getId());
					assertEquals("hermes/nlu/query", msg.getTopic());
				}),
				new TestData<Query>(Query.createBuilder("input words").build(),msg->{
					assertEquals(0, msg.getAlternatives().length);
					assertEquals("hermes/nlu/query", msg.getTopic());
				}),
				new TestData<not.alexa.hermes.nlu.IntentNotRecognized>(Query.createBuilder("input words").build().createError(),msg->{
					assertEquals("input words", msg.getInput());
					assertEquals("default", msg.getSiteId());
					assertNull(msg.getSessionId());
					assertNull(msg.getId());
					assertEquals("hermes/nlu/intentNotRecognized", msg.getTopic());
				}),
				new TestData<NLUIntent>(Query.createBuilder("input words")
						.addInitialSlot(new Slot("slotName","v"))
						.addInitialSlot(new Slot("slotName2","value"))
						.addInitialSlot(new Slot("slotName","value"))
						.setId(null)
						.setSiteId("default")
						.setSessionId("sessionId")
						.setAsrConfidence(0.1f)
						.setIntentFilter(new String[] {"intent"})
						.setAlternatives(Collections.singleton(
								Query.createBuilder("input words2").build()))
						.build().createAnswer()
						.setIntent(new Intent("intent",0.2f))
						.addSlot(new Slot("slotName","v"))
						.addSlot(new Slot("entity","slotName3","raw","v",new Range(0,2)))
						.addSlots(null)
						.setCustomData(null)
						.setAsrTokens(null)
						.build(),msg->{
					assertEquals("intent",msg.getIntent());
					assertEquals("default", msg.getSiteId());
					assertEquals("input words", msg.getInput());
					assertEquals("sessionId", msg.getSessionId());
					assertEquals(0.1f, msg.getAsrConfidence());
					assertEquals(0.2f, msg.getConfidenceScore());
					assertNull(msg.getId());
					assertNull(msg.getCustomData());
					assertNull(msg.getSlot("unknownslot"));
					assertNotNull(msg.getSlot("entity"));
					assertEquals(0,msg.getAsrTokens().length);
					assertEquals(3,msg.getSlots().length);
					assertEquals("entity",msg.getSlots()[2].getEntity());
					assertEquals("slotName3",msg.getSlots()[2].getSlotName());
					assertEquals("raw",msg.getSlots()[2].getRawValue());
					assertEquals("v",msg.getSlots()[2].getValue());
					assertNotNull(msg.getSlots()[2].getRange());
					assertEquals(0,msg.getSlots()[2].getRange().getStart());
					assertEquals(2,msg.getSlots()[2].getRange().getEnd());
					assertEquals("hermes/intent/intent", msg.getTopic());
				}),
				new TestData<NLUIntent>(Query.createBuilder("input words")
						.setId(null)
						.setSiteId("default")
						.setSessionId("sessionId")
						.setAsrConfidence(0.1f)
						.setIntentFilter(new String[] {"intent"})
						.setAlternatives(Collections.singleton(
								Query.createBuilder("input words2").build()))
						.build().createAnswer()
						.setIntent(new Intent("intent",0.2f))
						.addSlots(null)
						.setCustomData(null)
						.setAsrTokens(new AsrToken[][] {
							new AsrToken[] {
									new AsrToken("asrToken", 1f, 0, 2)
							}
						})
						.build(),msg->{
					assertEquals("default", msg.getSiteId());
					assertEquals("input words", msg.getInput());
					assertEquals("sessionId", msg.getSessionId());
					assertEquals(0.1f, msg.getAsrConfidence());
					assertEquals(0.2f, msg.getConfidenceScore());
					assertNull(msg.getId());
					assertNull(msg.getCustomData());
					assertEquals(1,msg.getAsrTokens().length);
					assertEquals("asrToken",msg.getAsrTokens()[0][0].getValue());
					assertEquals(1f,msg.getAsrTokens()[0][0].getConfidence());
					assertEquals(0,msg.getAsrTokens()[0][0].getStart());
					assertEquals(2,msg.getAsrTokens()[0][0].getEnd());
					assertEquals(0,msg.getSlots().length);
					assertEquals("hermes/intent/intent", msg.getTopic());
				})


				);
	}

	@ParameterizedTest
	@MethodSource("testData")
	void test1(TestData testData) {
		testData.check.accept(testData.msg);
		
	}
	
	@ParameterizedTest
	@MethodSource("testData")
	void test2(TestData testData) {
		class HermesApiTest extends HermesApi {
			public HermesApiTest(Context context) {
				super(context);
				if(!Binary.class.isAssignableFrom(testData.msg.getClass())) {
					classMap.put(testData.msg.getTopic(),(Class<HermesMessage<? extends HermesMessage<?>>>)testData.msg.getClass());
				}
			}

			@Override
			public void publish(HermesMessage<?> msg) throws BaseException {
				try {
					received(msg.getTopic(),encode(msg));
				} catch(IllegalTopicException e) {
					BaseException.throwException(e);
				}
			}			
		}
		
		try {
			new HermesApiTest(Context.createRootContext()).publish(testData.msg);
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
		
	}

	public static class TestData<T extends HermesMessage<T>> {
		T msg;
		Consumer<T> check;
		TestData(T msg,Consumer<T> check) {
			this.msg=msg;
			this.check=check;
		}
	}
}
