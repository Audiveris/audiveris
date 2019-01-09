//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A n n o t a t i o n V i e w                                  //
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.EntityView;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.util.Navigable;

import java.awt.Graphics2D;

/**
 * Class {@code AnnotationView} handles a sheet-level view on its related annotations.
 *
 * @author Hervé Bitteur
 */
public class AnnotationView
        extends EntityView<Annotation>
{

    /** Containing sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /**
     * Creates a new {@code AnnotationView} object.
     *
     * @param annotationService service on annotation instance
     * @param sheet             related sheet
     */
    public AnnotationView (EntityService<Annotation> annotationService,
                           Sheet sheet)
    {
        super(annotationService);
        this.sheet = sheet;

        // Inject dependency of pixel location
        setLocationService(sheet.getLocationService());

        setName("AnnotationView");
    }

    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics2D g)
    {
        AnnotationPainter.paint(sheet, g);
    }
}
