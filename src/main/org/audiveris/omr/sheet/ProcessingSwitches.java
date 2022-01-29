//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                P r o c e s s i n g S w i t c h e s                             //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

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

    static final Constants constants = new Constants();

    /** Default switches values. */
    private static volatile ProcessingSwitches defaultSwitches;

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Map of switches parameters.
     */
    protected final EnumMap<ProcessingSwitch, Param<Boolean>> map = new EnumMap<>(
            ProcessingSwitch.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a <code>ProcessingSwitches</code> object with its parent.
     *
     * @param parent parent switches
     */
    public ProcessingSwitches (ProcessingSwitches parent)
    {
        if (parent != null) {
            setParent(parent);
        }
    }

    // Meant for JAXB
    protected ProcessingSwitches ()
    {
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
     * Report whether this object provided no specific information.
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
     * Assign the parent of this object.
     *
     * @param parent the parent to assign
     */
    public final void setParent (ProcessingSwitches parent)
    {
        // Complete the map and link each switch to parent switch
        for (ProcessingSwitch key : ProcessingSwitch.values()) {
            Param<Boolean> param = getParam(key);

            if (param == null) {
                param = new Param<>();
                map.put(key, param);
            }

            param.setParent(parent.getParam(key));
        }
    }

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
            ProcessingSwitches switches = new ProcessingSwitches();

            // We populate entries for which we have a specific value
            for (ProcessingEntry entry : value.entries) {
                Param<Boolean> param = new Param<>();

                if (entry.value != null) {
                    param.setSpecific(entry.value);
                    switches.map.put(entry.key, param);
                }
            }

            // Then fill empty entries
            for (ProcessingSwitch key : ProcessingSwitch.values()) {
                if (switches.map.get(key) == null) {
                    switches.map.put(key, new Param<>());
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
                return new StringBuilder("MyEntry{")
                        .append("key:").append(key)
                        .append(",value:").append(value)
                        .append('}').toString();
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    static class Constants
            extends ConstantSet
    {

        final Constant.Boolean poorInputMode = new Constant.Boolean(
                false,
                "Use poor input mode");

        final Constant.Boolean indentations = new Constant.Boolean(
                true,
                "Use of system indentation");

        final Constant.Boolean bothSharedHeadDots = new Constant.Boolean(
                false,
                "Link augmentation dot to both shared heads");

        final Constant.Boolean keepGrayImages = new Constant.Boolean(
                false,
                "Keep loaded gray images");

        final Constant.Boolean articulations = new Constant.Boolean(
                true,
                "Support for articulations");

        final Constant.Boolean chordNames = new Constant.Boolean(
                false,
                "Support for chord names");

        final Constant.Boolean fingerings = new Constant.Boolean(
                false,
                "Support for fingering digits");

        final Constant.Boolean frets = new Constant.Boolean(
                false,
                "Support for frets roman digits (I, II, IV...)");

        final Constant.Boolean pluckings = new Constant.Boolean(
                false,
                "Support for plucking (p, i, m, a)");

        final Constant.Boolean lyrics = new Constant.Boolean(
                true,
                "Support for lyrics");

        final Constant.Boolean lyricsAboveStaff = new Constant.Boolean(
                false,
                "Support for lyrics even located above staff");

        final Constant.Boolean smallBlackHeads = new Constant.Boolean(
                false,
                "Support for small black note heads");

        final Constant.Boolean smallVoidHeads = new Constant.Boolean(
                false,
                "Support for small void note heads");

        final Constant.Boolean smallWholeHeads = new Constant.Boolean(
                false,
                "Support for small whole note heads");

        final Constant.Boolean crossHeads = new Constant.Boolean(
                false,
                "Support for cross note heads");

        final Constant.Boolean implicitTuplets = new Constant.Boolean(
                false,
                "Support for implicit tuplets");

        final Constant.Boolean sixStringTablatures = new Constant.Boolean(
                false,
                "Support for guitar tablatures (6 lines)");

        final Constant.Boolean fourStringTablatures = new Constant.Boolean(
                false,
                "Support for bass tablatures (4 lines)");

        final Constant.Boolean oneLineStaves = new Constant.Boolean(
                false,
                "Support for percussion staves (1 line)");

        final Constant.Boolean partialWholeRests = new Constant.Boolean(
                false,
                "Support for partial whole rests");

        final Constant.Boolean multiWholeHeadChords = new Constant.Boolean(
                false,
                "Support for multi-whole head chords");
    }

    //-----------------//
    // DefaultSwitches //
    //-----------------//
    private static class DefaultSwitches
            extends ProcessingSwitches
    {

        DefaultSwitches ()
        {
            for (ProcessingSwitch key : ProcessingSwitch.values()) {
                map.put(key, new ConstantBasedParam<>(key.getConstant()));
            }
        }
    }
}
