package not.alexa.hermes;

import org.junit.jupiter.api.Test;

import not.alexa.hermes.asr.AudioCaptured;
import not.alexa.hermes.audio.AudioFrame;
import not.alexa.hermes.audio.AudioPlayBytes;
import not.alexa.hermes.audio.AudioSessionFrame;
import not.alexa.hermes.features.FeaturesRequest;

public class FailureTest {

	public FailureTest() {
	}
	
	@Test
	void intentFailure1() {
		try {
			new FeaturesRequest().forTopic("xxx");
		} catch(IllegalTopicException e) {
			
		}
	}
	
	@Test
	void intentFailure2() {
		try {
			new AudioFrame("default",new byte[0]).forTopic("xxx");
		} catch(IllegalTopicException e) {
		}
	}

	@Test
	void intentFailure3() {
		try {
			new AudioFrame("default",new byte[0]).forTopic("hermes/audioServer/");
		} catch(IllegalTopicException e) {
		}
	}
	
	@Test
	void intentFailure4() {
		try {
			new AudioFrame("default",new byte[0]).forTopic("hermes/audioServer/default/x/audioFrame");
		} catch(IllegalTopicException e) {
		}
	}

	@Test
	void intentFailure5() {
		try {
			new AudioSessionFrame("default","sessionId",new byte[0]).forTopic("xxx");
		} catch(IllegalTopicException e) {
		}
	}

	@Test
	void intentFailure6() {
		try {
			new AudioSessionFrame("default","sessionId",new byte[0]).forTopic("hermes/audioServer/");
		} catch(IllegalTopicException e) {
		}
	}
	
	@Test
	void intentFailure7() {
		try {
			new AudioSessionFrame("default","sessionId",new byte[0]).forTopic("hermes/audioServer/default/audioSessionFrame");
		} catch(IllegalTopicException e) {
		}
	}
	@Test
	void intentFailure8() {
		try {
			new AudioSessionFrame("default","sessionId",new byte[0]).forTopic("hermes/audioServer/default/sessionId/xxx/audioSessionFrame");
		} catch(IllegalTopicException e) {
		}
	}

	@Test
	void intentFailure9() {
		try {
			new AudioPlayBytes("default","requestId",new byte[0]).forTopic("xxx");
		} catch(IllegalTopicException e) {
		}
	}

	@Test
	void intentFailure10() {
		try {
			new AudioPlayBytes("default","requestId",new byte[0]).forTopic("hermes/audioServer/");
		} catch(IllegalTopicException e) {
		}
	}
	
	@Test
	void intentFailure11() {
		try {
			new AudioPlayBytes("default","requestId",new byte[0]).forTopic("hermes/audioServer/default/xxx/playBytes/");
		} catch(IllegalTopicException e) {
		}
	}
	@Test
	void intentFailure12() {
		try {
			new AudioPlayBytes("default","requestId",new byte[0]).forTopic("hermes/audioServer/default/playBytes/xxx/requestId");
		} catch(IllegalTopicException e) {
		}
	}
	
	@Test
	void intentFailure13() {
		try {
			new AudioCaptured("default","sessionId",new byte[0]).forTopic("xxx");
		} catch(IllegalTopicException e) {
		}
	}

	@Test
	void intentFailure14() {
		try {
			new AudioCaptured("default","sessionId",new byte[0]).forTopic("hermes/asr/");
		} catch(IllegalTopicException e) {
		}
	}
	
	@Test
	void intentFailure15() {
		try {
			new AudioCaptured("default","sessionId",new byte[0]).forTopic("hermes/asr/default/audioCaptured");
		} catch(IllegalTopicException e) {
		}
	}
	@Test
	void intentFailure16() {
		try {
			new AudioCaptured("default","sessionId",new byte[0]).forTopic("hermes/asr/default/sessionId/xxx/audioCaptured");
		} catch(IllegalTopicException e) {
		}
	}
	@Test
	void intentFailure17() {
		try {
			new FeaturesRequest().forTopic("xxx");
		} catch(IllegalTopicException e) {
		}
	}

}
