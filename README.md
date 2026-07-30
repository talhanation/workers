# workers
Mod for Minecraft that adds Worker Villagers.

https://www.curseforge.com/minecraft/mc-mods/workers

## NeoForge 1.21.1 build

Requirements:

- Java 21
- NeoForge 21.1.219
- A built Recruits 1.21.1 development checkout

By default, the build uses:

```text
../recruits-neoforge-1.21.1/build/libs/recruits-1.21.1-1.15.0-all.jar
```

Build Recruits first, then run:

```bash
./gradlew clean build
```

To use a Recruits jar in another location:

```bash
./gradlew clean build -Precruits_jar=/absolute/path/to/recruits-1.21.1-1.15.0-all.jar
```

The distributable Workers artifact is the `-all.jar` file in `build/libs`.
It contains the relocated CoreLib classes and still requires Recruits 1.21.1.

All Rights Reserved unless otherwise explicitly stated.
