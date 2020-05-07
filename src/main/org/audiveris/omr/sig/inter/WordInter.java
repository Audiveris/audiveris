//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W o r d I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.ui.symbol.TextSymbol;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.StringUtil;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code WordInter} represents a text word.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "word")
public class WordInter
        extends AbstractInter
{

    private static final Logger logger = LoggerFactory.getLogger(WordInter.class);

    // Persistent data
    //----------------
    //
    /** Word text content. */
    @XmlAttribute
    protected String value;

    /** Detected font attributes. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.Adapter.class)
    protected FontInfo fontInfo;

    /** Precise word starting point on the baseline. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    protected Point2D location;

    /**
     * Creates a new {@code WordInter} object, with TEXT shape.
     *
     * @param textWord the OCR'ed text word
     */
    public WordInter (TextWord textWord)
    {
        this(textWord, Shape.TEXT);
    }

    /**
     * Creates a new {@code WordInter} object, with provided shape.
     *
     * @param textWord the OCR'ed text word
     * @param shape    specific shape (TEXT or LYRICS)
     */
    public WordInter (TextWord textWord,
                      Shape shape)
    {
        super(textWord.getGlyph(),
              textWord.getBounds(),
              shape,
              textWord.getConfidence() * Grades.intrinsicRatio);
        value = textWord.getValue();
        fontInfo = textWord.getFontInfo();
        location = textWord.getLocation();
    }

    /**
     * Creates a new {@code WordInter} object meant for manual assignment.
     *
     * @param shape specific shape (TEXT or LYRICS)
     * @param grade inter grade
     */
    public WordInter (Shape shape,
                      double grade)
    {
        super(null, null, shape, grade);

        this.value = "";
        this.fontInfo = null;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected WordInter ()
    {
        super(null, null, null, null);

        this.fontInfo = null;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        return bounds.contains(point);
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation,
                               Alignment alignment)
    {
        TextSymbol textSymbol = (TextSymbol) symbol;
        Model model = textSymbol.getModel(font, dropLocation, alignment);
        setValue(model.value);
        fontInfo = model.fontInfo;
        location = new Point2D.Double(model.baseLoc.getX(), model.baseLoc.getY());
        setBounds(null);

        return true;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        TextFont textFont = new TextFont(fontInfo);
        TextLayout layout = textFont.layout(value);
        Rectangle2D rect = layout.getBounds();

        return new Rectangle(bounds = new Rectangle((int) Math.rint(location.getX()),
                                                    (int) Math.rint(location.getY() + rect.getY()),
                                                    (int) Math.rint(rect.getWidth()),
                                                    (int) Math.rint(rect.getHeight())));
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (value != null) {
            sb.append(" codes[").append(StringUtil.codesOf(value, false)).append(']');
        }

        if (fontInfo != null) {
            sb.append(' ').append(fontInfo.getMnemo());
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //-------------//
    // getFontInfo //
    //-------------//
    /**
     * Report the related font attributes.
     *
     * @return the fontInfo
     */
    public FontInfo getFontInfo ()
    {
        return fontInfo;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * @return the location
     */
    public Point2D getLocation ()
    {
        return location;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * @return the value
     */
    public String getValue ()
    {
        return value;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new text value.
     *
     * @param value the new value
     */
    public void setValue (String value)
    {
        this.value = value;

        setBounds(null);

        if (sig != null) {
            // Update containing sentence
            Inter sentence = getEnsemble();

            if (sentence != null) {
                sentence.invalidateCache();
            }
        }
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        // Location?
        // FontInfo?
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel)
    {
        // Standard addition task for this word
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel));

        // Wrap this word into a new sentence
        SentenceInter sentence = new SentenceInter(TextRole.Direction, 1);
        sentence.setManual(true);
        sentence.setStaff(staff);

        tasks.add(new AdditionTask(
                staff.getSystem().getSig(),
                sentence,
                getBounds(),
                Arrays.asList(new Link(this, new Containment(), true))));

        return tasks;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "WORD";
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" \"").append(value).append("\"");

        return sb.toString();
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a word.
     * <p>
     * For a word, there are 2 handles:
     * <ul>
     * <li>Middle handle, moving the word in any direction
     * <li>TopRight handle, to increase/decrease font size
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {

        private final Model originalModel;

        private final Model model;

        private final Point2D middle;

        private final Point2D right;

        public Editor (WordInter word)
        {
            super(word);

            originalModel = new Model(word.getValue(), word.getLocation(), word.getFontInfo());
            model = new Model(word.getValue(), word.getLocation(), word.getFontInfo());

            final Rectangle box = word.getBounds();

            middle = new Point2D.Double(box.x + box.width / 2.0, box.y + box.height / 2.0);
            right = new Point2D.Double(box.x + box.width, box.y + box.height / 2.0);

            handles.add(selectedHandle = new InterEditor.Handle(middle)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Data
                    PointUtil.add(model.baseLoc, vector);

                    // Handles
                    for (InterEditor.Handle handle : handles) {
                        PointUtil.add(handle.getHandleCenter(), vector);
                    }

                    return true;
                }
            });

            // Move right, only horizontally
            handles.add(new Handle(right)
            {
                @Override
                public boolean move (Point vector)
                {
                    final int dx = vector.x;

                    if (dx == 0) {
                        return false;
                    }

                    // Data
                    box.width += dx;

                    if (box.width > 0) {
                        WordInter word = (WordInter) inter;
                        String value = word.getValue();
                        int fontSize = (int) Math.rint(
                                TextFont.computeFontSize(value, FontInfo.DEFAULT, box.width));
                        model.fontInfo = FontInfo.createDefault(fontSize);

                        // Handles
                        TextFont textFont = new TextFont(model.fontInfo);
                        TextLayout layout = textFont.layout(value);
                        Rectangle2D rect = layout.getBounds();
                        double y = model.baseLoc.getY() + rect.getY() + rect.getHeight() / 2;
                        middle.setLocation(box.x + rect.getWidth() / 2, y);
                        right.setLocation(box.x + rect.getWidth(), y);
                    }

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            WordInter word = (WordInter) inter;
            word.location.setLocation(model.baseLoc);
            word.fontInfo = model.fontInfo;

            inter.setBounds(null);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            WordInter word = (WordInter) inter;
            word.location.setLocation(originalModel.baseLoc);
            word.fontInfo = originalModel.fontInfo;

            inter.setBounds(null);
            super.undo();
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
    {

        public final String value;

        public final Point2D baseLoc;

        public FontInfo fontInfo;

        public Model (String value,
                      Point2D baseLoc,
                      FontInfo fontInfo)
        {
            this.value = value;
            this.baseLoc = new Point2D.Double(baseLoc.getX(), baseLoc.getY());
            this.fontInfo = fontInfo;
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(baseLoc, dx, dy);
        }
    }
}
