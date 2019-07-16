# sixpack-kotlin

Library for compression and decompression using the Sixpack algorithm originally written by Philip G. Gage.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.benedikt.compression/sixpack/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.benedikt.compression/sixpack))

I created this library, because I was in the process of reverse engineering a file format compressed using Sixpack. However, I barely found enough information describing the algorithm, which is why I took some time to create this library.

## Installation

The library is located in the central maven repository.

```xml
<dependency>
  <groupId>dev.benedikt.compression</groupId>
  <artifactId>sixpack</artifactId>
  <version>1.0.3</version>
</dependency>
```

## Usage

```java
// Use the default configuration.
SixpackConfig config = new SixpackConfig();

// Compress the data in the given byte buffer to a byte array.
SixpackCompressor compressor = new SixpackCompressor(config);
byte[] compressed = compressor.compress(byteBuffer);

// Decompress the data in the given byte buffer to a byte array.
SixpackCompressor decompressor = new SixpackDecompressor(config);
byte[] decompressed = decompressor.decompress(ByteBuffer.wrap(compressed));
```

```kotlin
// Use the default configuration.
val config = SixpackConfig();

// Compress the data in the given byte buffer to a byte array.
val compressor = SixpackCompressor(config);
val compressed = compressor.compress(byteBuffer);

// Decompress the data in the given byte buffer to a byte array.
val decompressor = SixpackDecompressor(config);
val decompressed = decompressor.decompress(ByteBuffer.wrap(compressed));
```
