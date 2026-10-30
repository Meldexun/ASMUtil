# ASMUtil
[![Maven Central](https://img.shields.io/maven-central/v/io.github.meldexun/asmutil)](https://central.sonatype.com/artifact/io.github.meldexun/asmutil)

A small utility library on top of the [ASM](https://asm.ow2.io/) tree API for writing bytecode transformers
("patches") with little boilerplate. It was written for Minecraft modding but has no dependency on Minecraft and
works anywhere you can get hold of class bytes before they are loaded: Java agents, custom class loaders, build-time
bytecode processing, LaunchWrapper/Forge coremods.

What it gives you:

- **Transformer base classes** that do the reading, writing and error handling for you, so a patch is just
  "class name + method name + lambda".
- **`InsnFinder`**, a fluent way to locate instructions (`ASMUtil.first(method).methodInsn("foo").find()`), with
  useful error messages when a target can't be found.
- **Helpers to build and splice instruction lists** (`listOf`, `listWithLabel`, `replace`, `remove`, ...).
- **`NonLoadingClassWriter`**, a `ClassWriter` that computes stack map frames without loading classes.
- **Debugging helpers** to print methods and dump transformed classes to disk.

This README is a short tutorial built from how the library is used in practice. The Javadoc covers every method in
detail.

---

## Contents

1. [Setup](#1-setup)
2. [Names in ASM, a two-minute refresher](#2-names-in-asm-a-two-minute-refresher)
3. [Your first transformer](#3-your-first-transformer)
4. [Hooking the transformer up](#4-hooking-the-transformer-up)
5. [Finding methods](#5-finding-methods)
6. [Finding instructions](#6-finding-instructions)
7. [Building and inserting code](#7-building-and-inserting-code)
8. [Common patch recipes](#8-common-patch-recipes)
9. [Local variables](#9-local-variables)
10. [Write flags and `NonLoadingClassWriter`](#10-write-flags-and-nonloadingclasswriter)
11. [Registry reference: required, optional, priorities](#11-registry-reference-required-optional-priorities)
12. [Transforming many classes at once](#12-transforming-many-classes-at-once)
13. [Debugging](#13-debugging)
14. [Minecraft Forge (1.12.2) coremods](#14-minecraft-forge-1122-coremods)
15. [Pitfalls checklist](#15-pitfalls-checklist)

---

## 1. Setup

```groovy
dependencies {
    implementation 'io.github.meldexun:asmutil:2.8.1'
}
```

```xml
<dependency>
    <groupId>io.github.meldexun</groupId>
    <artifactId>asmutil</artifactId>
    <version>2.8.1</version>
</dependency>
```

ASMUtil declares ASM (`asm` and `asm-tree`, 6.2 or newer) and `log4j-api` as *compile-only* dependencies. Your
environment has to provide them at runtime. Minecraft/Forge already does; for a plain Java project, add them
yourself.

Because the library is tiny, it is usually shaded into the jar that contains the transformer. If you shade it, keep
the package name or relocate it consistently, and make sure the transformer classes themselves are never
transformed (see the Forge section for `TransformerExclusions`).

## 2. Names in ASM, a two-minute refresher

You will see three spellings of the same type, and mixing them up is the most common bug in transformers:

| Kind | Example | Used for |
|---|---|---|
| Binary name | `com.example.Foo$Bar` | Class names you **register** transformers for in ASMUtil |
| Internal name | `com/example/Foo$Bar` | `owner` of method/field instructions, `NEW`/`CHECKCAST` operands, `ClassNode.name` |
| Descriptor | `Lcom/example/Foo$Bar;` | Field types and method signatures: `(ILjava/lang/String;)V` |

Primitive descriptors: `Z` boolean, `B` byte, `C` char, `S` short, `I` int, `J` long, `F` float, `D` double,
`V` void, arrays are prefixed with `[` (`[I`, `[Ljava/lang/String;`).

`javap -c -p -l SomeClass.class` (or your IDE's bytecode viewer) shows the instructions you will be searching for.
Always look at the bytecode, not the source, before writing a patch.

## 3. Your first transformer

Say a third-party library has this class and you want to know every time something is put into the cache:

```java
package com.thirdparty.cache;

public class LruCache {
    public void put(Object key, Object value) {
        // ...
    }
}
```

Write the code you want to run as a plain static method. Keeping injected bytecode down to "load arguments, call
hook" makes patches short, readable and easy to debug:

```java
package com.example.patches;

public final class Hooks {
    public static void onPut(Object cache, Object key, Object value) {
        System.out.println("put " + key + " -> " + value);
    }
}
```

The transformer then only has to insert one call at the start of `put`:

```java
package com.example.patches;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import meldexun.asmutil2.ASMUtil;
import meldexun.asmutil2.HashMapClassNodeClassTransformer;
import meldexun.asmutil2.IClassTransformerRegistry;

public class MyTransformer extends HashMapClassNodeClassTransformer {

    @Override
    protected void registerTransformers(IClassTransformerRegistry registry) {
        registry.add("com.thirdparty.cache.LruCache", "put", "(Ljava/lang/Object;Ljava/lang/Object;)V",
                ClassWriter.COMPUTE_MAXS, methodNode -> {
            methodNode.instructions.insert(ASMUtil.listOf(
                    new VarInsnNode(Opcodes.ALOAD, 0), // this
                    new VarInsnNode(Opcodes.ALOAD, 1), // key
                    new VarInsnNode(Opcodes.ALOAD, 2), // value
                    new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "onPut",
                            "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false)));
        });
    }

}
```

What the base class does for you:

- It only parses classes that have registered transformers. Everything else passes through untouched.
- It checks that **exactly one** method matches. If the library changes and the method disappears (or, when
  matching by name only, gains an overload), you get a `ClassTransformException` naming the class and method
  instead of a silently missing patch.
- It writes the class with the requested `ClassWriter` flags (see [section 10](#10-write-flags-and-nonloadingclasswriter)).
- Exceptions are logged and rethrown as `ClassTransformException`.

All transformers registered for the same class share one `ClassNode`, and the class is written once.

> **Note:** `registerTransformers` is called from the superclass constructor, so instance fields of your subclass
> are not initialized yet when it runs. Use static fields or locals for configuration, or read the config inside it.

## 4. Hooking the transformer up

ASMUtil's `IClassTransformer` has a single method:

```java
byte[] transform(String obfName, String name, byte[] basicClass);
```

`name` is the binary name the transformers are registered under, and `obfName` is the name of the actual class file
(only different in obfuscated environments, otherwise pass the same name twice). It returns the new bytes, or
`basicClass` itself if nothing changed. The signature is identical to LaunchWrapper's `IClassTransformer`, so in
Minecraft you just add `implements net.minecraft.launchwrapper.IClassTransformer` (see section 14).

### As a Java agent

```java
public final class Agent {

    public static void premain(String args, Instrumentation instrumentation) {
        MyTransformer transformer = new MyTransformer();
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                if (className == null) {
                    return null; // lambdas and other hidden classes
                }
                String name = className.replace('/', '.');
                byte[] result = transformer.transform(name, name, classfileBuffer);
                return result == classfileBuffer ? null : result; // null = unchanged
            }
        });
    }

}
```

Note that the JVM silently ignores exceptions thrown by a `ClassFileTransformer` and loads the class untransformed.
ASMUtil logs the failure before rethrowing, so check the log when a patch doesn't seem to apply.

### In a custom class loader or a build step

Anything that has the bytes works the same way:

```java
byte[] patched = transformer.transform("com.thirdparty.cache.LruCache", "com.thirdparty.cache.LruCache", original);
```

## 5. Finding methods

Inside a class-level transformer, or to get a second method of the same class, use `ASMUtil.find*`. They all
**throw** `NoSuchElementException` instead of returning `null`, so there is no need for null checks:

```java
MethodNode put   = ASMUtil.find(classNode, "put");                        // first method named "put"
MethodNode put2  = ASMUtil.find(classNode, "put", "(Ljava/lang/Object;Ljava/lang/Object;)V");
MethodNode ctor  = ASMUtil.find(classNode, "<init>", "()V");                // constructor
MethodNode clinit = ASMUtil.find(classNode, "<clinit>");                    // static initializer
```

To handle "the method might not exist", catch the exception:

```java
MethodNode clinit;
try {
    clinit = ASMUtil.find(classNode, "<clinit>");
} catch (NoSuchElementException e) {
    clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
    clinit.instructions.add(new InsnNode(Opcodes.RETURN));
    classNode.methods.add(clinit);
}
```

For anything more involved, methods are just a list, so use streams:

```java
Optional<MethodNode> callback = ASMUtil.stream(classNode)
        .filter(m -> (m.access & Opcodes.ACC_STATIC) != 0)
        .filter(m -> m.desc.equals("()V"))
        .filter(m -> m.visibleAnnotations != null && m.visibleAnnotations.stream()
                .anyMatch(a -> a.desc.equals("Lcom/example/api/Callback;")))
        .findFirst();
```

### Obfuscated names

Every finder and registry method has an `Obf` variant that takes two names and accepts a member named **either**
one. There is no environment detection; both names are always accepted, so the same transformer runs against
obfuscated and deobfuscated builds of a library:

```java
MethodNode update = ASMUtil.findObf(classNode, "update", "a");
registry.addObf("com.thirdparty.Engine", "update", "a", "(F)V", ClassWriter.COMPUTE_FRAMES, methodNode -> { ... });
```

## 6. Finding instructions

`ASMUtil.first/last/next/prev(...)` return an `InsnFinder`. You add filters, then call `find()`:

```java
// first call to StringBuilder.append(String)
MethodInsnNode append = ASMUtil.first(methodNode)
        .methodInsn("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
        .find();

// last RETURN of the method
AbstractInsnNode ret = ASMUtil.last(methodNode).opcode(Opcodes.RETURN).find();

// the third ICONST_2 (ordinal counts from 0)
AbstractInsnNode two = ASMUtil.first(methodNode).opcode(Opcodes.ICONST_2).ordinal(2).find();
```

### Where to start and in which direction

| Factory | Direction | Starts at |
|---|---|---|
| `ASMUtil.first(m)` | forwards | first instruction (inclusive) |
| `ASMUtil.last(m)` | backwards | last instruction (inclusive) |
| `ASMUtil.next(m, insn)` | forwards | `insn` (inclusive) |
| `ASMUtil.prev(m, insn)` | backwards | `insn` (inclusive) |
| `ASMUtil.nextExclusive(m, insn)` | forwards | instruction after `insn` |
| `ASMUtil.prevExclusive(m, insn)` | backwards | instruction before `insn` |

`ordinal` is counted in search direction: `ASMUtil.last(m).varInsn(3).ordinal(1)` is the *second to last* access
of local 3.

### Filters

| Method | Matches |
|---|---|
| `opcode(int)` | instructions with that opcode |
| `type(Class)` | instructions of that node class, e.g. `LabelNode.class`, `JumpInsnNode.class` |
| `methodInsn(name)`, `(name, desc)`, `(owner, name, desc)` | method calls |
| `fieldInsn(name)`, `(name, desc)`, `(owner, name, desc)` | field reads/writes |
| `methodInsnObf(...)`, `fieldInsnObf(...)` | same, accepting either of two names |
| `ldcInsn(Object)` | `LDC` of that constant (`"text"`, `1.5D`, `1.5F`, `10000L`, `Type`) |
| `intInsn(int)` | `BIPUSH`/`SIPUSH`/`NEWARRAY` with that operand |
| `typeInsn(String)` | `NEW`/`CHECKCAST`/`INSTANCEOF`/`ANEWARRAY` of an internal name |
| `varInsn(int)`, `varInsn(String name)`, ... | loads/stores of a local variable |
| `predicate(p, errorDetails)` | anything you can express in Java |

Opcode and type filters can be combined with one of the `xyzInsn` filters, e.g.
`.opcode(Opcodes.INVOKESTATIC).methodInsn("helper")` or `.opcode(Opcodes.PUTFIELD).fieldInsn("count")`.

> **Each filter has only one slot.** Every `xyzInsn(...)` call sets both the type and the predicate, so a second
> one *replaces* the first: `.methodInsn("a").ldcInsn("b")` only searches for the `LDC`. To combine conditions,
> write one `predicate`.

Constants deserve a note: `ldcInsn` compares with `equals`, so the boxed type must match (`1.5` is a `Double`,
use `1.5F` for floats). Small ints are not loaded with `LDC` at all: `-1..5` are `ICONST_*` (match with
`opcode`), up to ±32767 use `BIPUSH`/`SIPUSH` (match with `intInsn`).

### Relative searches

Most real patches locate an anchor first and then search around it. You can chain finders:

```java
// the jump right after the call to hasNext()
JumpInsnNode jump = ASMUtil.first(methodNode)
        .methodInsn("hasNext")
        .findThenNext()
        .type(JumpInsnNode.class)
        .find();

// the label that starts the statement containing a call
MethodInsnNode call = ASMUtil.first(methodNode).methodInsn("begin").find();
LabelNode statementStart = ASMUtil.prev(methodNode, call).type(LabelNode.class).find();
```

Searching backwards for a `LabelNode` from an instruction is a common way to get to the start of the statement (in
code compiled with line numbers, each statement starts with a label). Inserting there means you don't land in the
middle of argument evaluation.

To skip over non-code nodes (labels, line numbers, frames), use a predicate:

```java
AbstractInsnNode realInsn = ASMUtil.next(methodNode, label)
        .predicate(insn -> insn.getOpcode() >= 0, null)
        .find();
```

### When nothing is found

`find()` never returns `null`. It throws a `NoSuchElementException` that describes the search:

```
No matching instruction found! start=0 advance=next type=MethodInsnNode opcode=INVOKEVIRTUAL owner=java/util/List name=add desc=(Ljava/lang/Object;)Z ordinal=0
```

Inside a registered transformer this becomes a `ClassTransformException` that says which class failed. Treat it as
"the target code changed, the patch needs updating".

## 7. Building and inserting code

### `listOf`

`InsnList`s are tedious to build by hand. `ASMUtil.listOf(...)` creates one from the given nodes:

```java
methodNode.instructions.insert(anchor, ASMUtil.listOf(
        new VarInsnNode(Opcodes.ALOAD, 0),
        new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "onTick", "(Ljava/lang/Object;)V", false)));
```

The standard `InsnList` methods do the inserting:

| Call | Inserts |
|---|---|
| `instructions.insert(list)` | at the very beginning of the method |
| `instructions.insert(anchor, list)` | right after `anchor` |
| `instructions.insertBefore(anchor, list)` | right before `anchor` |
| `instructions.add(list)` | at the very end |

An instruction node can only be in one list at a time. Don't reuse a node you found in the method inside
`listOf` (create a new one or `clone` it), and don't insert the same `InsnList` twice (it is empty after the first
insert).

### Labels without boilerplate

Jumps need a `LabelNode` that is both the jump target and part of the list. `listWithLabel` creates it for you:

```java
methodNode.instructions.insert(ASMUtil.listWithLabel(skip -> ASMUtil.listOf(
        new VarInsnNode(Opcodes.ALOAD, 1),
        new JumpInsnNode(Opcodes.IFNONNULL, skip), // if (arg1 != null) continue normally
        new InsnNode(Opcodes.RETURN),              // else return
        skip)));
```

`listWithLabels((l1, l2) -> ...)` supports up to four labels.

### Replacing and removing

```java
// replace one instruction with one or more
ASMUtil.replace(methodNode, target, new InsnNode(Opcodes.POP));
ASMUtil.replace(methodNode, target, ASMUtil.listOf(...));

// replace or remove a range (both ends inclusive)
ASMUtil.replace(methodNode, first, last, ASMUtil.listOf(...));
InsnList removed = ASMUtil.remove(methodNode, first, last); // removed code can be inserted elsewhere

// replace the whole body
ASMUtil.replaceAll(methodNode, ASMUtil.listOf(new InsnNode(Opcodes.ICONST_1), new InsnNode(Opcodes.IRETURN)));
```

If you replace or remove code that contained try-catch blocks or local variable scopes, remember that
`methodNode.tryCatchBlocks` and `methodNode.localVariables` still reference the old labels. Clear them (or remove
the affected entries), otherwise writing the class fails.

Removing must keep the stack balanced: if the removed range pushed a value that later code consumes, push a
replacement value.

## 8. Common patch recipes

The following recipes are the patterns that come up again and again in real transformers built on ASMUtil.

### Run code at the start of a method, optionally cancelling it

```java
// boolean Hooks.shouldCancel(Foo self) -> return early from a void method
registry.add("com.thirdparty.Foo", "process", "(Ljava/lang/String;)V", ClassWriter.COMPUTE_FRAMES, m -> {
    m.instructions.insert(ASMUtil.listWithLabel(proceed -> ASMUtil.listOf(
            new VarInsnNode(Opcodes.ALOAD, 0),
            new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "shouldCancel",
                    "(Lcom/thirdparty/Foo;)Z", false),
            new JumpInsnNode(Opcodes.IFEQ, proceed),
            new InsnNode(Opcodes.RETURN),
            proceed)));
});
```

Use `COMPUTE_FRAMES` whenever you add a jump.

### Run code before every return

Collect first, then modify, so you don't change the list you are iterating over:

```java
ASMUtil.stream(m)
        .filter(insn -> insn.getOpcode() == Opcodes.RETURN)
        .collect(Collectors.toList())
        .forEach(ret -> m.instructions.insertBefore(ret,
                new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "afterProcess", "()V", false)));
```

### Redirect a method call

Changing an existing instruction in place is the simplest patch of all. For a static call, point it to a static
method with the same descriptor:

```java
MethodInsnNode call = ASMUtil.first(m).methodInsn("java/lang/Thread", "sleep", "(J)V").find();
call.owner = "com/example/patches/Hooks";
// Hooks.sleep(long) now runs instead
```

For an instance call, turn it into a static call that takes the receiver as first argument:

```java
MethodInsnNode call = ASMUtil.first(m).opcode(Opcodes.INVOKEVIRTUAL)
        .methodInsn("com/thirdparty/Connection", "send", "(Ljava/lang/String;)V").find();
call.setOpcode(Opcodes.INVOKESTATIC);
call.owner = "com/example/patches/Hooks";
call.desc = "(Lcom/thirdparty/Connection;Ljava/lang/String;)V";
call.itf = false;
```

### Wrap the result of a call

Insert directly after the call. The return value is on the stack, so pass it through a hook that returns the same
type:

```java
MethodInsnNode call = ASMUtil.first(m).methodInsn("getTimeout", "()I").find();
m.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks",
        "adjustTimeout", "(I)I", false));
```

The same trick replaces a constant with a computed value:

```java
LdcInsnNode cst = ASMUtil.first(m).ldcInsn(1.5D).find();
m.instructions.insert(cst, ASMUtil.listOf(
        new InsnNode(Opcodes.POP2), // drop the double constant
        new VarInsnNode(Opcodes.ALOAD, 0),
        new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "getFactor", "(Ljava/lang/Object;)D", false)));
```

### Skip a block of code

Find the first and last instruction of the block and jump over it:

```java
LabelNode blockStart = ASMUtil.first(m).methodInsn("begin").findThenPrev().type(LabelNode.class).find();
LabelNode blockEnd   = ASMUtil.first(m).methodInsn("draw").findThenNext().type(LabelNode.class).find();
m.instructions.insert(blockStart, ASMUtil.listOf(
        new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "drawInstead", "()V", false),
        new JumpInsnNode(Opcodes.GOTO, blockEnd)));
```

With `COMPUTE_FRAMES`, ASM replaces the now unreachable code with `NOP`s and an `ATHROW`, which is fine.

### Add a method, an interface, a field

Class-level transformers receive the whole `ClassNode`. A common combination is "make the class implement my
interface and add the implementation", which lets the rest of your code call into the class without reflection:

```java
registry.add("com.thirdparty.Widget", ClassWriter.COMPUTE_MAXS, classNode -> {
    classNode.interfaces.add("com/example/api/HasOffset");

    MethodNode getOffset = new MethodNode(Opcodes.ACC_PUBLIC, "getOffset", "()I", null, null);
    getOffset.instructions.add(ASMUtil.listOf(
            new VarInsnNode(Opcodes.ALOAD, 0),
            new FieldInsnNode(Opcodes.GETFIELD, classNode.name, "offset", "I"),
            new InsnNode(Opcodes.IRETURN)));
    classNode.methods.add(getOffset);

    classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "offset", "I", null, null));
});
```

Use `COMPUTE_MAXS` (or `COMPUTE_FRAMES`) for new methods: a fresh `MethodNode` has `maxStack = maxLocals = 0`.

### Change access flags

No instructions involved, so no write flags are needed:

```java
registry.add("com.thirdparty.Server", 0, classNode -> {
    classNode.fields.stream()
            .filter(f -> f.name.equals("message"))
            .forEach(f -> f.access |= Opcodes.ACC_VOLATILE);
    ASMUtil.find(classNode, "setMessage").access &= ~Opcodes.ACC_SYNCHRONIZED;
    ASMUtil.find(classNode, "getMessage").access &= ~Opcodes.ACC_SYNCHRONIZED;
});
```

### Rewrite every access to a field

```java
registry.add("com.thirdparty.Client", 0, classNode -> {
    for (MethodNode method : classNode.methods) {
        ASMUtil.stream(method)
                .filter(FieldInsnNode.class::isInstance)
                .map(FieldInsnNode.class::cast)
                .filter(insn -> insn.owner.equals(classNode.name) && insn.name.equals("cachedPlayer"))
                .collect(Collectors.toList()) // collect first, we modify the list below
                .forEach(insn -> {
                    if (insn.getOpcode() == Opcodes.PUTSTATIC) {
                        ASMUtil.replace(method, insn, new InsnNode(Opcodes.POP)); // drop the write
                    } else {
                        ASMUtil.replace(method, insn, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "com/example/patches/Hooks", "currentPlayer", "()Lcom/thirdparty/Player;", false));
                    }
                });
    }
});
```

Alternatively, `ASMUtil.forEach(instructions, FieldInsnNode.class, (iterator, insn) -> ...)` hands you the list
iterator, which supports `iterator.remove()`, `iterator.set(...)` and `iterator.add(...)` while iterating.

## 9. Local variables

Hooks often need a local variable of the target method. If you know its slot (from `javap -l` or
`ASMUtil.methodToString`) you can hardcode it; parameters start at 0 for static methods and at 1 (after `this`)
for instance methods, and `long`/`double` take two slots each.

When the class has a local variable table, you can look variables up by name or type instead, which survives small
changes to the target code:

```java
int parser = ASMUtil.findLocalVariable(m, "jsonParser").index;
int folder = ASMUtil.findLocalVariable(m, "configFolder").index;
int world  = ASMUtil.findLocalVariableDesc(m, "Lcom/thirdparty/World;").index;

m.instructions.insertBefore(ASMUtil.first(m).opcode(Opcodes.RETURN).find(), ASMUtil.listOf(
        new VarInsnNode(Opcodes.ALOAD, parser),
        new VarInsnNode(Opcodes.ALOAD, folder),
        new MethodInsnNode(Opcodes.INVOKESTATIC, "com/example/patches/Hooks", "load",
                "(Lcom/google/gson/JsonParser;Ljava/io/File;)V", false)));
```

The finder can match accesses directly: `ASMUtil.last(m).varInsn("gui").ordinal(1).find()`.

Caveats:

- The local variable table is debug information. Gradle and Maven compile with it by default, plain `javac` only
  with `-g`, and obfuscators usually strip it. Without it, every name-based lookup throws.
- A name can appear several times (e.g. two loops both declaring `i`). Use the `ordinal` overloads.

### Adding a local variable

```java
LabelNode start = new LabelNode();
LabelNode end = new LabelNode();
ASMUtil.addLocalVariable(m, "replacement", "Lcom/thirdparty/Screen;", start, end);
int replacement = ASMUtil.findLocalVariable(m, "replacement").index;

m.instructions.insert(ASMUtil.listOf(
        start,
        // ... ASTORE replacement / ALOAD replacement ...
        end));
```

`addLocalVariable` only creates the table entry. You still have to insert `start` and `end` into the method. The
index is "highest index in the table + 1", which can collide with an existing variable if the table is incomplete
or the last variable is a `long`/`double`. When in doubt, use `m.maxLocals` as the index (with `COMPUTE_MAXS`
afterwards).

## 10. Write flags and `NonLoadingClassWriter`

Every transformer declares the `ClassWriter` flags it needs:

| Flag | Use when |
|---|---|
| `0` | Only flags, names or operands changed, or the added code doesn't change stack depth or control flow |
| `ClassWriter.COMPUTE_MAXS` | Code was added (needs a deeper stack or more locals) but no jumps were added |
| `ClassWriter.COMPUTE_FRAMES` | Jumps or new branches were added (also computes maxs) |

When several transformers modify the same class, their flags are combined, but only flags of transformers that
actually reported a change count.

`COMPUTE_FRAMES` makes ASM compute the common super class of two types at merge points. ASM's default
implementation calls `Class.forName`, which inside a transformer loads classes too early (they can no longer be
transformed), can recurse into your transformer, or fails because the class isn't visible. ASMUtil's transformers
use `NonLoadingClassWriter` instead, which reads the class file headers via `ClassUtil` without loading anything.

If the classes are only visible through a specific class loader, or if the names in the bytecode differ from the
class file names on disk (obfuscation), override `createClassWriter`:

```java
private static final ClassUtil CLASS_UTIL = ClassUtil.getInstance(
        new ClassUtil.Configuration(myClassLoader, deobfToObf, obfToDeobf)); // maps may be null

@Override
protected ClassWriter createClassWriter(int flags) {
    return new NonLoadingClassWriter(flags, CLASS_UTIL);
}
```

`ClassUtil` is also handy on its own for checking whether a class extends or implements something, without loading
it:

```java
boolean isModel = ClassUtil.DEFAULT.findInClassHierarchy(
        name.replace('.', '/'), "com/thirdparty/render/Model"::equals) != null;
```

It throws `MissingResourceException` if a class in the hierarchy can't be found, so catch that when scanning
arbitrary classes.

## 11. Registry reference: required, optional, priorities

All `registry.add*` shortcuts follow one scheme:

```java
registry.add(className, transformer);                                    // a ClassNodeTransformer
registry.add(className, writeFlags, (ClassNode c) -> { ... });            // class-level, always "changed"
registry.add(className, writeFlags, (ClassNode c) -> { ...; return changed; }); // class-level, Predicate
registry.add(className, methodName, writeFlags, m -> { ... });             // one method by name
registry.add(className, methodName, desc, writeFlags, m -> { ... });       // one method by name + desc
registry.add(className, methodName, desc, required, writeFlags, m -> { ... });
registry.addOptional(className, methodName, desc, writeFlags, m -> { ... });
registry.addObf(...), registry.addObfOptional(...)                         // two names, see section 5
```

| Variant | Matching methods allowed |
|---|---|
| default | exactly 1 |
| `required = n` (n > 0) | exactly n, every one is transformed |
| `required = 0` / `addOptional` | any number including none, **all** matches are transformed |

Use `addOptional` for patches against code that may not exist in every version of a library. Use `required` when
you match by name only and know there are e.g. two overloads that both need the patch.

For anything else, use the builder:

```java
registry.add("com.thirdparty.Foo", MethodNodeTransformer.builder("update")
        .minMatches(1)          // at least one...
        .maxMatches(0)          // ...and no upper limit
        .writeFlags(ClassWriter.COMPUTE_FRAMES)
        .priority(-10)          // runs before the other transformers of this class (default 0)
        .build(m -> { ... }));
```

Transformers for one class run in ascending priority, and in registration order for equal priorities.
`build(Predicate<MethodNode>)` lets the transformer report "nothing to do" by returning `false`.

Registering conditionally is plain Java, which makes config-driven patch sets straightforward:

```java
@Override
protected void registerTransformers(IClassTransformerRegistry registry) {
    PatchConfig config = PatchConfig.load(); // static, see the note in section 3
    if (config.fixCacheLeak) {
        CacheLeakPatch.register(registry);
    }
    if (config.fasterLookup) {
        FastLookupPatch.register(registry);
    }
}
```

Splitting patches into small classes with a static `register(IClassTransformerRegistry)` method keeps large
transformers organized. Hook code can live right next to the patch as a nested `Hook` class
(`"com/example/patches/CacheLeakPatch$Hook"`).

## 12. Transforming many classes at once

`HashMapClassNodeClassTransformer` looks transformers up by exact class name. To patch classes you can't name in
advance (all subclasses of something, all classes calling some API), extend `ClassNodeClassTransformer` and decide
per class. Return `false` from the transformer when nothing changed, so untouched classes are not rewritten:

```java
public class TimeSourceTransformer extends ClassNodeClassTransformer {

    // implemented directly because ClassNodeTransformer.create(...) logs every class it is applied to
    private static final List<ClassNodeTransformer> TRANSFORMERS = Collections.singletonList(new ClassNodeTransformer() {
        @Override
        public boolean transform(ClassNode classNode) {
            return ASMUtil.stream(classNode)
                    .flatMap(ASMUtil::stream)
                    .filter(MethodInsnNode.class::isInstance)
                    .map(MethodInsnNode.class::cast)
                    .filter(insn -> insn.owner.equals("java/lang/System") && insn.name.equals("currentTimeMillis"))
                    .peek(insn -> insn.owner = "com/example/patches/FakeClock")
                    .count() > 0;
        }

        @Override
        public int writeFlags() {
            return 0;
        }

        @Override
        public int priority() {
            return 0;
        }
    });

    @Override
    protected List<ClassNodeTransformer> getClassNodeTransformers(String className) {
        if (className.startsWith("com.example.patches.")) {
            return null; // never transform yourself
        }
        return TRANSFORMERS;
    }

}
```

This parses every class, so keep `getClassNodeTransformers` and the transformer cheap. If you only need to look at a
class header before deciding, use `ClassUtil.findInClassHierarchy` first (section 10).

For full control, `AbstractClassTransformer` only asks you to implement
`byte[] transformOrNull(String obfName, String name, byte[] basicClass)` and still provides the error handling and
export. That is the right base for visitor-based transformers or ones that run their own analysis on a `ClassNode`.

## 13. Debugging

**Print a method.** `ASMUtil.methodToString(methodNode)` lists every instruction with its index, opcode, node type
and operands. Log it before and after your patch while developing:

```java
ASMUtil.LOGGER.info(ASMUtil.methodToString(methodNode));
```

```
put (Ljava/lang/Object;Ljava/lang/Object;)V
  0                              LabelNode L1823470582
  1                         LineNumberNode 12
  2           ALOAD            VarInsnNode 0
  3        GETFIELD          FieldInsnNode com/thirdparty/cache/LruCache map Ljava/util/Map;
...
```

`ASMUtil.classToString(classNode)` does the same for all methods of a class, and
`ASMUtil.instructionToString(insn)` for one instruction.

**Dump transformed classes.** Start the JVM with `-Dmeldexun.asm.export=true` and every class transformed by an
`AbstractClassTransformer` subclass is written to `.meldexun/asm/export/<package>/<Class>.class` (the directory is
cleared at the first export of each run). Open them in a decompiler or with `javap -c` to see exactly what your
patch produced. This is the fastest way to understand a `VerifyError`.

**Quieter logs.** Each transformed class and method is logged ("Transforming method com/thirdparty/Foo.bar()V").
`-Dmeldexun.asm.disableLogging=true` turns that off.

## 14. Minecraft Forge (1.12.2) coremods

The library's origin shows in a few conveniences for LaunchWrapper-based Minecraft. A typical coremod setup:

```java
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({ "com.example.mymod.asm", "meldexun.asmutil2" })
@IFMLLoadingPlugin.SortingIndex(1001) // run after Forge's deobfuscation transformer
public class MyModPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { MyModClassTransformer.class.getName() };
    }

    // getModContainerClass, getSetupClass, getAccessTransformerClass return null, injectData does nothing
}
```

```java
public class MyModClassTransformer extends HashMapClassNodeClassTransformer
        implements net.minecraft.launchwrapper.IClassTransformer {

    @Override
    protected void registerTransformers(IClassTransformerRegistry registry) {
        registry.addObf("net.minecraft.client.renderer.EntityRenderer", "getMouseOver", "func_78473_a", "(F)V",
                ClassWriter.COMPUTE_FRAMES, m -> { ... });
    }

}
```

Things specific to this environment:

- **`TransformerExclusions`** must include your transformer package and `meldexun.asmutil2` (or wherever you
  relocated it), so they aren't transformed themselves.
- **Sorting index.** Forge's deobfuscation transformer has sorting index 1000 and coremods default to 0. With
  `@SortingIndex(1001)` (or higher) your transformer sees deobfuscated class names; everything below assumes that.
- **Names.** With that, class names (in registrations, owners and descriptors) are always the MCP/deobfuscated ones,
  but member names are SRG names (`func_78473_a`, `field_72995_K`) in production and MCP names in the dev
  environment. Use the `Obf` variants with `(mcpName, srgName)` for methods and fields of Minecraft classes. Members
  of other mods are not obfuscated and use the plain variants.
- **Creating references to Minecraft members.** An instruction you *create* that references a Minecraft member has
  to use the member name of the environment it runs in. Patches commonly write the SRG name (adding the MCP name as
  a comment), or map it at runtime with `FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc)` /
  `mapFieldName(...)`.
- **Frames.** At runtime the class files on the class path have Notch names (`bib`), while the bytecode you
  transform uses deobfuscated names. Give `NonLoadingClassWriter` a `ClassUtil` with Forge's mappings so
  `COMPUTE_FRAMES` can find super classes:

  ```java
  private static final ClassUtil REMAPPING_CLASS_UTIL;
  static {
      try {
          Field instance = FMLDeobfuscatingRemapper.class.getField("INSTANCE");
          Field classNameBiMap = FMLDeobfuscatingRemapper.class.getDeclaredField("classNameBiMap");
          classNameBiMap.setAccessible(true);
          @SuppressWarnings("unchecked")
          BiMap<String, String> obfToDeobf = (BiMap<String, String>) classNameBiMap.get(instance.get(null));
          REMAPPING_CLASS_UTIL = ClassUtil.getInstance(
                  new ClassUtil.Configuration(Launch.classLoader, obfToDeobf.inverse(), obfToDeobf));
      } catch (ReflectiveOperationException e) {
          throw new UnsupportedOperationException(e);
      }
  }

  @Override
  protected ClassWriter createClassWriter(int flags) {
      return new NonLoadingClassWriter(flags, REMAPPING_CLASS_UTIL);
  }
  ```

  Load `FMLDeobfuscatingRemapper` through `Launch.classLoader` (e.g. `Class.forName(..., true, Launch.classLoader)`)
  if your transformer class is loaded before it is available.
- **Local variable names** are available for many Minecraft and Forge classes (as MCP names), so
  `findLocalVariable` works there, but not reliably everywhere. Check with `methodToString` and `javap -l`.
- **Additional transformers** can be registered from the plugin constructor with
  `Launch.classLoader.registerTransformer(OtherTransformer.class.getName())`, e.g. a `ClassNodeClassTransformer`
  that applies to all classes.

## 15. Pitfalls checklist

- [ ] Registered class names use **dots** (`com.example.Foo$Inner`), instruction owners use **slashes**
      (`com/example/Foo$Inner`), types in descriptors use `L...;`.
- [ ] `find(...)` and `InsnFinder.find()` never return `null`. They throw, so don't null-check, catch.
- [ ] A second `xyzInsn(...)` filter on the same finder replaces the first.
- [ ] `ldcInsn(1.5)` matches a `double`, `ldcInsn(1.5F)` a `float`, small ints are not `LDC` at all.
- [ ] Don't add or remove instructions while streaming over them. Collect into a list first.
- [ ] Don't insert a node that is already in a list, or the same `InsnList` twice.
- [ ] Added a jump? `COMPUTE_FRAMES`. Added code or a new method? At least `COMPUTE_MAXS`.
- [ ] Replaced a whole body? Clear `tryCatchBlocks`/`localVariables` that referenced the old code.
- [ ] Local variable names need debug information. Hardcode indices when it is missing.
- [ ] `registerTransformers` runs inside the superclass constructor, before your instance fields are set.
- [ ] Hook classes must be loadable from the transformed class's class loader and must not be transformed
      themselves.
- [ ] A patch that "does nothing" usually failed. Look for `ClassTransformException` in the log, and dump the class
      with `-Dmeldexun.asm.export=true`.
