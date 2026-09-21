/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Internal utility class for file operations.
 */
class FileUtil {

	/**
	 * Recursively deletes a directory and all its contents.
	 *
	 * @param dir the directory to delete
	 * @throws IOException if an I/O error occurs
	 */
	static void deleteDirectory(Path dir) throws IOException {
		if (Files.exists(dir)) {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	/**
	 * Writes data to a file, creating parent directories if needed.
	 *
	 * @param file the file path to write
	 * @param data the data to write
	 * @throws IOException if an I/O error occurs
	 */
	static void writeFile(Path file, byte[] data) throws IOException {
		Files.createDirectories(file.getParent());
		Files.write(file, data);
	}

}
