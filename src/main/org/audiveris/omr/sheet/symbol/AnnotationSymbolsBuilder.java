//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         A n n o t a t i o n S y m b o l s B u i l d e r                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code AnnotationSymbolsBuilder} is in charge at system level to convert all
 * remaining annotations into inters.
 *
 * @author Hervé Bitteur
 */
public class AnnotationSymbolsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AnnotationSymbolsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** The relevant annotations. */
    private final List<Annotation> annotations;

    /** Companion factory for symbols inters. */
    private final InterFactory factory;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnnotationSymbolsBuilder} object.
     *
     * @param system      the dedicated system
     * @param annotations the relevant annotations for this system
     * @param factory     the dedicated inter factory
     */
    public AnnotationSymbolsBuilder (SystemInfo system,
                                     List<Annotation> annotations,
                                     InterFactory factory)
    {
        this.system = system;
        this.annotations = annotations;
        this.factory = factory;

        sheet = system.getSheet();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Browse all relevant annotations (they have not been used for Inter yet)
     * and create the corresponding inters as possible.
     */
    public void process ()
    {
        for (Annotation annotation : annotations) {
            processAnnotation(annotation);
        }
    }

    //-------------------//
    // processAnnotation //
    //-------------------//
    private void processAnnotation (Annotation annotation)
    {
        if (annotation.isVip()) {
            logger.info("VIP AnnotationSymbolsBuilder.processAnnotation for {}", annotation);
        }

        final Rectangle bounds = annotation.getBounds();
        final Point center = GeoUtil.centerOf(bounds);
        final Staff closestStaff = system.getClosestStaff(center); // Just an indication!

        if (closestStaff == null) {
            return;
        }

        factory.create(annotation, closestStaff);
    }
}
