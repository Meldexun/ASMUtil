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

class IOUtil {

	static InputStream openStream(URL url) throws IOException, URISyntaxException {
		if (url == null) {
			return null;
		}
		if (url.getProtocol().equals("file")) {
			return Files.newInputStream(Paths.get(url.toURI()));
		}
		return url.openStream();
	}

	static void skip(DataInput in, int n) throws IOException {
		for (int i = 0; i < n; i++) {
			in.readByte();
		}
	}

	static byte[] read(DataInput in, int n) throws IOException {
		byte[] bytes = new byte[n];
		in.readFully(bytes);
		return bytes;
	}

}
