//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R h y t h m C o n f i g                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.Inter;
import omr.sig.inter.Inters;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.RestInter;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Meant to store one configuration of rhythm adjustable data.
 *
 * @author Hervé Bitteur
 */
public class RhythmConfig
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Compare configs by their decreasing size. */
    public static final Comparator<RhythmConfig> byReverseSize = new Comparator<RhythmConfig>()
    {
        @Override
        public int compare (RhythmConfig c1,
                            RhythmConfig c2)
        {
            return Integer.compare(c2.inters.size(), c1.inters.size());
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Rhythm data for this config, always ordered byFullAbscissa. */
    final TreeSet<Inter> inters = new TreeSet<Inter>(Inter.byFullAbscissa);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RhythmConfig} object.
     *
     * @param inters DOCUMENT ME!
     */
    public RhythmConfig (Collection<? extends Inter> inters)
    {
        this.inters.addAll(inters);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // add //
    //-----//
    public void add (Inter inter)
    {
        // Add inter to config
        inters.add(inter);

        // As well as its augmentation dot(s) if relevant (checked in backup sig)
        if (inter instanceof RestChordInter) {
            RestChordInter restChord = (RestChordInter) inter;
            RestInter rest = (RestInter) restChord.getNotes().get(0);
            AugmentationDotInter firstDot = rest.getFirstAugmentationDot();

            if (firstDot != null) {
                inters.add(firstDot);

                AugmentationDotInter secondDot = firstDot.getSecondAugmentationDot();

                if (secondDot != null) {
                    inters.add(secondDot);
                }
            }
        }
    }

    //------//
    // copy //
    //------//
    public RhythmConfig copy ()
    {
        return new RhythmConfig(inters);
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof RhythmConfig)) {
            return false;
        }

        RhythmConfig that = (RhythmConfig) obj;

        return inters.equals(that.inters);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;

        return hash;
    }

    //-----//
    // ids //
    //-----//
    public String ids ()
    {
        return Inters.ids(inters);
    }

    //--------//
    // remove //
    //--------//
    public void remove (Inter inter)
    {
        // Remove the inter from config
        inters.remove(inter);

        // As well as its augmentation dot(s) if relevant
        if (inter instanceof RestChordInter) {
            RestChordInter restChord = (RestChordInter) inter;
            RestInter rest = (RestInter) restChord.getNotes().get(0);
            AugmentationDotInter firstDot = rest.getFirstAugmentationDot();

            if (firstDot != null) {
                inters.remove(firstDot);

                AugmentationDotInter secondDot = firstDot.getSecondAugmentationDot();

                if (secondDot != null) {
                    inters.remove(secondDot);
                }
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return inters.toString();
    }
}
