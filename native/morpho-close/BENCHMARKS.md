<!-- SPDX-License-Identifier: AGPL-3.0-only -->

# Measured parity and performance

The implementation was measured on Apple Silicon macOS with Java 25 and Zig
0.16.0. Stock and native runs used identical Audiveris builds, input bytes and
batch settings. MusicXML comparison removed only the volatile `encoding-date`;
the remaining XML bytes were exact.

## Initial five sheets

| Sheet | Stock, s | Zig native, s | Improvement |
|---|---:|---:|---:|
| Pirate cold | 25.649 | 15.826 | 38.298% |
| Pirate warm p50 | 22.876 | 14.543 | 36.427% |
| Pirate warm p95 | 23.654 | 14.719 | 37.774% |
| Saint-Saëns | 26.966 | 16.926 | 37.232% |
| Moszkowski | 20.374 | 13.768 | 32.424% |
| Weber/Liszt | 18.740 | 13.262 | 29.232% |
| Berlioz/Liszt | 23.921 | 16.995 | 28.954% |

All 20 processes exited successfully. Every native run reported nonzero native
calls, zero fallback calls and `circuit_open=false`. Pirate was measured once
cold and five times warm per route. Every normalized MusicXML pair was exact.

## Additional 20-sheet corpus

Seventeen stock/native pairs exported successfully and had exact normalized
MusicXML parity. Three inputs produced the same early rejection or export
failure on both routes and are not counted as successful recognition claims.

Across the 17 successful exports, stock totaled 216.73 seconds and native
167.87 seconds: a 22.544% wall-clock improvement. Across all 20 observed pairs,
including matched failures, totals were 240.83 and 190.29 seconds: a 20.986%
improvement. Every successful native run was faster; individual improvement
ranged from 4.253% to 42.430%.

## Evidence boundary

- These are wall-clock measurements from one Apple Silicon macOS machine, not
  a cross-platform guarantee.
- The measurements cover the optional morphology path, not a change to OMR
  models or musical interpretation.
- Raw MXL containers may contain volatile metadata. The exact parity claim is
  for extracted score XML after replacing only `encoding-date`.
- The three matched failures prove equivalent failure behavior only; they do
  not count as successful exports.
