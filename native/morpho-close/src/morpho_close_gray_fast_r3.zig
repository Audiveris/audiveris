const abi = @import("morpho_close_abi.zig");
const testing = @import("std").testing;

const usize_max: usize = ~@as(usize, 0);

// The queue is deliberately fixed-size.  Its capacity is also the largest
// horizontal span accepted by the fast path, so all storage is known before
// either destination is touched.
const queue_capacity: usize = 1 << 15;
const queue_mask: usize = queue_capacity - 1;
const max_radius: usize = (queue_capacity - 1) / 2;
const max_rows: usize = 1024;
const max_taps: usize = 65536;
const max_width: usize = 1 << 22;
const max_height: usize = 1 << 20;

const RowSpec = struct {
    dy: i32,
    radius: usize,
};

const QueueEntry = struct {
    sequence: usize,
    value: u8,
};

const SlidingQueue = struct {
    entries: [queue_capacity]QueueEntry,
    head: usize,
    length: usize,

    fn reset(self: *SlidingQueue) void {
        self.head = 0;
        self.length = 0;
    }

    fn slot(self: *const SlidingQueue, offset: usize) usize {
        return (self.head + offset) & queue_mask;
    }

    fn front(self: *const SlidingQueue) QueueEntry {
        return self.entries[self.head];
    }

    fn back(self: *const SlidingQueue) QueueEntry {
        return self.entries[self.slot(self.length - 1)];
    }

    fn popFront(self: *SlidingQueue) void {
        self.head = (self.head + 1) & queue_mask;
        self.length -= 1;
    }

    fn popBack(self: *SlidingQueue) void {
        self.length -= 1;
    }

    fn pushBack(self: *SlidingQueue, entry: QueueEntry) void {
        // Preflight proves that a complete source window fits in the queue.
        // The monotonic queue can never contain more entries than that window.
        if (self.length == queue_capacity) unreachable;
        self.entries[self.slot(self.length)] = entry;
        self.length += 1;
    }

    fn pushMax(self: *SlidingQueue, sequence: usize, value: u8) void {
        while (self.length != 0 and self.back().value <= value) {
            self.popBack();
        }
        self.pushBack(.{ .sequence = sequence, .value = value });
    }

    fn pushMin(self: *SlidingQueue, sequence: usize, value: u8) void {
        while (self.length != 0 and self.back().value >= value) {
            self.popBack();
        }
        self.pushBack(.{ .sequence = sequence, .value = value });
    }
};

const Reduce = enum { max, min };

fn checkedShiftedRow(y: usize, dy: i32, height: usize) ?usize {
    const signed_y: i64 = @intCast(y);
    const signed_dy: i64 = @intCast(dy);
    const shifted = signed_y + signed_dy;
    if (shifted < 0 or shifted >= @as(i64, @intCast(height))) return null;
    return @intCast(shifted);
}

fn extendedSample(
    source: [*]const u8,
    source_row: usize,
    width: usize,
    radius: usize,
    sequence: usize,
    outside: u8,
) u8 {
    const signed_sequence: i64 = @intCast(sequence);
    const signed_radius: i64 = @intCast(radius);
    const source_x = signed_sequence - signed_radius;
    if (source_x < 0 or source_x >= @as(i64, @intCast(width))) return outside;
    return source[source_row + @as(usize, @intCast(source_x))];
}

fn combine(destination: [*]u8, index: usize, candidate: u8, reduce: Reduce) void {
    switch (reduce) {
        .max => {
            if (candidate > destination[index]) destination[index] = candidate;
        },
        .min => {
            if (candidate < destination[index]) destination[index] = candidate;
        },
    }
}

fn horizontalSpan(
    source: [*]const u8,
    source_row: usize,
    destination: [*]u8,
    destination_row: usize,
    width: usize,
    radius: usize,
    outside: u8,
    reduce: Reduce,
    queue: *SlidingQueue,
) void {
    const window_length = radius * 2 + 1;
    queue.reset();

    var sequence: usize = 0;
    while (sequence < window_length) : (sequence += 1) {
        const value = extendedSample(source, source_row, width, radius, sequence, outside);
        switch (reduce) {
            .max => queue.pushMax(sequence, value),
            .min => queue.pushMin(sequence, value),
        }
    }

    var x: usize = 0;
    while (x < width) : (x += 1) {
        if (x != 0) {
            while (queue.length != 0 and queue.front().sequence < x) queue.popFront();

            const incoming_sequence = x + window_length - 1;
            const value = extendedSample(
                source,
                source_row,
                width,
                radius,
                incoming_sequence,
                outside,
            );
            switch (reduce) {
                .max => queue.pushMax(incoming_sequence, value),
                .min => queue.pushMin(incoming_sequence, value),
            }
        }

        combine(destination, destination_row + x, queue.front().value, reduce);
    }
}

fn runStage(
    source: [*]const u8,
    destination: [*]u8,
    width: usize,
    height: usize,
    rows: *const [max_rows]RowSpec,
    row_count: usize,
    outside: u8,
    reduce: Reduce,
    queue: *SlidingQueue,
) void {
    var y: usize = 0;
    while (y < height) : (y += 1) {
        const destination_row = y * width;
        var x: usize = 0;
        while (x < width) : (x += 1) {
            destination[destination_row + x] = outside;
        }

        var row_index: usize = 0;
        while (row_index < row_count) : (row_index += 1) {
            const row = rows.*[row_index];
            const source_y = checkedShiftedRow(y, row.dy, height) orelse continue;
            const source_row = source_y * width;
            horizontalSpan(
                source,
                source_row,
                destination,
                destination_row,
                width,
                row.radius,
                outside,
                reduce,
                queue,
            );
        }
    }
}

fn preflight(
    width: usize,
    height: usize,
    taps: [*]const abi.Tap,
    taps_len: usize,
    rows: *[max_rows]RowSpec,
) ?usize {
    if (width == 0 or height == 0 or taps_len == 0) return null;
    if (width > max_width or height > max_height) return null;
    if (width > usize_max / height) return null;
    if (taps_len > max_taps) return null;
    if (taps_len > usize_max / @sizeOf(abi.Tap)) return null;

    var tap_index: usize = 0;
    var row_count: usize = 0;
    var previous_dy: i32 = 0;
    var have_previous_dy = false;

    while (tap_index < taps_len) {
        if (row_count == max_rows) return null;

        const row_start = tap_index;
        const row_dy = taps[row_start].dy;
        if (have_previous_dy and row_dy <= previous_dy) return null;
        previous_dy = row_dy;
        have_previous_dy = true;

        const first_dx: i64 = @intCast(taps[row_start].dx);
        if (first_dx > 0) return null;
        const radius_i64 = -first_dx;
        const radius: usize = @intCast(radius_i64);
        if (radius > max_radius) return null;
        if (radius > (usize_max - width) / 2) return null;
        const span = radius * 2 + 1;

        var offset: usize = 0;
        while (tap_index < taps_len and taps[tap_index].dy == row_dy) {
            if (offset >= span) return null;
            const expected_dx: i64 = -radius_i64 + @as(i64, @intCast(offset));
            const tap = taps[tap_index];
            if (tap.weight != 255) return null;
            if (@as(i64, @intCast(tap.dx)) != expected_dx) return null;
            tap_index += 1;
            offset += 1;
        }

        if (offset != span) return null;
        rows[row_count] = .{ .dy = row_dy, .radius = radius };
        row_count += 1;
    }

    return row_count;
}

pub fn tryClose(
    input: [*]const u8,
    dilated: [*]u8,
    output: [*]u8,
    width: usize,
    height: usize,
    taps: [*]const abi.Tap,
    taps_len: usize,
) bool {
    var rows: [max_rows]RowSpec = undefined;
    const row_count = preflight(width, height, taps, taps_len, &rows) orelse return false;

    var queue: SlidingQueue = undefined;
    queue.reset();

    runStage(input, dilated, width, height, &rows, row_count, 0, .max, &queue);
    runStage(dilated, output, width, height, &rows, row_count, 255, .min, &queue);
    return true;
}

test "eligible gray row spans preserve border and gray values" {
    const input = [_]u8{
        1,  2,  3,  4,  5,
        6,  7,  8,  9,  10,
        11, 12, 13, 14, 15,
    };
    const taps = [_]abi.Tap{
        .{ .dy = -1, .dx = 0, .weight = 255 },
        .{ .dy = 0, .dx = -1, .weight = 255 },
        .{ .dy = 0, .dx = 0, .weight = 255 },
        .{ .dy = 0, .dx = 1, .weight = 255 },
        .{ .dy = 1, .dx = 0, .weight = 255 },
    };
    const expected_dilated = [_]u8{
        6,  7,  8,  9,  10,
        11, 12, 13, 14, 15,
        12, 13, 14, 15, 15,
    };
    const expected_output = [_]u8{
        6,  6,  7,  8,  9,
        6,  7,  8,  9,  10,
        11, 12, 13, 14, 15,
    };
    var dilated = [_]u8{0xaa} ** input.len;
    var output = [_]u8{0x55} ** input.len;

    try testing.expect(tryClose(
        input[0..].ptr,
        dilated[0..].ptr,
        output[0..].ptr,
        5,
        3,
        taps[0..].ptr,
        taps.len,
    ));
    try testing.expectEqualSlices(u8, expected_dilated[0..], dilated[0..]);
    try testing.expectEqualSlices(u8, expected_output[0..], output[0..]);
}

test "ineligible row span leaves destinations untouched" {
    const input = [_]u8{ 17, 128, 241, 33 };
    const input_before = input;
    const taps = [_]abi.Tap{.{ .dy = 0, .dx = 0, .weight = 0 }};
    var dilated = [_]u8{0xa5} ** input.len;
    const dilated_before = dilated;
    var output = [_]u8{0x5a} ** input.len;
    const output_before = output;

    try testing.expect(!tryClose(
        input[0..].ptr,
        dilated[0..].ptr,
        output[0..].ptr,
        4,
        1,
        taps[0..].ptr,
        taps.len,
    ));
    try testing.expectEqualSlices(u8, input_before[0..], input[0..]);
    try testing.expectEqualSlices(u8, dilated_before[0..], dilated[0..]);
    try testing.expectEqualSlices(u8, output_before[0..], output[0..]);
}
