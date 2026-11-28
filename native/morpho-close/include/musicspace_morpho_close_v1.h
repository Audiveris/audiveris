/* SPDX-License-Identifier: AGPL-3.0-only */
/*
 * Isolated Audiveris morphology worker v1. Corresponding Source is supplied
 * with this package; see README.md and NOTICE for the source-offer notice.
 */

#ifndef MUSICSPACE_MORPHO_CLOSE_V1_H
#define MUSICSPACE_MORPHO_CLOSE_V1_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct musicspace_morpho_tap_v1 {
    int32_t dy;
    int32_t dx;
    int32_t weight;
} musicspace_morpho_tap_v1;

typedef enum musicspace_morpho_status_v1 {
    MUSICSPACE_MORPHO_OK = 0,
    MUSICSPACE_MORPHO_ZERO_WIDTH = 1,
    MUSICSPACE_MORPHO_ZERO_HEIGHT = 2,
    MUSICSPACE_MORPHO_ZERO_TAPS = 3,
    MUSICSPACE_MORPHO_WRONG_INPUT_LENGTH = 4,
    MUSICSPACE_MORPHO_WRONG_SCRATCH_LENGTH = 5,
    MUSICSPACE_MORPHO_WRONG_OUTPUT_LENGTH = 6,
    MUSICSPACE_MORPHO_INPUT_SCRATCH_OVERLAP = 7,
    MUSICSPACE_MORPHO_SCRATCH_OUTPUT_OVERLAP = 8,
    MUSICSPACE_MORPHO_INPUT_OUTPUT_OVERLAP = 9,
    MUSICSPACE_MORPHO_DIMENSION_OVERFLOW = 10,
    MUSICSPACE_MORPHO_NULL_POINTER = 11,
    MUSICSPACE_MORPHO_ARITHMETIC_OVERFLOW = 12,
} musicspace_morpho_status_v1;

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

#ifdef __cplusplus
}
#endif

#endif
