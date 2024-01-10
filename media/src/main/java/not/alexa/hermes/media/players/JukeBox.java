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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.PlayerState;
import not.alexa.hermes.media.streams.MP3AudioStream;
import not.alexa.hermes.media.streams.Silence;

/**
 * A jukebox resolves audio streams from files. 
 * To configure, set the jukebox {@code name} and {@code baseDir}.
 * A jukebox can be turned on using the url {@code player:&lt;name&gt;}. In this case a random file is choosen if no file is currently set,
 * otherwise, the current track is resumed.
 * Additionally tracks can be selected using {@code track:&lt;name&gt;} (without file extension) and albums with {@code album:&lt;name&gt;}. In both cases,
 * the name must match the file resp. directory name exactly.
 * <br>It's possible to define different juke boxes (to distinguish between different genres for example). For example
 * <pre>
 * players:
 * - class: not.alexa.hermes.media.players.JukeBox
 *   name: jukebox
 *   baseDir: ${user.home}/Music
 * - class: not.alexa.hermes.media.players.JukeBox
 *   name: pop
 *   baseDir: ${user.home}/Music/Pop
 * - class: not.alexa.hermes.media.players.JukeBox
 *   name: classical_music
 *   baseDir: ${user.home}/Music/Classical_Music
 * </pre>
 * (in {@link AudioPlayers}) defines three boxes, one for all music, one for pop and one for classic.
 * 
 * <p><b>Restrictions:</b> Currently onyl MP3 files are supported and the peek operations are not implemented.
 * 
 * @author notalexa
 *  
 */
public class JukeBox extends AbstractPlayer<File> implements AudioPlayer {
	private static Logger LOGGER=LoggerFactory.getLogger(JukeBox.class);
	
	private static final FileFilter TRACK_FILTER=new FileFilter() {
		
		@Override
		public boolean accept(File f) {
			return f.isFile()&&f.canRead()&&f.getName().endsWith(".mp3")&&f.length()>10240;
		}
	};
	private static final java.util.Comparator<File> NAME_COMPARATOR=new java.util.Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	};
	private static final java.util.Comparator<File> REVERSE_NAME_COMPARATOR=new java.util.Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			return NAME_COMPARATOR.compare(o2, o1);
		}
	};
	
	@JsonProperty String baseDir;
	@JsonProperty String name;
	private Random random=new SecureRandom();
	private boolean repeatTrack;
	private boolean repeatAlbum;
	private Track currentTrack;
	private File base;
	
	protected JukeBox() {
		state.capabilities|=PlayerState.NEXT_ALBUM|PlayerState.NEXT_TRACK
				|PlayerState.PREVIOUS_ALBUM|PlayerState.PREVIOUS_TRACK
				|PlayerState.REPEAT_ALBUM|PlayerState.REPEAT_TRACK
				|PlayerState.RANDOM_ALBUM|PlayerState.RANDOM_TRACK
				|PlayerState.REPLAY|PlayerState.FIRST
				|PlayerState.SEEK|PlayerState.SHUFFLE;
	}
	
	public JukeBox(String name,File baseDir) {
		this();
		this.name=name;
		this.baseDir=baseDir.getPath();
		this.base=baseDir;
	}
	
	private synchronized File baseDir() {
		if(base==null) {
			base=new File(baseDir);
		}
		return base;
	}
		
	private void play(Track track) {
		if(track!=null) {
			currentTrack=track;
			history.add(track.f);
			play(track.open());
			LOGGER.info("Play {}",track);
		}
	}
	
	@Override
	public boolean isStream() {
		return false;
	}
	
	@Override
	public void nextTrack() {
		if(currentTrack==null) {
			randomTrack();
		} else {
			play(currentTrack.next(1));
		}
	}

	@Override
	public void repeatTrack(boolean repeat) {
		if(repeat!=repeatTrack) {
			repeatTrack=repeat;
			fireStateChanged();
		}
	}

	@Override
	public void repeatAlbum(boolean repeat) {
		if(repeat!=repeatAlbum) {
			repeatAlbum=repeat;
			fireStateChanged();
		}
	}

	@Override
	public void previousTrack() {
		if(currentTrack==null) {
			randomTrack();
		} else {
			File f=history.pop();
			if(f==null) {
				play(currentTrack.next(-1));
			} else {
				play(new Track(f));
			}
		}
	}
	
	@Override
	public void firstTrack() {
		if(currentTrack!=null) {
			play(currentTrack.firstTrack());
		}
	}
	
	@Override
	public void randomTrack() {
		Track track=new Album(baseDir()).randomTrack();
		if(track!=null) {
			play(track);
		}  else {
			LOGGER.warn("No tracks in {}",baseDir().getAbsolutePath());
		}
	}

	@Override
	public void randomAlbum() {
		Album album=new Album(baseDir()).next(0);
		Track track=album==null?null:album.first(null);
		if(track!=null) {
			play(track);
		}
	}

	@Override
	public void nextAlbum() {
		Album album=currentTrack==null?null:currentTrack.getAlbum().next(1);
		Track track=album==null?null:album.first(null);
		if(track!=null) {
			play(track);
		}
	}

	@Override
	public void previousAlbum() {
		Album album=currentTrack==null?null:currentTrack.getAlbum().next(-1);
		Track track=album==null?null:album.first(null);
		if(track!=null) {
			play(track);
		}
	}

	@Override
	public int supports(String src) {
		if(src.startsWith("player:")&&name.equalsIgnoreCase(src.substring("player:".length()))) {
			return 0;
		} else if(src.startsWith("album:")) {
			return findAlbum(baseDir(),src.substring("album:".length()))!=null?0:Integer.MAX_VALUE;
		} else if(src.startsWith("track:")) {
			return findTrack(baseDir(),src.substring("track:".length())+".mp3")!=null?0:Integer.MAX_VALUE;
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	protected Album findAlbum(File f,String name) {
		if(f.isDirectory()) {
			if(f.getName().equalsIgnoreCase(name)) {
				return new Album(f);
			} else for(File c:f.listFiles()) {
				Album album=findAlbum(c, name);
				if(album!=null) {
					return album;
				}
				
			}
		}
		return null;
	}
	
	protected File findTrack(File f,String name) {
		if(f.isDirectory()) {
			for(File c:f.listFiles()) {
				File track=findTrack(c, name);
				if(track!=null) {
					return track;
				}
			}
		} else if(f.getName().equalsIgnoreCase(name)) {
			return f;
		}
		return null;
	}

	@Override
	public boolean play(String src) {
		File mediaSource=null;
		if(src.startsWith("player:")&&name.equalsIgnoreCase(src.substring("player:".length()))) {
			if(!hasAudio()) {
				randomTrack();
			}
			controls.onAudioFormatChanged(getFormat());
			controls.onStateChanged();
			return true;
		} else if(src.startsWith("album:")) {
			Album album=findAlbum(baseDir(),src.substring("album:".length()));
			mediaSource=album!=null&&!album.isEmpty()?album.files[0]:null;//(baseDir(),src.substring("album:".length())).files[0];
		} else if(src.startsWith("track:")) {
			mediaSource=findTrack(baseDir(),src.substring("track:".length())+".mp3");
		}
		if(mediaSource!=null) {
			play(new Track(mediaSource));
			return true;
		}
		return false;
	}
	
	protected Album getAlbum(File f) {
		return new Album(f.getParentFile(),f.getParentFile().listFiles(TRACK_FILTER));
	}
	
	public class Album {
		File  dir;
		File[] files;
		public Album(File parent) {
			this(parent,parent.listFiles(TRACK_FILTER));
		}
		
		public Album(File parent,File[] files) {
			this.dir=parent;
			this.files=files==null?new File[0]:files.clone();
			Arrays.sort(this.files,NAME_COMPARATOR);
		}
		
		public boolean isEmpty() {
			return files.length==0;
		}
		
		public Track shuffle(Track current) {
			if(files.length<=1) {
				return current;
			} else {
				while(true) {
					File f=files[random.nextInt(files.length)];
					if(!f.equals(current.f)) {
						return new Track(this,f);
					}
				}
			}
		}
		
		public Track first(Track current) {
			return files.length==0?current:new Track(this,files[0]);
		}
		
		public Album next(int step) {
			Album album=step>0?goUp(dir,true, step):null;
			if(album==null)  {
				if(dir.equals(base)||step==0)  {
					album=goUp(dir,true, step);
				} else {
					album=findAlbum(dir, step);
				}
			}
			return album==null?this:album;
		}
		
		private Album findAlbum(File dir,int step)  {
			File p=dir.getParentFile();
			File[] directories=p.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory();
				}
			});
			Arrays.sort(directories,step>0?NAME_COMPARATOR:REVERSE_NAME_COMPARATOR);
			for(int i=0;i<directories.length;i++) {
				if(directories[i].getName().equals(dir.getName())) {
					for(int j=i+1;j<i+directories.length+1;j++) {
						if(j==directories.length) {
							if(!p.equals(base)) {
								return findAlbum(p, step);
							} else {
								Album candidate=new Album(base);
								if(!candidate.isEmpty()) {
									return candidate;
								}
							}
						}
						Album candidate=goUp(directories[j%directories.length],false,step);
						if(candidate!=null) {
							return candidate;
						}
					}
					return null;
				}
			}
			return null;
		}
		
		private Album goUp(File dir,boolean ignoreDir,int step) {
			AtomicBoolean  admissable=new AtomicBoolean(false);
			File[] directories=dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					admissable.set(admissable.get()||TRACK_FILTER.accept(f));
					return f.isDirectory();
				}
			});
			if(admissable.get()&&!ignoreDir) {
				return new Album(dir);
			} else if(admissable.get()||directories.length>0) {
				Arrays.sort(directories,step>0?NAME_COMPARATOR:REVERSE_NAME_COMPARATOR);
				int offset=step==0?random.nextInt(directories.length+(admissable.get()?1:0)):0;
				if(step==0&&offset==directories.length) {
					return new Album(dir);
				}
				for(int i=offset;i<offset+directories.length;i++) {
					Album candidate=goUp(directories[i%directories.length],false,step);
					if(candidate!=null) {
						return candidate;
					}
				}
			}
			return null;
		}
		
		public Track randomTrack() {
			Album album=next(0);
			if(album!=null) {
				File f=album.files[random.nextInt(album.files.length)];
				return new Track(album,f);
			}
			return null;
		}
		
		public Track next(Track current,int step) {
			if(files.length==0) {
				return current;
			} else {
				int next=getIndex(current.f)+step;
				if(next>=0&&next<files.length)  {
					return new Track(this,files[next]);
				} else {
					return null;//files[next%files.length];
				}
			}
		}
		
		protected int getIndex(File current) {
			for(int i=0;i<files.length;i++) {
				if(files[i].equals(current)) {
					return i;
				}
			}
			return 0;
		}
		
		public String toString() {
			return "Album["+dir.getAbsolutePath()+"]";
		}
	}
	
	public class Track {
		Album album;
		File f;
		
		public Track(File f) {
			this(null,f);
		}

		public Track(Album album,File f) {
			this.album=album;
			this.f=f;
		}
		
		public AudioStream open() {
			Track candidate=this;
			for(int i=0;i<10;i++) try {
				RandomAccessFile file=new RandomAccessFile(candidate.f,"r");
				return new MP3AudioStream(new FileInputStream(file.getFD()) {
					@Override
					public void close() throws IOException {
						super.close();
						file.close();
					}
				},candidate.getInfo(), 1f, candidate.f.length()) {
					@Override
					protected boolean seekBytes(long position) {
						try {
							file.seek(position);
							return true;
						} catch(Throwable t) {
							return false;
						}
					}
				};
			} catch(Throwable t) {
				LoggerFactory.getLogger(JukeBox.class).error("Getting "+f.getAbsolutePath()+" failed.",t);
				candidate=new Album(baseDir()).randomTrack();
			}
			return new Silence();
		}
		
		public String getInfo() {
			String name=f.getName();
			if(name.endsWith(".mp3")) {
				name=name.substring(0,name.length()-4);
			}
			if(name.lastIndexOf(" - ")>0) {
				name=name.substring(name.lastIndexOf(" - ")+3).trim();
			}
			return name.trim();
		}
		
		public Album getAlbum() {
			if(album==null) {
				album=JukeBox.this.getAlbum(f);
			}
			return album;
		}
		public Track firstTrack() {
			return getAlbum().first(this);
		}
		
		public Track next(int step) {
			if(repeatTrack) {
				return this;
			} else if(shuffle) {
				return shuffle();
			} else {
				if(album==null) {
					album=JukeBox.this.getAlbum(f);
				}
				Track next=album.next(this, step);
				if(next==null) {
					if(repeatAlbum)  {
						return album.first(this);
					} else {
						return album.next(step<0?-1:1).first(this);
					}
				} else {
					return next;
				}
			}
		}
		public Track shuffle() {
			return getAlbum().shuffle(this);
		}
		
		public String toString() {
			return "Audio["+f.getAbsolutePath()+"]";
		}
	}
}
