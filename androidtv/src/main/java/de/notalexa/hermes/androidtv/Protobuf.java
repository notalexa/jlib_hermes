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
package de.notalexa.hermes.androidtv;

import java.util.Arrays;

/**
 * Intermediate class handling protobuffering as currently needed. This should be replaced using a protobuf coding scheme
 * in the netobject library in future.
 * 
 */
public class Protobuf {
	private static Protobuf INSTANCE=new Protobuf();
	public static Protobuf getInstance() {
		return INSTANCE;
	}
	
	private Protobuf() {
	}

	byte[] string(int tag,String s) {
		byte[] result=new byte[2+s.length()];
		result[0]=(byte)((tag<<3)|2);
		result[1]=(byte)s.length();
		System.arraycopy(s.getBytes(), 0, result, 2, s.length());
		return result;
	}
	
	byte[] struct(int tag,byte[]...fields) {
		int l=0;
		for(byte[] field:fields) {
			l+=field.length;
		}
		tag<<=3;
		byte[] result=new byte[(tag>127?3:2)+l];
		int index=0;
		result[index++]=(byte)((tag&0x7f)|2);
		if(tag>=128) {
			result[0]|=(byte)0x80;
			result[index++]=(byte)(tag>>7);
		}
		result[index++]=(byte)l;
		for(byte[] field:fields) {
			System.arraycopy(field, 0, result, index, field.length);
			index+=field.length;
		}
		return result;
	}
	byte[] integer(int tag,int value) {
		byte[] result=new byte[2];
		result[0]=(byte)(tag<<3);
		while(true) {
			if(value<128) {
				result[result.length-1]=(byte)value;
				return result;
			} else {
				result[result.length-1]=(byte)(0x80|(value&0x7f));
				result=Arrays.copyOf(result, result.length+1);
				value>>=7;
			}
		}
	}
		
	byte[] message(byte[]...a) {
		int l=0;
		for(byte[] b:a) {
			l+=b.length;
		}
		byte[] result=new byte[l];
		int index=0;
		for(byte[] b:a) {
			System.arraycopy(b, 0, result, index, b.length);
			index+=b.length;
		}
		return result;
	}

}
