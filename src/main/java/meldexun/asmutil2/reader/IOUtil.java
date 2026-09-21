/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2.reader;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Internal I/O utilities for reading class files.
 */
class IOUtil {

	/**
	 * Opens an input stream for a URL, using file system access for file:// URLs.
	 *
	 * @param url the URL to open
	 * @return an input stream, or null if url is null
	 * @throws IOException if an I/O error occurs
	 * @throws URISyntaxException if the URL cannot be converted to a URI
	 */
	static InputStream openStream(URL url) throws IOException, URISyntaxException {
		if (url == null) {
			return null;
		}
		if (url.getProtocol().equals("file")) {
			return Files.newInputStream(Paths.get(url.toURI()));
		}
		return url.openStream();
	}

	/**
	 * Skips n bytes from the input.
	 *
	 * @param in the input to skip from
	 * @param n the number of bytes to skip
	 * @throws IOException if an I/O error occurs
	 */
	static void skip(DataInput in, int n) throws IOException {
		for (int i = 0; i < n; i++) {
			in.readByte();
		}
	}

	/**
	 * Reads n bytes from the input into a byte array.
	 *
	 * @param in the input to read from
	 * @param n the number of bytes to read
	 * @return a byte array containing the read bytes
	 * @throws IOException if an I/O error occurs
	 */
	static byte[] read(DataInput in, int n) throws IOException {
		byte[] bytes = new byte[n];
		in.readFully(bytes);
		return bytes;
	}

}
