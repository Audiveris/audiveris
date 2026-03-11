//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     N o t e M a p p i n g                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.score;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>NoteMapping</code> collects note mapping entries during MusicXML export
 * and serializes to JSON. Maps every MusicXML note element to its pixel position on the
 * original sheet image for playback highlighting and singing assessment.
 *
 * @author Hervé Bitteur
 */
public class NoteMapping
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The divisions per quarter note. */
    private int divisions;

    /** List of tempo markings. */
    private final List<TempoInfo> tempos = new ArrayList<>();

    /** List of time signatures. */
    private final List<TimeSignatureInfo> timeSignatures = new ArrayList<>();

    /** List of key signatures. */
    private final List<KeySignatureInfo> keySignatures = new ArrayList<>();

    /** List of sheets. */
    private final List<SheetInfo> sheets = new ArrayList<>();

    /** List of systems. */
    private final List<SystemInfo> systems = new ArrayList<>();

    /** List of measures. */
    private final List<MeasureInfo> measures = new ArrayList<>();

    /** List of note entries. */
    private final List<NoteEntry> notes = new ArrayList<>();

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Set the divisions per quarter note.
     *
     * @param divisions the divisions value
     */
    public void setDivisions (int divisions)
    {
        this.divisions = divisions;
    }

    /**
     * Add a sheet info entry.
     *
     * @param sheet the sheet info
     */
    public void addSheet (SheetInfo sheet)
    {
        sheets.add(sheet);
    }

    /**
     * Add a system info entry.
     *
     * @param system the system info
     */
    public void addSystem (SystemInfo system)
    {
        systems.add(system);
    }

    /**
     * Add a measure info entry.
     *
     * @param measure the measure info
     */
    public void addMeasure (MeasureInfo measure)
    {
        measures.add(measure);
    }

    /**
     * Add a tempo info entry.
     *
     * @param tempo the tempo info
     */
    public void addTempo (TempoInfo tempo)
    {
        tempos.add(tempo);
    }

    /**
     * Add a time signature info entry.
     *
     * @param timeSig the time signature info
     */
    public void addTimeSignature (TimeSignatureInfo timeSig)
    {
        timeSignatures.add(timeSig);
    }

    /**
     * Add a key signature info entry.
     *
     * @param keySig the key signature info
     */
    public void addKeySignature (KeySignatureInfo keySig)
    {
        keySignatures.add(keySig);
    }

    /**
     * Add a note entry.
     *
     * @param note the note entry
     */
    public void addNote (NoteEntry note)
    {
        notes.add(note);
    }

    /**
     * Report the divisions per quarter note.
     *
     * @return the divisions value
     */
    public int getDivisions ()
    {
        return divisions;
    }

    /**
     * Report the collected sheet entries.
     *
     * @return immutable view on sheet entries
     */
    public List<SheetInfo> getSheets ()
    {
        return List.copyOf(sheets);
    }

    /**
     * Report the collected system entries.
     *
     * @return immutable view on system entries
     */
    public List<SystemInfo> getSystems ()
    {
        return List.copyOf(systems);
    }

    /**
     * Report the collected measure entries.
     *
     * @return immutable view on measure entries
     */
    public List<MeasureInfo> getMeasures ()
    {
        return List.copyOf(measures);
    }

    /**
     * Report the collected note entries.
     *
     * @return immutable view on note entries
     */
    public List<NoteEntry> getNotes ()
    {
        return List.copyOf(notes);
    }

    /**
     * Check if this mapping is empty.
     *
     * @return true if there are no notes
     */
    public boolean isEmpty ()
    {
        return notes.isEmpty();
    }

    /**
     * Serialize this mapping to JSON string.
     *
     * @return JSON representation
     */
    public String toJson ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"divisions\": ").append(divisions).append(",\n");
        
        // Tempos
        sb.append("  \"tempos\": [\n");
        for (int i = 0; i < tempos.size(); i++) {
            TempoInfo t = tempos.get(i);
            sb.append("    {\n");
            sb.append("      \"partId\": ").append(jsonString(t.partId)).append(",\n");
            sb.append("      \"measureNumber\": ").append(jsonString(t.measureNumber)).append(",\n");
            sb.append("      \"timeOffset\": ").append(t.timeOffset).append(",\n");
            sb.append("      \"bpm\": ").append(t.bpm).append(",\n");
            sb.append("      \"beatUnit\": ").append(jsonString(t.beatUnit)).append("\n");
            sb.append("    }");
            if (i < tempos.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Time signatures
        sb.append("  \"timeSignatures\": [\n");
        for (int i = 0; i < timeSignatures.size(); i++) {
            TimeSignatureInfo ts = timeSignatures.get(i);
            sb.append("    {\n");
            sb.append("      \"partId\": ").append(jsonString(ts.partId)).append(",\n");
            sb.append("      \"measureNumber\": ").append(jsonString(ts.measureNumber)).append(",\n");
            sb.append("      \"numerator\": ").append(ts.numerator).append(",\n");
            sb.append("      \"denominator\": ").append(ts.denominator).append("\n");
            sb.append("    }");
            if (i < timeSignatures.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Key signatures
        sb.append("  \"keySignatures\": [\n");
        for (int i = 0; i < keySignatures.size(); i++) {
            KeySignatureInfo ks = keySignatures.get(i);
            sb.append("    {\n");
            sb.append("      \"partId\": ").append(jsonString(ks.partId)).append(",\n");
            sb.append("      \"measureNumber\": ").append(jsonString(ks.measureNumber)).append(",\n");
            sb.append("      \"fifths\": ").append(ks.fifths).append(",\n");
            sb.append("      \"mode\": ").append(jsonString(ks.mode)).append("\n");
            sb.append("    }");
            if (i < keySignatures.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Sheets
        sb.append("  \"sheets\": [\n");
        for (int i = 0; i < sheets.size(); i++) {
            SheetInfo s = sheets.get(i);
            sb.append("    {\n");
            sb.append("      \"sheetNumber\": ").append(s.sheetNumber).append(",\n");
            sb.append("      \"imageWidth\": ").append(s.imageWidth).append(",\n");
            sb.append("      \"imageHeight\": ").append(s.imageHeight).append("\n");
            sb.append("    }");
            if (i < sheets.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Systems
        sb.append("  \"systems\": [\n");
        for (int i = 0; i < systems.size(); i++) {
            SystemInfo sys = systems.get(i);
            sb.append("    {\n");
            sb.append("      \"systemIndex\": ").append(sys.systemIndex).append(",\n");
            sb.append("      \"sheetNumber\": ").append(sys.sheetNumber).append(",\n");
            sb.append("      \"bounds\": ").append(boundsToJson(sys.bounds)).append("\n");
            sb.append("    }");
            if (i < systems.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Measures
        sb.append("  \"measures\": [\n");
        for (int i = 0; i < measures.size(); i++) {
            MeasureInfo m = measures.get(i);
            sb.append("    {\n");
            sb.append("      \"partId\": ").append(jsonString(m.partId)).append(",\n");
            sb.append("      \"measureNumber\": ").append(jsonString(m.measureNumber)).append(",\n");
            sb.append("      \"sheetNumber\": ").append(m.sheetNumber).append(",\n");
            sb.append("      \"systemIndex\": ").append(m.systemIndex).append(",\n");
            sb.append("      \"cumulativeTimeOffset\": ").append(m.cumulativeTimeOffset).append(",\n");
            sb.append("      \"cumulativeTimeSeconds\": ").append(m.cumulativeTimeSeconds).append(",\n");
            sb.append("      \"measureDuration\": ").append(m.measureDuration).append(",\n");
            sb.append("      \"measureDurationSeconds\": ").append(m.measureDurationSeconds).append(",\n");
            sb.append("      \"bounds\": ").append(boundsToJson(m.bounds)).append(",\n");
            sb.append("      \"staves\": [\n");
            for (int j = 0; j < m.staves.size(); j++) {
                StaffInfo st = m.staves.get(j);
                sb.append("        {\n");
                sb.append("          \"staffIndex\": ").append(st.staffIndex).append(",\n");
                sb.append("          \"topY\": ").append(st.topY).append(",\n");
                sb.append("          \"bottomY\": ").append(st.bottomY).append("\n");
                sb.append("        }");
                if (j < m.staves.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }");
            if (i < measures.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Notes
        sb.append("  \"notes\": [\n");
        for (int i = 0; i < notes.size(); i++) {
            NoteEntry n = notes.get(i);
            sb.append("    {\n");
            sb.append("      \"noteIndex\": ").append(n.noteIndex).append(",\n");
            sb.append("      \"globalNoteIndex\": ").append(n.globalNoteIndex).append(",\n");
            sb.append("      \"partId\": ").append(jsonString(n.partId)).append(",\n");
            sb.append("      \"measureNumber\": ").append(jsonString(n.measureNumber)).append(",\n");
            sb.append("      \"staff\": ").append(n.staff).append(",\n");
            sb.append("      \"voice\": ").append(jsonString(n.voice)).append(",\n");
            sb.append("      \"noteIndexInChord\": ").append(n.noteIndexInChord).append(",\n");
            sb.append("      \"sheetNumber\": ").append(n.sheetNumber).append(",\n");
            sb.append("      \"systemIndex\": ").append(n.systemIndex).append(",\n");
            sb.append("      \"isRest\": ").append(n.isRest).append(",\n");
            sb.append("      \"isGrace\": ").append(n.isGrace).append(",\n");
            sb.append("      \"isMeasureRest\": ").append(n.isMeasureRest).append(",\n");
            sb.append("      \"isTiedStart\": ").append(n.isTiedStart).append(",\n");
            sb.append("      \"isTiedStop\": ").append(n.isTiedStop).append(",\n");
            sb.append("      \"step\": ").append(jsonString(n.step)).append(",\n");
            sb.append("      \"octave\": ").append(n.octave).append(",\n");
            sb.append("      \"alter\": ").append(n.alter).append(",\n");
            sb.append("      \"absolutePitch\": ").append(n.absolutePitch).append(",\n");
            sb.append("      \"integerPitch\": ").append(n.integerPitch).append(",\n");
            sb.append("      \"expectedFrequency\": ").append(n.expectedFrequency).append(",\n");
            sb.append("      \"noteType\": ").append(jsonString(n.noteType)).append(",\n");
            sb.append("      \"dots\": ").append(n.dots).append(",\n");
            sb.append("      \"stemDirection\": ").append(n.stemDirection).append(",\n");
            sb.append("      \"beamGroupId\": ").append(n.beamGroupId != null ? n.beamGroupId : "null").append(",\n");
            sb.append("      \"timeOffset\": ").append(n.timeOffset).append(",\n");
            sb.append("      \"duration\": ").append(n.duration).append(",\n");
            sb.append("      \"measureCumulativeTimeOffset\": ")
                    .append(n.measureCumulativeTimeOffset)
                    .append(",\n");
            sb.append("      \"timeOffsetSeconds\": ").append(n.timeOffsetSeconds).append(",\n");
            sb.append("      \"durationSeconds\": ").append(n.durationSeconds).append(",\n");
            sb.append("      \"tiedDuration\": ").append(n.tiedDuration).append(",\n");
            sb.append("      \"tiedDurationSeconds\": ").append(n.tiedDurationSeconds).append(",\n");
            sb.append("      \"bounds\": ").append(boundsToJson(n.bounds)).append(",\n");
            sb.append("      \"center\": ").append(pointToJson(n.center)).append(",\n");
            sb.append("      \"chordBounds\": ").append(boundsToJson(n.chordBounds)).append(",\n");
            sb.append("      \"staffTopY\": ").append(n.staffTopY).append(",\n");
            sb.append("      \"staffBottomY\": ").append(n.staffBottomY).append("\n");
            sb.append("    }");
            if (i < notes.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        
        sb.append("}");
        return sb.toString();
    }
    
    private String jsonString (String s)
    {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
    
    private String boundsToJson (BoundsInfo b)
    {
        return String.format("{\"x\": %d, \"y\": %d, \"width\": %d, \"height\": %d}", 
                             b.x, b.y, b.width, b.height);
    }
    
    private String pointToJson (PointInfo p)
    {
        return String.format("{\"x\": %d, \"y\": %d}", p.x, p.y);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    /**
     * Sheet information.
     */
    public static class SheetInfo
    {
        public final int sheetNumber;
        public final int imageWidth;
        public final int imageHeight;

        public SheetInfo (int sheetNumber, int imageWidth, int imageHeight)
        {
            this.sheetNumber = sheetNumber;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }

    /**
     * System information.
     */
    public static class SystemInfo
    {
        public final int systemIndex;
        public final int sheetNumber;
        public final BoundsInfo bounds;

        public SystemInfo (int systemIndex, int sheetNumber, Rectangle bounds)
        {
            this.systemIndex = systemIndex;
            this.sheetNumber = sheetNumber;
            this.bounds = new BoundsInfo(bounds);
        }
    }

    /**
     * Staff information within a measure.
     */
    public static class StaffInfo
    {
        public final int staffIndex;
        public final int topY;
        public final int bottomY;

        public StaffInfo (int staffIndex, int topY, int bottomY)
        {
            this.staffIndex = staffIndex;
            this.topY = topY;
            this.bottomY = bottomY;
        }
    }

    /**
     * Measure information.
     */
    public static class MeasureInfo
    {
        public final String partId;
        public final String measureNumber;
        public final int sheetNumber;
        public final int systemIndex;
        public final int cumulativeTimeOffset;
        public final double cumulativeTimeSeconds;
        public final int measureDuration;
        public final double measureDurationSeconds;
        public final BoundsInfo bounds;
        public final List<StaffInfo> staves;

        public MeasureInfo (String partId, String measureNumber, int sheetNumber, int systemIndex,
                           int cumulativeTimeOffset, double cumulativeTimeSeconds,
                           int measureDuration, double measureDurationSeconds,
                           Rectangle bounds, List<StaffInfo> staves)
        {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.sheetNumber = sheetNumber;
            this.systemIndex = systemIndex;
            this.cumulativeTimeOffset = cumulativeTimeOffset;
            this.cumulativeTimeSeconds = cumulativeTimeSeconds;
            this.measureDuration = measureDuration;
            this.measureDurationSeconds = measureDurationSeconds;
            this.bounds = new BoundsInfo(bounds);
            this.staves = staves;
        }
    }

    /**
     * Tempo information.
     */
    public static class TempoInfo
    {
        public final String partId;
        public final String measureNumber;
        public final int timeOffset;
        public final double bpm;
        public final String beatUnit;

        public TempoInfo (String partId, String measureNumber, int timeOffset, double bpm,
                         String beatUnit)
        {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.timeOffset = timeOffset;
            this.bpm = bpm;
            this.beatUnit = beatUnit;
        }
    }

    /**
     * Time signature information.
     */
    public static class TimeSignatureInfo
    {
        public final String partId;
        public final String measureNumber;
        public final int numerator;
        public final int denominator;

        public TimeSignatureInfo (String partId, String measureNumber, int numerator,
                                 int denominator)
        {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

    /**
     * Key signature information.
     */
    public static class KeySignatureInfo
    {
        public final String partId;
        public final String measureNumber;
        public final int fifths;
        public final String mode;

        public KeySignatureInfo (String partId, String measureNumber, int fifths, String mode)
        {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.fifths = fifths;
            this.mode = mode != null ? mode : "major";
        }
    }

    /**
     * Bounding box information.
     */
    public static class BoundsInfo
    {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public BoundsInfo (Rectangle rect)
        {
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
        }

        public BoundsInfo (int x, int y, int width, int height)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Point information.
     */
    public static class PointInfo
    {
        public final int x;
        public final int y;

        public PointInfo (Point point)
        {
            this.x = point.x;
            this.y = point.y;
        }

        public PointInfo (int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Note entry with all required fields for mapping.
     */
    public static class NoteEntry
    {
        // Positioning
        public final int noteIndex;
        public final int globalNoteIndex;
        public final String partId;
        public final String measureNumber;
        public final int staff;
        public final String voice;
        public final int noteIndexInChord;
        public final int sheetNumber;
        public final int systemIndex;

        // Note properties
        public final boolean isRest;
        public final boolean isGrace;
        public final boolean isMeasureRest;
        public final boolean isTiedStart;
        public final boolean isTiedStop;

        // Pitch
        public final String step;
        public final int octave;
        public final int alter;
        public final int absolutePitch;
        public final int integerPitch;
        public final double expectedFrequency;

        // Duration/Type
        public final String noteType;
        public final int dots;
        public final int stemDirection;
        public final Integer beamGroupId;

        // Time
        public final int timeOffset;
        public final int duration;
        public final int measureCumulativeTimeOffset;
        public final double timeOffsetSeconds;
        public final double durationSeconds;
        public final int tiedDuration;
        public final double tiedDurationSeconds;

        // Geometry
        public final BoundsInfo bounds;
        public final PointInfo center;
        public final BoundsInfo chordBounds;
        public final int staffTopY;
        public final int staffBottomY;

        public NoteEntry (int noteIndex, int globalNoteIndex, String partId, String measureNumber,
                         int staff, String voice, int noteIndexInChord, int sheetNumber,
                         int systemIndex, boolean isRest, boolean isGrace, boolean isMeasureRest,
                         boolean isTiedStart, boolean isTiedStop, String step, int octave,
                         int alter, int absolutePitch, int integerPitch, double expectedFrequency,
                         String noteType, int dots, int stemDirection, Integer beamGroupId,
                         int timeOffset, int duration, int measureCumulativeTimeOffset,
                         double timeOffsetSeconds, double durationSeconds, int tiedDuration,
                         double tiedDurationSeconds, Rectangle bounds, Point center,
                         Rectangle chordBounds, int staffTopY, int staffBottomY)
        {
            this.noteIndex = noteIndex;
            this.globalNoteIndex = globalNoteIndex;
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.staff = staff;
            this.voice = voice;
            this.noteIndexInChord = noteIndexInChord;
            this.sheetNumber = sheetNumber;
            this.systemIndex = systemIndex;
            this.isRest = isRest;
            this.isGrace = isGrace;
            this.isMeasureRest = isMeasureRest;
            this.isTiedStart = isTiedStart;
            this.isTiedStop = isTiedStop;
            this.step = step;
            this.octave = octave;
            this.alter = alter;
            this.absolutePitch = absolutePitch;
            this.integerPitch = integerPitch;
            this.expectedFrequency = expectedFrequency;
            this.noteType = noteType;
            this.dots = dots;
            this.stemDirection = stemDirection;
            this.beamGroupId = beamGroupId;
            this.timeOffset = timeOffset;
            this.duration = duration;
            this.measureCumulativeTimeOffset = measureCumulativeTimeOffset;
            this.timeOffsetSeconds = timeOffsetSeconds;
            this.durationSeconds = durationSeconds;
            this.tiedDuration = tiedDuration;
            this.tiedDurationSeconds = tiedDurationSeconds;
            this.bounds = new BoundsInfo(bounds);
            this.center = new PointInfo(center);
            this.chordBounds = new BoundsInfo(chordBounds);
            this.staffTopY = staffTopY;
            this.staffBottomY = staffBottomY;
        }
    }
}
