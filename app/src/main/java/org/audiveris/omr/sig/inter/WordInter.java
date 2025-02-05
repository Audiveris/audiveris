//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W o r d I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.ui.symbol.TextSymbol;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.StringUtil;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
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
 * Class <code>WordInter</code> represents a word made of text characters.
 * <p>
 * By default, a WordInter uses a {@link TextFont}.
 * But the {@link BeatUnitInter} subclass uses a {@link MusicFont} instead of a TextFont.
 * <p>
 * The containing {@link SentenceInter} is linked by a {@link Containment} relation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "word")
public class WordInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(WordInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Word content. */
    @XmlAttribute
    protected volatile String value;

    /** Font attributes. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.JaxbAdapter.class)
    protected FontInfo fontInfo;

    /** Precise word starting point on the baseline. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    protected Point2D location;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    protected WordInter ()
    {
        super(null, null, null, (Double) null);

        this.fontInfo = null;
    }

    /**
     * Creates a new <code>WordInter</code> object, with all details.
     *
     * @param glyph    underlying glyph
     * @param bounds   bounding box
     * @param shape    specific shape (TEXT or LYRICS or METRONOME)
     * @param grade    quality
     * @param value    the word content
     * @param fontInfo font information
     * @param location location
     */
    public WordInter (Glyph glyph,
                      Rectangle bounds,
                      Shape shape,
                      Double grade,
                      String value,
                      FontInfo fontInfo,
                      Point location)
    {
        super(glyph, bounds, shape, grade);
        this.value = value;
        this.fontInfo = fontInfo;
        this.location = location;
    }

    /**
     * Creates a new <code>WordInter</code> object meant for manual assignment.
     *
     * @param shape specific shape (TEXT or LYRICS)
     * @param grade inter grade
     */
    public WordInter (Shape shape,
                      Double grade)
    {
        super(null, null, shape, grade);

        this.value = "";
        this.fontInfo = null;
    }

    /**
     * Creates a new <code>WordInter</code> object, with TEXT shape.
     *
     * @param textWord the OCR'd text word
     */
    public WordInter (TextWord textWord)
    {
        this(textWord, Shape.TEXT);
    }

    /**
     * Creates a new <code>WordInter</code> object, with provided shape.
     *
     * @param textWord the OCR'd text word
     * @param shape    specific shape (TEXT or LYRICS)
     */
    public WordInter (TextWord textWord,
                      Shape shape)
    {
        this(
                textWord.getGlyph(),
                textWord.getBounds(),
                shape,
                textWord.getConfidence() * Grades.intrinsicRatio,
                textWord.getValue(),
                textWord.getFontInfo(),
                textWord.getLocation());
    }

    /**
     * Creates a new <code>WordInter</code> object from an original WordInter,
     * with provided shape.
     *
     * @param word  the original word inter
     * @param shape specific shape (TEXT or LYRICS)
     */
    public WordInter (WordInter word,
                      Shape shape)
    {
        this(
                word.getGlyph(),
                word.getBounds(),
                shape,
                1.0,
                word.getValue(),
                word.getFontInfo(),
                PointUtil.rounded(word.getLocation()));
    }

    //~ Methods ------------------------------------------------------------------------------------

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
                               Point dropLocation)
    {
        TextSymbol textSymbol = (TextSymbol) symbol;
        Model model = textSymbol.getModel(font, dropLocation);
        setValue(model.value);
        fontInfo = model.fontInfo;
        location = new Point2D.Double(model.baseLoc.getX(), model.baseLoc.getY());
        setBounds(null);

        return true;
    }

    //------------//
    // getAdvance //
    //------------//
    public int getAdvance ()
    {
        if (value.isEmpty()) {
            return 0;
        }

        final TextFont font = new TextFont(fontInfo);
        final TextLayout layout = font.layout(value);

        return (int) Math.rint(layout.getAdvance());
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

        if (value.isEmpty()) {
            return new Rectangle(
                    bounds = new Rectangle(
                            (int) Math.rint(location.getX()),
                            (int) Math.rint(location.getY()),
                            0,
                            0));
        }

        TextFont textFont = new TextFont(fontInfo);
        TextLayout layout = textFont.layout(value);
        Rectangle2D rect = layout.getBounds();

        return new Rectangle(
                bounds = new Rectangle(
                        (int) Math.rint(location.getX()),
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
        final StringBuilder sb = new StringBuilder(super.getDetails());

        if (value != null) {
            sb.append((sb.length() != 0) ? " " : "");
            sb.append("codes[").append(StringUtil.codesOf(value, false)).append(']');
        }

        return sb.toString();
    }

    //--------------//
    // getDimension //
    //--------------//
    public Dimension getDimension ()
    {
        if (bounds != null) {
            return bounds.getSize();
        }

        if (value.isEmpty()) {
            return new Dimension(0, 0);
        }

        final TextFont textFont = new TextFont(fontInfo);
        final TextLayout layout = textFont.layout(value);
        final Rectangle2D rect = layout.getBounds();

        return new Dimension((int) Math.rint(rect.getWidth()), (int) Math.rint(rect.getHeight()));
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

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "WORD";
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return new StringBuilder(super.internals()) //
                .append(" \"").append(value).append("\"") //
                .append((fontInfo != null) ? " font:" + fontInfo.getMnemo() : "") //
                .toString();
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        // Standard addition task for this word
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel, toPublish));

        // Wrap this word into a new sentence
        SentenceInter sentence = new SentenceInter(TextRole.Direction, 1.0);
        sentence.setManual(true);
        sentence.setStaff(staff);

        tasks.add(
                new AdditionTask(
                        staff.getSystem().getSig(),
                        sentence,
                        getBounds(),
                        Arrays.asList(new Link(this, new Containment(), true))));

        return tasks;
    }

    //-------------//
    // setFontInfo //
    //-------------//
    public void setFontInfo (FontInfo fontInfo)
    {
        this.fontInfo = fontInfo;
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

    //-------------//
    // setLocation //
    //-------------//
    public void setLocation (Point location)
    {
        this.location = location;
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
            final SentenceInter sentence = (SentenceInter) getEnsemble();

            if (sentence != null) {
                sentence.invalidateCache();

                if (sentence.getRole() == TextRole.PartName) {
                    // Update partRef name as well
                    final Part thePart = sentence.getStaff().getPart();
                    thePart.setName(sentence);
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a word.
     * <p>
     * For a word, there are 2 handles:
     * <ul>
     * <li>Middle handle, moving the word in any direction
     * <li>Right handle, to increase/decrease font size
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

            middle = new Point2D.Double(box.x + (box.width / 2.0), box.y + (box.height / 2.0));
            right = new Point2D.Double(box.x + box.width, box.y + (box.height / 2.0));

            handles.add(selectedHandle = new InterEditor.Handle(middle)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Data
                    PointUtil.add(model.baseLoc, dx, dy);

                    // Handles
                    for (InterEditor.Handle handle : handles) {
                        PointUtil.add(handle.getPoint(), dx, dy);
                    }

                    return true;
                }
            });

            // Move right, only horizontally
            handles.add(new Handle(right)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (dx == 0) {
                        return false;
                    }

                    // Data
                    box.width += dx;

                    if (box.width > 0) {
                        final WordInter word = (WordInter) getInter();
                        final String value = word.getValue();

                        // Select proper text font (family and size)
                        final TextFamily textFamily = TextFont.getCurrentFamily();
                        TextFont textFont = new TextFont(textFamily.getFontName(), null, 0, 50);
                        final int fontSize = textFont.computeSize(value, box.getSize());
                        model.fontInfo = new FontInfo(fontSize, textFamily.getFontName());
                        textFont = textFont.deriveFont((float) fontSize);

                        // Handles
                        final TextLayout layout = textFont.layout(value);
                        final Rectangle2D rect = layout.getBounds();
                        final double y = model.baseLoc.getY() + rect.getY() //
                                + (rect.getHeight() / 2);
                        middle.setLocation(box.x + (rect.getWidth() / 2), y);
                        right.setLocation(box.x + rect.getWidth(), y);
                    }

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final Inter inter = getInter();
            final WordInter word = (WordInter) inter;
            word.location.setLocation(model.baseLoc);
            word.fontInfo = model.fontInfo;

            inter.setBounds(null);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            final Inter inter = getInter();
            final WordInter word = (WordInter) inter;
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
            implements ObjectUIModel
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
