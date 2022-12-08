//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          D r u m S e t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.glyph.ShapeSet.HeadMotif;
import org.audiveris.omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
 * <li>the percussion instrument sound name,
 * <li>the motif of note head shape,
 * <li>the pitch position of note head with respect to staff.
 * </ul>
 *
 * @author Brian Boe
 * @author Hervé Bitteur
 */
public class DrumSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DrumSet.class);

    /** File name for drum-set definitions. */
    private static final String fileName = "drum-set.xml";

    /** Context for JAXB unmarshalling. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Instruments indexed by MIDI id.
     */
    public final Map<Integer, DrumInstrument> byId = new TreeMap<>();

    /**
     * Instruments indexed by pitch position.
     */
    public final Map<Integer, Set<DrumInstrument>> byPitch = new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------
    private DrumSet ()
    {
        loadAllEntries();
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //----------------//
    // loadAllEntries //
    //----------------//
    /**
     * Load all drum-set files as found in system and user configuration files.
     */
    private void loadAllEntries ()
    {
        // First load system drum-set, then load user drum-set if any
        final URI[] uris = new URI[]{
            UriUtil.toURI(WellKnowns.RES_URI, fileName),
            WellKnowns.CONFIG_FOLDER.resolve(fileName).toUri().normalize()};

        for (int i = 0; i < uris.length; i++) {
            final URI uri = uris[i];

            try {
                final URL url = uri.toURL();
                try (InputStream input = url.openStream()) {
                    logger.info("Loading drum set entries from {}", uri);
                    loadEntries(input);
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
    }

    //-------------//
    // loadEntries //
    //-------------//
    private void loadEntries (InputStream in)
            throws JAXBException
    {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(DrumSetEntries.class);
        }

        final Unmarshaller um = jaxbContext.createUnmarshaller();
        final DrumSetEntries entries = (DrumSetEntries) um.unmarshal(in);

        for (DrumInstrument inst : entries.instrumentList) {
            // Populate/update byId
            // Remove existing instrument if any
            final DrumInstrument existing = byId.get(inst.id);
            if (existing != null) {
                logger.debug("    byId removing {}", existing);
            }

            logger.debug("    byId adding {}", inst);
            byId.put(inst.id, inst);

            // Populate/update byPitch
            // Remove existing instrument if any
            final String name = inst.name;
            for (Entry<Integer, Set<DrumInstrument>> entry : byPitch.entrySet()) {
                final Set<DrumInstrument> set = entry.getValue();
                if (set != null) {
                    for (DrumInstrument i : set) {
                        if (i.name.equalsIgnoreCase(name)) {
                            logger.debug("    byPitch removing {}", inst);
                            set.remove(i);
                            break;
                        }
                    }
                }
            }

            if (inst.pitchPosition != null) {
                // Include
                Set<DrumInstrument> set = byPitch.get(inst.pitchPosition);
                if (set == null) {
                    byPitch.put(inst.pitchPosition, set = new LinkedHashSet<>());
                }

                logger.debug("    byPitch adding {}", inst);
                set.add(inst);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // DrumInstrument //
    //----------------//
    /**
     * Class representing an individual percussion instrument.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "instrument")
    public static class DrumInstrument
    {

        /** MIDI key. */
        @XmlAttribute(name = "id")
        public final int id;

        /** Instrument sound name. */
        @XmlAttribute(name = "name")
        public final String name;

        /** Note head motif. Can be null. */
        @XmlAttribute(name = "motif")
        public final HeadMotif headMotif;

        /** Staff pitch, 0 = middle line, increasing downwards. Can be null. */
        @XmlAttribute(name = "pitch-position")
        @XmlJavaTypeAdapter(PitchAdapter.class)
        public final Integer pitchPosition;

        public DrumInstrument (int id,
                               String name,
                               HeadMotif headMotif,
                               Integer pitchPosition)
        {
            this.id = id;
            this.name = name;
            this.headMotif = headMotif;
            this.pitchPosition = pitchPosition;
        }

        // No-arg constructor needed by JAXB
        private DrumInstrument ()
        {
            this(0, null, null, 0);
        }

        @Override
        public String toString ()
        {
            return new StringBuilder(getClass().getSimpleName())
                    .append('{')
                    .append("id:").append(id)
                    .append(" name:").append('"').append(name).append('"')
                    .append(" motif:").append(headMotif)
                    .append(" pitch:").append(pitchPosition)
                    .append('}')
                    .toString();
        }
    }

    //----------------//
    // DrumSetEntries //
    //----------------//
    /**
     * Flat list of entries, to be unmarshalled from drum-set file.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "drum-set")
    private static class DrumSetEntries
    {

        @XmlElement(name = "instrument")
        private final List<DrumInstrument> instrumentList = new ArrayList<>();
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {

        static final DrumSet INSTANCE = new DrumSet();
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
}
