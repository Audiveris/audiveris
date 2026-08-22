// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

const std = @import("std");
const abi = @import("morpho_close_abi.zig");
const gray = @import("morpho_close_gray_fast_r3.zig");

const usize_max: usize = ~@as(usize, 0);
const i32_max: i32 = @bitCast(@as(u32, 0x7fffffff));
const i32_min: i32 = @bitCast(@as(u32, 0x80000000));

// The fast kernel has no dimension-dependent stack storage. These bounds make
// its local word arithmetic explicit and keep every packed offset checked.
const fast_max_words_per_row: usize = 1 << 15;
const fast_max_height: usize = 1 << 20;
const fast_max_taps: usize = 4096;

const ValidatedCall = struct {
    input: [*]const u8,
    scratch: [*]u8,
    output: [*]u8,
    taps: [*]const abi.Tap,
    pixel_count: usize,
};

const Validation = union(enum) {
    status: abi.Status,
    ok: ValidatedCall,
};

const FastLayout = struct {
    words_per_row: usize,
    valid_last_bits: usize,
    last_mask: u64,
    packed_row_len: usize,
    word_count: usize,
    packed_len: usize,
};

fn checkedEnd(start: usize, length: usize) ?usize {
    if (length > usize_max - start) return null;
    return start + length;
}

fn rangesOverlap(a_start: usize, a_end: usize, b_start: usize, b_end: usize) bool {
    return a_start < b_end and b_start < a_end;
}

fn addChecked(a: i32, b: i32) ?i32 {
    if (b > 0 and a > i32_max - b) return null;
    if (b < 0 and a < i32_min - b) return null;
    return a + b;
}

fn subChecked(a: i32, b: i32) ?i32 {
    if (b > 0 and a < i32_min + b) return null;
    if (b < 0 and a > i32_max + b) return null;
    return a - b;
}

fn shifted(base: usize, delta: i32, limit: usize) ?usize {
    const coordinate: i128 = @as(i128, @intCast(base)) + @as(i128, @intCast(delta));
    const signed_limit: i128 = @intCast(limit);
    if (coordinate < 0 or coordinate >= signed_limit) return null;
    return @intCast(coordinate);
}

fn wrappedPlusOne(value: i32) u8 {
    const bits: u32 = @bitCast(value);
    const raw: u8 = @truncate(bits);
    return raw +% 1;
}

fn wrappedMinusOne(value: i32) u8 {
    const bits: u32 = @bitCast(value);
    const raw: u8 = @truncate(bits);
    return raw -% 1;
}

fn validate(
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
) Validation {
    if (width == 0) return .{ .status = .zero_width };
    if (height == 0) return .{ .status = .zero_height };
    if (taps_len == 0) return .{ .status = .zero_taps };

    if (width > usize_max / height) return .{ .status = .dimension_overflow };
    const pixel_count = width * height;

    if (input_len != pixel_count) return .{ .status = .wrong_input_length };
    if (scratch_len != pixel_count) return .{ .status = .wrong_scratch_length };
    if (output_len != pixel_count) return .{ .status = .wrong_output_length };

    const input = input_ptr orelse return .{ .status = .null_pointer };
    const scratch = scratch_ptr orelse return .{ .status = .null_pointer };
    const output = output_ptr orelse return .{ .status = .null_pointer };
    const taps = taps_ptr orelse return .{ .status = .null_pointer };

    const input_start = @intFromPtr(input);
    const scratch_start = @intFromPtr(scratch);
    const output_start = @intFromPtr(output);
    const input_end = checkedEnd(input_start, input_len) orelse return .{ .status = .arithmetic_overflow };
    const scratch_end = checkedEnd(scratch_start, scratch_len) orelse return .{ .status = .arithmetic_overflow };
    const output_end = checkedEnd(output_start, output_len) orelse return .{ .status = .arithmetic_overflow };

    if (rangesOverlap(input_start, input_end, scratch_start, scratch_end)) {
        return .{ .status = .input_scratch_overlap };
    }
    if (rangesOverlap(scratch_start, scratch_end, output_start, output_end)) {
        return .{ .status = .scratch_output_overlap };
    }
    if (rangesOverlap(input_start, input_end, output_start, output_end)) {
        return .{ .status = .input_output_overlap };
    }

    return .{ .ok = .{
        .input = input,
        .scratch = scratch,
        .output = output,
        .taps = taps,
        .pixel_count = pixel_count,
    } };
}

fn dilationSample(
    input: [*]const u8,
    width: usize,
    height: usize,
    y: usize,
    x: usize,
    tap: abi.Tap,
) u8 {
    const source_y = shifted(y, tap.dy, height) orelse return 0;
    const source_x = shifted(x, tap.dx, width) orelse return 0;
    return input[source_y * width + source_x];
}

fn erosionSample(
    scratch: [*]const u8,
    width: usize,
    height: usize,
    y: usize,
    x: usize,
    tap: abi.Tap,
) u8 {
    const source_y = shifted(y, tap.dy, height) orelse return 255;
    const source_x = shifted(x, tap.dx, width) orelse return 255;
    return scratch[source_y * width + source_x];
}

fn genericClose(call: ValidatedCall, width: usize, height: usize, taps_len: usize) abi.Status {
    var pixel_index: usize = 0;
    while (pixel_index < call.pixel_count) : (pixel_index += 1) {
        const y = pixel_index / width;
        const x = pixel_index % width;
        var aggregate: i32 = i32_min;

        var tap_index: usize = 0;
        while (tap_index < taps_len) : (tap_index += 1) {
            const tap = call.taps[tap_index];
            const sample = dilationSample(call.input, width, height, y, x, tap);
            const score = addChecked(@intCast(sample), tap.weight) orelse return .arithmetic_overflow;
            if (score > aggregate) aggregate = score;
        }
        call.scratch[pixel_index] = wrappedPlusOne(aggregate);
    }

    pixel_index = 0;
    while (pixel_index < call.pixel_count) : (pixel_index += 1) {
        const y = pixel_index / width;
        const x = pixel_index % width;
        var aggregate: i32 = i32_max;

        var tap_index: usize = 0;
        while (tap_index < taps_len) : (tap_index += 1) {
            const tap = call.taps[tap_index];
            const sample = erosionSample(call.scratch, width, height, y, x, tap);
            const score = subChecked(@intCast(sample), tap.weight) orelse return .arithmetic_overflow;
            if (score < aggregate) aggregate = score;
        }
        call.output[pixel_index] = wrappedMinusOne(aggregate);
    }

    return .ok;
}

fn fastLayout(width: usize, height: usize, taps_len: usize, pixel_count: usize) ?FastLayout {
    if (width > fast_max_words_per_row * 64) return null;
    if (height > fast_max_height) return null;
    if (taps_len > fast_max_taps) return null;

    const words_per_row = width / 64 + @as(usize, if (width % 64 == 0) 0 else 1);
    if (words_per_row == 0 or words_per_row > fast_max_words_per_row) return null;
    if (words_per_row > usize_max / height) return null;
    const word_count = words_per_row * height;
    if (word_count > usize_max / 8) return null;
    const packed_len = word_count * 8;
    if (packed_len > pixel_count) return null;
    if (words_per_row > usize_max / 8) return null;
    const packed_row_len = words_per_row * 8;

    const valid_last_bits = if (width % 64 == 0) 64 else width % 64;
    const last_mask: u64 = if (valid_last_bits == 64)
        ~@as(u64, 0)
    else
        (@as(u64, 1) << @as(u6, @intCast(valid_last_bits))) - 1;

    return .{
        .words_per_row = words_per_row,
        .valid_last_bits = valid_last_bits,
        .last_mask = last_mask,
        .packed_row_len = packed_row_len,
        .word_count = word_count,
        .packed_len = packed_len,
    };
}

inline fn loadWordAt(bytes: [*]const u8, offset: usize) u64 {
    return std.mem.readInt(u64, @ptrCast(bytes + offset), .little);
}

inline fn storeWordAt(bytes: [*]u8, offset: usize, value: u64) void {
    std.mem.writeInt(u64, @ptrCast(bytes + offset), value, .little);
}

fn loadWord(
    row: [*]const u8,
    word_index: i128,
    words_per_row: usize,
    last_mask: u64,
    fill: u64,
) u64 {
    if (word_index < 0 or word_index >= @as(i128, @intCast(words_per_row))) return fill;

    const index: usize = @intCast(word_index);
    const word = loadWordAt(row, index * 8);
    if (index == words_per_row - 1) {
        return (word & last_mask) | (fill & ~last_mask);
    }
    return word;
}

fn shiftedWord(
    row: [*]const u8,
    output_word: usize,
    dx: i32,
    words_per_row: usize,
    last_mask: u64,
    fill: u64,
) u64 {
    const base_bit: i128 = @as(i128, @intCast(output_word)) * 64 + @as(i128, @intCast(dx));
    const source_word: i128 = @divFloor(base_bit, @as(i128, 64));
    const shift: u6 = @intCast(@mod(base_bit, @as(i128, 64)));
    const low = loadWord(row, source_word, words_per_row, last_mask, fill);
    if (shift == 0) return low;

    const high = loadWord(row, source_word + 1, words_per_row, last_mask, fill);
    const reverse_shift: u6 = @intCast(64 - @as(u7, shift));
    return (low >> shift) | (high << reverse_shift);
}

fn fastPreflight(
    input: [*]const u8,
    pixel_count: usize,
    taps: [*]const abi.Tap,
    taps_len: usize,
) bool {
    var tap_index: usize = 0;
    while (tap_index < taps_len) : (tap_index += 1) {
        if (taps[tap_index].weight != 255) return false;
    }

    var pixel_index: usize = 0;
    while (pixel_index < pixel_count) : (pixel_index += 1) {
        const value = input[pixel_index];
        if (value != 0 and value != 255) return false;
    }

    return true;
}

fn fastPathLayout(
    input: [*]const u8,
    pixel_count: usize,
    width: usize,
    height: usize,
    taps: [*]const abi.Tap,
    taps_len: usize,
) ?FastLayout {
    const layout = fastLayout(width, height, taps_len, pixel_count) orelse return null;
    if (!fastPreflight(input, pixel_count, taps, taps_len)) return null;
    return layout;
}

pub fn fastPathEligible(
    input: []const u8,
    width: usize,
    height: usize,
    taps: []const abi.Tap,
) bool {
    if (width == 0 or height == 0 or taps.len == 0) return false;
    if (width > usize_max / height) return false;
    const pixel_count = width * height;
    if (input.len != pixel_count) return false;
    return fastPathLayout(input.ptr, pixel_count, width, height, taps.ptr, taps.len) != null;
}

fn packInput(call: ValidatedCall, width: usize, height: usize, layout: FastLayout) void {
    var y: usize = 0;
    while (y < height) : (y += 1) {
        const input_row = y * width;
        const packed_row = y * layout.packed_row_len;
        var word: usize = 0;
        while (word < layout.words_per_row) : (word += 1) {
            const first_x = word * 64;
            const remaining = width - first_x;
            const bit_count = if (remaining < 64) remaining else 64;
            var bits: u64 = 0;

            var bit: usize = 0;
            while (bit < bit_count) : (bit += 1) {
                if (call.input[input_row + first_x + bit] == 255) {
                    bits |= @as(u64, 1) << @as(u6, @intCast(bit));
                }
            }
            storeWordAt(call.output, packed_row + word * 8, bits);
        }
    }
}

fn fastClose(call: ValidatedCall, width: usize, height: usize, taps_len: usize, layout: FastLayout) void {
    packInput(call, width, height, layout);

    var y: usize = 0;
    while (y < height) : (y += 1) {
        const destination_row = y * layout.packed_row_len;
        var word: usize = 0;
        while (word < layout.words_per_row) : (word += 1) {
            var aggregate: u64 = 0;

            var tap_index: usize = 0;
            while (tap_index < taps_len) : (tap_index += 1) {
                const tap = call.taps[tap_index];
                const source_y: i128 = @as(i128, @intCast(y)) + @as(i128, @intCast(tap.dy));
                if (source_y < 0 or source_y >= @as(i128, @intCast(height))) {
                    continue;
                }

                const source_row = @as(usize, @intCast(source_y)) * layout.packed_row_len;
                aggregate |= shiftedWord(
                    call.output + source_row,
                    word,
                    tap.dx,
                    layout.words_per_row,
                    layout.last_mask,
                    0,
                );
            }

            if (word == layout.words_per_row - 1) aggregate &= layout.last_mask;
            storeWordAt(call.scratch, destination_row + word * 8, aggregate);
        }
    }

    y = 0;
    while (y < height) : (y += 1) {
        const destination_row = y * layout.packed_row_len;
        var word: usize = 0;
        while (word < layout.words_per_row) : (word += 1) {
            var aggregate: u64 = ~@as(u64, 0);

            var tap_index: usize = 0;
            while (tap_index < taps_len) : (tap_index += 1) {
                const tap = call.taps[tap_index];
                const source_y: i128 = @as(i128, @intCast(y)) + @as(i128, @intCast(tap.dy));
                if (source_y < 0 or source_y >= @as(i128, @intCast(height))) {
                    continue;
                }

                const source_row = @as(usize, @intCast(source_y)) * layout.packed_row_len;
                aggregate &= shiftedWord(
                    call.scratch + source_row,
                    word,
                    tap.dx,
                    layout.words_per_row,
                    layout.last_mask,
                    ~@as(u64, 0),
                );
            }

            if (word == layout.words_per_row - 1) aggregate &= layout.last_mask;
            storeWordAt(call.output, destination_row + word * 8, aggregate);
        }
    }

    y = height;
    while (y > 0) {
        y -= 1;
        const destination_row = y * width;
        const packed_row = y * layout.packed_row_len;
        var word = layout.words_per_row;
        while (word > 0) {
            word -= 1;
            const bits_offset = packed_row + word * 8;
            var bits = loadWordAt(call.scratch, bits_offset);
            const first_x = word * 64;
            const remaining = width - first_x;
            const bit_count = if (remaining < 64) remaining else 64;

            var bit: usize = 0;
            while (bit < bit_count) : (bit += 1) {
                call.scratch[destination_row + first_x + bit] = if ((bits & 1) != 0) 255 else 0;
                bits >>= 1;
            }
        }
    }

    y = height;
    while (y > 0) {
        y -= 1;
        const destination_row = y * width;
        const packed_row = y * layout.packed_row_len;
        var word = layout.words_per_row;
        while (word > 0) {
            word -= 1;
            const bits_offset = packed_row + word * 8;
            var bits = loadWordAt(call.output, bits_offset);
            const first_x = word * 64;
            const remaining = width - first_x;
            const bit_count = if (remaining < 64) remaining else 64;

            var bit: usize = 0;
            while (bit < bit_count) : (bit += 1) {
                call.output[destination_row + first_x + bit] = if ((bits & 1) != 0) 255 else 0;
                bits >>= 1;
            }
        }
    }
}

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
    const validation = validate(
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
    const call: ValidatedCall = switch (validation) {
        .status => |status| return status,
        .ok => |valid| valid,
    };

    if (gray.tryClose(call.input, call.scratch, call.output, width, height, call.taps, taps_len)) return .ok;

    if (fastPathLayout(
        call.input,
        call.pixel_count,
        width,
        height,
        call.taps,
        taps_len,
    )) |layout| {
        fastClose(call, width, height, taps_len, layout);
        return .ok;
    }

    return genericClose(call, width, height, taps_len);
}
