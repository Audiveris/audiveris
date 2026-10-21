// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

const std = @import("std");
const abi = @import("morpho_close_abi.zig");
const candidate = @import("morpho_close_fast_candidate_r1.zig");
const library = @import("library.zig");
const protocol = @import("protocol.zig");

test "public C ABI has the frozen status values and validates lengths" {
    const close: abi.CloseFn = library.musicspace_morpho_close_v1;
    try std.testing.expectEqual(@as(u32, 0), @intFromEnum(abi.Status.ok));
    try std.testing.expectEqual(@as(u32, 12), @intFromEnum(abi.Status.arithmetic_overflow));

    var input = [_]u8{ 1, 2, 3, 4 };
    var scratch = [_]u8{ 0xa5, 0xa5, 0xa5, 0xa5 };
    var output = [_]u8{ 0x5a, 0x5a, 0x5a, 0x5a };
    const taps = [_]abi.Tap{.{ .dy = 0, .dx = 0, .weight = 0 }};

    const status = close(
        input[0..].ptr,
        input.len - 1,
        scratch[0..].ptr,
        scratch.len,
        output[0..].ptr,
        output.len,
        2,
        2,
        taps[0..].ptr,
        taps.len,
    );
    try std.testing.expectEqual(abi.Status.wrong_input_length, status);
    try std.testing.expectEqualSlices(u8, &[_]u8{ 0xa5, 0xa5, 0xa5, 0xa5 }, scratch[0..]);
    try std.testing.expectEqualSlices(u8, &[_]u8{ 0x5a, 0x5a, 0x5a, 0x5a }, output[0..]);
}

test "binary fast path preserves a center tap across a word boundary" {
    var input = [_]u8{0} ** 140;
    for (input[0..], 0..) |*value, index| {
        value.* = if ((index % 5) == 0) 255 else 0;
    }
    var scratch = [_]u8{0xcc} ** 140;
    var output = [_]u8{0x33} ** 140;
    const taps = [_]abi.Tap{.{ .dy = 0, .dx = 0, .weight = 255 }};

    try std.testing.expect(candidate.fastPathEligible(input[0..], 70, 2, taps[0..]));
    const status = library.musicspace_morpho_close_v1(
        input[0..].ptr,
        input.len,
        scratch[0..].ptr,
        scratch.len,
        output[0..].ptr,
        output.len,
        70,
        2,
        taps[0..].ptr,
        taps.len,
    );
    try std.testing.expectEqual(abi.Status.ok, status);
    try std.testing.expectEqualSlices(u8, input[0..], output[0..]);
}

test "generic gray path keeps the center sample exact" {
    const input = [_]u8{ 10, 20, 30, 40 };
    var scratch = [_]u8{0} ** 4;
    var output = [_]u8{0} ** 4;
    const taps = [_]abi.Tap{.{ .dy = 0, .dx = 0, .weight = 0 }};

    try std.testing.expect(!candidate.fastPathEligible(input[0..], 2, 2, taps[0..]));
    const status = library.closeInto(
        input[0..].ptr,
        input.len,
        scratch[0..].ptr,
        scratch.len,
        output[0..].ptr,
        output.len,
        2,
        2,
        taps[0..].ptr,
        taps.len,
    );
    try std.testing.expectEqual(abi.Status.ok, status);
    try std.testing.expectEqualSlices(u8, input[0..], output[0..]);
}

test "probe decoder reads signed little-endian triples" {
    const bytes = [_]u8{
        0x01, 0x00, 0x00, 0x00,
        0xfe, 0xff, 0xff, 0xff,
        0xff, 0x00, 0x00, 0x00,
        0xfd, 0xff, 0xff, 0xff,
        0x04, 0x00, 0x00, 0x00,
        0xfb, 0xff, 0xff, 0xff,
    };
    const taps = try protocol.decodeProbe(std.testing.allocator, bytes[0..]);
    defer std.testing.allocator.free(taps);

    try std.testing.expectEqual(@as(usize, 2), taps.len);
    try std.testing.expectEqual(abi.Tap{ .dy = 1, .dx = -2, .weight = 255 }, taps[0]);
    try std.testing.expectEqual(abi.Tap{ .dy = -3, .dx = 4, .weight = -5 }, taps[1]);
}

test "probe decoder rejects empty and non-record-aligned input" {
    try std.testing.expectError(error.EmptyProbe, protocol.decodeProbe(std.testing.allocator, &[_]u8{}));
    try std.testing.expectError(error.WrongProbeLength, protocol.decodeProbe(std.testing.allocator, &[_]u8{ 0, 1, 2 }));
}
