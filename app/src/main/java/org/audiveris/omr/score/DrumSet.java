//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          D r u m S e t                                         //
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

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet.HeadMotif;
import org.audiveris.omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>DrumSet</code> defines for MIDI channel 10 the mapping between MIDI id and:
 * <ul>
 * <li>the pitch position of note head with respect to staff,
 * <li>the motif of note head shape,
 * <li>the playing technique sign if any,
 * <li>the percussion instrument sound name.
 * </ul>
 *
 * @author Brian Boe
 * @author Hervé Bitteur
 */
public class DrumSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DrumSet.class);

    /** File name for drum-set definitions. */
    private static final String fileName = "drum-set.xml";

    /** Context for JAXB unmarshalling. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Instruments structure.
     * StaffSize => Pitch => Motif+Sign => Instrument
     */
    private final Map<Integer, Map<Integer, Map<MotifSign, DrumInstrument>>> structure =
            new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    private DrumSet ()
    {
        loadAllConfigurations();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------------//
    // dumpResultingDrumSet //
    //----------------------//
    /**
     * Print out the DrumSet that results from loading of system (and possibly user)
     * configuration(s).
     */
    public void dumpResultingDrumSet ()
    {
        logger.info("");
        logger.info("Resulting Drum Set:");

        for (Entry<Integer, Map<Integer, Map<MotifSign, DrumInstrument>>> staffEntry : structure
                .entrySet()) {
            logger.info("");
            logger.info("  line-count: {}", staffEntry.getKey());
            final Map<Integer, Map<MotifSign, DrumInstrument>> byPitch = staffEntry.getValue();

            for (Entry<Integer, Map<MotifSign, DrumInstrument>> entry : byPitch.entrySet()) {
                logger.info("    pitch-position: {}", entry.getKey());
                final Map<MotifSign, DrumInstrument> msMap = entry.getValue();

                if (msMap != null) {
                    for (Entry<MotifSign, DrumInstrument> msEntry : msMap.entrySet()) {
                        final MotifSign ms = msEntry.getKey();
                        final DrumInstrument inst = msEntry.getValue();
                        logger.info(
                                "      motif: {}{} sound: {}",
                                String.format("%-8s", ms.motif),
                                ms.sign != null ? String.format(" sign: %-17s", ms.sign) : "",
                                inst != null ? inst.sound : "NULL");
                    }
                }
            }
        }

        logger.info("");
    }

    //-------------//
    // getStaffSet //
    //-------------//
    /**
     * Report the drum set that applies for a percussion staff of provided line count.
     *
     * @param lineCount count of lines (1 or 5) in percussion staff
     * @return the byPitch map for this staff size
     */
    public Map<Integer, Map<MotifSign, DrumInstrument>> getStaffSet (int lineCount)
    {
        return structure.get(lineCount);
    }

    //-----------------------//
    // loadAllConfigurations //
    //-----------------------//
    /**
     * Load all drum-set files as found in system (and user?) configuration files.
     */
    private void loadAllConfigurations ()
    {
        // First load system drum-set which must exist
        // Second load user drum-set if any
        final URI[] uris = new URI[]
        {
                UriUtil.toURI(WellKnowns.RES_URI, fileName),
                WellKnowns.CONFIG_FOLDER.resolve(fileName).toUri().normalize() };

        for (int i = 0; i < uris.length; i++) {
            final URI uri = uris[i];

            try {
                final URL url = uri.toURL();
                try (InputStream input = url.openStream()) {
                    logger.info("Loading drum set entries from {}", uri);
                    loadConfiguration(input);
                }
            } catch (IOException ex) {
                // Item does not exist
                if (i == 0) {
                    // Only the first item (system) is mandatory
                    logger.error("Mandatory file not found {}", uri);
                }
            } catch (JAXBException ex) {
                logger.warn("Error loading drum set from {}", uri, ex);
            }
        }

        if (constants.dumpDrumSet.isSet()) {
            dumpResultingDrumSet();
        }
    }

    //-------------------//
    // loadConfiguration //
    //------------------//
    /**
     * Load Drum Set configuration from the provided input stream.
     *
     * @param in input stream
     * @throws JAXBException
     */
    private void loadConfiguration (InputStream in)
        throws JAXBException
    {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(DrumSetEntries.class);
        }

        final Unmarshaller um = jaxbContext.createUnmarshaller();
        final DrumSetEntries entries = (DrumSetEntries) um.unmarshal(in);

        for (StaffEntries staffEntries : entries.staffList) {
            final Integer lineCount = staffEntries.lineCount;
            if (lineCount == null) {
                logger.error("For a staff, line-count cannot be null ");
                continue;
            }

            Map<Integer, Map<MotifSign, DrumInstrument>> byPitch = structure.get(lineCount);
            if (byPitch == null) {
                structure.put(lineCount, byPitch = new TreeMap<>());
            }

            for (DrumSetEntry entry : staffEntries.list) {
                // Populate/update byPitch map
                final Integer pp = entry.pitchPosition;

                if (entry.sound == null) {
                    // This entry is a removal
                    if (entry.headMotif == null) {
                        logger.error("{} For a removal, motif cannot be null ", entry);
                        continue;
                    }

                    if (pp == null) {
                        logger.error("{} For a removal, pitch-position cannot be null ", entry);
                        continue;
                    }

                    final Map<MotifSign, DrumInstrument> msMap = byPitch.get(pp);
                    if (msMap != null) {
                        final MotifSign ms = new MotifSign(entry.headMotif, entry.sign);
                        final DrumInstrument inst = msMap.get(ms);
                        if (inst != null) {
                            logger.info("  at pitch-position: {} removing {}", pp, inst);
                            msMap.put(ms, null);
                        }
                    }
                } else {
                    // This entry is an addition or a replacement
                    if (pp == null) {
                        logger.debug("  null pitch-position in {}. Entry skipped.", entry);
                        continue;
                    }

                    if (entry.headMotif == null) {
                        logger.warn("  at pitch-position: {} null motif {}", pp, entry);
                        continue;
                    }

                    Map<MotifSign, DrumInstrument> msMap = byPitch.get(pp);
                    if (msMap == null) {
                        byPitch.put(pp, msMap = new LinkedHashMap<>());
                    }

                    final MotifSign ms = new MotifSign(entry.headMotif, entry.sign);
                    final DrumInstrument inst = new DrumInstrument(
                            entry.headMotif,
                            entry.sign,
                            entry.sound);
                    final DrumInstrument old = msMap.put(ms, inst);

                    if (old == null) {
                        logger.debug("  at pitch-position: {} adding {}", pp, inst);
                    } else {
                        logger.info("  at pitch-position: {} replacing {} by {}", pp, old, inst);
                    }
                }
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of DrumSet class in Audiveris application.
     *
     * @return the DrumSet instance
     */
    public static DrumSet getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean dumpDrumSet = new Constant.Boolean(
                true,
                "Should we dump the resulting drum set?");
    }

    //----------------//
    // DrumInstrument //
    //----------------//
    /**
     * Class representing an individual percussion instrument.
     */
    public static class DrumInstrument
    {
        /** Note head motif. */
        public final HeadMotif headMotif;

        /** Playing technique sign, if any. */
        public final Shape sign;

        /** Instrument sound name. */
        public final DrumSound sound;

        public DrumInstrument (HeadMotif headMotif,
                               Shape sign,
                               DrumSound sound)
        {
            this.headMotif = headMotif;
            this.sign = sign;
            this.sound = sound;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (obj instanceof DrumInstrument that) {
                return this.headMotif == that.headMotif //
                        && this.sign == that.sign //
                        && this.sound == that.sound;
            }

            return false;
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.headMotif);
            hash = 29 * hash + Objects.hashCode(this.sign);
            hash = 29 * hash + Objects.hashCode(this.sound);
            return hash;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder(getClass().getSimpleName()).append('{') //
                    .append("motif: ").append(headMotif) //
                    .append(" sign: ").append(sign) //
                    .append(" sound: ").append(sound) //
                    .append('}').toString();
        }
    }

    //----------------//
    // DrumSetEntries //
    //----------------//
    /**
     * Flat list of entries per staff size, to be unmarshalled from drum-set file.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "drum-set")
    private static class DrumSetEntries
    {
        @XmlElement(name = "staff")
        private final List<StaffEntries> staffList = new ArrayList<>();
    }

    //--------------//
    // DrumSetEntry //
    //--------------//
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "entry")
    private static class DrumSetEntry
    {
        /**
         * Pitch position.
         * Staff pitch, 0 = middle line, increasing downwards.
         */
        @XmlAttribute(name = "pitch-position")
        @XmlJavaTypeAdapter(PitchAdapter.class)
        public Integer pitchPosition;

        /**
         * Note head motif.
         */
        @XmlAttribute(name = "motif")
        @XmlJavaTypeAdapter(MotifAdapter.class)
        public HeadMotif headMotif;

        /**
         * Playing technique sign , if any.
         */
        @XmlAttribute(name = "sign")
        public Shape sign;

        /**
         * Instrument sound name.
         * A null value removes this entry from drum set.
         */
        @XmlAttribute(name = "sound")
        @XmlJavaTypeAdapter(SoundAdapter.class)
        public DrumSound sound;

        @Override
        public String toString ()
        {
            return new StringBuilder(getClass().getSimpleName()).append('{') //
                    .append("pitch-position: ").append(pitchPosition) //
                    .append(" motif: ").append(headMotif) //
                    .append(" sign: ").append(sign) //
                    .append(" sound: ").append(sound) //
                    .append('}').toString();
        }
    }

    //-----------//
    // DrumSound //
    //-----------//
    /**
     * Enum <code>DrumSound</code> provides all drum sound names together with their MIDI value.
     * <p>
     * See https://computermusicresource.com/GM.Percussion.KeyMap.html for a complete mapping
     * of MIDI id vs sound name.
     */
    public static enum DrumSound
    {
        Acoustic_Bass_Drum(35),
        Bass_Drum_1(36),
        Side_Stick(37),
        Acoustic_Snare(38),
        Hand_Clap(39),
        Electric_Snare(40),
        Low_Floor_Tom(41),
        Closed_Hi_Hat(42),
        High_Floor_Tom(43),
        Pedal_Hi_Hat(44),
        Low_Tom(45),
        Open_Hi_Hat(46),
        Low_Mid_Tom(47),
        Hi_Mid_Tom(48),
        Crash_Cymbal_1(49),
        High_Tom(50),
        Ride_Cymbal_1(51),
        Chinese_Cymbal(52),
        Ride_Bell(53),
        Tambourine(54),
        Splash_Cymbal(55),
        Cowbell(56),
        Crash_Cymbal_2(57),
        Vibraslap(58),
        Ride_Cymbal_2(59),
        Hi_Bongo(60),
        Low_Bongo(61),
        Mute_Hi_Conga(62),
        Open_Hi_Conga(63),
        Low_Conga(64),
        High_Timbale(65),
        Low_Timbale(66),
        High_Agogo(67),
        Low_Agogo(68),
        Cabasa(69),
        Maracas(70),
        Short_Whistle(71),
        Long_Whistle(72),
        Short_Guiro(73),
        Long_Guiro(74),
        Claves(75),
        Hi_Wood_Block(76),
        Low_Wood_Block(77),
        Mute_Cuica(78),
        Open_Cuica(79),
        Mute_Triangle(80),
        Open_Triangle(81);

        private final int midi;

        private DrumSound (int midi)
        {
            this.midi = midi;
        }

        public int getMidi ()
        {
            return midi;
        }
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final DrumSet INSTANCE = new DrumSet();

        private LazySingleton ()
        {
        }
    }

    //--------------//
    // MotifAdapter //
    //--------------//
    /**
     * Class needed to handle the case of a non recognized head motif name.
     */
    private static class MotifAdapter
            extends XmlAdapter<String, HeadMotif>
    {
        @Override
        public String marshal (HeadMotif motif)
            throws Exception
        {
            return (motif == null) ? null : motif.name();
        }

        @Override
        public HeadMotif unmarshal (String str)
            throws Exception
        {
            try {
                if (str == null || str.equals("null")) {
                    return null;
                } else {
                    return HeadMotif.valueOf(str);
                }
            } catch (Exception ex) {
                logger.warn("Unknown head motif: {}", str, ex);
                return null;
            }
        }
    }

    //-----------//
    // MotifSign //
    //-----------//
    /**
     * Class used as the (motif + sign) key for instrument at a given pitch-position.
     */
    public static class MotifSign
    {
        public final HeadMotif motif;

        public final Shape sign;

        public MotifSign (HeadMotif motif,
                          Shape sign)
        {
            this.motif = motif;
            this.sign = sign;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (obj instanceof MotifSign that) {
                return this.motif == that.motif //
                        && this.sign == that.sign;
            }

            return false;
        }

        @Override
        public int hashCode ()
        {
            int hash = 3;
            hash = 13 * hash + Objects.hashCode(this.motif);
            hash = 13 * hash + Objects.hashCode(this.sign);
            return hash;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder() //
                    .append("motif:").append(motif)//
                    .append(" sign:").append(sign)//
                    .toString();
        }
    }

    //--------------//
    // PitchAdapter //
    //--------------//
    /**
     * Class needed to handle the case of a null pitch-position attribute.
     */
    private static class PitchAdapter
            extends XmlAdapter<String, Integer>
    {
        @Override
        public String marshal (Integer i)
            throws Exception
        {
            // For the sake of completion
            return (i == null) ? "null" : Integer.toString(i);
        }

        @Override
        public Integer unmarshal (String s)
            throws Exception
        {
            return ((s == null) || s.equalsIgnoreCase("null")) ? null : Integer.valueOf(s);
        }
    }

    //--------------//
    // StaffEntries //
    //--------------//
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "staff")
    private static class StaffEntries
    {
        @XmlAttribute(name = "line-count")
        public Integer lineCount;

        @XmlElement(name = "entry")
        public final List<DrumSetEntry> list = new ArrayList<>();
    }

    //--------------//
    // SoundAdapter //
    //--------------//
    /**
     * Class needed to handle the case of a non recognized sound name.
     */
    private static class SoundAdapter
            extends XmlAdapter<String, DrumSound>
    {
        @Override
        public String marshal (DrumSound sound)
            throws Exception
        {
            return (sound == null) ? null : sound.name();
        }

        @Override
        public DrumSound unmarshal (String str)
            throws Exception
        {
            try {
                if (str == null || str.equals("null")) {
                    return null;
                } else {
                    return DrumSound.valueOf(str);
                }
            } catch (Exception ex) {
                logger.warn("Unknown drum sound: {}", str, ex);
                return null;
            }
        }
    }
}
