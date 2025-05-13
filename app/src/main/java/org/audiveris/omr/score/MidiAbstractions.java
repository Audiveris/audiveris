//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                M i d i A b s t r a c t i o n s                                 //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Class <code>MidiAbstractions</code> handles various MIDI abstractions.
 *
 * @author Hervé Bitteur
 */
public abstract class MidiAbstractions
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MidiAbstractions.class);

    /** The extension used for Midi output files: {@value} */
    public static final String MIDI_EXTENSION = ".mid";

    /** Definition of all Midi instruments in proper order. */
    private static final String[] instrumentNames = { //
            "Acoustic Grand Piano", // 1
            "Bright Acoustic Piano", // 2
            "Electric Grand Piano", // 3
            "Honky-tonk Piano", // 4
            "Electric Piano 1", // 5
            "Electric Piano 2", // 6
            "Harpsichord", // 7
            "Clavi", // 8
            "Celesta", // 9
            "Glockenspiel", // 10
            "Music Box", // 11
            "Vibraphone", // 12
            "Marimba", // 13
            "Xylophone", // 14
            "Tubular Bells", // 15
            "Dulcimer", // 16
            "Drawbar Organ", // 17
            "Percussive Organ", // 18
            "Rock Organ", // 19
            "Church Organ", // 20
            "Reed Organ", // 21
            "Accordion", // 22
            "Harmonica", // 23
            "Tango Accordion", // 24
            "Acoustic Guitar (nylon)", // 25
            "Acoustic Guitar (steel)", // 26
            "Electric Guitar (jazz)", // 27
            "Electric Guitar (clean)", // 28
            "Electric Guitar (muted)", // 29
            "Overdriven Guitar", // 30
            "Distortion Guitar", // 31
            "Guitar harmonics", // 32
            "Acoustic Bass", // 33
            "Electric Bass (finger)", // 34
            "Electric Bass (pick)", // 35
            "Fretless Bass", // 36
            "Slap Bass 1", // 37
            "Slap Bass 2", // 38
            "Synth Bass 1", // 39
            "Synth Bass 2", // 40
            "Violin", // 41
            "Viola", // 42
            "Cello", // 43
            "Contrabass", // 44
            "Tremolo Strings", // 45
            "Pizzicato Strings", // 46
            "Orchestral Harp", // 47
            "Timpani", // 48
            "String Ensemble 1", // 49
            "String Ensemble 2", // 50
            "SynthStrings 1", // 51
            "SynthStrings 2", // 52
            "Choir Aahs", // 53
            "Voice Oohs", // 54
            "Synth Voice", // 55
            "Orchestra Hit", // 56
            "Trumpet", // 57
            "Trombone", // 58
            "Tuba", // 59
            "Muted Trumpet", // 60
            "French Horn", // 61
            "Brass Section", // 62
            "SynthBrass 1", // 63
            "SynthBrass 2", // 64
            "Soprano Sax", // 65
            "Alto Sax", // 66
            "Tenor Sax", // 67
            "Baritone Sax", // 68
            "Oboe", // 69
            "English Horn", // 70
            "Bassoon", // 71
            "Clarinet", // 72
            "Piccolo", // 73
            "Flute", // 74
            "Recorder", // 75
            "Pan Flute", // 76
            "Blown Bottle", // 77
            "Shakuhachi", // 78
            "Whistle", // 79
            "Ocarina", // 80
            "Lead 1 (square)", // 81
            "Lead 2 (sawtooth)", // 82
            "Lead 3 (calliope)", // 83
            "Lead 4 (chiff)", // 84
            "Lead 5 (charang)", // 85
            "Lead 6 (voice)", // 86
            "Lead 7 (fifths)", // 87
            "Lead 8 (bass + lead)", // 88
            "Pad 1 (new age)", // 89
            "Pad 2 (warm)", // 90
            "Pad 3 (polysynth)", // 91
            "Pad 4 (choir)", // 92
            "Pad 5 (bowed)", // 93
            "Pad 6 (metallic)", // 94
            "Pad 7 (halo)", // 95
            "Pad 8 (sweep)", // 96
            "FX 1 (rain)", // 97
            "FX 2 (soundtrack)", // 98
            "FX 3 (crystal)", // 99
            "FX 4 (atmosphere)", // 100
            "FX 5 (brightness)", // 101
            "FX 6 (goblins)", // 102
            "FX 7 (echoes)", // 103
            "FX 8 (sci-fi)", // 104
            "Sitar", // 105
            "Banjo", // 106
            "Shamisen", // 107
            "Koto", // 108
            "Kalimba", // 109
            "Bag pipe", // 110
            "Fiddle", // 111
            "Shanai", // 112
            "Tinkle Bell", // 113
            "Agogo", // 114
            "Steel Drums", // 115
            "Woodblock", // 116
            "Taiko Drum", // 117
            "Melodic Tom", // 118
            "Synth Drum", // 119
            "Reverse Cymbal", // 120
            "Guitar Fret Noise", // 121
            "Breath Noise", // 122
            "Seashore", // 123
            "Bird Tweet", // 124
            "Telephone Ring", // 125
            "Helicopter", // 126
            "Applause", // 127
            "Gunshot" }; // 128

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Not meant to be instantiated.
     */
    private MidiAbstractions ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // getAnnotatedNames //
    //-------------------//
    /**
     * Report the ordered table of midi instruments, each name being led by the program id.
     *
     * @return an annotated table of Midi instruments
     */
    public static String[] getAnnotatedNames ()
    {
        final String[] annotated = new String[instrumentNames.length];

        for (int i = 0; i < annotated.length; i++) {
            annotated[i] = (i + 1) + " / " + instrumentNames[i];
        }

        return annotated;
    }

    //--------------//
    // getProgramId //
    //--------------//
    /**
     * Report the program ID that corresponds to the provided annotation.
     *
     * @param annotation a String like "58 / Trombone"
     * @return the id, like 58. Null if annotation is invalid
     */
    public static Integer getProgramId (String annotation)
    {
        final int slash = annotation.indexOf('/');

        if (slash == -1) {
            logger.warn("Illegal Midi annotation: \"{}\"", annotation);
        } else {
            final String idString = annotation.substring(0, slash).trim();
            try {
                final int id = Integer.decode(idString);
                if (id < 1 || id > instrumentNames.length) {
                    logger.warn("Illegal Midi id: {}", id);
                } else {
                    return id;
                }
            } catch (NumberFormatException ex) {
                logger.warn("Illegal Midi id: '{}'", idString);
            }
        }

        return null;
    }

    //----------------//
    // getProgramName //
    //----------------//
    /**
     * Report the instrument name for a given program id
     *
     * @param id the given program id, counted from 1
     * @return the corresponding instrument name
     */
    public static String getProgramName (int id)
    {
        try {
            return instrumentNames[id - 1];
        } catch (Exception ex) {
            logger.error("Illegal MIDI program id: " + id, ex);

            return "";
        }
    }

    //-----------------//
    // getProgramNames //
    //-----------------//
    /**
     * Report the ordered table of midi instruments
     *
     * @return (a copy of) the table of Midi instruments
     */
    public static String[] getProgramNames ()
    {
        return Arrays.copyOf(instrumentNames, instrumentNames.length);
    }
}
