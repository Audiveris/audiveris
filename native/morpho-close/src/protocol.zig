// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

const std = @import("std");
const abi = @import("morpho_close_abi.zig");

pub const probe_record_size: usize = 12;

pub const DecodeError = error{
    EmptyProbe,
    WrongProbeLength,
};

fn readI32LittleEndian(bytes: []const u8, offset: usize) i32 {
    const bits: u32 =
        @as(u32, bytes[offset]) |
        (@as(u32, bytes[offset + 1]) << 8) |
        (@as(u32, bytes[offset + 2]) << 16) |
        (@as(u32, bytes[offset + 3]) << 24);
    return @bitCast(bits);
}

pub fn decodeProbe(allocator: std.mem.Allocator, bytes: []const u8) (DecodeError || std.mem.Allocator.Error)![]abi.Tap {
    if (bytes.len == 0) return error.EmptyProbe;
    if (bytes.len % probe_record_size != 0) return error.WrongProbeLength;

    const taps = try allocator.alloc(abi.Tap, bytes.len / probe_record_size);
    for (taps, 0..) |*tap, index| {
        const offset = index * probe_record_size;
        tap.* = .{
            .dy = readI32LittleEndian(bytes, offset),
            .dx = readI32LittleEndian(bytes, offset + 4),
            .weight = readI32LittleEndian(bytes, offset + 8),
        };
    }
    return taps;
}
