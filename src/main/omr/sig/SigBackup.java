//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S i g B a c k u p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.sig.inter.Inter;

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
    //~ Instance fields ----------------------------------------------------------------------------

    /** Saved data. */
    protected List<Inter> seeds;

    /** The attic where data can be saved to and restored from. */
    protected final SigAttic attic = new SigAttic();

    /** The SIG where work is done. */
    protected final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SigBackup} object.
     *
     * @param sig the related sig
     */
    public SigBackup (SIGraph sig)
    {
        this.sig = sig;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
