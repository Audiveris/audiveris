// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

pub const Tap = extern struct {
    dy: i32,
    dx: i32,
    weight: i32,
};

pub const Status = enum(u32) {
    ok = 0,
    zero_width = 1,
    zero_height = 2,
    zero_taps = 3,
    wrong_input_length = 4,
    wrong_scratch_length = 5,
    wrong_output_length = 6,
    input_scratch_overlap = 7,
    scratch_output_overlap = 8,
    input_output_overlap = 9,
    dimension_overflow = 10,
    null_pointer = 11,
    arithmetic_overflow = 12,
};

pub const CloseFn = *const fn (
    input_ptr: ?[*]const u8,
    input_len: usize,
    scratch_ptr: ?[*]u8,
    scratch_len: usize,
    output_ptr: ?[*]u8,
    output_len: usize,
    width: usize,
    height: usize,
    taps_ptr: ?[*]const Tap,
    taps_len: usize,
) callconv(.c) Status;
