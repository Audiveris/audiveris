//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e F i l l e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.inter.ChordInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyInter;

import java.util.List;

/**
 * Class {@code MeasureFiller} works at system level to fill each measure with its
 * relevant inters.
 *
 * @author Hervé Bitteur
 */
public class MeasureFiller
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Filling classes. */
    public static final Class<?>[] fillingClasses = new Class<?>[]{
        ClefInter.class, // Clefs
        KeyInter.class, // Key signatures
        ChordInter.class // Chords (heads & rests)
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing system. */
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MeasureFiller} object.
     *
     * @param system the containing system
     */
    public MeasureFiller (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
    {
        // Lookup the relevant inters from system SIG
        final List<Inter> systemInters = system.getSig().inters(fillingClasses);

        // Dispatch system inters per stack & measure
        for (MeasureStack stack : system.getMeasureStacks()) {
            final List<Inter> stackInters = stack.filter(systemInters);

            for (Inter inter : stackInters) {
                Staff staff = inter.getStaff();

                if (staff != null) {
                    Measure measure = stack.getMeasureAt(staff);
                    measure.addInter(inter);
                }
            }
        }
    }
}
