//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A n n o t a t i o n V i e w                                  //
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
import org.audiveris.omr.classifier.AnnotationIndex;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.EntityView;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.Navigable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Collection;

/**
 * Class {@code AnnotationView}
 *
 * @author Hervé Bitteur
 */
public class AnnotationView
        extends EntityView<Annotation>
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected final AnnotationIndex annotationIndex;

    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
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

        annotationIndex = (AnnotationIndex) annotationService.getIndex();

        // Inject dependency of pixel location
        setLocationService(sheet.getLocationService());

        setName("AnnotationView");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics2D g)
    {
        // Display underlying image
        RunTable table = sheet.getPicture().getTable(Picture.TableKey.BINARY);
        table.render(g, new Point(0, 0));

        Collection<Annotation> annotations = annotationIndex.getEntities();

        // Bounds
        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        g.setColor(Color.GREEN);

        for (Annotation a : annotations) {
            Rectangle b = a.getBounds();
            g.draw(b);
        }

        // Names
        double ratio = g.getTransform().getScaleX();

        if (ratio >= 2) {
            Font oldFont = g.getFont();
            g.setFont(oldFont.deriveFont(10f / (float) ratio));
            g.setColor(Color.MAGENTA);

            for (Annotation a : annotations) {
                Rectangle b = a.getBounds();
                g.drawString(a.getOmrShape().name(), b.x, b.y);
            }

            g.setFont(oldFont);
        }

        // Restore graphics context
        g.setStroke(oldStroke);
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        // Global sheet renderers if any
        sheet.renderItems(g);
    }
}
