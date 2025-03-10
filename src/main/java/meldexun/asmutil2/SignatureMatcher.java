/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public interface SignatureMatcher<T> extends Predicate<T>, Consumer<StringBuilder> {

	interface Signature {

		String owner();

		String name();

		String desc();

		static Signature of(String owner, String name, String desc) {
			return new Signature() {
				@Override
				public String owner() {
					return owner;
				}

				@Override
				public String name() {
					return name;
				}

				@Override
				public String desc() {
					return desc;
				}
			};
		}

		static Signature of(FieldNode field) {
			return of(null, field.name, field.desc);
		}

		static Signature of(MethodNode method) {
			return of(null, method.name, method.desc);
		}

		static Signature of(FieldInsnNode fieldInsn) {
			return of(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
		}

		static Signature of(MethodInsnNode methodInsn) {
			return of(methodInsn.owner, methodInsn.name, methodInsn.desc);
		}

	}

	void accept(StringBuilder sb);

	static SignatureMatcher<FieldNode> matchingFieldName(String name) {
		return matchingName(Signature::of, name);
	}

	static SignatureMatcher<FieldNode> matchingFieldNameDesc(String name, String desc) {
		return matchingNameDesc(Signature::of, name, desc);
	}

	static SignatureMatcher<FieldNode> matchingFieldOwnerNameDesc(String owner, String name, String desc) {
		return matchingOwnerNameDesc(Signature::of, owner, name, desc);
	}

	static SignatureMatcher<FieldNode> matchingFieldNameObf(String name, String obfName) {
		return matchingNameObf(Signature::of, name, obfName);
	}

	static SignatureMatcher<FieldNode> matchingFieldNameDescObf(String name, String obfName, String desc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc);
	}

	static SignatureMatcher<FieldNode> matchingFieldNameDescObf(String name, String obfName, String desc,
			String obfDesc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<FieldNode> matchingFieldOwnerNameDescObf(String owner, String name, String obfName,
			String desc) {
		return matchingOwnerNameDescObf(Signature::of, owner, name, obfName, desc);
	}

	static SignatureMatcher<FieldNode> matchingFieldOwnerNameDescObf(String owner, String obfOwner, String name,
			String obfName, String desc, String obfDesc) {
		return matchingOwnerNameDescObf(Signature::of, owner, obfOwner, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<MethodNode> matchingMethodName(String name) {
		return matchingName(Signature::of, name);
	}

	static SignatureMatcher<MethodNode> matchingMethodNameDesc(String name, String desc) {
		return matchingNameDesc(Signature::of, name, desc);
	}

	static SignatureMatcher<MethodNode> matchingMethodOwnerNameDesc(String owner, String name, String desc) {
		return matchingOwnerNameDesc(Signature::of, owner, name, desc);
	}

	static SignatureMatcher<MethodNode> matchingMethodNameObf(String name, String obfName) {
		return matchingNameObf(Signature::of, name, obfName);
	}

	static SignatureMatcher<MethodNode> matchingMethodNameDescObf(String name, String obfName, String desc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc);
	}

	static SignatureMatcher<MethodNode> matchingMethodNameDescObf(String name, String obfName, String desc,
			String obfDesc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<MethodNode> matchingMethodOwnerNameDescObf(String owner, String name, String obfName,
			String desc) {
		return matchingOwnerNameDescObf(Signature::of, owner, name, obfName, desc);
	}

	static SignatureMatcher<MethodNode> matchingMethodOwnerNameDescObf(String owner, String obfOwner, String name,
			String obfName, String desc, String obfDesc) {
		return matchingOwnerNameDescObf(Signature::of, owner, obfOwner, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnName(String name) {
		return matchingName(Signature::of, name);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnNameDesc(String name, String desc) {
		return matchingNameDesc(Signature::of, name, desc);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnOwnerNameDesc(String owner, String name, String desc) {
		return matchingOwnerNameDesc(Signature::of, owner, name, desc);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnNameObf(String name, String obfName) {
		return matchingNameObf(Signature::of, name, obfName);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnNameDescObf(String name, String obfName, String desc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnNameDescObf(String name, String obfName, String desc,
			String obfDesc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnOwnerNameDescObf(String owner, String name, String obfName,
			String desc) {
		return matchingOwnerNameDescObf(Signature::of, owner, name, obfName, desc);
	}

	static SignatureMatcher<FieldInsnNode> matchingFieldInsnOwnerNameDescObf(String owner, String obfOwner, String name,
			String obfName, String desc, String obfDesc) {
		return matchingOwnerNameDescObf(Signature::of, owner, obfOwner, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnName(String name) {
		return matchingName(Signature::of, name);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnNameDesc(String name, String desc) {
		return matchingNameDesc(Signature::of, name, desc);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnOwnerNameDesc(String owner, String name, String desc) {
		return matchingOwnerNameDesc(Signature::of, owner, name, desc);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnNameObf(String name, String obfName) {
		return matchingNameObf(Signature::of, name, obfName);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnNameDescObf(String name, String obfName, String desc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnNameDescObf(String name, String obfName, String desc,
			String obfDesc) {
		return matchingNameDescObf(Signature::of, name, obfName, desc, obfDesc);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnOwnerNameDescObf(String owner, String name,
			String obfName, String desc) {
		return matchingOwnerNameDescObf(Signature::of, owner, name, obfName, desc);
	}

	static SignatureMatcher<MethodInsnNode> matchingMethodInsnOwnerNameDescObf(String owner, String obfOwner,
			String name, String obfName, String desc, String obfDesc) {
		return matchingOwnerNameDescObf(Signature::of, owner, obfOwner, name, obfName, desc, obfDesc);
	}

	static <T> SignatureMatcher<T> matchingName(Function<T, Signature> wrapper, String name) {
		return create(wrapper, signature -> {
			return name.equals(signature.name());
		}, sb -> {
			sb.append("name=").append(name);
		});
	}

	static <T> SignatureMatcher<T> matchingNameDesc(Function<T, Signature> wrapper, String name, String desc) {
		return create(wrapper, signature -> {
			return name.equals(signature.name()) && desc.equals(signature.desc());
		}, sb -> {
			sb.append("name=").append(name);
			sb.append(" ");
			sb.append("desc=").append(desc);
		});
	}

	static <T> SignatureMatcher<T> matchingOwnerNameDesc(Function<T, Signature> wrapper, String owner, String name,
			String desc) {
		return create(wrapper, signature -> {
			return owner.equals(signature.owner()) && name.equals(signature.name()) && desc.equals(signature.desc());
		}, sb -> {
			sb.append("owner=").append(owner);
			sb.append(" ");
			sb.append("name=").append(name);
			sb.append(" ");
			sb.append("desc=").append(desc);
		});
	}

	static <T> SignatureMatcher<T> matchingNameObf(Function<T, Signature> wrapper, String name, String obfName) {
		return create(wrapper, signature -> {
			return (obfName.equals(signature.name()) || name.equals(signature.name()));
		}, sb -> {
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
		});
	}

	static <T> SignatureMatcher<T> matchingNameDescObf(Function<T, Signature> wrapper, String name, String obfName,
			String desc) {
		return create(wrapper, signature -> {
			return (obfName.equals(signature.name()) || name.equals(signature.name())) && desc.equals(signature.desc());
		}, sb -> {
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=").append(desc);
		});
	}

	static <T> SignatureMatcher<T> matchingNameDescObf(Function<T, Signature> wrapper, String name, String obfName,
			String desc, String obfDesc) {
		return create(wrapper, signature -> {
			return (obfName.equals(signature.name()) || name.equals(signature.name()))
					&& (obfDesc.equals(signature.desc()) || desc.equals(signature.desc()));
		}, sb -> {
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=[").append(desc).append(", ").append(obfDesc).append("]");
		});
	}

	static <T> SignatureMatcher<T> matchingOwnerNameDescObf(Function<T, Signature> wrapper, String owner, String name,
			String obfName, String desc) {
		return create(wrapper, signature -> {
			return owner.equals(signature.owner())
					&& (obfName.equals(signature.name()) || name.equals(signature.name()))
					&& desc.equals(signature.desc());
		}, sb -> {
			sb.append("owner=").append(owner);
			sb.append(" ");
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=").append(desc);
		});
	}

	static <T> SignatureMatcher<T> matchingOwnerNameDescObf(Function<T, Signature> wrapper, String owner,
			String obfOwner, String name, String obfName, String desc, String obfDesc) {
		return create(wrapper, signature -> {
			return (obfOwner.equals(signature.owner()) || owner.equals(signature.owner()))
					&& (obfName.equals(signature.name()) || name.equals(signature.name()))
					&& (obfDesc.equals(signature.desc()) || desc.equals(signature.desc()));
		}, sb -> {
			sb.append("owner=[").append(owner).append(", ").append(obfOwner).append("]");
			sb.append(" ");
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=[").append(desc).append(", ").append(obfDesc).append("]");
		});
	}

	static <T> SignatureMatcher<T> create(Function<T, Signature> wrapper, Predicate<Signature> predicate,
			Consumer<StringBuilder> errorDetailAppender) {
		return new SignatureMatcher<T>() {
			@Override
			public boolean test(T t) {
				return predicate.test(wrapper.apply(t));
			}

			@Override
			public void accept(StringBuilder sb) {
				errorDetailAppender.accept(sb);
			}
		};
	}

}
