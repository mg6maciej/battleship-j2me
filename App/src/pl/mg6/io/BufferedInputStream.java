/*
 * Copyright (C) 2013 Maciej Górski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.mg6.io;

import java.io.IOException;
import java.io.InputStream;
import pl.mg6.Application;

/**
 *
 * @author maciej
 */
public class BufferedInputStream extends InputStream {

	private InputStream is;
	private byte[] buffer;
	private int bufferOffset;
	private int bufferLength;

	public static final int DEFAULT_BUFFER_SIZE = 1024;

	public BufferedInputStream(InputStream stream) {
		this(stream, DEFAULT_BUFFER_SIZE);
	}

	public BufferedInputStream(InputStream stream, int bufferSize) {
		is = stream;
		buffer = new byte[bufferSize];
	}

	public int available() throws IOException {
		return bufferLength - bufferOffset + is.available();
	}

	public void close() throws IOException {
		is.close();
	}

	public synchronized void mark(int readlimit) {
		is.mark(readlimit);
	}

	public boolean markSupported() {
		return is.markSupported();
	}

	public int read() throws IOException {
		if (bufferOffset == bufferLength) {
			bufferOffset = 0;
			bufferLength = is.read(buffer);
			//#debug
			Application.debug("BIS,read(),len:" + bufferLength, null);
			if (bufferLength == -1) {
				bufferLength = 0;
				return -1;
			}
		}
		int b = buffer[bufferOffset] & 0xff;
		bufferOffset++;
		return b;
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > b.length) {
			throw new IndexOutOfBoundsException();
		}
		int ret = 0;
		if (len > 0) {
			if (bufferOffset < bufferLength) {
				ret = Math.min(bufferLength - bufferOffset, len);
				System.arraycopy(buffer, bufferOffset, b, off, ret);
				bufferOffset += ret;
				off += ret;
				len -= ret;
			}
			if (len > 0) {
				int x = is.read(b, off, len);
				if (x == -1) {
					if (ret == 0) {
						ret = -1;
					}
				} else {
					ret += x;
				}
			}
		}
		return ret;
	}

	public synchronized void reset() throws IOException {
		is.reset();
		bufferOffset = bufferLength;
	}

	public long skip(long n) throws IOException {
		long ret = 0;
		if (n > 0) {
			if (bufferOffset < bufferLength) {
				ret = Math.min(bufferLength - bufferOffset, n);
				bufferOffset += ret;
				n -= ret;
			}
			if (n > 0) {
				ret += is.skip(n);
			}
		}
		return ret;
	}
}
