// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

const abi = @import("morpho_close_abi.zig");
const candidate = @import("morpho_close_fast_candidate_r1.zig");

pub fn closeInto(
    input_ptr: ?[*]const u8,
    input_len: usize,
    scratch_ptr: ?[*]u8,
    scratch_len: usize,
    output_ptr: ?[*]u8,
    output_len: usize,
    width: usize,
    height: usize,
    taps_ptr: ?[*]const abi.Tap,
    taps_len: usize,
) callconv(.c) abi.Status {
    return candidate.closeInto(
        input_ptr,
        input_len,
        scratch_ptr,
        scratch_len,
        output_ptr,
        output_len,
        width,
        height,
        taps_ptr,
        taps_len,
    );
}

pub export fn musicspace_morpho_close_v1(
    input_ptr: ?[*]const u8,
    input_len: usize,
    scratch_ptr: ?[*]u8,
    scratch_len: usize,
    output_ptr: ?[*]u8,
    output_len: usize,
    width: usize,
    height: usize,
    taps_ptr: ?[*]const abi.Tap,
    taps_len: usize,
) callconv(.c) abi.Status {
    return closeInto(
        input_ptr,
        input_len,
        scratch_ptr,
        scratch_len,
        output_ptr,
        output_len,
        width,
        height,
        taps_ptr,
        taps_len,
    );
}
