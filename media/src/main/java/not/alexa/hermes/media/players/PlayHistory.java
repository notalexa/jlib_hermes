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
package not.alexa.hermes.media.players;

import java.util.ArrayList;
import java.util.List;

/**
 * History class for player implementations
 * 
 * @param <T> the type of history
 */
public class PlayHistory<T> {
	private int max;
	private List<T>  history=new ArrayList<T>();
	public PlayHistory(int max) {
		this.max=max;
	}
	
	/**
	 * Add the entry to the history
	 * @param entry the entry to add
	 */
	public void add(T entry) {
		while(history.size()>max) {
			history=history.subList(0,max);
		}
		history.add(0, entry);
	}
	
	/**
	 * @return the topmost element in the history
	 */
	public T peek() {
		if(history.isEmpty()) {
			return null;
		} else {
			return history.get(0);
		}
	}
	
	/**
	 * Retrieves the last element after the current element in the list and removes both from history
	 * 
	 * @return the last active element in the list
	 */
	public T pop() {
		if(history.size()>1) {
			history.remove(0);
			return history.remove(0);
		} else {
			return null;
		}
	}


	/**
	 * @return {@code true} if the history has a history element
	 */
	public boolean hasHistory() {
		return history.size()>1;
	}
}
