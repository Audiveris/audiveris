//----------------------------------------------------------------------------//
//                                                                            //
//                         T e x t G l y p h L i n e                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.constant.ConstantSet;

import omr.glyph.*;

import omr.log.Logger;

import omr.math.Population;

import omr.score.Score;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.entity.ScoreSystem;
import omr.score.entity.ScoreSystem.StaffPosition;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.util.*;

/**
 * Class <code>TextGlyphLine</code> gathers one line of text glyphs,
 * assembled according to their ordinate, and sorted within the same line by
 * their starting abscissa. The main purpose is to consolidate and to extend to
 * potential text glyphs nearby. It is also expected that such text line will
 * give birth to one or several sentences.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TextGlyphLine
    implements Comparable<TextGlyphLine>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextGlyphLine.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing system */
    private final SystemInfo systemInfo;

    /** The related scale */
    private final Scale scale;

    /** The line number */
    private int id;

    /** The mean baseline ordinate (in units) within the containing system */
    private Integer y;

    /** The x-ordered collection of text items */
    private final SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    /** The collection of sentences found in this text line */
    private final SortedSet<Sentence> sentences = new TreeSet<Sentence>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TextGlyphLine object.
     * @param systemInfo the containing system
     */
    public TextGlyphLine (SystemInfo systemInfo)
    {
        this.systemInfo = systemInfo;

        scale = systemInfo.getScoreSystem()
                          .getScale();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // isAlignedWith //
    //---------------//
    /**
     * Check whether a system point is roughly aligned with this line instance
     *
     * @param pixPt the system point to check
     * @param maxDy the maximum difference in pixel ordinate
     * @return true if aligned
     */
    public boolean isAlignedWith (PixelPoint pixPt,
                                  int        maxDy)
    {
        return Math.abs(pixPt.y - getY()) <= maxDy;
    }

    //-------//
    // setId //
    //-------//
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getMaxItemDy //
    //--------------//
    /**
     * Report the maximum vertical distance between a glyph candidate and a text
     * line
     * @return the interline fraction
     */
    public static Scale.Fraction getMaxItemDy ()
    {
        return constants.maxItemDy;
    }

    //---------//
    // addItem //
    //---------//
    /**
     * Add a glyph to this line
     * @param item the glyph to add
     * @return true if item did not already exist in the glyphs set
     */
    public boolean addItem (Glyph item)
    {
        // Force recomputation of line mean ordinate
        y = null;

        return glyphs.add(item);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement the y-based order
     * @param other another line
     * @return the result of the comparison
     */
    public int compareTo (TextGlyphLine other)
    {
        return Integer.signum(getY() - other.getY());
    }

    //---------------//
    // inspectGlyphs //
    //---------------//
    public void processGlyphs ()
    {
        mergeEnclosedTextGlyphs();
        includeAliens();
        retrieveSentences();
        recognizeTexts();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{TextLine #")
          .append(getId());

        sb.append(" y:");

        try {
            getY();
            sb.append(y);
        } catch (Exception ex) {
            sb.append("unknown");
        }

        sb.append(Glyph.toString(glyphs));

        return sb.toString();
    }

    //------------//
    // removeItem //
    //------------//
    void removeItem (Glyph item)
    {
        if ((glyphs.size() == 1) && (glyphs.first() == item)) {
            // Harakiri
            systemInfo.getTextLines()
                      .remove(this);
        } else {
            glyphs.remove(item);
            y = null;
        }
    }

    //----------------//
    // removeSentence //
    //----------------//
    void removeSentence (Sentence sentence)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Removing " + sentence);
        }

        sentences.remove(sentence);
    }

    //---------//
    // isAlien //
    //---------//
    private boolean isAlien (Glyph glyph)
    {
        return (glyph.getShape() == null) || !glyph.getShape()
                                                   .isText();
    }

    //-----------//
    // getAliens //
    //-----------//
    private Collection<Glyph> getAliens ()
    {
        final int               maxDy = scale.toPixels(constants.maxItemDy);
        final int               maxDx = scale.toPixels(constants.maxAlienDx);
        final Collection<Glyph> neighbors = new ArrayList<Glyph>();

        // Check alien glyphs aligned with this line
        for (Glyph glyph : systemInfo.getGlyphs()) {
            Shape shape = glyph.getShape();

            if (!glyph.isKnown() ||
                (shape == Shape.CLUTTER) ||
                (shape == Shape.DOT) ||
                (shape == Shape.STRUCTURE)) {
                // Check ordinate wrt baseline
                if (isAlignedWith(glyph.getTextInfo().getTextStart(), maxDy)) {
                    // Check abscissa wrt line items
                    PixelRectangle fatBox = new PixelRectangle(
                        glyph.getContourBox());
                    fatBox.x -= maxDx;
                    fatBox.width += (2 * maxDx);

                    for (Glyph item : glyphs) {
                        if (fatBox.intersects(item.getContourBox())) {
                            if (logger.isFineEnabled()) {
                                logger.fine("Neighbor alien #" + glyph.getId());
                            }

                            neighbors.add(glyph);

                            break;
                        }
                    }
                }
            }
        }

        return neighbors;
    }

    //---------------//
    // getFirstAfter //
    //---------------//
    private Glyph getFirstAfter (Glyph start)
    {
        boolean started = start == null;

        for (Glyph glyph : glyphs) {
            if (started) {
                return glyph;
            }

            if (glyph == start) {
                started = true;
            }
        }

        return null;
    }

    //---------------//
    // getFirstAlien //
    //---------------//
    private Glyph getFirstAlien ()
    {
        for (Glyph glyph : glyphs) {
            if (isAlien(glyph)) {
                return glyph;
            }
        }

        return null;
    }

    //---------------//
    // getLastBefore //
    //---------------//
    private Glyph getLastBefore (Glyph stop)
    {
        Glyph okGlyph = null;

        for (Glyph glyph : glyphs) {
            if (glyph == stop) {
                return okGlyph;
            }

            okGlyph = glyph;
        }

        return okGlyph;
    }

    //------//
    // getY //
    //------//
    /**
     * Report the baseline ordinate of this line
     *
     * @return the mean line ordinate, wrt the containing system
     */
    private int getY ()
    {
        if (y == null) {
            Population population = new Population();

            for (Glyph item : glyphs) {
                population.includeValue(item.getLocation().y);
            }

            if (population.getCardinality() > 0) {
                y = (int) Math.rint(population.getMeanValue());
            }
        }

        return y;
    }

    //---------------//
    // includeAliens //
    //---------------//
    /**
     * Try to extend the line (which is made of only text items so far) with
     * 'alien' non-text shape items, but which together could make text lines
     */
    private void includeAliens ()
    {
        Collection<Glyph> aliens = getAliens();

        if (aliens.isEmpty()) {
            return;
        }

        // Insert aliens in proper place in the glyphs sequence
        glyphs.addAll(aliens);

        Glyph alien;

        while ((alien = getFirstAlien()) != null) {
            if (!resolve(alien)) {
                glyphs.remove(alien);
            }
        }
    }

    //-------------------------//
    // mergeEnclosedTextGlyphs //
    //-------------------------//
    /**
     * If a text glyph is surrounded by another text glyph, make it one glyph
     */
    private void mergeEnclosedTextGlyphs ()
    {
        boolean done = false;

        while (!done) {
            done = true;

            innerLoop: 
            for (Iterator<Glyph> innerIt = glyphs.iterator();
                 innerIt.hasNext();) {
                Glyph          inner = innerIt.next();
                PixelRectangle innerBox = inner.getContourBox();

                for (Iterator<Glyph> outerIt = glyphs.iterator();
                     outerIt.hasNext();) {
                    Glyph outer = outerIt.next();

                    if (outer == inner) {
                        continue;
                    }

                    PixelRectangle outerBox = outer.getContourBox();

                    if ((outerBox.x <= innerBox.x) &&
                        ((outerBox.x + outerBox.width) >= (innerBox.x +
                                                          innerBox.width))) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "System " +
                                systemInfo.getScoreSystem().getId() +
                                " text #" + inner.getId() +
                                " enclosed in text #" + outer.getId());
                        }

                        Glyph compound = systemInfo.buildCompound(
                            Arrays.asList(outer, inner));
                        compound = systemInfo.addGlyph(compound);
                        systemInfo.computeGlyphFeatures(compound);
                        compound.setShape(Shape.TEXT);

                        addItem(compound);
                        removeItem(outer);
                        removeItem(inner);

                        done = false;

                        break innerLoop;
                    }
                }
            }
        }
    }

    //----------------//
    // recognizeTexts //
    //----------------//
    private void recognizeTexts ()
    {
        // Default language
        Score  score = systemInfo.getScoreSystem()
                                 .getScore();
        String language = score.getLanguage();

        if (language == null) {
            language = "eng";
        }

        for (Sentence sentence : sentences) {
            Glyph glyph = null;

            if (sentence.getGlyphs()
                        .size() > 1) {
                Glyph compound = systemInfo.buildCompound(sentence.getGlyphs());
                //                glyphsBuilder.addGlyph(compound);
                compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);
                glyph = sentence.getGlyphs()
                                .first()
                                .getFirstSection()
                                .getGlyph();
            } else {
                glyph = sentence.getGlyphs()
                                .first();
            }

            ///for (Glyph glyph : sentence.getGlyphs()) {
            if (glyph.getTextInfo()
                     .getContent() == null) {
                try {
                    String line = TesseractOCR.getInstance()
                                              .recognize(
                        glyph.getImage(),
                        language)
                                              .get(0);
                    logger.info("Glyph#" + glyph.getId() + "->" + line);

                    glyph.getTextInfo()
                         .setOcrContent(line);
                } catch (Exception ex) {
                    logger.warning(
                        "OCR error with glyph #" + glyph.getId(),
                        ex);
                }
            }
        }
    }

    //---------//
    // resolve //
    //---------//
    private boolean resolve (Glyph alien)
    {
        ///logger.info("Resolving alien #" + alien.getId());
        ScoreSystem system = systemInfo.getScoreSystem();

        // Going both ways, stopping at line ends
        Glyph       first = alien;
        SystemPoint firstLeft = system.toSystemPoint(first.getLocation());
        boolean     withinStaves = system.getStaffPosition(firstLeft) == StaffPosition.within;

        while (first != null) {
            Glyph last = alien;

            // Extending to the end
            while (last != null) {
                // We don't accept sentences that cross the starting bar line
                boolean crossingTheBarline = false;

                if (withinStaves) {
                    SystemPoint lastRight = system.toSystemPoint(
                        last.getLocation());

                    if (system.isLeftOfStaves(firstLeft) != system.isLeftOfStaves(
                        lastRight)) {
                        crossingTheBarline = true;
                    }
                }

                if (!crossingTheBarline) {
                    Merge merge = new Merge(first, last);

                    if (merge.isOk()) {
                        merge.insert();

                        Glyph glyph = merge.compound;

                        if (glyph.getId() == 0) {
                            glyph = systemInfo.addGlyph(glyph);
                        }

                        systemInfo.computeGlyphFeatures(glyph);
                        glyph.setShape(merge.vote.shape, merge.vote.doubt);

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Alien #" + alien.getId() + " solved from #" +
                                first.getId() + " to " + last.getId() +
                                " as #" + merge.compound.getId());
                        }

                        return true;
                    }
                }

                last = getFirstAfter(last);
            }

            // Extending to the beginning
            first = getLastBefore(first);

            if (first != null) {
                firstLeft = system.toSystemPoint(first.getLocation());
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("Could not resolve text alien #" + alien.getId());
        }

        return false;
    }

    //-------------------//
    // retrieveSentences //
    //-------------------//
    private void retrieveSentences ()
    {
        glyphsLoop: 
        for (Glyph glyph : glyphs) {
            // Look for a suitable sentence
            for (Sentence sentence : sentences) {
                if (sentence.feed(glyph)) {
                    continue glyphsLoop;
                }
            }

            // Not found, build a brand new sentence
            sentences.add(new Sentence(systemInfo, this, glyph));
        }

        if (logger.isFineEnabled()) {
            for (Sentence sentence : sentences) {
                logger.fine(sentence.toString());
            }
        }

        // Sentence type
        for (Sentence sentence : sentences) {
            sentence.getTextType();
            sentence.getTextHeight();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxItemDy = new Scale.Fraction(
            0.5,
            "Maximum vertical distance between a text line and a text item");
        Scale.Fraction maxAlienDx = new Scale.Fraction(
            20,
            "Maximum horizontal distance between an alien and a text item");
    }

    //-------//
    // Merge //
    //-------//
    private class Merge
    {
        //~ Instance fields ----------------------------------------------------

        private List<Glyph> parts = new ArrayList<Glyph>();
        private Glyph       compound;
        private Evaluation  vote;

        //~ Constructors -------------------------------------------------------

        public Merge (Glyph seed)
        {
            add(seed);
        }

        public Merge (Glyph first,
                      Glyph last)
        {
            boolean started = false;

            for (Glyph glyph : glyphs) {
                if (glyph == first) {
                    started = true;
                }

                if (started) {
                    parts.add(glyph);
                }

                if (glyph == last) {
                    break;
                }
            }
        }

        //~ Methods ------------------------------------------------------------

        public boolean isOk ()
        {
            if (parts.size() > 1) {
                compound = systemInfo.buildCompound(parts);
            } else if (parts.size() == 1) {
                compound = parts.get(0);
            } else {
                compound = null;

                return false;
            }

            Evaluator evaluator = GlyphNetwork.getInstance();

            vote = evaluator.vote(compound, GlyphInspector.getSymbolMaxDoubt());

            return (vote != null) && vote.shape.isText();
        }

        public Merge add (Glyph glyph)
        {
            parts.add(glyph);
            compound = null;

            return this;
        }

        public void insert ()
        {
            glyphs.removeAll(parts);
            glyphs.add(compound);
        }

        public Merge remove (Glyph glyph)
        {
            parts.remove(glyph);
            compound = null;

            return this;
        }
    }
}
