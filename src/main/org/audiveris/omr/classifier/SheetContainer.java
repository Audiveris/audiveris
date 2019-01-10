//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t C o n t a i n e r                                  //
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.classifier.SheetContainer.Adapter;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamException;

/**
 * Class {@code SheetContainer} contains descriptions of sample sheets, notably their
 * ID, their name and alias(es) and the hash-code of their binary image if any.
 * <p>
 * Its main purpose is to avoid unnecessary loading of sheet images in memory.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "container")
public class SheetContainer
{

    private static final Logger logger = LoggerFactory.getLogger(SheetContainer.class);

    /** Name of the specific entry for container. */
    public static final String CONTAINER_ENTRY_NAME = "META-INF/container.xml";

    /** Regex pattern for unique names. */
    private static final Pattern UNIQUE_PATTERN = Pattern.compile("(.*)(_[0-9][0-9])");

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    // Persistent data
    //----------------
    /** Map (RunTable Hash code => list of sheet descriptors). */
    @XmlElement(name = "sheets")
    @XmlJavaTypeAdapter(Adapter.class)
    private HashMap<Integer, List<Descriptor>> hashMap = new HashMap<>();

    // Transient data
    //---------------
    /** True if container has been modified. */
    private boolean modified;

    /** Descriptors to be deleted from disk. */
    private Set<Descriptor> defunctDescriptors = new LinkedHashSet<>();

    /**
     * Creates a new {@code SheetContainer} object. Needed for JAXB.
     */
    public SheetContainer ()
    {
    }

    //---------------//
    // addDescriptor //
    //---------------//
    /**
     * Add a new descriptor to this container.
     *
     * @param desc the descriptor to add
     */
    public void addDescriptor (Descriptor desc)
    {
        List<Descriptor> descriptors = hashMap.get(desc.hash);

        if (descriptors == null) {
            hashMap.put(desc.hash, descriptors = new ArrayList<>());
        }

        descriptors.add(desc);
        setModified(true);
    }

    //------//
    // dump //
    //------//
    /**
     * Dump internals of the SheetContainer.
     */
    public void dump ()
    {
        logger.info("SheetContainer: {}", hashMap);
    }

    //-------------//
    // forgeUnique //
    //-------------//
    /**
     * Forge a unique name from the provided one.
     *
     * @param name provided name
     * @return the unique String forged from provided name
     */
    public String forgeUnique (final String name)
    {
        final String radix; // Name without "_nn" suffix if any
        final Matcher matcher = UNIQUE_PATTERN.matcher(name);

        if (matcher.find()) {
            radix = matcher.group(1);
        } else {
            radix = name;
        }

        final List<String> similars = new ArrayList<>();
        boolean collided = false;

        for (List<Descriptor> descriptors : hashMap.values()) {
            for (Descriptor desc : descriptors) {
                final String descName = desc.getName();

                if (descName.startsWith(radix)) {
                    similars.add(descName);
                }

                if (descName.equals(name)) {
                    collided = true;
                }
            }
        }

        if (!collided) {
            return name;
        }

        for (int i = 1; i < 100; i++) {
            String newName = String.format("%s_%02d", radix, i);

            if (!similars.contains(newName)) {
                return newName;
            }
        }

        logger.warn("No unique name could be forged for {}", name);

        return null; // Very unlikely!
    }

    //-------------------//
    // getAllDescriptors //
    //-------------------//
    /**
     * Report all the registered descriptors.
     *
     * @return all known descriptors
     */
    public List<Descriptor> getAllDescriptors ()
    {
        List<Descriptor> all = new ArrayList<>();

        for (List<Descriptor> descriptors : hashMap.values()) {
            all.addAll(descriptors);
        }

        Collections.sort(all);

        return all;
    }

    //---------------//
    // getDescriptor //
    //---------------//
    /**
     * Retrieve a descriptor by its unique sheet name.
     *
     * @param name unique sheet name
     * @return the descriptor found or null
     */
    public Descriptor getDescriptor (String name)
    {
        for (List<Descriptor> descriptors : hashMap.values()) {
            for (Descriptor desc : descriptors) {
                if (desc.isAlias(name)) {
                    return desc;
                }
            }
        }

        return null;
    }

    //--------------------//
    // getDescriptorCount //
    //--------------------//
    /**
     * Report the number of sheet descriptors.
     *
     * @return number of sheet descriptors (= number of sample sheets)
     */
    public int getDescriptorCount ()
    {
        int count = 0;

        for (List<Descriptor> descriptors : hashMap.values()) {
            count += descriptors.size();
        }

        return count;
    }

    //----------------//
    // getDescriptors //
    //----------------//
    /**
     * Report the list of sheet descriptors that match a given table hash code.
     *
     * @param hash hash code of run table
     * @return the sheet descriptors for same hash
     */
    public List<Descriptor> getDescriptors (int hash)
    {
        List<Descriptor> list = hashMap.get(hash);

        if (list == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(list);
    }

    //------------//
    // isModified //
    //------------//
    /**
     * @return the modified
     */
    public boolean isModified ()
    {
        return modified;
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Set the modified status of this container.
     *
     * @param modified the modified to set
     */
    public void setModified (boolean modified)
    {
        this.modified = modified;
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal this instance to disk.
     *
     * @param samplesRoot path of samples system
     * @param imagesRoot  path of images system
     */
    public void marshal (Path samplesRoot,
                         Path imagesRoot)
    {
        try {
            logger.debug("Marshalling {}", this);

            final Path path = samplesRoot.resolve(CONTAINER_ENTRY_NAME);

            // Make sure the folder exists
            Files.createDirectories(path.getParent());

            // Container
            Jaxb.marshal(this, path, getJaxbContext());
            logger.info("Stored {}", path);

            // Remove defunct sheets if any
            for (Descriptor descriptor : defunctDescriptors) {
                SampleSheet.delete(descriptor, samplesRoot, imagesRoot);
            }

            defunctDescriptors.clear();

            setModified(false);
        } catch (IOException |
                 JAXBException |
                 XMLStreamException ex) {
            logger.error("Error marshalling " + this + " " + ex, ex);
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(SheetContainer.class);
        }

        return jaxbContext;
    }

    //------------------//
    // removeDescriptor //
    //------------------//
    /**
     * Remove a descriptor.
     *
     * @param desc the descriptor to remove
     */
    public void removeDescriptor (Descriptor desc)
    {
        List<Descriptor> descriptors = hashMap.get(desc.hash);

        descriptors.remove(desc);

        if (descriptors.isEmpty()) {
            hashMap.remove(desc.hash);
        }

        defunctDescriptors.add(desc);

        setModified(true);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('{');
        sb.append("hashes:").append(hashMap.size());
        sb.append('}');

        return sb.toString();
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the SheetContainer instance from disk.
     *
     * @param root root path of samples system
     * @return the unmarshalled instance, null if exception
     */
    public static SheetContainer unmarshal (Path root)
    {
        try {
            final Path path = root.resolve(CONTAINER_ENTRY_NAME);
            logger.debug("SheetContainer unmarshalling {}", path);

            SheetContainer sheetContainer = (SheetContainer) Jaxb.unmarshal(path, getJaxbContext());
            logger.info("Unmarshalled {}", sheetContainer);

            return sheetContainer;
        } catch (IOException |
                 JAXBException ex) {
            logger.warn("Error unmarshalling SheetContainer " + ex, ex);

            return null;
        }
    }

    //---------//
    // Adapter //
    //---------//
    /**
     * JAXB adapter to support a HashMap.
     */
    public static class Adapter
            extends XmlAdapter<ContainerValue, HashMap<Integer, List<Descriptor>>>
    {

        @Override
        public ContainerValue marshal (HashMap<Integer, List<Descriptor>> map)
                throws Exception
        {
            return new ContainerValue(map);
        }

        @Override
        public HashMap<Integer, List<Descriptor>> unmarshal (ContainerValue value)
                throws Exception
        {
            HashMap<Integer, List<Descriptor>> map = new HashMap<>();

            for (Descriptor desc : value.descriptors) {
                List<Descriptor> descriptors = map.get(desc.hash);

                if (descriptors == null) {
                    map.put(desc.hash, descriptors = new ArrayList<>());
                }

                if (!descriptors.contains(desc)) {
                    descriptors.add(desc);
                }
            }

            return map;
        }
    }

    //----------------//
    // ContainerValue //
    //----------------//
    /**
     * A flat list of descriptors to (un)marshal descriptors map.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ContainerValue
    {

        /** The collection of sheet descriptors. */
        @XmlElement(name = "sheet")
        private final List<Descriptor> descriptors = new ArrayList<>();

        /**
         * Populate the flat list.
         *
         * @param map the map of descriptors.
         */
        public ContainerValue (HashMap<Integer, List<Descriptor>> map)
        {
            for (List<Descriptor> list : map.values()) {
                descriptors.addAll(list);
            }

            Collections.sort(descriptors);
        }

        private ContainerValue ()
        {
        }
    }

    //------------//
    // Descriptor //
    //------------//
    /**
     * A descriptor for a sample sheet.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Descriptor
            implements Comparable<Descriptor>
    {

        /** Short unique sheet name. */
        @XmlAttribute(name = "name")
        private String name;

        /** Hash value of binary RunTable, if any. */
        @XmlAttribute(name = "hash")
        private Integer hash;

        /** Collection of all name aliases, perhaps empty. */
        @XmlElement(name = "alias")
        private final ArrayList<String> aliases = new ArrayList<>();

        /**
         * Create descriptor for a sample sheet.
         *
         * @param name sheet name
         * @param hash hash code of related image run table or null
         */
        public Descriptor (String name,
                           Integer hash)
        {
            this(name, hash, EMPTY_LIST);
        }

        /**
         * Create descriptor for a sample sheet and its aliases.
         *
         * @param name    sheet name
         * @param hash    hash code of related image run table or null
         * @param aliases other names for the same sheet
         */
        public Descriptor (String name,
                           Integer hash,
                           List<String> aliases)
        {
            this.hash = hash;
            this.name = name;
            this.aliases.addAll(aliases);
        }

        // For JAXB
        private Descriptor ()
        {
        }

        /**
         * Register an alias for the sheet.
         *
         * @param alias new alias
         */
        public void addAlias (String alias)
        {
            if ((alias != null) && !isAlias(alias)) {
                logger.info("Added alias {} to {}", alias, this);
                aliases.add(alias);
            }
        }

        @Override
        public int compareTo (Descriptor other)
        {
            return name.compareTo(other.name);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Descriptor) {
                return compareTo((Descriptor) obj) == 0;
            }

            return false;
        }

        /**
         * Report the collection of aliases.
         *
         * @return the sheet aliases, perhaps empty.
         */
        public List<String> getAliases ()
        {
            return aliases;
        }

        /**
         * Report concatenated aliases.
         *
         * @return string of aliases
         */
        public String getAliasesString ()
        {
            if (aliases.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();

            for (String alias : aliases) {
                if (sb.length() > 0) {
                    sb.append(",");
                }

                sb.append(alias);
            }

            return sb.toString();
        }

        /**
         * Report the sheet name.
         *
         * @return sheet name
         */
        public String getName ()
        {
            return name;
        }

        /**
         * Set sheet name.
         *
         * @param name name for sheet
         */
        public void setName (String name)
        {
            this.name = name;
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = (59 * hash) + Objects.hashCode(this.name);

            return hash;
        }

        /**
         * Check whether the provided name is an alias of this sheet.
         *
         * @param str provided name
         * @return true if so
         */
        public boolean isAlias (String str)
        {
            if (str.equalsIgnoreCase(name)) {
                return true;
            }

            for (String nm : aliases) {
                if (str.equalsIgnoreCase(nm)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String toString ()
        {
            return name;
        }
    }
}
