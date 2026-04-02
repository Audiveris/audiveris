package org.audiveris.omr.score;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.audiveris.omr.util.BaseTestCase;

import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Tests for geometry sidecar playback bindings.
 */
public class GeometrySidecarExporterTest
        extends BaseTestCase
{
    @Test
    public void testPlaybackBindingsUsePlayablePartOrdinalsAndPlaybackOrder ()
        throws Exception
    {
        final NoteMapping mapping = new NoteMapping();
        mapping.addSheet(new NoteMapping.SheetInfo(1, 1000, 1000));
        mapping.addSystem(new NoteMapping.SystemInfo(0, 1, new Rectangle(0, 0, 1000, 400)));
        mapping.addMeasure(
                new NoteMapping.MeasureInfo(
                        "P1",
                        "1",
                        1,
                        0,
                        0,
                        0.0,
                        4,
                        1.0,
                        new Rectangle(0, 0, 1000, 200),
                        List.of(new NoteMapping.StaffInfo(0, 0, 100))));
        mapping.addMeasure(
                new NoteMapping.MeasureInfo(
                        "P2",
                        "1",
                        1,
                        0,
                        0,
                        0.0,
                        4,
                        1.0,
                        new Rectangle(0, 200, 1000, 200),
                        List.of(new NoteMapping.StaffInfo(0, 200, 300))));

        mapping.addNote(note(0, 0, "P1", "1", "1", true, null, 0.0));
        mapping.addNote(note(1, 1, "P1", "1", "2", false, "C", 0.0));
        mapping.addNote(note(2, 2, "P1", "1", "1", false, "E", 0.0));
        mapping.addNote(note(0, 3, "P2", "1", "1", false, "G", 0.0));

        final String json = GeometrySidecarExporter.buildJson(mapping, null, null);
        final JsonNode refs = new ObjectMapper().readTree(json).path("playback").path("noteRefs");

        assertEquals(3, refs.size());

        assertEquals("note-2", refs.get(0).path("noteId").asText());
        assertEquals(0, refs.get(0).path("playbackIndex").asInt());
        assertEquals(1, refs.get(0).path("musicXmlNoteOrdinal").asInt());

        assertEquals("note-1", refs.get(1).path("noteId").asText());
        assertEquals(1, refs.get(1).path("playbackIndex").asInt());
        assertEquals(0, refs.get(1).path("musicXmlNoteOrdinal").asInt());

        assertEquals("note-3", refs.get(2).path("noteId").asText());
        assertEquals(2, refs.get(2).path("playbackIndex").asInt());
        assertEquals(0, refs.get(2).path("musicXmlNoteOrdinal").asInt());
    }

    private static NoteMapping.NoteEntry note (int noteIndex,
                                               int globalNoteIndex,
                                               String partId,
                                               String measureNumber,
                                               String voice,
                                               boolean isRest,
                                               String step,
                                               double startSeconds)
    {
        final int x = 20 + (globalNoteIndex * 20);
        final int y = "P1".equals(partId) ? 40 : 240;
        final Rectangle noteBounds = new Rectangle(x, y, 10, 10);
        final Rectangle chordBounds = new Rectangle(x - 2, y - 2, 14, 14);
        final Point center = new Point(x + 5, y + 5);

        return new NoteMapping.NoteEntry(
                noteIndex,
                globalNoteIndex,
                partId,
                measureNumber,
                1,
                voice,
                0,
                1,
                0,
                isRest,
                false,
                false,
                false,
                false,
                step,
                isRest ? 0 : 4,
                0,
                isRest ? 0 : midiForStep(step),
                isRest ? 0 : midiForStep(step),
                isRest ? 0.0 : 440.0,
                "quarter",
                0,
                0,
                null,
                0,
                4,
                0,
                startSeconds,
                0.5,
                4,
                0.5,
                noteBounds,
                center,
                chordBounds,
                y - 20,
                y + 20);
    }

    private static int midiForStep (String step)
    {
        return switch (step) {
            case "C" -> 60;
            case "E" -> 64;
            case "G" -> 67;
            default -> 60;
        };
    }
}
