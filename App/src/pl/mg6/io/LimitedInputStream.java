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

/**
 *
 * @author maciej
 */
public class LimitedInputStream extends InputStream {

	private InputStream is;
	private int limit;

	public LimitedInputStream(InputStream stream, int readLimit) {
		is = stream;
		limit = readLimit;
	}

	public int available() throws IOException {
		int avail = is.available();
		if (avail > limit) {
			avail = limit;
		}
		return avail;
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
		if (limit > 0) {
			limit--;
			return is.read();
		}
		return -1;
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (limit > 0) {
			if (len > limit) {
				len = limit;
			}
			len = is.read(b, off, len);
			if (len != -1) {
				limit -= len;
			}
			return len;
		}
		return -1;
	}

	public synchronized void reset() throws IOException {
		super.reset();
	}

	public long skip(long n) throws IOException {
		if (limit > 0) {
			if (n > limit) {
				n = limit;
			}
			n = is.skip(n);
			limit -= n;
			return n;
		}
		return 0;
	}
}
