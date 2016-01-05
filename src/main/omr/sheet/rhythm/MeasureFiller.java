//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e F i l l e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(MeasureFiller.class);

    /** Filling classes. */
    public static final Class<?>[] FILLING_CLASSES = new Class<?>[]{
        ClefInter.class, // Clefs
        KeyInter.class, // Key signatures
        AbstractChordInter.class // Chords (heads & rests) BOF! BOF! BOF! BOF! BOF!
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
        final List<Inter> systemInters = system.getSig().inters(FILLING_CLASSES);

        // Dispatch system inters per stack & measure
        for (MeasureStack stack : system.getMeasureStacks()) {
            final List<Inter> stackInters = stack.filter(systemInters);

            for (Inter inter : stackInters) {
                Part part = inter.getPart();

                if (part != null) {
                    Measure measure = stack.getMeasureAt(part);
                    measure.addInter(inter);
                }
            }
        }
    }
}
