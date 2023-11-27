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

import not.alexa.netobjects.BaseException;

public interface HermesMessage<T extends HermesMessage<T>> extends Cloneable {
	public String getTopic();
	
	@SuppressWarnings("unchecked")
	public default T forTopic(String topic) throws IllegalTopicException {
		if(topic.equals(getTopic())) {
			return (T)this;
		}
		throw new IllegalTopicException(topic);
	}
	
	public default void publish(HermesApi api) throws BaseException {
		api.publish(this);
	}

	public default void received(HermesApi api) throws BaseException {
	}
}
