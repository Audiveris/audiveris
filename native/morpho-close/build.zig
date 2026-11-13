// SPDX-License-Identifier: AGPL-3.0-only
//
// Isolated Audiveris morphology worker v1. Corresponding Source is supplied
// with this package; see README.md and NOTICE for the source-offer notice.

const std = @import("std");

pub fn build(b: *std.Build) void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});

    const library = b.addLibrary(.{
        .name = "musicspace_morpho_close_v1",
        .linkage = .dynamic,
        .root_module = b.createModule(.{
            .root_source_file = b.path("src/library.zig"),
            .target = target,
            .optimize = optimize,
        }),
    });
    b.installArtifact(library);

    const worker = b.addExecutable(.{
        .name = "audiveris-morpho-worker",
        .root_module = b.createModule(.{
            .root_source_file = b.path("src/worker.zig"),
            .target = target,
            .optimize = optimize,
        }),
    });
    b.installArtifact(worker);

    const test_artifact = b.addTest(.{
        .root_module = b.createModule(.{
            .root_source_file = b.path("src/morpho_close_test.zig"),
            .target = target,
            .optimize = optimize,
        }),
    });
    const run_tests = b.addRunArtifact(test_artifact);
    const test_step = b.step("test", "Run focused ABI, kernel, and protocol tests");
    test_step.dependOn(&run_tests.step);
}
