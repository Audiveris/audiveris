//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     H e a d s B u i l d e r                                    //
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code HeadsBuilder} builds heads from head annotations in a given system.
 *
 * @author Hervé Bitteur
 */
public class HeadsBuilder
{

    private static final Logger logger = LoggerFactory.getLogger(HeadsBuilder.class);

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    @Navigable(false)
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** Relevant head annotations for this system. */
    private final List<Annotation> headAnnotations;

    /**
     * Creates a new {@code HeadsBuilder} object.
     *
     * @param system          underlying system
     * @param headAnnotations detected head annotations
     */
    public HeadsBuilder (SystemInfo system,
                         List<Annotation> headAnnotations)
    {
        this.system = system;
        this.headAnnotations = headAnnotations;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
    }

    //------------//
    // buildHeads //
    //------------//
    /**
     * Convert head annotations to head inters and link them to proper staff.
     * <p>
     * We use staff lines and ledgers to connect each head to its staff.
     */
    public void buildHeads ()
    {
        for (Annotation annotation : headAnnotations) {
            final Rectangle bounds = annotation.getBounds();
            final Point center = GeoUtil.centerOf(bounds);
            final NotePosition np = system.getNoteStaffAt(center);

            if (np != null) {
                final double grade = annotation.getConfidence() * Grades.intrinsicRatio;
                final HeadInter head = new HeadInter(
                        annotation.getId(),
                        bounds,
                        annotation.getOmrShape(),
                        grade,
                        np.getStaff(),
                        np.getPitchPosition());
                sig.addVertex(head);
            } else {
                logger.info("{} No staff for {}", system, annotation);
            }
        }
    }
}
