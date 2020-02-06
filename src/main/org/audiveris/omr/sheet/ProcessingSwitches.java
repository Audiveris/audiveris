//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                P r o c e s s i n g S w i t c h e s                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.util.param.BooleanParam;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

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
 * Class {@code ProcessingSwitches} handles a set of named processing switches.
 *
 * @author Hervé Bitteur
 */
public class ProcessingSwitches
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ProcessingSwitches.class);

    /** Default switches values. */
    private static volatile ProcessingSwitches defaultSwitches;

    /** Map of switches. */
    protected final EnumMap<Switch, Param<Boolean>> map = new EnumMap<>(Switch.class);

    /** Parent switches, if any. */
    private ProcessingSwitches parent;

    /**
     * Report the parameter for the provided key.
     *
     * @param key provided key
     * @return related parameter
     */
    public Param<Boolean> getParam (Switch key)
    {
        return map.get(key);
    }

    /**
     * Report the current value for the provided key.
     *
     * @param key provided key
     * @return current value, perhaps null
     */
    public Boolean getValue (Switch key)
    {
        Param<Boolean> param = getParam(key);

        if (param == null) {
            return null;
        }

        return param.getValue();
    }

    /**
     * Report whether this object provided no specific information.
     *
     * @return true if empty
     */
    public boolean isEmpty ()
    {
        for (Entry<Switch, Param<Boolean>> entry : map.entrySet()) {
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
    public void setParent (ProcessingSwitches parent)
    {
        this.parent = parent;

        // Populate the map
        for (Switch key : Switch.values()) {
            Param<Boolean> param = getParam(key);

            if (param == null) {
                param = new BooleanParam();
                param.setParent(parent.getParam(key));
                map.put(key, param);
            }
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

    /** Enumerated names, based on defined constants. */
    public static enum Switch
    {
        indentations(constants.indentations),
        articulations(constants.articulations),
        chordNames(constants.chordNames),
        fingerings(constants.fingerings),
        frets(constants.frets),
        pluckings(constants.pluckings),
        lyrics(constants.lyrics),
        lyricsAboveStaff(constants.lyricsAboveStaff),
        smallBlackHeads(constants.smallBlackHeads),
        smallVoidHeads(constants.smallVoidHeads),
        smallWholeHeads(constants.smallWholeHeads),
        crossHeads(constants.crossHeads),
        implicitTuplets(constants.implicitTuplets);

        /** Underlying boolean constant. */
        Constant.Boolean constant;

        Switch (Constant.Boolean constant)
        {
            this.constant = constant;
        }

        public Constant.Boolean getConstant ()
        {
            return constant;
        }
    }

    /**
     * JAXB adapter for ProcessingSwitches type.
     */
    public static class Adapter
            extends XmlAdapter<Adapter.MyEntries, ProcessingSwitches>
    {

        @Override
        public MyEntries marshal (ProcessingSwitches switches)
                throws Exception
        {
            if (switches == null) {
                return null;
            }

            final MyEntries myList = new MyEntries();

            for (Map.Entry<Switch, Param<Boolean>> entry : switches.map.entrySet()) {
                MyEntry myEntry = new MyEntry();
                myEntry.key = entry.getKey();
                myEntry.value = entry.getValue().getSpecific();

                if (myEntry.value != null) {
                    myList.entries.add(myEntry);
                }
            }

            return myList;
        }

        @Override
        public ProcessingSwitches unmarshal (MyEntries value)
                throws Exception
        {
            ProcessingSwitches switches = new ProcessingSwitches();

            for (MyEntry entry : value.entries) {
                BooleanParam b = new BooleanParam();

                if (entry.value != null) {
                    b.setSpecific(entry.value);
                    switches.map.put(entry.key, b);
                }
            }

            return switches;
        }

        /**
         * Flat list of entries.
         */
        public static class MyEntries
        {

            @XmlElement(name = "switch")
            List<MyEntry> entries = new ArrayList<>();
        }

        /**
         * Plain entry: key / value.
         */
        public static class MyEntry
        {

            @XmlAttribute(name = "key")
            public Switch key;

            @XmlValue
            public Boolean value;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean indentations = new Constant.Boolean(
                true,
                "Support for use of system indentation");

        private final Constant.Boolean articulations = new Constant.Boolean(
                true,
                "Support for articulations");

        private final Constant.Boolean chordNames = new Constant.Boolean(
                false,
                "Support for chord names");

        private final Constant.Boolean fingerings = new Constant.Boolean(
                false,
                "Support for fingering digits");

        private final Constant.Boolean frets = new Constant.Boolean(
                false,
                "Support for frets roman digits (I, II, IV...)");

        private final Constant.Boolean pluckings = new Constant.Boolean(
                false,
                "Support for plucking (p, i, m, a)");

        private final Constant.Boolean lyrics = new Constant.Boolean(true, "Support for lyrics");

        private final Constant.Boolean lyricsAboveStaff = new Constant.Boolean(
                false,
                "Support for lyrics even located above staff");

        private final Constant.Boolean smallBlackHeads = new Constant.Boolean(
                false,
                "Support for small black note heads");

        private final Constant.Boolean smallVoidHeads = new Constant.Boolean(
                false,
                "Support for small void note heads");

        private final Constant.Boolean smallWholeHeads = new Constant.Boolean(
                false,
                "Support for small whole note heads");

        private final Constant.Boolean crossHeads = new Constant.Boolean(
                false,
                "Support for cross note heads");

        private final Constant.Boolean implicitTuplets = new Constant.Boolean(
                false,
                "Support for implicit tuplets");
    }

    //-----------------//
    // DefaultSwitches //
    //-----------------//
    private static class DefaultSwitches
            extends ProcessingSwitches
    {

        DefaultSwitches ()
        {
            for (Switch key : Switch.values()) {
                map.put(key, new ConstantBasedParam<Boolean, Constant.Boolean>(key.getConstant()));
            }
        }
    }
}
