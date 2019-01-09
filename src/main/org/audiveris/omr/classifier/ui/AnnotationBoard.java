//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A n n o t a t i o n B o a r d                                 //
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

import com.jgoodies.forms.layout.CellConstraints;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.field.LTextField;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omrdataset.api.OmrShape;

import javax.swing.JLabel;

/**
 * Class {@code AnnotationBoard} defines a UI board dedicated to the display of
 * {@link Annotation} information.
 *
 * @author Hervé Bitteur
 */
public class AnnotationBoard
        extends EntityBoard<Annotation>
{

    /** Output : shape icon. */
    private final JLabel shapeIcon = new JLabel();

    /** Output : confidence. */
    private final LTextField grade = new LTextField("Conf", "Confidence");

    /** Output : shape. */
    private final LTextField shapeField = new LTextField("", "Shape for this annotation");

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

        grade.setEnabled(false);
        shapeField.setEnabled(false);

        defineLayout();
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    @Override
    protected void handleEntityListEvent (EntityListEvent<Annotation> listEvent)
    {
        super.handleEntityListEvent(listEvent);

        final Annotation annotation = listEvent.getEntity();

        // Shape
        OmrShape omrShape = (annotation != null) ? annotation.getOmrShape() : null;

        if (omrShape != null) {
            shapeField.setText(omrShape.toString());

            ///shapeIcon.setIcon(omrShape.getDecoratedSymbol());
        } else {
            shapeField.setText("");
            shapeIcon.setIcon(null);
        }

        if (annotation != null) {
            grade.setText(String.format("%.2f", annotation.getConfidence()));
        } else {
            grade.setText("");
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for InterBoard specific fields.
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        // Layout
        int r = 1; // -----------------------------

        // Shape Icon (start, spans several rows) + grade + Deassign button
        builder.add(shapeIcon, cst.xywh(1, r, 1, 5));

        builder.add(grade.getLabel(), cst.xy(5, r));
        builder.add(grade.getField(), cst.xy(7, r));

        r += 2; // --------------------------------

        builder.add(shapeField.getField(), cst.xyw(7, r, 5));
    }
}
