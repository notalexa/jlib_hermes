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

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.intent.handling.IntentHandler;
import not.alexa.hermes.nlu.NLUIntent;

/**
 * Hermes handler for the Android TV using the Remote Service API. The only supported intent is {@code tv:executeCommand} with a
 * slot {@code cmd} containing a sequence of messages as described in {@link Message#resolve(String)}.
 * <p>Usage: To setup the handler, the ip address of the TV, a key store with an <b>already paired</b> key (and certificate) and the
 * password for the key store is needed:
 * 
 * <pre>
 * class: de.notalexa.hermes.androidtv.IntentHandler
 * ip: 192.168.0.33
 * keystore: &lt;path to the keystore&gt;
 * passwd: &lt;secret of the keystore (and key)&gt;
 * </pre>
 * is a typical configuration.
 * 
 */
public class Handler implements IntentHandler {
	static Logger LOGGER=LoggerFactory.getLogger(Handler.class);
	private static long WAIT_TIME=15000;
	private static Protobuf proto=Protobuf.getInstance();
	private KeyStore keyStore;
	@JsonProperty(required = true) String ip;
	@JsonProperty(value="keystore", required = true) String keyFile;
	@JsonProperty(required = true) String passwd;
	@JsonProperty(defaultValue = "tv") String intentBase="tv";
	@JsonProperty String mac;
	private Connection con;
	private LinkedBlockingQueue<String> commandQueue=new LinkedBlockingQueue<>(20);
	private CountDownLatch awaitLatch;
	
	@JsonCreator
	public Handler(@JsonProperty("ip") String ip,@JsonProperty("keystore") String keyFile, @JsonProperty("passwd") String passwd) {
		this.ip=ip;
		this.keyFile=keyFile;
		this.passwd=passwd;
		try(InputStream stream=new FileInputStream(keyFile)) {
			keyStore=KeyStore.getInstance("PKCS12");
			keyStore.load(stream,passwd.toCharArray());
			con=new Connection(keyStore, commandQueue);
		} catch(Throwable t) {
			LOGGER.error(getClass().getName()+" unusable due to initialization errors.",t);
			keyStore=null;
		}
	}
		
	public void await() {
		if(awaitLatch==null) synchronized(this) {
			if(awaitLatch==null) {
				awaitLatch=new CountDownLatch(1);
			}
		}
		try {
			awaitLatch.await();
		} catch(Throwable t) {
		}
	}
	
	protected void connectionFinished(Connection con) {
		if(con==this.con) {
			if(awaitLatch==null) try {
				// keyStore is never null at this stage
				this.con=new Connection(keyStore, commandQueue);
			} catch(Throwable t) {
				LOGGER.error("AndroidTV becomes unavailable.",t);
				this.con=null;
			} else {
				awaitLatch.countDown();
			}
		}
	}

	public Handler cmd(String code) {
		if(con!=null) {
			con.checkAlive();
			commandQueue.offer(code);
		}
		return this;
	}
		
	class Connection extends Thread {
		SSLContext context;
		CountDownLatch readyLatch=new CountDownLatch(1);
		CountDownLatch stopLatch=new CountDownLatch(1);
		Socket socket;
		Object runLock=new Object();
		long lastCmd;
		boolean running;
		LinkedBlockingQueue<String> queue;
		boolean stopped;
		
		protected Connection(KeyStore keyStore,LinkedBlockingQueue<String> queue) throws NoSuchAlgorithmException, NoSuchProviderException, UnrecoverableKeyException, KeyStoreException, KeyManagementException, IOException {
			super("antroidtv-executor-"+ip);
			setDaemon(true);
			this.queue=queue;
			context=SSLContext.getInstance("TLS");
			KeyManagerFactory keyManagerFactory=KeyManagerFactory.getInstance("SunX509","SunJSSE");
			keyManagerFactory.init(keyStore,passwd.toCharArray());
			context.init(keyManagerFactory.getKeyManagers(), new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
					}
			}, new SecureRandom());
			socket=context.getSocketFactory().createSocket();
			start();
		}
		
		public void checkAlive() {
			if(socket.isConnected()&&System.currentTimeMillis()-lastCmd>WAIT_TIME) try {
				stopped=true;
				stopLatch.await();
				LOGGER.info("Alive check failed.");
			} catch(Throwable t) {
			}
		}
		
		public void run() {
			try {
				while(!stopped) {
					String cmd=queue.poll(200, TimeUnit.MILLISECONDS);
					if(cmd!=null) {
						Message[] cmds=Message.resolve(cmd);
						connect(cmds).cmd(cmds);
					}
				}
			} catch(Throwable t) {
				t.printStackTrace();
			} finally {
				connectionFinished(this);
				try {
					socket.close();
				} catch(Throwable t) {
				}
				stopLatch.countDown();
			}
		}
		
		private void handleResponse(byte[] msg) {
			switch(msg[0]) {
				case 10:
					writeCommand(
							proto.message(proto.struct(1,
								proto.integer(1,622),
								proto.struct(2,
									proto.string(1,"model"),
									proto.string(2,"vendor"),
									proto.integer(3,1),
									proto.string(4,"unknown2"),
									proto.string(5,"Package_name"),
									proto.string(6,"APP_VERSION"))))
					);
					break;
				case 18:
					writeCommand(
							proto.message(
									proto.struct(2,
											proto.integer(1,622))));
					try {
						Thread.sleep(500);
					} catch(Throwable t) {
					}
					LOGGER.info("Logged in to service@{}",ip);
					break;
				case 66:
					if(System.currentTimeMillis()-lastCmd<15000) {
						writeCommand(false,proto.message(proto.struct(9, proto.integer(1, msg[3]))));
					} else {
						LOGGER.info("Connection idle for {}ms. Terminate.",System.currentTimeMillis()-lastCmd);
						stopped=true;
					}
					break;
				case -62: // Tag 40: RemoteStart (Tag 1: Started)
					// [-62, 2, 2, 8, 1]	
					synchronized (runLock) {
						running=msg[4]!=0;
						if(running) {
							runLock.notifyAll();
						}
						lastCmd=System.currentTimeMillis();
					}
					LOGGER.info("TV is on: {}",running);
					readyLatch.countDown();
					//break;
				case -110: // Tag 50: RemoteSetVolumeLevel
					// [-110, 3, 40, 8, 2, 16, 2, 26, 24, 50, 48, 50, 48, 47, 50, 48, 50, 49, 32, 85, 72, 68, 32, 65, 110, 100, 114, 111, 105, 100, 32, 84, 86, 32, 2, 40, 0, 48, 100, 56, 30, 64, 0]
				case -94: // Tag 20: RemoteImeKeyInject
					// [-94, 1, 30, 10, 28, 98, 26, 99, 111, 109, 46, 109, 101, 100, 105, 97, 116, 101, 107, 46, 119, 119, 116, 118, 46, 116, 118, 99, 101, 110, 116, 101, 114]
					break;
				default: LOGGER.warn("Msg ignored: length={}, msg={}",msg.length,Arrays.toString(msg));
					break;
			}
		}

		
		private synchronized Connection connect(Message[] cmds) throws IOException {
			if(cmds.length>0&&!socket.isConnected()) {
				TVWakeup wakeUp=new TVWakeup(mac!=null&&cmds[0]==KeyCode.KEYCODE_TURN_ON?mac:null);
				try {
					for(int i=0;i<10;i++) try {
						socket.connect(new InetSocketAddress(ip,6466),5000);
						InputStream socketInput=socket.getInputStream();
						new Thread("androidtv-receiver-"+ip) {
							{
								setDaemon(true);
							}
							public void run() {
								try(InputStream in=socketInput) {
									while(true) {
										byte[] msg=readMsg(in);
										if(msg!=null) {
											handleResponse(msg);
										} else {
											break;
										}
									}
								} catch(Throwable t) {
									if(!stopped) {
										LOGGER.error("Message reader failed.",t);
									}
								} finally {
									LOGGER.info("Terminate current connection to {}",ip);
									stopped=true;
								}
							}
						}.start();
						return this;
					} catch(Throwable t) {
						socket=context.getSocketFactory().createSocket();
						if(i<9) {
							LOGGER.warn("Login to {} failed (retry=true)",ip);
						} else {
							LOGGER.warn("Login to "+ip+" failed (retry=false)", t);
						}
						if(i==0) {
							wakeUp.start();
						}
						try {
							Thread.sleep(5000);
						} catch(Throwable t0) {
						}
					}
				} finally {
					wakeUp.shutdown();
				}
				throw new IOException();
			} else {
				return this;
			}
		}

		
		private Connection cmd(Message[] cmds) throws Throwable {
			for(Message keyCode:cmds) {
				if(!cmd(keyCode)) {
					break;
				}
			}
			return this;
		}

		boolean cmd(Message code) throws Throwable {
			try {
				if(readyLatch.await(60000,TimeUnit.MILLISECONDS)) {
					return code.perform(this);
				}
				LOGGER.info("Awaiting the latch failed after 60sec.");
			} catch(InterruptedException e) {
			}
			LOGGER.warn("Key {} ignored.",code);
			// Do not ignore next keys
			return true;
		}
		
		protected boolean writeCommand(byte[] cmd) {
			return writeCommand(true,cmd);
		}
		
		protected boolean writeCommand(boolean log,byte[] cmd) {
			if(socket!=null) try {
				synchronized(socket) {
					OutputStream stream=socket.getOutputStream();
					stream.write(cmd.length);
					stream.write(cmd);
					stream.flush();
					if(log) {
						lastCmd=System.currentTimeMillis();
					}
					return true;
				}
			} catch(Throwable t) {
				LOGGER.error("write command failed.",t);
			}
			return false;
		}
		
		protected byte[] readMsg(InputStream stream) throws Throwable {
			int l=stream.read();
			if(l<0) {
				return null;
			}
			byte[] result=new byte[l];
			int o=0;
			while(o<l) {
				int n=stream.read(result, o,l-o);
				if(n<0) {
					throw new EOFException();
				}
				o+=n;
			}
			return result;
		}

		public String getIp() {
			return ip;
		}

		public boolean waitForRunningTV() throws InterruptedException {
			if(!running) try {
				synchronized(runLock) {
					runLock.wait(WAIT_TIME-(System.currentTimeMillis()-lastCmd));
					return running;
				}
			} catch(InterruptedException e) {
				return false;
			}
			return true;
		}		
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		if(intent.getIntent().startsWith(intentBase)) {
			if((intentBase+":executeCommand").equals(intent.getIntent())) {
				Slot slot=intent.getSlot("cmd");
				if(slot!=null) {
					cmd(slot.getValue());
				}
			}
			return true;
		}
		return false;
	}
}
