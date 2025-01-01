/*
 * Copyright (C) 2025 Not Alexa
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
package not.alexa.hermes.media.streams.dsp;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder.Buffer;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.ObjectType;

/**
 * Client part of the communication with an Analog Devices Digitial Signal Processor. The communication is done via
 * TCP with a Sigma Studio compatible server. Most of the code is an adoption of software in <a href="https://github.com/hifiberry/hifiberry-dsp">the hifiberry dsp repository</a>.
 * This repo provides a {@code sigmatcpserver} too, which can be used as the communication server.
 * 
 * @author notalexa
 */
public class DSP {
	private static final XMLCodingScheme PROGRAM_SCHEME=XMLCodingScheme.builder().setRootTag("profile").setRootType(Profile.class)
			.setIndent("  ", "\n")
			.addCodec(ObjectType.createClassType(byte[].class),new HexCodec()).build();
	private static final int HEADER_SIZE=14;
	private static final byte COMMAND_READ=0x0a;
	private static final byte COMMAND_WRITE=0x09;
	private static final byte COMMAND_GET_METADATA=(byte)0xf8;
	private static final int RESET_REGISTER=0xf890;
	private static final byte[] CHECKSUM_REQUEST=Arrays.copyOf(new byte[] { (byte)0xf1 },HEADER_SIZE);
	
	private String host;
	private int port;
	private Socket socket;
	// Currently used profile
	private Profile profile;
	private byte[] eepromChecksum;
	
	public DSP(String host,int port) {
		this.host=host;
		this.port=port;
	}
	
	private byte[] read_request(int addr,int length) {
		byte[] packet=new byte[HEADER_SIZE];
		packet[0]=COMMAND_READ;
		packet[4]=14;
		packet[8]=(byte)(length>>8);
		packet[9]=(byte)length;
		packet[10]=(byte)(addr>>8);
		packet[11]=(byte)addr;
		return packet;
	}
	
	@SuppressWarnings("unused")
	private byte[] metadata_request(String name) {
		try {
			byte[] key=name.getBytes("UTF-8");
			byte[] packet=new byte[HEADER_SIZE+key.length];
			packet[0]=COMMAND_GET_METADATA;
			packet[3]=(byte)(packet.length>>8);
			packet[4]=(byte)packet.length;
			System.arraycopy(key, 0, packet, HEADER_SIZE,key.length);
			return packet;
		} catch(Throwable t) {
			return null;
		}
	}
	
	public Profile load(Context context,InputStream stream) throws BaseException {
		try {
			try(Decoder decoder=PROGRAM_SCHEME.createDecoder(context, stream)) {
				return decoder.decode(Profile.class);
			}
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}
	
	public static String toHex(byte[] val) {
		StringBuilder builder=new StringBuilder();
		for(byte b:val) {
			int v=(b&0xff);
			if(v<16) {
				builder.append('0').append(Integer.toString(v,16));
			} else {
				builder.append(Integer.toString(v,16));
			}
		}
		return builder.toString().toUpperCase();
	}
	
	public int addr(String name) throws IOException {
		return profile==null?-1:profile.getAddr(name);
	}
	
	private byte[] readFully(int length) throws IOException {
		byte[] result=new byte[length];
		InputStream in=socket.getInputStream();
		int n=0;
		int offset=0;
		while(offset<result.length) {
			n=in.read(result, offset,result.length-offset);
			if(n<0) {
				throw new EOFException();
			} else {
				offset+=n;
			}
		}
		return result;		
	}
	
	private byte[] write_request(int addr,byte[] data) {
		byte[] packet=new byte[HEADER_SIZE+data.length];
		packet[0]=COMMAND_WRITE;
		packet[5]=(byte)(packet.length>>8);
		packet[6]=(byte)(packet.length);
		packet[10]=(byte)(data.length>>8);
		packet[11]=(byte)(data.length);
		packet[12]=(byte)(addr>>8);
		packet[13]=(byte)(addr);
		System.arraycopy(data, 0, packet, HEADER_SIZE, data.length);
		return packet;
	}
	
	public void poke(int addr,byte[] data) throws IOException {
		send(write_request(addr, data));
	}
	
	public byte[] peek(int addr, int length) throws IOException {
		return sendForAnswer(read_request(addr, length), length);
	}
	
	public int peekReg(int addr) throws IOException {
		return peekInt(addr,2);
	}
	
	public int peekInt(int addr) throws IOException {
		return peekInt(addr,4);
	}
	
	private static int toInt(byte[] bytes) {
		int accu=0;
		for(int i=0;i<bytes.length;i++) {
			accu=(accu<<8)+(bytes[i]&0xff);
		}
		return accu;
	}
	
	public int peekInt(int addr,int length) throws IOException {
		byte[] bytes=peek(addr,length);
		return toInt(bytes);
	}
	
	public void reset() throws IOException {
		reset(true);
	}
	
	public void reset(boolean force) throws IOException {
		try {
			if(force||eepromChecksum==null||!Arrays.equals(eepromChecksum, checksum())) {
				System.out.println("Reset DSP...");
				poke(RESET_REGISTER,new byte[] { 0,0 });
				Thread.sleep(500);
				poke(RESET_REGISTER,new byte[] { 0,1 });
				Thread.sleep(500);
			}
			if(eepromChecksum==null) {
				eepromChecksum=checksum();
				System.out.println("Checksum is "+Arrays.toString(eepromChecksum));
			}
		} catch(Throwable t) {
			t.printStackTrace();
		} finally {
			profile=null;
		}
	}
	
	public float peekFloat(int addr) throws IOException {
		return ((float)peekInt(addr,4))/((float)(0x1000000));
	}
	
	public byte[] checksum() throws IOException {
		return sendForAnswer(CHECKSUM_REQUEST,16);
	}
	
	private void send(byte[] packet) throws IOException {
		if(socket!=null) {
			if(packet!=null) {
				socket.getOutputStream().write(packet);
			}
		}
	}
	
	private byte[] sendForAnswer(byte[] packet,int answerLength) throws IOException {
		if(socket!=null) {
			if(packet!=null) {
				socket.getOutputStream().write(packet);
			}
			byte[] result=readFully(HEADER_SIZE+Math.max(0,answerLength));
			return answerLength<0?Arrays.copyOf(result, -answerLength):Arrays.copyOfRange(result, HEADER_SIZE,result.length);
		} else {
			throw new IOException();
		}
	}
	
	public void apply(Profile rom) throws IOException {
		apply(rom,true);
	}
	
	public void apply(Profile rom,boolean forceCheck) throws IOException {
		if(rom.program!=null&&rom.program.checksum!=null) {
			boolean load=profile==null||!rom.program.checksum.equals(profile.program.checksum);
			if(load||forceCheck) {
				load=!Arrays.equals(rom.program.checksum,checksum());
			}
			if(load) {
				for(Action action:rom.program.actions) {
					if("delay".equals(action.type)) try {
						Thread.sleep(100);
					} catch(Throwable t) {
					} else {
						poke(action.addr,action.value);
					}
				}
				try {
					Thread.sleep(100);
				} catch(Throwable t) {
				}
			}
			profile=rom;
		}
	}
	
	public void prepare(InputStream stream) throws BaseException {
		try {
			connect();
			Context context=Context.createRootContext();
			DOMResult result=new DOMResult(); 
			Transformer transformer=TransformerFactory.newInstance().newTransformer(new StreamSource(DSP.class.getResourceAsStream("profile.xslt")));
			transformer.transform(new StreamSource(stream), result);
			try(Decoder decoder=PROGRAM_SCHEME.createDecoder(context, result.getNode())) {
				Profile profile=decoder.decode(Profile.class);
				if(profile!=null&&profile.program!=null) {
					apply(profile);
					profile.program.checksum=checksum();
					reset();
					PROGRAM_SCHEME.createEncoder(context, System.out).encode(profile);
					System.out.println();
				}
			}
		} catch(Throwable t) {
			BaseException.throwException(t);
		} finally {
			try {
				disconnect();
			} catch(IOException e) {
			}
		}

	}
	
	public void connect() throws IOException {
		if(socket==null) try {
			socket=new Socket(host, port);
		} catch(IOException e) {
			socket=null;
			throw e;
		}
	}
	
	public boolean isConnected() {
		return socket!=null;
	}
	
	public void disconnect() throws IOException {
		if(socket!=null) try {
			socket.close();
		} finally {
			socket=null;
		}
	}
	
	
	public static void main(String[] args) throws Throwable {
		String host="localhost";
		String file=null;
		int mode=-1;
		for(String arg:args) {
			if("--host".equals(arg)||"-h".equals(arg)) {
				if(mode>=0) {
					throw new Exception("Host expected");
				} else {
					mode=0;
				}
			} else if("--file".equals(arg)||"-f".equals(arg)) {
				if(mode>=0) {
					throw new Exception("File expected");
				} else {
					mode=1;
				}
			} else if(arg.startsWith("-")) {
				throw new Exception("Illegal argument: "+arg);
			} else {
				switch(mode) {
					case 0: host=arg;
						break;
					case -1:
					case 1: file=arg;
				}
			}
		}
		if(file==null) {
			throw new NullPointerException("file");
		}
		DSP dsp=new DSP(host,8086);
		try(InputStream stream=new FileInputStream(file)) {
			dsp.prepare(stream);
		}
	}
	
	/**
	 * A profile for the DSP. Profiles can be applied via {@link DSP#apply(Profile)} and generated via {@link DSP#main(String[])} (or the wrapper binary provided in this project).
	 * 
	 * @author notalexa
	 */
	public static class Profile {
		@JsonProperty(value="@chip") String chip;
		@JsonProperty Program program;
		public int getAddr(String name) {
			return getAddr(null,name);
		}
		public int getAddr(String type,String name) {
			return program==null?-1:program.getAddr(type,name);
		}
		
		public long getChecksum() {
			return program==null?0:program.getChecksum();
		}
	}
	
	/**
	 * The (dsp) program and metadata inside a profile.
	 * 
	 * @author notalexa
	 */
	public static class Program {
		@JsonProperty(value="@target") String target;
		@JsonProperty(value="@checksum") byte[] checksum;
		@JsonProperty ProgramMetaData metadata;
		@JsonProperty(value="action") List<Action> actions;
		
		public int getAddr(String name) {
			return getAddr(null,name);
		}
		
		public int getAddr(String type,String name) {
			return metadata==null?-1:metadata.getAddr(type,name);
		}
		
		public long getChecksum() {
			if(actions!=null) {
				int sum=0;
				for(Action action:actions)  {
					if("Program Data".equals(action.name)) {
						IntBuffer buffer=ByteBuffer.wrap(action.value).asIntBuffer();
						while(buffer.hasRemaining()) {
							int c=buffer.get();
							System.out.println("Add "+c);
							sum-=c;
						}
						return (sum&0xffffffffL);
					}
				}
			}
			return 0;
		}
	}
	
	/**
	 * Program metadata in the profile. This consist of a list of registers which can be resolved using logical names.
	 * 
	 */
	public static class ProgramMetaData {
		@JsonProperty(value="register") List<Register> registers;

		public int getAddr(String type,String name) {
			if(registers!=null) for(Register register:registers) {
				if((type==null||type.equals(register.type))&&name.equals(register.name)) {
					return register.addr;
				}
			}
			return -1;
		}
	}
	
	/**
	 * A "register" in the profile.
	 * 
	 * @author notalexa
	 */
	public static class Register {
		@JsonProperty(value="@type") String type;
		@JsonProperty(value="@name") String name;
		@JsonProperty(value="@addr") int addr;
	}

	/**
	 * One action in the profile
	 * 
	 * @author notalexa
	 */
	public static class Action implements Cloneable {
		@JsonProperty(value="@type") String type;
		@JsonProperty(value="@addr") int addr;
		@JsonProperty(value="@name") String name;
		@JsonProperty(value="#text") byte[] value;
		
		public Action cloneFor(int addr) {
			try {
				Action clone=(Action)super.clone();
				clone.addr=addr;
				return clone;
			} catch(Throwable t) {
				return null;
			}
		}
		public String toString() {
			return "Mem["+addr+"] of length "+value.length;
		}
	}
	
	private static class HexCodec implements Codec {

		@Override
		public void encode(Buffer buffer, Object t) throws BaseException {
			byte[] bytes=(byte[])t;
			StringBuilder encoded=new StringBuilder();
			for(byte b:bytes) {
				int i=(b&0xff);
				if(encoded.length()>0) {
					encoded.append(' ');
				}
				if(i<16) {
					encoded.append('0');
				}
				encoded.append(Integer.toString(i,16).toUpperCase());
			}
			buffer.write(encoded);
		}

		@Override
		public Object decode(not.alexa.netobjects.coding.Decoder.Buffer buffer) throws BaseException {
			String v=buffer.getCharContent().toString().replace(" ", "");
			if(v.length()%2!=0) {
				System.out.println("V="+v);
				throw new BaseException();
			}
			byte[] result=new byte[v.length()/2];
			for(int i=0;i<result.length;i++) {
				result[i]=(byte)Integer.parseInt(v.substring(2*i, 2*(i+1)), 16);
			}
			return result;
		}
	}

}
