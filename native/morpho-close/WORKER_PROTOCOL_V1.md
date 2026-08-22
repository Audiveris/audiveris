<!-- SPDX-License-Identifier: AGPL-3.0-only -->

# Worker protocol v1

The worker is intentionally bounded to one deterministic file operation:

```text
audiveris-morpho-worker close --width W --height H --input INPUT.raw --probe PROBE.bin --output OUTPUT.raw
```

Arguments must appear in this order with no additional arguments. `W` and `H`
are base-10 unsigned `size_t` values and must be nonzero. The input path,
probe path, and output path must be nonempty.

## Files

- `INPUT.raw`: exactly `W*H` bytes, one grayscale byte per pixel in row-major
  order.
- `PROBE.bin`: a nonempty byte sequence whose length is divisible by 12. Each
  record is three little-endian signed 32-bit integers in this order:
  `{dy, dx, weight}`.
- `OUTPUT.raw`: exactly `W*H` bytes, published only after input/probe
  validation and morphology return `ok`.

The process reads all inputs and decodes all probes before invoking the frozen
ABI. It writes a sibling `OUTPUT.raw.part` only after a successful morphology,
then closes and renames it into place. A failed validation, morphology, write,
or rename returns nonzero and leaves no worker-created partial output.

## Stable status line

The process writes one machine-readable line to standard error:

```text
status=<status-name> code=<u32-code> <human-detail>
```

The code and name are the frozen ABI statuses:

| Code | Name |
| ---: | --- |
| 0 | `ok` |
| 1 | `zero_width` |
| 2 | `zero_height` |
| 3 | `zero_taps` |
| 4 | `wrong_input_length` |
| 5 | `wrong_scratch_length` |
| 6 | `wrong_output_length` |
| 7 | `input_scratch_overlap` |
| 8 | `scratch_output_overlap` |
| 9 | `input_output_overlap` |
| 10 | `dimension_overflow` |
| 11 | `null_pointer` |
| 12 | `arithmetic_overflow` |

CLI parse, filesystem, allocation, and atomic-publication failures are mapped
to the same stable status domain. A malformed input/probe length maps to
`wrong_input_length`; an empty probe maps to `zero_taps`.

## C ABI

The shared library exports:

```c
musicspace_morpho_status_v1 musicspace_morpho_close_v1(
    const uint8_t *input_ptr,
    size_t input_len,
    uint8_t *scratch_ptr,
    size_t scratch_len,
    uint8_t *output_ptr,
    size_t output_len,
    size_t width,
    size_t height,
    const musicspace_morpho_tap_v1 *taps_ptr,
    size_t taps_len);
```

The ABI validates zero dimensions/taps, dimension multiplication, exact buffer
lengths, null pointers, checked pointer ends, and pairwise overlap before
running the selected generic or binary fast path. ABI callers own all buffers;
the function does not create files.

## Scope

This is an isolated CPU exactness seam. A passing worker report does not claim
full Audiveris integration, Java bridge integration, production readiness,
canonical admission, or a full-Audiveris speedup.
