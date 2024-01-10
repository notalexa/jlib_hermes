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
package not.alexa.hermes.media.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class handling ID3 tags.
 * 
 */
class ID3 {
	byte[] buffer;

	
	public final int parse(InputStream in) throws IOException {
		if(in.markSupported()) {
			in.mark(-1);
			buffer=new byte[2048];
			return parseInternal(in);
		}
		return 0;
	}
	
	
    private void readFully(InputStream in,int n) throws IOException {
    	readFully(in,n,false);
    }
    
    private int readFully(InputStream in,int n,boolean optional) throws IOException {
    	int c=0;
    	int i;
    	while((i=in.read(buffer, c, n-c))>=0) {
    		c+=i;
    		if(c==n) {
    			return n;
    		}
    	}
    	if(optional) {
    		return c;
    	} else {
    		throw new IOException();
    	}
    }
    
    private void skipFully(InputStream in,int n) throws IOException {
    	long c=0;
    	long i;
    	while((i=in.skip(n-c))>=0) {
    		c+=i;
    		if(c==n) {
    			return;
    		}
    	}
    	throw new IOException();
    }

    private int parseInternal(InputStream in) throws IOException {
        if (readFully(in,6,true) != 6) {
        	in.reset();
        	return 0;
        }
        if (!new String(buffer,0,3).equals("ID3")) {
            in.reset();
            return 0;
        }
        boolean extendedHeader=0!=(buffer[5]&0x40);
        boolean footer=0!=(buffer[5]&0x10);
        readFully(in, 4);
        int tagSize = ((buffer[0]&0x7f) << 1) + ((buffer[1]&0x7f) << 14) + ((buffer[2]&0x7f) << 7) + (buffer[3]&0x7f);
        int offset=tagSize+10;
        //tagSize -= 10;
        if(extendedHeader) {
        	readFully(in, 4);
        	int len=((buffer[0]&0x7f) << 21) + ((buffer[1]&0x7f) << 14) + ((buffer[2]&0x7f) << 7) + (buffer[3]&0x7f);
        	skipFully(in,len);
        	tagSize-=len;
        }
        int c=0;
        while(c<tagSize) {
        	c+=readTag(in,tagSize-c);
        }
        // Check for footer
        if(footer) {
        	readFully(in,10,false);
        	return offset+10;
        }
        return offset;
    }
    
    protected int readTag(InputStream in,int remaining) throws IOException {
        readFully(in,4);
        if(buffer[0]==0) {
        	// Padding
        	skipFully(in, remaining);
        	return remaining;
        }
        String tag=new String(buffer,0,4);
        readFully(in, 6);
        int tagSize = ((buffer[0]&0xff) << 24) + ((buffer[1]&0xff) << 16) + ((buffer[2]&0xff) << 8) + (buffer[3]&0xff);
        if(tagSize+10>remaining) {
        	throw new IOException();
        }
        if(tagSize<buffer.length) {
        	readFully(in, tagSize);
        	handleTag(tag,buffer,tagSize);
        } else {
        	skipFully(in,tagSize);
    	}
        return tagSize+10;        
    }
    
    protected void handleTag(String tag,byte[] data,int size) throws IOException {
    	
    }
    
    protected String id3String(byte[] data,int size) throws IOException {
    	if(data[0]==0) {
    		return new String(data,1,size-1);
    	} else if(data[0]==1) {
    		return new String(data,1,size-1,"UTF-16");
    	} else {
    		return null;
    	}    	
    }

}
