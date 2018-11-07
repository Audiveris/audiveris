//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S i g B a c k u p                                       //
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
package org.audiveris.omr.sig;

import org.audiveris.omr.sig.inter.Inter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SigBackup} allows to save and restore inters (with their relations)
 * from/to a sig.
 *
 * @author Hervé Bitteur
 */
public class SigBackup
{

    /** Saved data. */
    protected List<Inter> seeds;

    /** The attic where data can be saved to and restored from. */
    protected final SigAttic attic = new SigAttic();

    /** The SIG where work is done. */
    protected final SIGraph sig;

    /**
     * Creates a new {@code SigBackup} object.
     *
     * @param sig the related sig
     */
    public SigBackup (SIGraph sig)
    {
        this.sig = sig;
    }

    //----------//
    // getSeeds //
    //----------//
    public List<Inter> getSeeds ()
    {
        return seeds;
    }

    //---------//
    // restore //
    //---------//
    public void restore (Collection<Inter> inters)
    {
        attic.restore(sig, inters);
    }

    //------//
    // save //
    //------//
    public void save (Collection<Inter> inters)
    {
        // Copy the initial seeds
        setSeeds(inters);

        // Save relevant sig inters & relations
        attic.save(sig, seeds);
    }

    //----------//
    // setSeeds //
    //----------//
    public void setSeeds (Collection<Inter> seeds)
    {
        this.seeds = new ArrayList<Inter>(seeds);
    }
}
