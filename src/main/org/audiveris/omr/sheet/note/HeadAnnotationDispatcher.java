//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         H e a d A n n o t a t i o n D i s p a t c h e r                        //
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.classifier.AnnotationIndex;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omrdataset.api.OmrShapes;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code HeadAnnotationDispatcher} dispatches all head annotations to relevant
 * systems.
 * <p>
 * A given head annotation can be dispatched to two systems when located in the inter-system gutter.
 *
 * @author Hervé Bitteur
 */
public class HeadAnnotationDispatcher
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    private final Sheet sheet;

    /** Spot glyphs, per system. */
    Map<SystemInfo, List<Annotation>> annotationMap = new HashMap<SystemInfo, List<Annotation>>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadAnnotationDispatcher} object.
     *
     * @param sheet the related sheet
     */
    public HeadAnnotationDispatcher (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // getHeadAnnotations //
    //--------------------//
    /**
     * Report the system-based map of relevant head annotations.
     *
     * @return the head annotations gathered by relevant system
     */
    public Map<SystemInfo, List<Annotation>> getHeadAnnotations ()
    {
        final Map<SystemInfo, List<Annotation>> map = new HashMap<SystemInfo, List<Annotation>>();
        final AnnotationIndex index = sheet.getAnnotationIndex();
        final SystemManager systemManager = sheet.getSystemManager();
        final List<SystemInfo> systemsFound = new ArrayList<SystemInfo>();

        for (Annotation ann : index.getEntities()) {
            // Only heads
            if (!OmrShapes.HEADS.contains(ann.getOmrShape())) {
                continue;
            }

            // Relevant systems for this head
            final Rectangle bounds = ann.getBounds();
            systemManager.getSystemsOf(bounds, systemsFound);

            for (SystemInfo system : systemsFound) {
                List<Annotation> list = map.get(system);

                if (list == null) {
                    map.put(system, list = new ArrayList<Annotation>());
                }

                list.add(ann);
            }
        }

        return map;
    }
}
