// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

const std = @import("std");
const abi = @import("morpho_close_abi.zig");
const library = @import("library.zig");
const protocol = @import("protocol.zig");

const Io = std.Io;
const usize_max: usize = ~@as(usize, 0);

const Cli = struct {
    width: usize,
    height: usize,
    input_path: []const u8,
    probe_path: []const u8,
    output_path: []const u8,
};

fn reportStatus(status: abi.Status, detail: []const u8) void {
    std.debug.print("status={s} code={d} {s}\n", .{ @tagName(status), @intFromEnum(status), detail });
}

fn fail(status: abi.Status, detail: []const u8) noreturn {
    reportStatus(status, detail);
    std.process.exit(1);
}

fn parseUnsigned(text: []const u8) ?usize {
    if (text.len == 0) return null;
    return std.fmt.parseInt(usize, text, 10) catch null;
}

fn parseCli(args: []const []const u8) ?Cli {
    if (args.len != 12) return null;
    if (!std.mem.eql(u8, args[1], "close")) return null;
    if (!std.mem.eql(u8, args[2], "--width")) return null;
    if (!std.mem.eql(u8, args[4], "--height")) return null;
    if (!std.mem.eql(u8, args[6], "--input")) return null;
    if (!std.mem.eql(u8, args[8], "--probe")) return null;
    if (!std.mem.eql(u8, args[10], "--output")) return null;

    const width = parseUnsigned(args[3]) orelse return null;
    const height = parseUnsigned(args[5]) orelse return null;
    if (args[7].len == 0 or args[9].len == 0 or args[11].len == 0) return null;

    return .{
        .width = width,
        .height = height,
        .input_path = args[7],
        .probe_path = args[9],
        .output_path = args[11],
    };
}

fn readFailureStatus(err: anyerror) abi.Status {
    if (std.mem.eql(u8, @errorName(err), "FileTooBig") or
        std.mem.eql(u8, @errorName(err), "StreamTooLong")) return .wrong_input_length;
    return .arithmetic_overflow;
}

fn writeOutputAtomically(io: Io, allocator: std.mem.Allocator, output_path: []const u8, bytes: []const u8) abi.Status {
    const temp_path = std.fmt.allocPrint(allocator, "{s}.part", .{output_path}) catch {
        return .arithmetic_overflow;
    };

    const cwd = Io.Dir.cwd();
    var temp_exists = false;
    defer if (temp_exists) cwd.deleteFile(io, temp_path) catch {};

    var file = cwd.createFile(io, temp_path, .{ .exclusive = true, .truncate = true }) catch |err| {
        std.debug.print("create={s}\n", .{@errorName(err)});
        return .arithmetic_overflow;
    };
    temp_exists = true;
    defer file.close(io);

    file.writeStreamingAll(io, bytes) catch |err| {
        std.debug.print("write={s}\n", .{@errorName(err)});
        return .arithmetic_overflow;
    };

    cwd.rename(temp_path, cwd, output_path, io) catch |err| {
        std.debug.print("rename={s}\n", .{@errorName(err)});
        return .arithmetic_overflow;
    };
    temp_exists = false;
    return .ok;
}

pub fn main(init: std.process.Init) !void {
    const allocator = init.arena.allocator();

    const args = init.minimal.args.toSlice(allocator) catch {
        fail(.arithmetic_overflow, "argument allocation failed");
    };

    const cli = parseCli(args) orelse {
        fail(.dimension_overflow, "usage: close --width W --height H --input INPUT.raw --probe PROBE.bin --output OUTPUT.raw");
    };

    if (cli.width == 0) {
        fail(.zero_width, "width must be nonzero");
    }
    if (cli.height == 0) {
        fail(.zero_height, "height must be nonzero");
    }
    if (cli.width > usize_max / cli.height) {
        fail(.dimension_overflow, "width*height overflows size_t");
    }

    const pixel_count = cli.width * cli.height;
    const input_limit: Io.Limit = if (pixel_count == usize_max)
        .unlimited
    else
        .limited(pixel_count + 1);
    const cwd = Io.Dir.cwd();

    const input_bytes = cwd.readFileAlloc(init.io, cli.input_path, allocator, input_limit) catch |err| {
        fail(readFailureStatus(err), "input read failed");
    };
    if (input_bytes.len != pixel_count) {
        fail(.wrong_input_length, "input length does not equal width*height");
    }

    const probe_bytes = cwd.readFileAlloc(init.io, cli.probe_path, allocator, .unlimited) catch |err| {
        fail(readFailureStatus(err), "probe read failed");
    };
    const taps = protocol.decodeProbe(allocator, probe_bytes) catch |err| {
        const status: abi.Status = switch (err) {
            error.EmptyProbe => .zero_taps,
            error.WrongProbeLength => .wrong_input_length,
            else => .arithmetic_overflow,
        };
        fail(status, "probe must contain nonempty little-endian i32 triples");
    };

    const scratch = allocator.alloc(u8, pixel_count) catch {
        fail(.arithmetic_overflow, "scratch allocation failed");
    };
    const output = allocator.alloc(u8, pixel_count) catch {
        fail(.arithmetic_overflow, "output allocation failed");
    };

    const status = library.closeInto(
        input_bytes.ptr,
        input_bytes.len,
        scratch.ptr,
        scratch.len,
        output.ptr,
        output.len,
        cli.width,
        cli.height,
        taps.ptr,
        taps.len,
    );
    if (status != .ok) {
        fail(status, "morphology failed");
    }

    const write_status = writeOutputAtomically(init.io, allocator, cli.output_path, output);
    if (write_status != .ok) {
        fail(write_status, "output write failed");
    }
    reportStatus(.ok, "close complete");
}
