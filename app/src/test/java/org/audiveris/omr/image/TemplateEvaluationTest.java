/*
 * Copyright (C) 2026 NoteLite contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.audiveris.omr.image;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.ui.symbol.MusicFamily;

import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks the exact score contract of the original binary-distance calculation. */
public class TemplateEvaluationTest
{
    @Test
    public void scoreBitsPreserveWeightsOrderClippingAndSignedDistances () throws Exception
    {
        final Constant.Double[] constants = weights();
        final double[] previous = values(constants);
        final double[][] configurations = {
                {6, 1, 4}, {0.1, 0.2, 0.3}, {0, 0, 0}, {1, 0, 0},
                {1e16, 1, 0.1}, {Double.MIN_NORMAL, Double.MIN_VALUE, 1e-300}};
        final int[] distanceValues = {ChamferDistance.VALUE_UNKNOWN, 0, 1, 2, 3, 32767};
        final double[] pointDistances = {-3, -0.0, 0, 2, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        final Random random = new Random(0x4845414453L);
        final List<PixelDistance> points = new ArrayList<>();

        for (int i = 0; i < 128; i++) {
            points.add(new PixelDistance(random.nextInt(13) - 3, random.nextInt(11) - 2,
                    pointDistances[i % pointDistances.length]));
        }

        final DistanceTable integer = new DistanceTable.Integer(11, 9, 3);
        final DistanceTable.Short shortParent = new DistanceTable.Short(15, 13, 3);
        final DistanceTable shortView = shortParent.getView(new Rectangle(2, 2, 11, 9));

        for (DistanceTable table : List.of(integer, shortView)) {
            for (int y = 0; y < table.getHeight(); y++) {
                for (int x = 0; x < table.getWidth(); x++) {
                    table.setValue(x, y, distanceValues[random.nextInt(distanceValues.length)]);
                }
            }
        }

        int comparisons = 0;

        try {
            for (double[] configuration : configurations) {
                setValues(constants, configuration);

                for (int order = 0; order < 2; order++) {
                    if (order != 0) {
                        Collections.reverse(points);
                    }

                    final Template template = template(points);
                    template.putOffset(Anchor.CENTER, 2.5, -1.5);

                    for (DistanceTable table : List.of(integer, shortView)) {
                        for (Anchor anchor : new Anchor[]{null, Anchor.CENTER}) {
                            for (int x = -4; x <= 14; x++) {
                                for (int y = -3; y <= 12; y++) {
                                    final double expected = originalScore(template, x, y, anchor,
                                            table, configuration);
                                    assertEquals(Double.doubleToRawLongBits(expected),
                                            Double.doubleToRawLongBits(
                                                    template.evaluate(x, y, anchor, table, false)));
                                    comparisons++;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            setValues(constants, previous);
        }

        assertEquals(14592, comparisons);
    }

    @Test
    public void emptyOrNeutralizedSupportKeepsMaximumValueSentinel ()
    {
        final DistanceTable table = new DistanceTable.Short(2, 2, 3);
        table.fill(ChamferDistance.VALUE_UNKNOWN);
        assertEquals(Double.MAX_VALUE, template(List.of()).evaluate(0, 0, null, table, false), 0);
        assertEquals(Double.MAX_VALUE, template(List.of(new PixelDistance(0, 0, 0)))
                .evaluate(0, 0, null, table, false), 0);
        table.fill(0);
        assertEquals(Double.MAX_VALUE, template(List.of(new PixelDistance(0, 0, 0)))
                .evaluate(Integer.MAX_VALUE, Integer.MIN_VALUE, null, table, false), 0);
    }

    @Test
    public void pointOrderRemainsObservableWithUnequalWeights () throws Exception
    {
        final Constant.Double[] constants = weights();
        final double[] previous = values(constants);

        try {
            setValues(constants, new double[]{1e16, 1, 1});
            final DistanceTable table = new DistanceTable.Short(1, 1, 3);
            table.fill(1);
            final List<PixelDistance> points = new ArrayList<>();
            points.add(new PixelDistance(0, 0, 0));

            for (int i = 0; i < 16; i++) {
                points.add(new PixelDistance(0, 0, 1));
            }

            final double forward = template(points).evaluate(0, 0, null, table, false);
            Collections.reverse(points);
            final double reverse = template(points).evaluate(0, 0, null, table, false);
            assertTrue("Grouping points or weights changes the original score bits",
                    Double.doubleToRawLongBits(forward) != Double.doubleToRawLongBits(reverse));
        } finally {
            setValues(constants, previous);
        }
    }

    @Test
    public void foregroundSlackRequiresStemAndStopsAtBoundary ()
    {
        final Template template = template(List.of(new PixelDistance(0, 0, 0, 2)));
        final DistanceTable table = new DistanceTable.Short(1, 1, 3);
        final double[] withoutStem = {0, 1, 1, 1};
        final double[] withStem = {0, 0, 0, 1};

        for (int distance = 0; distance <= 3; distance++) {
            table.fill(distance);
            assertEquals(withoutStem[distance], template.evaluate(0, 0, null, table, false), 0);
            assertEquals(withStem[distance], template.evaluate(0, 0, null, table, true), 0);
        }
    }

    @Test
    public void slackDoesNotRelaxBackgroundOrHole ()
    {
        final DistanceTable table = new DistanceTable.Short(1, 1, 3);

        for (double expectedDistance : new double[]{2, -3}) {
            final Template template = template(List.of(
                    new PixelDistance(0, 0, expectedDistance, 3)));

            for (int distance = 0; distance <= 4; distance++) {
                table.fill(distance);
                final double expected = distance == 0 ? 1 : 0;
                assertEquals(expected, template.evaluate(0, 0, null, table, false), 0);
                assertEquals(expected, template.evaluate(0, 0, null, table, true), 0);
            }
        }
    }

    @Test
    public void neutralizedForegroundIsSkippedEvenInsideSlack ()
    {
        final Template template = template(List.of(new PixelDistance(0, 0, 0, 2)));
        final DistanceTable table = new DistanceTable.Short(1, 1, 3);
        table.fill(ChamferDistance.VALUE_UNKNOWN);
        assertEquals(Double.MAX_VALUE, template.evaluate(0, 0, null, table, true), 0);
    }

    private static Template template (List<PixelDistance> points)
    {
        return new Template(Shape.NOTEHEAD_BLACK, MusicFamily.Bravura, 20, 7, 7, points,
                new Rectangle(0, 0, 7, 7), true);
    }

    /** Legacy binary score contract for nonnegative distances without stem slack. */
    private static double originalScore (Template template, int x, int y, Anchor anchor,
                                         DistanceTable table, double[] configuration)
    {
        final Point offset = anchor == null ? new Point() : template.getOffset(anchor);
        final int left = x - offset.x;
        final int top = y - offset.y;
        double total = 0;
        double weights = 0;

        for (PixelDistance point : template.getKeyPoints()) {
            final int nx = left + point.x;
            final int ny = top + point.y;

            if (nx >= 0 && nx < table.getWidth() && ny >= 0 && ny < table.getHeight()) {
                final int actualDistance = table.getValue(nx, ny);

                if (actualDistance != ChamferDistance.VALUE_UNKNOWN) {
                    final double weight = point.d == 0 ? configuration[0]
                            : point.d > 0 ? configuration[1] : configuration[2];
                    final double expected = point.d == 0 ? 0 : 1;
                    final double actual = actualDistance == 0 ? 0 : 1;
                    total += weight * Math.abs(actual - expected);
                    weights += weight;
                }
            }
        }

        return weights == 0 ? Double.MAX_VALUE : total / weights;
    }

    private static Constant.Double[] weights () throws Exception
    {
        final Field constantsField = Template.class.getDeclaredField("constants");
        constantsField.setAccessible(true);
        final Object constants = constantsField.get(null);
        final String[] names = {"foreWeight", "backWeight", "holeWeight"};
        final Constant.Double[] result = new Constant.Double[names.length];

        for (int i = 0; i < names.length; i++) {
            final Field field = constants.getClass().getDeclaredField(names[i]);
            field.setAccessible(true);
            result[i] = (Constant.Double) field.get(constants);
        }

        return result;
    }

    private static double[] values (Constant.Double[] constants)
    {
        return new double[]{constants[0].getValue(), constants[1].getValue(),
                constants[2].getValue()};
    }

    private static void setValues (Constant.Double[] constants, double[] values)
    {
        for (int i = 0; i < constants.length; i++) {
            constants[i].setValue(values[i]);
        }
    }
}
