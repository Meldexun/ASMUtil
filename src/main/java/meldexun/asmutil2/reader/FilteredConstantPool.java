/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2.reader;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

class FilteredConstantPool {

	private int[] keys = new int[8];
	private Object[] values = new Object[8];
	private int size;

	private FilteredConstantPool() {

	}

	static FilteredConstantPool read(DataInput in, FilteringConstantReader reader) throws IOException {
		FilteredConstantPool constantPool = new FilteredConstantPool();
		int constants = in.readUnsignedShort();
		for (int i = 1; i < constants; i++) {
			byte type = in.readByte();
			Object constant = reader.readConstant(in, type);
			if (constant != null) {
				constantPool.put(i, constant);
			}
			if (type == 5 || type == 6) { // long, double
				i++;
			}
		}
		return constantPool;
	}

	private void put(int k, Object v) {
		if (this.size == this.keys.length) {
			int newCapacity = this.size << 1;
			this.keys = Arrays.copyOf(this.keys, newCapacity);
			this.values = Arrays.copyOf(this.values, newCapacity);
		}
		this.keys[this.size] = k;
		this.values[this.size] = v;
		this.size++;
	}

	Object get(int k) {
		int i = Arrays.binarySearch(this.keys, 0, this.size, k);
		return i != -1 ? this.values[i] : null;
	}

	String getClass(int i) {
		if (i == 0) {
			return null;
		}
		return this.getUtf8((int) this.get(i));
	}

	String getUtf8(int i) {
		if (i == 0) {
			return null;
		}
		return new String((byte[]) this.get(i));
	}

}
