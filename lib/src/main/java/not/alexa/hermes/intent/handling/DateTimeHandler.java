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
package not.alexa.hermes.intent.handling;

import java.text.MessageFormat;
import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.nlu.NLUIntent;

/**
 * Handler replying the current date and time. This is defined here for
 * demo. The handler has four properties:
 * <ul>
 * <li>{@¢ode dateIntent}: The intent for replying the date. Defaults to {@code currentDate}.
 * <li>{@code timeIntent}: The intent for replying the time. Defaults to {@code currentTime}.
 * <li>{@code dateFormat}: The format to use for replying the date. Defaults to <code>Wir haben heute den {1}ten {0}ten</code>.
 * First argument is the month of the yera, second argument is the day of the month.
 * <li>{@code timeFormat}: The format to use for replying the time. Defaults to <code>Es ist jetzt {0} Uhr{1,choice,0#|0<{1}}</code>.
 * First argument is the hour of the day (from {@code 0..23}), second argument is the minute of the hour.
 * </ul>
 */
public class DateTimeHandler implements IntentHandler {
	@JsonProperty(defaultValue = "currentDate") String dateIntent="currentDate";
	@JsonProperty(defaultValue = "currentTime")  String timeIntent="currentTime";
	@JsonProperty(defaultValue="Wir haben heute den {1}ten {0}ten") String dateFormat="Wir haben heute den {1}ten {0}ten";
	@JsonProperty(defaultValue="Es ist jetzt {0} Uhr{1,choice,0#|0<{1}}") String timeFormat="Es ist jetzt {0} Uhr{1,choice,0#|0< {1}}";

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		Calendar calendar=Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		try {
			if(intent.getIntent().contentEquals(dateIntent)) {
				intent.reply(api,MessageFormat.format(dateFormat, calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)));
				return true;
			} else if(intent.getIntent().contentEquals(timeIntent)) {
				intent.reply(api,MessageFormat.format(timeFormat, calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE)));
				return true;
			}
		} catch(Throwable t) {
			return true;
		}
		return false;
	}
}
