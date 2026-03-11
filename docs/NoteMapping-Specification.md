# Note Mapping Specification

## Overview

When Audiveris exports a MusicXML file, it simultaneously generates:

- a raw `.mapping.json` file that maps every MusicXML `<note>` element to pixel positions on the source raster
- a normalized `.geometry.sidecar.json` companion that reshapes the same data into `page -> system -> measure -> note`

The raw mapping enables powerful features:

1. **Playback highlighting** — Highlight notes/measures on the original image during MusicXML playback
2. **Voice filtering** — Highlight only the voice/part being played
3. **Real-time singing assessment** — Compare user's sung pitch/rhythm/duration against expected values with visual feedback on the original score image

## File Format

The raw mapping file is named by replacing the MusicXML file extension with `.mapping.json`:
- `score.xml` → `score.mapping.json`
- `score.mxl` → `score.mapping.json`

The normalized geometry companion is named by replacing the MusicXML file extension with
`.geometry.sidecar.json`:
- `score.xml` → `score.geometry.sidecar.json`
- `score.mxl` → `score.geometry.sidecar.json`

The file contains a single JSON object with the following top-level fields:

```json
{
  "divisions": 768,
  "tempos": [...],
  "timeSignatures": [...],
  "keySignatures": [...],
  "sheets": [...],
  "systems": [...],
  "measures": [...],
  "notes": [...]
}
```

The companion sidecar uses normalized page coordinates and is intended for PDF/image overlay
renderers. Its top-level structure is:

```json
{
  "schemaVersion": "1.0",
  "generatedAt": "2026-03-11T00:00:00.000Z",
  "engine": "audiveris-omr",
  "source": {...},
  "coordinateSpace": {
    "canonical": "page-normalized",
    "origin": "top-left",
    "axes": "x-right-y-down"
  },
  "pages": [...],
  "playback": {...}
}
```

## Coordinate System

All pixel coordinates are relative to the **original sheet image** (not the MusicXML coordinate system):
- **Origin (0, 0)**: Top-left corner of the image
- **X-axis**: Increases to the right
- **Y-axis**: Increases downward
- **Units**: Pixels

## Time Units

### Divisions

`divisions` represents the number of time divisions per quarter note. This is the fundamental unit used in MusicXML for note durations and time offsets.

- A **quarter note** = `divisions` time units
- A **half note** = `2 * divisions` time units
- An **eighth note** = `divisions / 2` time units
- Example: If `divisions = 768`, a quarter note has duration 768

### Time in Seconds

Time offsets and durations are also provided in seconds, calculated based on:
- The current tempo (in BPM, default 120 if no tempo marking)
- The formula: `seconds = (divisions_value * 60.0) / (divisions * BPM)`

Example with `divisions = 768` and tempo 120 BPM:
- Quarter note: `(768 * 60) / (768 * 120) = 0.5` seconds
- Half note: `(1536 * 60) / (768 * 120) = 1.0` seconds

## Top-Level Fields

### divisions (integer)

The number of divisions per quarter note used throughout the score.

### tempos (array)

Array of tempo markings found in the score. Each entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `partId` | string | Part identifier (e.g., "P1") |
| `measureNumber` | string | Measure number where tempo occurs |
| `timeOffset` | integer | Offset within measure in divisions |
| `bpm` | number | Beats per minute (quarters per minute) |
| `beatUnit` | string | Note type for the beat (e.g., "quarter", "half") |

### timeSignatures (array)

Array of time signature changes. Each entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `partId` | string | Part identifier |
| `measureNumber` | string | Measure number where time signature occurs |
| `numerator` | integer | Time signature numerator (e.g., 4 in 4/4) |
| `denominator` | integer | Time signature denominator (e.g., 4 in 4/4) |

### keySignatures (array)

Array of key signature changes. Each entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `partId` | string | Part identifier |
| `measureNumber` | string | Measure number where key signature occurs |
| `fifths` | integer | Number of sharps (positive) or flats (negative) |
| `mode` | string | "major" or "minor" |

### sheets (array)

Array of sheet (page) information. Each entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `sheetNumber` | integer | 1-based sheet number |
| `imageWidth` | integer | Width of sheet image in pixels |
| `imageHeight` | integer | Height of sheet image in pixels |

### systems (array)

Array of system (staff system) information. Each entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `systemIndex` | integer | 0-based system index within page |
| `sheetNumber` | integer | 1-based sheet number |
| `bounds` | object | Bounding box: `{x, y, width, height}` |

### measures (array)

Array of measure information. Each entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `partId` | string | Part identifier |
| `measureNumber` | string | Measure number |
| `sheetNumber` | integer | 1-based sheet number |
| `systemIndex` | integer | 0-based system index within page |
| `cumulativeTimeOffset` | integer | Time offset from start of piece (divisions) |
| `cumulativeTimeSeconds` | number | Time offset from start of piece (seconds) |
| `measureDuration` | integer | Duration of measure (divisions) |
| `measureDurationSeconds` | number | Duration of measure (seconds) |
| `bounds` | object | Bounding box: `{x, y, width, height}` |
| `staves` | array | Array of staff positions (see below) |

Each `staves` entry contains:

| Field | Type | Description |
|-------|------|-------------|
| `staffIndex` | integer | 0-based staff index within part |
| `topY` | integer | Y coordinate of top staff line |
| `bottomY` | integer | Y coordinate of bottom staff line |

### notes (array)

Array of note entries. This is the core data for note-to-pixel mapping. Each entry contains ALL of the following fields:

#### Positioning Fields

| Field | Type | Description |
|-------|------|-------------|
| `noteIndex` | integer | Per-part note sequence number (0-based) |
| `globalNoteIndex` | integer | Global note sequence across all parts (0-based) |
| `partId` | string | Part identifier (e.g., "P1") |
| `measureNumber` | string | Measure number |
| `staff` | integer | 1-based staff index within part |
| `voice` | string | Voice identifier |
| `noteIndexInChord` | integer | 0-based position within chord (0 for first note) |
| `sheetNumber` | integer | 1-based sheet number |
| `systemIndex` | integer | 0-based system index within page |

#### Note Property Fields

| Field | Type | Description |
|-------|------|-------------|
| `isRest` | boolean | True if this is a rest |
| `isGrace` | boolean | True for grace notes (small notes) |
| `isMeasureRest` | boolean | True for whole-measure rests |
| `isTiedStart` | boolean | True if this note starts a tie |
| `isTiedStop` | boolean | True if this note ends a tie |

#### Pitch Fields

| Field | Type | Description |
|-------|------|-------------|
| `step` | string or null | Note step: "C", "D", "E", "F", "G", "A", "B" (null for rests) |
| `octave` | integer | Octave number (0 for rests) |
| `alter` | integer | Alteration: -2 (double flat), -1 (flat), 0 (natural), 1 (sharp), 2 (double sharp) |
| `absolutePitch` | integer | MIDI pitch number (0-127, 0 for rests) |
| `integerPitch` | integer | Staff pitch position |
| `expectedFrequency` | number | Expected frequency in Hz (0.0 for rests) |

The `expectedFrequency` is calculated using the standard formula:
```
frequency = 440.0 * 2^((absolutePitch - 69) / 12)
```
where 69 is the MIDI number for A4 (440 Hz).

#### Duration/Type Fields

| Field | Type | Description |
|-------|------|-------------|
| `noteType` | string | Note type: "whole", "half", "quarter", "eighth", "16th", "32nd", "64th", "128th", "256th" |
| `dots` | integer | Number of augmentation dots (0, 1, or 2) |
| `stemDirection` | integer | Stem direction: 1 (up), -1 (down), 0 (none) |
| `beamGroupId` | integer or null | Unique ID for beam group (null if not beamed) |

#### Time Fields

| Field | Type | Description |
|-------|------|-------------|
| `timeOffset` | integer | Offset within measure (divisions) |
| `duration` | integer | Note duration (divisions), 0 for grace notes |
| `measureCumulativeTimeOffset` | integer | Start of containing measure from beginning of piece (divisions) |
| `timeOffsetSeconds` | number | Absolute time from start of piece (seconds) |
| `durationSeconds` | number | Note duration (seconds) |
| `tiedDuration` | integer | Total duration including tied notes (divisions) |
| `tiedDurationSeconds` | number | Total duration including tied notes (seconds) |

Stable absolute note start can be reconstructed as:

```text
absoluteStartDivision = measureCumulativeTimeOffset + timeOffset
```

For tied notes:
- `isTiedStart = true`: `tiedDuration` includes this note and all following tied notes
- `isTiedStop = true`: `tiedDuration` equals `duration` (only this note's duration)
- Non-tied notes: `tiedDuration` equals `duration`

#### Geometry Fields

| Field | Type | Description |
|-------|------|-------------|
| `bounds` | object | Note head bounding box: `{x, y, width, height}` |
| `center` | object | Note head center point: `{x, y}` |
| `chordBounds` | object | Entire chord bounding box: `{x, y, width, height}` |
| `staffTopY` | integer | Y coordinate of top staff line at note's X position |
| `staffBottomY` | integer | Y coordinate of bottom staff line at note's X position |

## Real-Time Singing Assessment Flow

### Overview

The note mapping enables real-time singing assessment by comparing the user's sung notes against the expected musical score. The system provides immediate visual and analytical feedback.

### Assessment Components

#### 1. Pitch Evaluation

**Goal**: Determine if the sung pitch matches the expected pitch.

**Method**:
1. Detect the fundamental frequency (F0) of the user's voice using pitch detection (e.g., YIN, CREPE, or pYIN algorithms)
2. Compare against `expectedFrequency` from the note entry
3. Calculate the cent difference:
   ```
   cents = 1200 * log2(sung_frequency / expected_frequency)
   ```
4. Classify result:
   - **Correct**: |cents| < 50 (within ±50 cents)
   - **Slightly off**: 50 ≤ |cents| < 100
   - **Wrong**: |cents| ≥ 100

**Visual Feedback**:
- Green highlight: Correct pitch
- Yellow highlight: Slightly off
- Red highlight: Wrong pitch
- Display cent deviation: "+15¢" or "-32¢"

#### 2. Rhythm Evaluation

**Goal**: Determine if the note is sung at the correct time.

**Method**:
1. Calculate expected onset time: `timeOffsetSeconds`
2. Detect actual onset time of sung note
3. Calculate timing error:
   ```
   timing_error = actual_onset - expected_onset (in seconds)
   ```
4. Classify result:
   - **On time**: |timing_error| < 0.1 seconds
   - **Slightly early/late**: 0.1 ≤ |timing_error| < 0.2 seconds
   - **Too early/late**: |timing_error| ≥ 0.2 seconds

**Visual Feedback**:
- Pulse animation at correct time
- Early/late indicator with millisecond deviation

#### 3. Duration Evaluation

**Goal**: Determine if the note is held for the correct duration.

**Method**:
1. Expected duration: `durationSeconds` (or `tiedDurationSeconds` for tied notes)
2. Measure actual sung duration
3. Calculate duration ratio:
   ```
   ratio = actual_duration / expected_duration
   ```
4. Classify result:
   - **Correct**: 0.8 < ratio < 1.2 (within ±20%)
   - **Too short**: ratio ≤ 0.8
   - **Too long**: ratio ≥ 1.2

**Visual Feedback**:
- Progress bar showing expected vs. actual duration
- Color-coded duration indicator

### Tie Handling

For tied notes (where `isTiedStart = true`):

1. Use `tiedDuration` and `tiedDurationSeconds` for duration evaluation
2. Allow the sung note to span multiple visual note heads
3. Highlight all tied notes in the chain during playback
4. Only evaluate pitch at the first note of the tie

Example:
```
Note 1: isTiedStart=true, duration=768, tiedDuration=1536
  → Expect singer to hold for 1536 divisions (2 quarter notes)
Note 2: isTiedStop=true, duration=768, tiedDuration=768
  → This is the ending note of the tie chain
```

### Voice Filtering Logic

To highlight only notes from a specific voice/part:

```javascript
function filterNotesByVoice(notes, targetPartId, targetVoiceId) {
  return notes.filter(note => 
    note.partId === targetPartId && 
    note.voice === targetVoiceId
  );
}
```

For multi-staff parts, you can further filter by staff:

```javascript
function filterNotesByStaff(notes, targetPartId, targetStaff) {
  return notes.filter(note => 
    note.partId === targetPartId && 
    note.staff === targetStaff
  );
}
```

### Playback Highlighting

#### Basic Note Highlighting

```javascript
function highlightNoteAtTime(notes, currentTimeSeconds) {
  return notes.filter(note => {
    const noteStart = note.timeOffsetSeconds;
    const noteEnd = noteStart + note.durationSeconds;
    return currentTimeSeconds >= noteStart && currentTimeSeconds < noteEnd;
  });
}
```

#### Measure Highlighting

```javascript
function highlightMeasureAtTime(measures, currentTimeSeconds) {
  return measures.find(measure => {
    const measureStart = measure.cumulativeTimeSeconds;
    const measureEnd = measureStart + measure.measureDurationSeconds;
    return currentTimeSeconds >= measureStart && currentTimeSeconds < measureEnd;
  });
}
```

#### Drawing the Highlight

Use the `bounds` or `center` fields to draw highlights on a canvas overlay:

```javascript
function drawNoteHighlight(canvas, note, color) {
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = color;
  ctx.globalAlpha = 0.3;
  ctx.fillRect(note.bounds.x, note.bounds.y, 
               note.bounds.width, note.bounds.height);
  ctx.globalAlpha = 1.0;
}
```

## Example JSON Output

```json
{
  "divisions": 768,
  "tempos": [
    {
      "partId": "P1",
      "measureNumber": "1",
      "timeOffset": 0,
      "bpm": 120.0,
      "beatUnit": "quarter"
    }
  ],
  "timeSignatures": [
    {
      "partId": "P1",
      "measureNumber": "1",
      "numerator": 4,
      "denominator": 4
    }
  ],
  "keySignatures": [
    {
      "partId": "P1",
      "measureNumber": "1",
      "fifths": 0,
      "mode": "major"
    }
  ],
  "sheets": [
    {
      "sheetNumber": 1,
      "imageWidth": 2480,
      "imageHeight": 3508
    }
  ],
  "systems": [
    {
      "systemIndex": 0,
      "sheetNumber": 1,
      "bounds": {"x": 150, "y": 300, "width": 2180, "height": 450}
    }
  ],
  "measures": [
    {
      "partId": "P1",
      "measureNumber": "1",
      "sheetNumber": 1,
      "systemIndex": 0,
      "cumulativeTimeOffset": 0,
      "cumulativeTimeSeconds": 0.0,
      "measureDuration": 3072,
      "measureDurationSeconds": 2.0,
      "bounds": {"x": 200, "y": 320, "width": 500, "height": 180},
      "staves": [
        {
          "staffIndex": 0,
          "topY": 320,
          "bottomY": 400
        }
      ]
    }
  ],
  "notes": [
    {
      "noteIndex": 0,
      "globalNoteIndex": 0,
      "partId": "P1",
      "measureNumber": "1",
      "staff": 1,
      "voice": "1",
      "noteIndexInChord": 0,
      "sheetNumber": 1,
      "systemIndex": 0,
      "isRest": false,
      "isGrace": false,
      "isMeasureRest": false,
      "isTiedStart": false,
      "isTiedStop": false,
      "step": "C",
      "octave": 4,
      "alter": 0,
      "absolutePitch": 60,
      "integerPitch": 0,
      "expectedFrequency": 261.6255653005986,
      "noteType": "quarter",
      "dots": 0,
      "stemDirection": 1,
      "beamGroupId": null,
      "timeOffset": 0,
      "duration": 768,
      "timeOffsetSeconds": 0.0,
      "durationSeconds": 0.5,
      "tiedDuration": 768,
      "tiedDurationSeconds": 0.5,
      "bounds": {"x": 250, "y": 360, "width": 24, "height": 16},
      "center": {"x": 262, "y": 368},
      "chordBounds": {"x": 248, "y": 330, "width": 28, "height": 50},
      "staffTopY": 320,
      "staffBottomY": 400
    },
    {
      "noteIndex": 1,
      "globalNoteIndex": 1,
      "partId": "P1",
      "measureNumber": "1",
      "staff": 1,
      "voice": "1",
      "noteIndexInChord": 0,
      "sheetNumber": 1,
      "systemIndex": 0,
      "isRest": false,
      "isGrace": false,
      "isMeasureRest": false,
      "isTiedStart": true,
      "isTiedStop": false,
      "step": "D",
      "octave": 4,
      "alter": 0,
      "absolutePitch": 62,
      "integerPitch": 1,
      "expectedFrequency": 293.6647679174076,
      "noteType": "quarter",
      "dots": 0,
      "stemDirection": 1,
      "beamGroupId": null,
      "timeOffset": 768,
      "duration": 768,
      "timeOffsetSeconds": 0.5,
      "durationSeconds": 0.5,
      "tiedDuration": 1536,
      "tiedDurationSeconds": 1.0,
      "bounds": {"x": 380, "y": 352, "width": 24, "height": 16},
      "center": {"x": 392, "y": 360},
      "chordBounds": {"x": 378, "y": 322, "width": 28, "height": 50},
      "staffTopY": 320,
      "staffBottomY": 400
    },
    {
      "noteIndex": 2,
      "globalNoteIndex": 2,
      "partId": "P1",
      "measureNumber": "1",
      "staff": 1,
      "voice": "1",
      "noteIndexInChord": 0,
      "sheetNumber": 1,
      "systemIndex": 0,
      "isRest": false,
      "isGrace": false,
      "isMeasureRest": false,
      "isTiedStart": false,
      "isTiedStop": true,
      "step": "D",
      "octave": 4,
      "alter": 0,
      "absolutePitch": 62,
      "integerPitch": 1,
      "expectedFrequency": 293.6647679174076,
      "noteType": "quarter",
      "dots": 0,
      "stemDirection": 1,
      "beamGroupId": null,
      "timeOffset": 1536,
      "duration": 768,
      "timeOffsetSeconds": 1.0,
      "durationSeconds": 0.5,
      "tiedDuration": 768,
      "tiedDurationSeconds": 0.5,
      "bounds": {"x": 510, "y": 352, "width": 24, "height": 16},
      "center": {"x": 522, "y": 360},
      "chordBounds": {"x": 508, "y": 322, "width": 28, "height": 50},
      "staffTopY": 320,
      "staffBottomY": 400
    }
  ]
}
```

## Implementation Notes

### Error Handling

All note mapping collection is wrapped in try-catch blocks. If any error occurs during mapping collection, it is logged but does not prevent the MusicXML export from completing successfully.

### Backward Compatibility

The original `PartwiseBuilder.build()` method remains unchanged. Only when using `PartwiseBuilder.buildWithMapping()` is note mapping data collected.

### Performance Considerations

- Note mapping collection adds minimal overhead to MusicXML export
- JSON serialization is performed using efficient StringBuilder-based approach
- No external dependencies required

### Limitations

1. **Mode Detection**: Key signatures always report "major" mode. Minor mode detection is not yet implemented but could be enhanced based on the key signature's fifths value and musical context.

2. **Percussion**: Unpitched percussion notes have `absolutePitch = 0` and `expectedFrequency = 0.0`. Use `integerPitch` for drum mapping instead.

3. **Microtones**: Only standard alterations (-2 to +2) are supported. Microtonal alterations are not captured.

4. **Cross-System Ties**: Tie chains are followed within and across systems, but edge cases with very complex tie structures may not be fully captured.

## Future Enhancements

Potential future improvements to the note mapping feature:

1. **Enhanced Mode Detection**: Analyze the key signature and note patterns to determine major vs. minor mode
2. **Articulation Mapping**: Include articulation marks (staccato, accent, etc.) in note entries
3. **Dynamic Markings**: Map dynamic markings (p, f, mf, etc.) to their positions
4. **Lyric Positions**: Include syllable positions for karaoke-style highlighting
5. **Ornament Details**: Include trill, mordent, and other ornament positions
6. **Fingering Numbers**: Map fingering numbers to note positions
7. **Compressed Output**: Option to export mapping as gzipped JSON for large scores

## Version History

- **1.0** (2025-01): Initial release with comprehensive note mapping support

## License

This specification and the Audiveris implementation are licensed under the GNU Affero General Public License v3.0 or later.
