//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                P r o c e s s i n g S w i t c h e s                             //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;
import static org.audiveris.omr.util.param.Param.GLOBAL_SCOPE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>ProcessingSwitches</code> handles the set of named processing switches.
 *
 * @author Hervé Bitteur
 */
public class ProcessingSwitches
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ProcessingSwitches.class);

    static final Constants constants = new Constants();

    /** Default switches values (the ones for the global scope). */
    private static volatile ProcessingSwitches defaultSwitches;

    //~ Instance fields ----------------------------------------------------------------------------

    /** Map of switches parameters. */
    protected final EnumMap<ProcessingSwitch, Param<Boolean>> map = new EnumMap<>(
            ProcessingSwitch.class);

    //~ Constructors -------------------------------------------------------------------------------

    // Meant for JAXB
    protected ProcessingSwitches ()
    {
    }

    /**
     * Create a <code>ProcessingSwitches</code> object with its parent.
     *
     * @param parent parent switches
     * @param scope  owning scope of these switches
     */
    public ProcessingSwitches (ProcessingSwitches parent,
                               Object scope)
    {
        for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
            map.put(key, new Param<Boolean>(null));
        }

        if (scope != null) {
            setScope(scope);
        }

        if (parent != null) {
            setParent(parent);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the parameter for the provided key.
     *
     * @param key provided key
     * @return related parameter
     */
    public Param<Boolean> getParam (ProcessingSwitch key)
    {
        return map.get(key);
    }

    /**
     * Report the current value for the provided key.
     *
     * @param key provided key
     * @return current value
     */
    public Boolean getValue (ProcessingSwitch key)
    {
        return getParam(key).getValue();
    }

    /**
     * Report whether this object provides no specific information.
     *
     * @return true if empty
     */
    public boolean isEmpty ()
    {
        for (Entry<ProcessingSwitch, Param<Boolean>> entry : map.entrySet()) {
            if (entry.getValue().isSpecific()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Assign parent for each existing switch.
     *
     * @param parent the parent set of switches
     */
    public final void setParent (ProcessingSwitches parent)
    {
        for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
            final Param<Boolean> param = getParam(key);

            if (param != null) {
                param.setParent(parent.getParam(key));
            }
        }
    }

    /**
     * Assign scope for each existing switch.
     *
     * @param scope the scope to set
     */
    public final void setScope (Object scope)
    {
        for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
            final Param<Boolean> param = getParam(key);

            if (param != null) {
                param.setScope(scope);
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Report the top level switches, which provide default values.
     *
     * @return top default switches.
     */
    public static ProcessingSwitches getDefaultSwitches ()
    {
        // Workaround for elaboration circularity
        if (defaultSwitches == null) {
            constants.initialize();
            defaultSwitches = new DefaultSwitches();
        }

        return defaultSwitches;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    static class Constants
            extends ConstantSet
    {
        final Constant.Boolean keepGrayImages = new Constant.Boolean(
                false,
                "Keep loaded gray images");

        final Constant.Boolean indentations = new Constant.Boolean(
                true,
                "Use of system indentation");

        final Constant.Boolean bothSharedHeadDots = new Constant.Boolean(
                false,
                "Link augmentation dot to both shared heads");

        final Constant.Boolean oneLineStaves = new Constant.Boolean(
                false,
                "1-line percussion staves");

        final Constant.Boolean fourStringTablatures = new Constant.Boolean(
                false,
                "4-line bass tablatures");

        final Constant.Boolean fiveLineStaves = new Constant.Boolean(
                true,
                "5-line standard staves");

        final Constant.Boolean drumNotation = new Constant.Boolean(
                false,
                "5-line unpitched percussion staves");

        final Constant.Boolean sixStringTablatures = new Constant.Boolean(
                false,
                "6-line guitar tablatures");

        final Constant.Boolean smallHeads = new Constant.Boolean(false, "Small heads");

        final Constant.Boolean smallBeams = new Constant.Boolean(false, "Small beams");

        final Constant.Boolean crossHeads = new Constant.Boolean(false, "Cross note heads");

        final Constant.Boolean tremolos = new Constant.Boolean(false, "Tremolos");

        final Constant.Boolean fingerings = new Constant.Boolean(false, "Fingering digits");

        final Constant.Boolean frets = new Constant.Boolean(
                false,
                "Frets roman digits (I, II, IV...)");

        final Constant.Boolean pluckings = new Constant.Boolean(false, "Plucking (p, i, m, a)");

        final Constant.Boolean partialWholeRests = new Constant.Boolean(
                false,
                "Partial whole rests");

        final Constant.Boolean multiWholeHeadChords = new Constant.Boolean(
                false,
                "Multi-whole head chords");

        final Constant.Boolean chordNames = new Constant.Boolean(false, "Chord names");

        final Constant.Boolean lyrics = new Constant.Boolean(true, "Lyrics");

        final Constant.Boolean lyricsAboveStaff = new Constant.Boolean(
                false,
                "Lyrics even located above staff");

        final Constant.Boolean articulations = new Constant.Boolean(true, "Articulations");

        final Constant.Boolean implicitTuplets = new Constant.Boolean(false, "Implicit tuplets");
    }

    //-----------------//
    // DefaultSwitches //
    //-----------------//
    private static class DefaultSwitches
            extends ProcessingSwitches
    {
        DefaultSwitches ()
        {
            for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
                map.put(key, new ConstantBasedParam<>(key.getConstant(), GLOBAL_SCOPE));
            }
        }
    }

    //-------------//
    // JaxbAdapter //
    //-------------//
    /**
     * JAXB adapter for ProcessingSwitches type.
     */
    public static class JaxbAdapter
            extends XmlAdapter<JaxbAdapter.ProcessingEntries, ProcessingSwitches>
    {
        @Override
        public ProcessingEntries marshal (ProcessingSwitches switches)
            throws Exception
        {
            if (switches == null) {
                return null;
            }

            final ProcessingEntries myList = new ProcessingEntries();

            for (Map.Entry<ProcessingSwitch, Param<Boolean>> entry : switches.map.entrySet()) {
                ProcessingEntry myEntry = new ProcessingEntry();
                myEntry.key = entry.getKey();
                myEntry.value = entry.getValue().getSpecific();

                // We marshal only entries for which we have a specific value
                if (myEntry.value != null) {
                    myList.entries.add(myEntry);
                }
            }

            return myList;
        }

        @Override
        public ProcessingSwitches unmarshal (ProcessingEntries value)
            throws Exception
        {
            // We need to convert obsolete switches to supported ones
            ProcessingSwitches switches = new ProcessingSwitches();

            // We populate entries for which we have a specific value
            for (ProcessingEntry entry : value.entries) {
                if (entry.key == null) {
                    logger.warn("Null processing switch");
                    continue;
                }

                ProcessingSwitch ps = entry.key;
                if (entry.value != null) {
                    if (ProcessingSwitch.obsoleteSwitches.contains(ps)) {
                        // Today this means a small head flag
                        // We consider the small black flag applies for all
                        // and we ignore the others (void and whole)
                        if (ps == ProcessingSwitch.smallBlackHeads) {
                            logger.info("Processing switch '{}' converted to 'smallHeads'", ps);
                            ps = ProcessingSwitch.smallHeads;
                        } else {
                            logger.info("Processing switch '{}' ignored", ps);
                            continue;
                        }
                    }

                    Param<Boolean> param = new Param<>(null); // NOTA: Actual scope is to be set later
                    param.setSpecific(entry.value);
                    switches.map.put(ps, param);
                }
            }

            // Then fill empty entries
            for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
                if (switches.map.get(key) == null) {
                    switches.map.put(key, new Param<>(null)); // IDEM
                }
            }

            return switches;
        }

        /**
         * Flat list of entries.
         */
        public static class ProcessingEntries
        {
            @XmlElement(name = "switch")
            List<ProcessingEntry> entries = new ArrayList<>();
        }

        /**
         * Class <code>ProcessingEntry</code> encodes a map entry as a plain
         * {key / value} pair.
         */
        public static class ProcessingEntry
        {
            /** The processing switch. */
            @XmlAttribute(name = "key")
            public ProcessingSwitch key;

            /** The boolean value for the switch: true or false. */
            @XmlValue
            public Boolean value;

            @Override
            public String toString ()
            {
                return new StringBuilder("ProcessingEntry{") //
                        .append("key:").append(key) //
                        .append(",value:").append(value) //
                        .append('}').toString();
            }
        }
    }
}
