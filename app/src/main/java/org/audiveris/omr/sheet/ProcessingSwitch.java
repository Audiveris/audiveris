//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P r o c e s s i n g S w i t c h                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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

import java.util.EnumSet;

/**
 * Class <code>ProcessingSwitch</code> is the enumeration of all possible processing
 * switches.
 * <p>
 * Every switch represents a boolean value that the user can modify.
 * The effective boolean value for a given switch complies with the following hierarchy of
 * assignments (from weakest to strongest):
 * <ol>
 * <li>Default value as written in Java source
 * <li>Value as modified by the user for <b>global</b> scope.
 * Switch value is persisted in user <code>config/run.properties</code> file
 * <li>Value as modified by the user for <b>book</b> scope.
 * Switch value is persisted in the <code>omr.xml</code> file
 * within <code>&lt;book&gt;.omr</code> project archive
 * <li>Value as modified by the user for <b>sheet</b> scope.
 * Switch value is persisted in a <code>sheet#N.xml</code> file
 * within <code>&lt;book&gt;.omr</code> project archive
 * </ol>
 */
public enum ProcessingSwitch
{
    oneLineStaves(ProcessingSwitches.constants.oneLineStaves),
    fourStringTablatures(ProcessingSwitches.constants.fourStringTablatures),
    fiveLineStaves(ProcessingSwitches.constants.fiveLineStaves),
    drumNotation(ProcessingSwitches.constants.drumNotation),
    sixStringTablatures(ProcessingSwitches.constants.sixStringTablatures),

    smallHeads(ProcessingSwitches.constants.smallHeads),
    smallBeams(ProcessingSwitches.constants.smallBeams),
    crossHeads(ProcessingSwitches.constants.crossHeads),
    tremolos(ProcessingSwitches.constants.tremolos),
    fingerings(ProcessingSwitches.constants.fingerings),
    frets(ProcessingSwitches.constants.frets),
    pluckings(ProcessingSwitches.constants.pluckings),
    partialWholeRests(ProcessingSwitches.constants.partialWholeRests),
    multiWholeHeadChords(ProcessingSwitches.constants.multiWholeHeadChords),
    chordNames(ProcessingSwitches.constants.chordNames),
    lyrics(ProcessingSwitches.constants.lyrics),
    lyricsAboveStaff(ProcessingSwitches.constants.lyricsAboveStaff),
    articulations(ProcessingSwitches.constants.articulations),

    keepGrayImages(ProcessingSwitches.constants.keepGrayImages),
    indentations(ProcessingSwitches.constants.indentations),
    bothSharedHeadDots(ProcessingSwitches.constants.bothSharedHeadDots),
    implicitTuplets(ProcessingSwitches.constants.implicitTuplets),

    // Obsolete switches:
    poorInputMode(null),
    smallBlackHeads(null),
    smallVoidHeads(null),
    smallWholeHeads(null);

    /**
     * The switches currently supported.
     */
    public static EnumSet<ProcessingSwitch> supportedSwitches = EnumSet.range(
            oneLineStaves,
            implicitTuplets);

    /**
     * The staff switches.
     */
    public static EnumSet<ProcessingSwitch> staffSwitches = EnumSet.range(
            oneLineStaves,
            sixStringTablatures);

    /**
     * The item switches.
     */
    public static EnumSet<ProcessingSwitch> itemSwitches = EnumSet.range( //
            smallHeads,
            articulations);

    /**
     * The processing switches.
     */
    public static EnumSet<ProcessingSwitch> standardSwitches = EnumSet.range(
            keepGrayImages,
            implicitTuplets);

    /**
     * The switches now obsolete.
     */
    public static EnumSet<ProcessingSwitch> obsoleteSwitches = EnumSet.range(
            poorInputMode,
            smallWholeHeads);

    /** Underlying boolean constant. */
    private final Constant.Boolean constant;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a switch from its dedicated application constant.
     *
     * @param constant the related backing constant
     */
    ProcessingSwitch (Constant.Boolean constant)
    {
        this.constant = constant;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // getConstant //
    //-------------//
    /**
     * Report the backing constant for this switch.
     *
     * @return the underlying application constant
     */
    public Constant.Boolean getConstant ()
    {
        return constant;
    }
}
