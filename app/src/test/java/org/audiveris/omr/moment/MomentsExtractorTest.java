/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.audiveris.omr.moment;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.SymbolSample;
import org.audiveris.omr.math.PointsCollector;
import org.audiveris.omr.moments.MomentsExtractor;
import org.audiveris.omr.moments.OrthogonalMoments;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import static org.audiveris.omr.ui.symbol.MusicFont.DEFAULT_INTERLINE;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.ui.symbol.TextSymbol;

import org.junit.Ignore;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.*;

import javax.imageio.ImageIO;

/**
 * Class <code>MomentsExtractorTest</code>
 *
 * @author Hervé Bitteur
 */
@Ignore
public class MomentsExtractorTest<D extends OrthogonalMoments<D>>
{
    Map<Shape, D> descriptors = new EnumMap<Shape, D>(Shape.class);

    File temp = new File("data/temp");

    /**
     * Creates a new MomentsExtractorTest object.
     */
    public MomentsExtractorTest ()
    {
    }

    //---------------//
    // testAllShapes //
    //---------------//
    /**
     * Use a symbol glyph as input for each shape
     */
    public void testAllShapes (MomentsExtractor<D> extractor,
                               Class<? extends D> classe)
        throws InstantiationException, IllegalAccessException
    {
        temp.mkdirs();

        // Retrieve descriptor for each physical shape
        final MusicFont font = MusicFont.getBaseFont(MusicFamily.Bravura, DEFAULT_INTERLINE);
        final TextFont textFont = TextFont.getBaseFont(TextFamily.Serif, DEFAULT_INTERLINE);

        MusicFont.populateAllSymbols();

        for (Shape shape : ShapeSet.allPhysicalShapes) {
            ShapeSymbol symbol = font.getSymbol(shape);

            if (symbol != null) {
                try {
                    System.out.println("shape:" + shape);

                    final SymbolSample sample = (symbol instanceof TextSymbol textSymbol)
                            ? SymbolSample.create(shape, textSymbol, textFont, DEFAULT_INTERLINE)
                            : SymbolSample.create(shape, symbol, font, DEFAULT_INTERLINE);

                    PointsCollector collector = new PointsCollector(null, sample.getWeight());
                    sample.getRunTable().cumulate(collector, null);

                    D descriptor = classe.getDeclaredConstructor().newInstance();
                    extractor.setDescriptor(descriptor);
                    extractor.extract(
                            collector.getXValues(),
                            collector.getYValues(),
                            collector.getSize());
                    descriptors.put(shape, descriptor);

                    // Reconstruct
                    ///reconstruct(shape, extractor);
                } catch (Exception ex) {
                    System.out.println("Error processing symbol " + symbol + " " + ex);
                }
            } else {
                System.out.println(shape + " no symbol");
            }
        }

        // Print moments per shape
        printMoments();

        // Print inter-shape distances
        printRelations();
    }

    //--------------//
    // printMoments //
    //--------------//
    private void printMoments ()
    {
        // Print moments per shape
        for (Map.Entry<Shape, D> entry : descriptors.entrySet()) {
            System.out.println(
                    String.format(
                            "%-30s %s",
                            entry.getKey().toString(),
                            entry.getValue().toString()));
        }

        System.out.println();
    }

    //----------------//
    // printRelations //
    //----------------//
    private void printRelations ()
    {
        List<ShapeRelations> allRelations = new ArrayList<ShapeRelations>();

        for (Map.Entry<Shape, D> entry : descriptors.entrySet()) {
            Shape shape = entry.getKey();
            List<Relation> relations = new ArrayList<Relation>();

            for (Map.Entry<Shape, D> e : descriptors.entrySet()) {
                Shape s = e.getKey();

                if (s == shape) {
                    continue;
                }

                OrthogonalMoments d = e.getValue();
                relations.add(new Relation(shape, s));
            }

            // Sort by increasing distance
            Collections.sort(relations, new Comparator<Relation>()
            {
                @Override
                public int compare (Relation r1,
                                    Relation r2)
                {
                    return Double.compare(r1.distance, r2.distance);
                }
            });

            allRelations.add(new ShapeRelations(shape, relations));
        }

        // Sort by increasing distance
        Collections.sort(allRelations, new Comparator<ShapeRelations>()
        {
            @Override
            public int compare (ShapeRelations o1,
                                ShapeRelations o2)
            {
                return Double.compare(o1.relations.get(0).distance, o2.relations.get(0).distance);
            }
        });

        for (ShapeRelations shapeRelations : allRelations) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < 5; i++) {
                Relation rel = shapeRelations.relations.get(i);
                sb.append(" ").append(rel);
            }

            System.out.println(String.format("%30s =>%s", shapeRelations.shape.toString(), sb));
        }
    }

    //-------------//
    // reconstruct //
    //-------------//
    private void reconstruct (Shape shape,
                              MomentsExtractor<D> extractor)
    {
        int size = 200;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();

        extractor.reconstruct(raster);

        try {
            ImageIO.write(img, "png", new File(temp, shape + ".png"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //----------//
    // Relation //
    //----------//
    private class Relation
    {
        final Shape from;

        final Shape to;

        final double distance;

        Relation (Shape from,
                  Shape to)
        {
            this.from = from;
            this.to = to;
            distance = descriptors.get(from).distanceTo(descriptors.get(to));
        }

        @Override
        public String toString ()
        {
            return String.format(Locale.US, "%30s %5.3f ", to.toString(), distance);
        }
    }

    //----------------//
    // ShapeRelations //
    //----------------//
    private class ShapeRelations
    {
        final Shape shape;

        final List<Relation> relations; // Sorted

        ShapeRelations (Shape shape,
                        List<Relation> relations)
        {
            this.shape = shape;
            this.relations = relations;
        }
    }
}
