//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S w i t c h e s P a r a m                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ProcessingSwitches.class);

    /** Default switches values. */
    private static ProcessingSwitches defaultSwitches;

    //~ Enumerations -------------------------------------------------------------------------------
    /** Enumerated names, based on defined constants. */
    public static enum Switch
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        articulations(constants.articulations),
        chordNames(constants.chordNames),
        fingerings(constants.fingerings),
        frets(constants.frets),
        pluckings(constants.pluckings),
        lyrics(constants.lyrics),
        lyricsAboveStaff(constants.lyricsAboveStaff),
        smallBlackHeads(constants.smallBlackHeads),
        smallVoidHeads(constants.smallVoidHeads),
        smallWholeHeads(constants.smallWholeHeads);

        //~ Instance fields ------------------------------------------------------------------------
        /** Underlying boolean constant. */
        Constant.Boolean constant;

        //~ Constructors ---------------------------------------------------------------------------
        Switch (Constant.Boolean constant)
        {
            this.constant = constant;
        }

        //~ Methods --------------------------------------------------------------------------------
        public Constant.Boolean getConstant ()
        {
            return constant;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Parent switches, if any. */
    private ProcessingSwitches parent;

    /** Map of switches. */
    protected final EnumMap<Switch, Param<Boolean>> map = new EnumMap<Switch, Param<Boolean>>(
            Switch.class);

    //~ Methods ------------------------------------------------------------------------------------
    public static ProcessingSwitches getDefaultSwitches ()
    {
        // Workaround for elaboration circularity
        if (defaultSwitches == null) {
            constants.initialize();
            defaultSwitches = new DefaultSwitches();
        }

        return defaultSwitches;
    }

    public Param<Boolean> getParam (Switch key)
    {
        return map.get(key);
    }

    public Boolean getSpecific (Switch key)
    {
        Param<Boolean> param = getParam(key);

        if (param == null) {
            return null;
        }

        return param.getSpecific();
    }

    public Boolean getValue (Switch key)
    {
        Param<Boolean> param = getParam(key);

        if (param == null) {
            return null;
        }

        return param.getValue();
    }

    public boolean isEmpty ()
    {
        for (Entry<Switch, Param<Boolean>> entry : map.entrySet()) {
            if (entry.getValue().isSpecific()) {
                return false;
            }
        }

        return true;
    }

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

    public void setSpecific (Switch key,
                             Boolean specific)
    {
        if (specific == null) {
            map.remove(key);
        } else {
            Param<Boolean> param = getParam(key);

            if (param == null) {
                map.put(key, param = new BooleanParam());

                if (parent != null) {
                    param.setParent(parent.getParam(key));
                }
            }

            param.setSpecific(specific);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public static class Adapter
            extends XmlAdapter<Adapter.MyEntries, ProcessingSwitches>
    {
        //~ Methods --------------------------------------------------------------------------------

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

        //~ Inner Classes --------------------------------------------------------------------------
        public static final class MyEntries
        {
            //~ Instance fields --------------------------------------------------------------------

            @XmlElement(name = "switch")
            List<MyEntry> entries = new ArrayList<MyEntry>();
        }

        public static final class MyEntry
        {
            //~ Instance fields --------------------------------------------------------------------

            @XmlAttribute(name = "key")
            public Switch key;

            @XmlValue
            public Boolean value;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

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
    }

    //-----------------//
    // DefaultSwitches //
    //-----------------//
    private static class DefaultSwitches
            extends ProcessingSwitches
    {
        //~ Constructors ---------------------------------------------------------------------------

        public DefaultSwitches ()
        {
            for (Switch key : Switch.values()) {
                map.put(key, new ConstantBasedParam<Boolean, Constant.Boolean>(key.getConstant()));
            }
        }
    }
}
