//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h a p e S h o r t c u t s                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.util.Jaxb;
import static org.audiveris.omr.util.UriUtil.toURI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;

/**
 * Class <code>ShapeShortcuts</code> manages keyboard shortcuts for shape assignments.
 * <p>
 * Shortcuts are 2-character sequences: the first character selects a shape set (family),
 * the second character selects a specific shape within that set.
 * <p>
 * Default shortcuts are loaded from system resource {@code shape-shortcuts.xml}.
 * User can override them by placing a custom {@code shape-shortcuts.xml} in the config folder.
 *
 * @author Audiveris
 */
public abstract class ShapeShortcuts
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ShapeShortcuts.class);

    /** Name of the configuration file. */
    private static final String FILE_NAME = "shape-shortcuts.xml";

    /** JAXB context for unmarshalling/marshalling. */
    private static volatile JAXBContext jaxbContext;

    /** Map first typed char to selected shape set. */
    private static final Map<Character, ShapeSet> setMap = new HashMap<>();

    /** Reverse of setMap. */
    private static final Map<ShapeSet, Character> reverseSetMap = new HashMap<>();

    /** Map 2-char typed string to selected shape. */
    private static final Map<String, Shape> shapeMap = new HashMap<>();

    /** Reverse of shapeMap. */
    private static final Map<Shape, String> reverseShapeMap = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /** Not meant to be instantiated. */
    private ShapeShortcuts ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // getJaxbContext //
    //--------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Local copy to avoid race
        JAXBContext ctx = jaxbContext;

        if (ctx == null) {
            synchronized (ShapeShortcuts.class) {
                ctx = jaxbContext;

                if (ctx == null) {
                    jaxbContext = ctx = JAXBContext.newInstance(ShortcutList.class);
                }
            }
        }

        return ctx;
    }

    //---------//
    // getSetMap //
    //---------//
    /**
     * Report the map from initial character to shape set.
     *
     * @return the setMap
     */
    public static Map<Character, ShapeSet> getSetMap ()
    {
        return setMap;
    }

    //---------//
    // getReverseSetMap //
    //---------//
    /**
     * Report the reverse map from shape set to initial character.
     *
     * @return the reverseSetMap
     */
    public static Map<ShapeSet, Character> getReverseSetMap ()
    {
        return reverseSetMap;
    }

    //---------//
    // getShapeMap //
    //---------//
    /**
     * Report the map from 2-char string to shape.
     *
     * @return the shapeMap
     */
    public static Map<String, Shape> getShapeMap ()
    {
        return shapeMap;
    }

    //---------//
    // getReverseShapeMap //
    //---------//
    /**
     * Report the reverse map from shape to 2-char string.
     *
     * @return the reverseShapeMap
     */
    public static Map<Shape, String> getReverseShapeMap ()
    {
        return reverseShapeMap;
    }

    //------------------------//
    // loadAllConfigurations //
    //------------------------//
    /**
     * Load shortcuts from system resource, then optionally override with user config.
     */
    public static void loadAllConfigurations ()
    {
        // System configuration (mandatory)
        final URI systemUri = toURI(WellKnowns.RES_URI, FILE_NAME);

        // User configuration (optional override)
        final Path userPath = WellKnowns.CONFIG_FOLDER.resolve(FILE_NAME);

        // Load system defaults first
        try {
            loadConfiguration(systemUri);
            logger.debug("Loaded system shape shortcuts from {}", systemUri);
        } catch (Exception ex) {
            logger.error("Failed to load system shape shortcuts from {}", systemUri, ex);
        }

        // Then load user overrides if present
        if (Files.exists(userPath)) {
            try {
                loadConfiguration(userPath.toUri());
                logger.info("Loaded user shape shortcuts from {}", userPath);
            } catch (Exception ex) {
                logger.warn("Failed to load user shape shortcuts from {}", userPath, ex);
            }
        }

        // Build reverse maps
        buildReverseMaps();
    }

    //---------//
    // loadConfiguration //
    //---------//
    private static void loadConfiguration (URI uri)
            throws IOException, JAXBException
    {
        try (InputStream is = uri.toURL().openStream()) {
            final ShortcutList list = (ShortcutList) Jaxb.unmarshal(is, getJaxbContext());

            if (list != null && list.sets != null) {
                for (SetEntry setEntry : list.sets) {
                    final ShapeSet shapeSet = ShapeSet.getShapeSet(setEntry.name);

                    if (shapeSet == null) {
                        logger.warn("Unknown shape set: {}", setEntry.name);
                        continue;
                    }

                    final char setChar = setEntry.key.charAt(0);
                    setMap.put(setChar, shapeSet);

                    if (setEntry.shapes != null) {
                        for (ShapeEntry shapeEntry : setEntry.shapes) {
                            try {
                                final Shape shape = Shape.valueOf(shapeEntry.name);
                                final String twoChar = "" + setChar + shapeEntry.key.charAt(0);
                                shapeMap.put(twoChar, shape);
                            } catch (IllegalArgumentException ex) {
                                logger.warn("Unknown shape: {}", shapeEntry.name);
                            }
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // buildReverseMaps //
    //------------------//
    private static void buildReverseMaps ()
    {
        reverseSetMap.clear();

        for (Entry<Character, ShapeSet> entry : setMap.entrySet()) {
            reverseSetMap.put(entry.getValue(), entry.getKey());
        }

        reverseShapeMap.clear();

        for (Entry<String, Shape> entry : shapeMap.entrySet()) {
            reverseShapeMap.put(entry.getValue(), entry.getKey());
        }
    }

    //---------------------//
    // saveUserShortcuts //
    //---------------------//
    /**
     * Save the current shortcut configuration to user config folder.
     */
    public static void saveUserShortcuts ()
    {
        final Path userPath = WellKnowns.CONFIG_FOLDER.resolve(FILE_NAME);

        try {
            final ShortcutList list = buildShortcutList();
            Jaxb.marshal(list, userPath, getJaxbContext());
            logger.info("Saved user shape shortcuts to {}", userPath);
        } catch (IOException | JAXBException | XMLStreamException ex) {
            logger.warn("Failed to save user shape shortcuts to {}", userPath, ex);
        }
    }

    //--------------------//
    // buildShortcutList //
    //--------------------//
    private static ShortcutList buildShortcutList ()
    {
        final ShortcutList list = new ShortcutList();
        list.sets = new ArrayList<>();

        for (Entry<Character, ShapeSet> setEntry : setMap.entrySet()) {
            final char setChar = setEntry.getKey();
            final ShapeSet shapeSet = setEntry.getValue();
            final SetEntry se = new SetEntry();
            se.name = shapeSet.getName();
            se.key = String.valueOf(setChar);
            se.shapes = new ArrayList<>();

            // Collect all shapes that belong to this set
            for (Entry<String, Shape> shapeEntry : shapeMap.entrySet()) {
                final String twoChar = shapeEntry.getKey();

                if (twoChar.charAt(0) == setChar) {
                    final ShapeEntry she = new ShapeEntry();
                    she.name = shapeEntry.getValue().name();
                    she.key = String.valueOf(twoChar.charAt(1));
                    se.shapes.add(she);
                }
            }

            list.sets.add(se);
        }

        return list;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------------//
    // ShortcutList //
    //--------------//
    /**
     * Root element for the shape-shortcuts XML file.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "shape-shortcuts")
    public static class ShortcutList
    {
        @XmlElement(name = "set")
        List<SetEntry> sets;
    }

    //----------//
    // SetEntry //
    //----------//
    /**
     * A shape set with its shortcut key and contained shape entries.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class SetEntry
    {
        /** Name of the ShapeSet (e.g., "Accidentals"). */
        @XmlAttribute(name = "name")
        String name;

        /** Single-char key to select this set. */
        @XmlAttribute(name = "key")
        String key;

        /** Shape entries within this set. */
        @XmlElement(name = "shape")
        List<ShapeEntry> shapes;
    }

    //------------//
    // ShapeEntry //
    //------------//
    /**
     * A single shape with its shortcut key.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class ShapeEntry
    {
        /** Name of the Shape enum constant (e.g., "FLAT"). */
        @XmlAttribute(name = "name")
        String name;

        /** Single-char key to select this shape within its set. */
        @XmlAttribute(name = "key")
        String key;
    }
}
