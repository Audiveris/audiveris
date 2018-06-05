//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A n n o t a t i o n B o a r d                                 //
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.selection.EntityService;

/**
 * Class {@code AnnotationBoard} defines a UI board dedicated to the display of
 * {@link Annotation} information.
 *
 * @author Hervé Bitteur
 */
public class AnnotationBoard
        extends EntityBoard<Annotation>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code AnnotationBoard} object.
     *
     * @param service  annotation service
     * @param selected true for pre-selected
     */
    public AnnotationBoard (EntityService<Annotation> service,
                            boolean selected)
    {
        super(Board.ANNOTATION, service, selected);
    }
}
