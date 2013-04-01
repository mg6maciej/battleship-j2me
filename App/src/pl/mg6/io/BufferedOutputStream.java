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
import java.io.OutputStream;


/**
 *
 * @author maciej
 */
public class BufferedOutputStream extends OutputStream {

	private OutputStream os;
	private byte[] buffer;
	private int bufferOffset;

	public static final int DEFAULT_BUFFER_SIZE = 1024;

	public BufferedOutputStream(OutputStream stream) {
		this(stream, DEFAULT_BUFFER_SIZE);
	}

	public BufferedOutputStream(OutputStream stream, int bufferSize) {
		os = stream;
		buffer = new byte[bufferSize];
	}

	public void close() throws IOException {
		os.close();
	}

	public void flush() throws IOException {
		if (bufferOffset > 0) {
			os.write(buffer, 0, bufferOffset);
			os.flush();
			bufferOffset = 0;
		}
	}

	public void write(int b) throws IOException {
		if (bufferOffset == buffer.length) {
			flush();
		}
		buffer[bufferOffset] = (byte) b;
	}

	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > b.length) {
			throw new IndexOutOfBoundsException();
		}
		if (len > 0) {
			if (bufferOffset > 0 && bufferOffset + len > buffer.length) {
				os.write(buffer, 0, bufferOffset);
				bufferOffset = 0;
			}
			if (len > buffer.length) {
				os.write(b, off, len);
			} else {
				System.arraycopy(b, off, buffer, bufferOffset, len);
				bufferOffset += len;
			}
		}
	}
}
