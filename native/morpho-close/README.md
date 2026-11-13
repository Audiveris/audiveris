<!-- SPDX-License-Identifier: AGPL-3.0-only -->

# Optional Zig accelerator for Audiveris morphology close

This directory contains the complete source for an optional CPU accelerator
for `MorphoProcessor.close`. The shared library exposes one versioned C ABI and
is loaded once by the Java 25 Foreign Function and Memory bridge in Audiveris.
Each close operation crosses the boundary once with a complete grayscale image
plane; no helper process or temporary file is used in the hot path.

The original Java implementation remains the fail-closed fallback. If the
native library is not configured, cannot be loaded, returns an error or fails
during execution, Audiveris continues through the stock Java path.

## Requirements

- Zig 0.16.0;
- Java 25 for the optional Audiveris FFM integration;
- a platform supported by Zig's shared-library output.

The published measurements cover Apple Silicon macOS only. See
[BENCHMARKS.md](BENCHMARKS.md) for the exact evidence and non-claims.

## Build and test

From this directory:

```sh
zig fmt --check build.zig src
zig build test
zig build -Doptimize=ReleaseFast
```

The build installs:

- `zig-out/lib/libmusicspace_morpho_close_v1.*`, the shared library used by
  the Java FFM path;
- `zig-out/bin/audiveris-morpho-worker`, a diagnostic CLI for differential
  checks outside the Audiveris hot path.

## Enable the optional path

Run Audiveris with Java 25, enable native access for the unnamed module and set
the absolute shared-library path:

```text
--enable-native-access=ALL-UNNAMED
-Daudiveris.morpho.library=/absolute/path/to/libmusicspace_morpho_close_v1.dylib
```

Use the platform-specific shared-library extension on Linux or Windows. When
the property is absent, the native circuit remains disabled and the existing
Java implementation is used.

At JVM shutdown the bridge prints one summary containing `native_calls`,
`fallback_calls` and `circuit_open`. A valid native benchmark requires a
positive native-call count, zero fallback calls and a closed circuit breaker.

## C ABI

The shared library exports `musicspace_morpho_close_v1`. Matching declarations
and stable status values are in
[`include/musicspace_morpho_close_v1.h`](include/musicspace_morpho_close_v1.h).
The function receives caller-owned input, scratch and output planes plus one
flattened morphology probe. It validates all pointers, lengths, dimensions and
overlaps before processing and performs no file I/O or allocation.

The diagnostic worker protocol is documented in
[WORKER_PROTOCOL_V1.md](WORKER_PROTOCOL_V1.md).

## License

This component and its corresponding source are distributed under
AGPL-3.0-only. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
