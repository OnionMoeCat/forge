package forge.gui.framework;

/** 
 * This interface provides a unifying type for all top-level
 * UI components.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public interface IVTopLevelUI {
    /** Called during the preload sequence, this method caches
     * all of the children singletons and instances. */
    void instantiate();

    /**
     * Removes all children and (re)populates top level content,
     * independent of constructor.  Expected to provide
     * a completely fresh layout on the component.
     * 
     */
    void populate();
}
