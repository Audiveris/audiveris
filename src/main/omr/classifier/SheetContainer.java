//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t C o n t a i n e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.classifier.SheetContainer.Adapter;

import omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SheetContainer} contains descriptions of sample sheets, notably their
 * ID, their name(s) and the hash-code of their binary image if any.
 * <p>
 * Its main purpose is to avoid unnecessary loading of sheet images in memory.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "container")
public class SheetContainer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SheetContainer.class);

    /** Name of the specific entry for container. */
    public static final String CONTAINER_ENTRY_NAME = "META-INF/container.xml";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map Hash code => sheet descriptors. */
    @XmlJavaTypeAdapter(Adapter.class)
    @XmlElement(name = "sheets")
    private HashMap<Integer, List<Descriptor>> hashMap = new HashMap<Integer, List<Descriptor>>();

    /** Current maximum ID value. */
    private int maxId;

    /** True if container has been modified. */
    private boolean modified;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SheetContainer} object.
     */
    public SheetContainer ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // addDescriptor //
    //---------------//
    public void addDescriptor (Descriptor desc)
    {
        List<Descriptor> descriptors = hashMap.get(desc.hash);

        if (descriptors == null) {
            hashMap.put(desc.hash, descriptors = new ArrayList<Descriptor>());
        }

        descriptors.add(desc);
        setModified(true);
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        logger.info("SheetContainer: {}", hashMap);
    }

    //-------------------//
    // getAllDescriptors //
    //-------------------//
    public List<Descriptor> getAllDescriptors ()
    {
        List<Descriptor> all = new ArrayList<Descriptor>();

        for (List<Descriptor> descriptors : hashMap.values()) {
            all.addAll(descriptors);
        }

        Collections.sort(all);

        return all;
    }

    //---------------//
    // getDescriptor //
    //---------------//
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

    //---------------//
    // getDescriptor //
    //---------------//
    public Descriptor getDescriptor (int id)
    {
        for (List<Descriptor> descriptors : hashMap.values()) {
            for (Descriptor desc : descriptors) {
                if (desc.id == id) {
                    return desc;
                }
            }
        }

        return null;
    }

    //----------------//
    // getDescriptors //
    //----------------//
    /**
     * Report the list of sheet descriptors for a given table hash code
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

    //----------//
    // getNewId //
    //----------//
    /**
     * @return the next Id
     */
    public int getNewId ()
    {
        return ++maxId;
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

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal this instance to disk.
     *
     * @param root root path of samples system
     */
    public void marshal (Path root)
    {
        try {
            logger.debug("Marshalling {}", this);

            final Path path = root.resolve(CONTAINER_ENTRY_NAME);

            // Make sure the folder exists
            Files.createDirectories(path.getParent());

            // Container
            JAXBContext jaxbContext = JAXBContext.newInstance(SheetContainer.class);
            Jaxb.marshal(this, path, jaxbContext);
            logger.info("Stored {}", path);
        } catch (Exception ex) {
            logger.error("Error marshalling " + this + " " + ex, ex);
        }
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * @param modified the modified to set
     */
    public void setModified (boolean modified)
    {
        this.modified = modified;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('{');
        sb.append("maxId:").append(maxId);
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

            JAXBContext jaxbContext = JAXBContext.newInstance(SheetContainer.class);
            SheetContainer sheetContainer = (SheetContainer) Jaxb.unmarshal(path, jaxbContext);
            logger.info("Unmarshalled {}", sheetContainer);

            return sheetContainer;
        } catch (Exception ex) {
            logger.warn("Error unmarshalling SheetContainer " + ex, ex);

            return null;
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called immediately after unmarshalling of this object.
     * We set maxId.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        for (List<Descriptor> descriptors : hashMap.values()) {
            for (Descriptor desc : descriptors) {
                maxId = Math.max(maxId, desc.id);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static class Adapter
            extends XmlAdapter<ContainerValue, HashMap<Integer, List<Descriptor>>>
    {
        //~ Methods --------------------------------------------------------------------------------

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
            HashMap<Integer, List<Descriptor>> map = new HashMap<Integer, List<Descriptor>>();

            for (Descriptor desc : value.descriptors) {
                List<Descriptor> descriptors = map.get(desc.hash);

                if (descriptors == null) {
                    map.put(desc.hash, descriptors = new ArrayList<Descriptor>());
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
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ContainerValue
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The collection of sheet descriptors. */
        @XmlElement(name = "sheet")
        private final List<Descriptor> descriptors = new ArrayList<Descriptor>();

        //~ Constructors ---------------------------------------------------------------------------
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
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Descriptor
            implements Comparable<Descriptor>
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute(name = "id")
        public int id;

        @XmlAttribute(name = "hash")
        private Integer hash;

        @XmlAttribute(name = "name")
        private String name;

        @XmlElement(name = "alias")
        private final ArrayList<String> aliases = new ArrayList<String>();

        //~ Constructors ---------------------------------------------------------------------------
        public Descriptor (int id,
                           Integer hash,
                           String name)
        {
            this(id, hash, name, EMPTY_LIST);
        }

        public Descriptor (int id,
                           Integer hash,
                           String name,
                           List<String> aliases)
        {
            this.id = id;
            this.hash = hash;
            this.name = name;
            this.aliases.addAll(aliases);
        }

        // For JAXB
        private Descriptor ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
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
            return Integer.compare(id, other.id);
        }

        public List<String> getAliases ()
        {
            return aliases;
        }

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

        public void setName (String name)
        {
            this.name = name;
        }

        @Override
        public String toString ()
        {
            return id + "/" + ((name != null) ? name : "");
        }
    }
}
