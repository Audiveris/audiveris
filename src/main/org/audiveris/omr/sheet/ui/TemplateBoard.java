//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e B o a r d                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.image.Anchored;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.image.AnchoredTemplate;
import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.image.PixelDistance;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.image.TemplateFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.note.NoteHeadsBuilder;
import org.audiveris.omr.sig.inter.AbstractInter;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.selection.AnchoredTemplateEvent;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>TemplateBoard</code> allows to select a template (shape, anchor) and present
 * the evaluation result at current location.
 *
 * @author Hervé Bitteur
 */
public class TemplateBoard
        extends Board
        implements ChangeListener // For all spinners

{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TemplateBoard.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventsRead = new Class<?>[]
    { LocationEvent.class };

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    private final Sheet sheet;

    /** Distance table. */
    private final DistanceTable table;

    /** Where template info is to be written to. */
    private final SelectionService templateService;

    /** Input: Shape selection. */
    private final JSpinner shapeSpinner;

    /** Input: Anchor selection. */
    private final JSpinner anchorSpinner;

    /** Output: distance. */
    private final JTextField evalField = new JTextField(6);

    /** Output: raw (non-boosted) grade. */
    private final LDoubleField rawField = new LDoubleField("Raw", "Inter grade", "%.3f");

    /** Output: (boosted) grade. */
    private final LDoubleField gradeField = new LDoubleField(
            "Grade",
            "Inter boosted grade",
            "%.3f");

    /** Output: key point value. */
    private final LDoubleField keyPointField = new LDoubleField(
            "KeyPt",
            "Template key point value",
            "%.1f");

    /** Template reference point. */
    private Point refPoint;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>TemplateBoard</code> object.
     *
     * @param sheet           related sheet
     * @param table           the table of distances
     * @param templateService template bus
     */
    public TemplateBoard (Sheet sheet,
                          DistanceTable table,
                          SelectionService templateService)
    {
        super(Board.TEMPLATE, sheet.getLocationService(), eventsRead, true, false, false, false);
        this.sheet = sheet;
        this.table = table;
        this.templateService = templateService;

        // Shape spinner
        shapeSpinner = new JSpinner(
                new SpinnerListModel(new ArrayList<>(ShapeSet.getTemplateNotesAll(sheet))));
        shapeSpinner.addChangeListener(this);
        shapeSpinner.setName("shapeSpinner");
        shapeSpinner.setToolTipText("Selection of template shape");

        // Anchor spinner (with only relevant anchor values for templates)
        anchorSpinner = new JSpinner(
                new SpinnerListModel(
                        Arrays.asList(Anchor.LEFT_STEM, Anchor.RIGHT_STEM, Anchor.MIDDLE_LEFT)));
        anchorSpinner.addChangeListener(this);
        anchorSpinner.setName("anchorSpinner");
        anchorSpinner.setToolTipText("Selection of template anchor");

        // Eval field
        evalField.setEditable(false);
        evalField.setHorizontalAlignment(JTextField.CENTER);
        evalField.setToolTipText("Matching grade");

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // areCompatible //
    //---------------//
    /**
     * Check whether the provided shape and anchor values are compatible.
     * <ul>
     * <li>Stem-less shapes (breve, wholes) go with Anchor.MIDDLE_LEFT only.</li>
     * <li>Stem shapes (halves, quarters) go with Anchor.LEFT_STEM and Anchor.RIGHT_STEM plus
     * Anchor.MIDDLE_LEFT</li>
     * </ul>
     *
     * @param shape  provided shape
     * @param anchor provided anchor
     * @return true if compatible
     */
    private boolean areCompatible (Shape shape,
                                   Anchor anchor)
    {
        if (ShapeSet.StemLessHeads.contains(shape)) {
            return anchor == Anchor.MIDDLE_LEFT;
        } else {
            return true;
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        FormLayout layout = Panel.makeFormLayout(2, 3);
        PanelBuilder builder = new PanelBuilder(layout, getBody());

        ///builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.add(evalField, cst.xy(1, r));
        builder.add(anchorSpinner, cst.xyw(3, r, 3));
        builder.add(shapeSpinner, cst.xyw(7, r, 5));

        r += 2; // --------------------------------
        builder.add(gradeField.getLabel(), cst.xy(1, r));
        builder.add(gradeField.getField(), cst.xy(3, r));

        builder.add(rawField.getLabel(), cst.xy(5, r));
        builder.add(rawField.getField(), cst.xy(7, r));

        builder.add(keyPointField.getLabel(), cst.xy(9, r));
        builder.add(keyPointField.getField(), cst.xy(11, r));
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Display rectangle attributes
     *
     * @param locEvent the location event
     */
    protected void handleLocationEvent (LocationEvent locEvent)
    {
        AnchoredTemplate anchoredTemplate = (AnchoredTemplate) templateService.getSelection(
                AnchoredTemplateEvent.class);

        if (anchoredTemplate == null) {
            return;
        }

        Rectangle rect = locEvent.getData();

        if ((rect == null) || (rect.width != 0) || (rect.height != 0)) {
            return;
        }

        Point pt = rect.getLocation();

        if (locEvent.hint == SelectionHint.CONTEXT_INIT) {
            // Template reference point has been changed, re-eval template at this location
            refPoint = pt;
            tryEvaluate(refPoint, anchoredTemplate);
        } else if (locEvent.hint == SelectionHint.LOCATION_INIT) {
            // User inspects location, display template key point value if any
            if (refPoint != null) {
                Template template = anchoredTemplate.template;
                Anchored.Anchor anchor = anchoredTemplate.anchor;
                Rectangle tplRect = template.getBoundsAt(refPoint.x, refPoint.y, anchor);
                pt.translate(-tplRect.x, -tplRect.y);

                for (PixelDistance pix : template.getKeyPoints()) {
                    if ((pix.x == pt.x) && (pix.y == pt.y)) {
                        keyPointField.setValue(pix.d / table.getNormalizer());

                        return;
                    }
                }

                keyPointField.setText("");
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            if (event instanceof LocationEvent) {
                handleLocationEvent((LocationEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * CallBack triggered by a change in one of the spinners.
     *
     * @param e the change event, this allows to retrieve the originating spinner
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        // Notify the new anchor (with current template shape)
        final Shape shape = (Shape) shapeSpinner.getValue();
        final Anchor anchor = (Anchor) anchorSpinner.getValue();
        AnchoredTemplate at = null;

        if (areCompatible(shape, anchor)) {
            final Scale scale = sheet.getScale();
            final Template template = TemplateFactory.getInstance().getCatalog(
                    sheet.getStub().getMusicFamily(),
                    MusicFont.getHeadPointSize(scale, scale.getInterline())).getTemplate(shape);
            at = new AnchoredTemplate(anchor, template);
        }

        templateService.publish(
                new AnchoredTemplateEvent(this, SelectionHint.ENTITY_INIT, null, at));
        tryEvaluate(refPoint, at);
    }

    //-------------//
    // tryEvaluate //
    //-------------//
    /**
     * Evaluate provided template at provided location.
     *
     * @param pt               the provided location in sheet
     * @param anchoredTemplate the template to use
     */
    private void tryEvaluate (Point pt,
                              AnchoredTemplate anchoredTemplate)
    {
        if ((pt != null) && (anchoredTemplate != null) && (pt.x < table.getWidth()) && (pt.y < table
                .getHeight())) {
            final Anchor anchor = anchoredTemplate.anchor;
            final Template template = anchoredTemplate.template;

            final double dist = template.evaluate(pt.x, pt.y, anchor, table);
            evalField.setText(String.format("%.3f", dist));

            final double grade = Math.max(0, Grades.intrinsicRatio * Template.impactOf(dist));
            rawField.setValue(grade);

            final Shape shape = (Shape) shapeSpinner.getValue();
            final double boosted = (grade == 0) ? 0
                    : (ShapeSet.StemLessHeads.contains(shape) ? AbstractInter.increaseGrade(
                            grade,
                            NoteHeadsBuilder.getStemLessBoost()) : grade);
            gradeField.setValue(boosted);
        } else {
            evalField.setText("");
            rawField.setText("");
            gradeField.setText("");
        }
    }
}
