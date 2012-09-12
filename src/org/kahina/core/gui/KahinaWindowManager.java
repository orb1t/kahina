package org.kahina.core.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.kahina.core.KahinaException;
import org.kahina.core.KahinaInstance;
import org.kahina.core.control.KahinaController;
import org.kahina.core.control.KahinaEvent;
import org.kahina.core.control.KahinaEventTypes;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.gui.event.KahinaPerspectiveEvent;
import org.kahina.core.gui.event.KahinaWindowEvent;
import org.kahina.core.gui.event.KahinaWindowEventType;
import org.kahina.core.gui.windows.KahinaDefaultWindow;
import org.kahina.core.gui.windows.KahinaDummyWindow;
import org.kahina.core.gui.windows.KahinaHorizontallySplitWindow;
import org.kahina.core.gui.windows.KahinaListWindow;
import org.kahina.core.gui.windows.KahinaMainWindow;
import org.kahina.core.gui.windows.KahinaTabbedWindow;
import org.kahina.core.gui.windows.KahinaVerticallySplitWindow;
import org.kahina.core.gui.windows.KahinaWindow;
import org.kahina.core.gui.windows.KahinaWindowType;
import org.kahina.core.io.util.XMLUtil;
import org.kahina.core.visual.KahinaEmptyView;
import org.kahina.core.visual.KahinaView;
import org.w3c.dom.Node;

public class KahinaWindowManager implements KahinaListener
{
    private static final boolean VERBOSE = false;
    
    protected final KahinaInstance<?, ?, ?, ?> kahina; 

    public KahinaMainWindow mainWindow;
    private KahinaPerspective psp;

    // store recent perspectives and cache default perspectives
    public List<KahinaPerspective> recentPerspectives;
    public List<KahinaPerspective> defaultPerspectives;

    // main registry for windows: access windows by their windowID
    private HashMap<Integer, KahinaWindow> windowByID;

    /**
     * Create a simple window manager..
     * 
     * @param kahina - the KahinaInstance bundling the other parts of the system
     * @param standaloneMode - true for a simple standalone application
     */
    public KahinaWindowManager(KahinaInstance<?, ?, ?, ?> kahina, boolean standaloneMode)
    {
    	this.kahina = kahina;
        getGuiControl().registerListener(KahinaEventTypes.WINDOW, this);
        setPerspective(new KahinaPerspective("default", "default"));
        this.windowByID = new HashMap<Integer, KahinaWindow>();
        
        if (!standaloneMode)
        {
        	getGuiControl().registerListener(KahinaEventTypes.PERSPECTIVE, this);
        	recentPerspectives = new LinkedList<KahinaPerspective>();
            // load the default perspectives in the bin folder of the respective KahinaGUI instance
            defaultPerspectives = new LinkedList<KahinaPerspective>();
            // This filter only returns XML files
            FileFilter fileFilter = new FileFilter()
            {
                public boolean accept(File file)
                {
                    // System.err.println("Filtering file " + file.getName() + ": "
                    // + file.getName().endsWith("xml"));
                    return file.getName().endsWith("xml");
                }
            };
            File[] files = new File(kahina.getClass().getResource("./gui").getFile()).listFiles(fileFilter);
            for (File f : files)
            {
                if (VERBOSE)
                {
                    System.err.println("Loading default perspective: " + f.getAbsolutePath());
                }
                defaultPerspectives.add(loadPerspective(f));
            }
        }
    }

    /**
     * Builds the windows according to some perspective. Must be called before
     * first display.
     */
    public void createWindows(KahinaPerspective psp)
    {
        registerRecentPerspective(psp);
        this.setPerspective(psp);

        if (VERBOSE) System.err.println("KahinaWindowManager is creating windows ...");
        // first create a window stub for all the windows mentioned in the
        // arrangement...
        for (int winID : getArrangement().getAllWindows())
        {
            if (VERBOSE) System.err.println("winID" + winID + ":");
            switch (getArrangement().getWindowType(winID))
            {
                case KahinaWindowType.DEFAULT_WINDOW:
                {
                    String binding = getArrangement().getBindingForWinID(winID);
                    KahinaView<?> view = kahina.gui.varNameToView.get(binding);
                    if (view == null)
                    {
                        System.err.println("  WARNING: No view defined for binding \"" + binding + "\"");
                        System.err.println("  Could not apply the perspective, aborting.");
                        return;
                    }
                    if (VERBOSE)
                    {
                        System.err.println("  Generating default view " + winID + " for binding " + binding + " (primary window: " + getArrangement().getPrimaryWinIDForName(binding) + ")");
                    }
                    view.setTitle(getArrangement().getTitle(winID));
                    KahinaWindow viewWindow = new KahinaDefaultWindow(view, this, kahina, winID);
                    viewWindow.setBorder(getArrangement().hasBorder(winID));
                    viewWindow.setScrollable(getArrangement().isScrollable(winID));
                    break;
                }
                case KahinaWindowType.CONTROL_WINDOW:
                {
                    String binding = getArrangement().getBindingForWinID(winID);
                    KahinaControlButtonWindow controlWindow = new KahinaControlButtonWindow(this, kahina, winID);
                    if (VERBOSE)
                    {
                        System.err.println("  Generating control view " + winID + " for binding " + binding + " (primary window: " + getArrangement().getPrimaryWinIDForName(binding) + ")");
                    }
                    controlWindow.setBorder(getArrangement().hasBorder(winID));
                    controlWindow.setScrollable(getArrangement().isScrollable(winID));
                    controlWindow.setTitle(getArrangement().getTitle(winID));
                    for (KahinaControlButton button : kahina.gui.controlWindows.get(binding))
                    {
                        controlWindow.addControlButton(button);
                    }
                    controlWindow.build();
                    break;
                }
                case KahinaWindowType.MAIN_WINDOW:
                {
                    if (VERBOSE)
                    {
                        System.err.println("  Generating main window!");
                    }
                    mainWindow = createMainWindow(this, winID);
                    mainWindow.setTitle(getArrangement().getTitle(winID));
                    mainWindow.setSize(getArrangement().getWidth(winID), getArrangement().getHeight(winID));
                    mainWindow.setLocation(getArrangement().getXPos(winID), getArrangement().getYPos(winID));
                    mainWindow.setBorder(getArrangement().hasBorder(winID));
                    mainWindow.setScrollable(getArrangement().isScrollable(winID));
                    mainWindow.processProjectStatus(kahina.getProjectStatus());
                    break;
                }
                case KahinaWindowType.HORI_SPLIT_WINDOW:
                {
                    KahinaWindow viewWindow = new KahinaHorizontallySplitWindow(this, kahina, winID);
                    viewWindow.setTitle(getArrangement().getTitle(winID));
                    viewWindow.setBorder(getArrangement().hasBorder(winID));
                    viewWindow.setScrollable(getArrangement().isScrollable(winID));
                    break;
                }
                case KahinaWindowType.VERT_SPLIT_WINDOW:
                {
                    KahinaWindow viewWindow = new KahinaVerticallySplitWindow(this, kahina, winID, getArrangement().getResizeWeight(winID));
                    viewWindow.setTitle(getArrangement().getTitle(winID));
                    viewWindow.setBorder(getArrangement().hasBorder(winID));
                    viewWindow.setScrollable(getArrangement().isScrollable(winID));
                    break;
                }
                case KahinaWindowType.TABBED_WINDOW:
                {
                    KahinaWindow viewWindow = new KahinaTabbedWindow(this, kahina, winID);
                    viewWindow.setTitle(getArrangement().getTitle(winID));
                    viewWindow.setBorder(getArrangement().hasBorder(winID));
                    viewWindow.setScrollable(getArrangement().isScrollable(winID));
                    break;
                }
                case KahinaWindowType.LIST_WINDOW:
                {
                    KahinaWindow viewWindow = new KahinaListWindow(this, kahina, winID);
                    viewWindow.setTitle(getArrangement().getTitle(winID));
                    viewWindow.setBorder(getArrangement().hasBorder(winID));
                    viewWindow.setScrollable(getArrangement().isScrollable(winID));
                    break;
                }
                default:
                {
                    System.err.println("  WARNING: Could not load default window without binding!");
                    System.err.println("           The perspective might contain descriptions of snapshot clones.");
                }
            }
        }
        if (VERBOSE) System.err.println("KahinaWindowManager finished creating the window stubs.");

        // ... adapt the coordinates and process the embedding structure ...
        if (VERBOSE) System.err.println("KahinaWindowManager is adapting coordnates and processing the embedding structure.");
        for (int winID : getArrangement().getAllWindows())
        {
            KahinaWindow w = windowByID.get(winID);
            w.setSize(getArrangement().getWidth(w.getID()), getArrangement().getHeight(w.getID()));
            w.setLocation(getArrangement().getXPos(w.getID()), getArrangement().getYPos(w.getID()));
            Integer embeddingID = getArrangement().getEmbeddingWindowID(winID);
            // System.err.println("Embedding window " + winID + " into window "
            // + embeddingID);
            if (embeddingID != null && embeddingID != -1)
            {
            	if (VERBOSE)
            	{
            		System.err.println("Embedding window " + winID + " into window " + embeddingID);
            	}
                KahinaWindow embeddingWindow = windowByID.get(embeddingID);
                boolean success = embeddingWindow.addSubwindow(w);
                if (!success)
                {
                    System.err.println("  ERROR: ill-defined window arrangement directly under window " + embeddingID);
                }
            }
        }
        if (VERBOSE) System.err.println("KahinaWindowManager finished adapting coordinates and processing the embedding structure.");

        // ... flip the subwindows of composed windows if inconsistent with the coordinates ...
        for (int winID : getArrangement().getAllWindows())
        {

            switch (getArrangement().getWindowType(winID))
            {
                case KahinaWindowType.HORI_SPLIT_WINDOW:
                {
                    ((KahinaHorizontallySplitWindow) windowByID.get(winID)).flipSubwindowsIfIndicatedByCoordinates();
                    break;
                }
                case KahinaWindowType.VERT_SPLIT_WINDOW:
                {
                    ((KahinaVerticallySplitWindow) windowByID.get(winID)).flipSubwindowsIfIndicatedByCoordinates();
                    break;
                }
                // TODO: adapt this system for tabbed and list windows as well (sorting involved!)
            }
        }

        // ... and fill the content windows with the content specified by the bindings.
        if (VERBOSE) System.err.println("KahinaWindowManager is filling the content windows...");
        for (int winID : getArrangement().getContentWindowsWithoutMainWindow())
        {
            // apply configuration as defined by the perspective to the view
            if (getArrangement().getWindowType(winID) == KahinaWindowType.DEFAULT_WINDOW)
            {
                String binding = getArrangement().getBindingForWinID(winID);
                // TODO: this calls the generic setConfig()-method, instead of
                // the specific overloaded versions
                // the more specific config eclipses the one we set; we seem to
                // need reflection here as well
                // ! better: overload and check for correct types in each
                // implementation
                // TODO: allow different configurations (and therefore different
                // views) for clones
                kahina.gui.varNameToView.get(binding).setConfig(psp.getConfiguration(winID));
            }
        }
        if (VERBOSE) System.err.println("KahinaWindowManager finished filling the content windows...");
    }

    /**
     * Discards the current perspective and rebuilds the GUI according to a
     * newly provided one.
     * 
     * @param psp
     *            the perspective to be applied
     */
    public void setAndApplyPerspective(KahinaPerspective psp)
    {
        disposeAllWindows();
        windowByID.clear();
        createWindows(psp);
        displayWindows();
    }

    /**
     * Gets the current perspective, can be manipulated and reapplied.
     * 
     * @return the current perspective
     */
    public KahinaPerspective getPerspective()
    {
        return psp;
    }

    public void registerWindow(KahinaWindow window)
    {
        windowByID.put(window.getID(), window);
        getArrangement().setWindowType(window.getID(), window.getWindowType());
    }

    public KahinaWindow getWindowByID(int winID)
    {
        return windowByID.get(winID);
    }

    protected KahinaMainWindow createMainWindow(KahinaWindowManager kahinaWindowManager)
    {
        return new KahinaMainWindow(this, kahina);
    }

    protected KahinaMainWindow createMainWindow(KahinaWindowManager kahinaWindowManager, int winID)
    {
        return new KahinaMainWindow(this, kahina, winID);
    }

    public void disposeAllWindows()
    {
        // TODO: this should be done a little more carefully
        for (int windowID : getArrangement().getTopLevelWindows())
        {
            getWindowByID(windowID).dispose();
        }
        mainWindow.dispose();
    }

    public boolean isTopLevelWindow(KahinaWindow w)
    {
        return (getArrangement().getEmbeddingWindowID(w.getID()) == -1);
    }

    public KahinaWindow integrateInDefaultWindow(KahinaView<?> view)
    {
        KahinaWindow viewWindow = new KahinaDefaultWindow(view, this, kahina);
        viewWindow.setTitle(view.getTitle());
        getPerspective().arr.setEmbeddingWindowID(viewWindow.getID(), -1);
        registerWindow(viewWindow);
        return viewWindow;
    }

    public KahinaWindow integrateInVerticallySplitWindow(int window1ID, int window2ID, String newTitle, KahinaController control)
    {
        KahinaWindow wrapperWindow1 = windowByID.get(window1ID);
        if (wrapperWindow1 == null)
        {
            wrapperWindow1 = new KahinaDefaultWindow(new KahinaEmptyView(kahina), this, kahina);
            System.err.println("WARNING: split window could not access window \"" + window1ID + "\"");
        }
        KahinaWindow wrapperWindow2 = windowByID.get(window2ID);
        if (wrapperWindow2 == null)
        {
            wrapperWindow2 = new KahinaDefaultWindow(new KahinaEmptyView(kahina), this, kahina);
            System.err.println("WARNING: split window could not access window \"" + window1ID + "\"");
        }
        KahinaVerticallySplitWindow splitWindow = new KahinaVerticallySplitWindow(this, kahina, .5);
        splitWindow.setTitle(newTitle);
        splitWindow.setUpperWindow(wrapperWindow1);
        splitWindow.setLowerWindow(wrapperWindow2);
        splitWindow.setSize(300, 250);
        splitWindow.setLocation(200, 200);
        splitWindow.setVisible(true);
        registerWindow(splitWindow);
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window1ID));
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window2ID));
        return splitWindow;
    }

    public KahinaWindow integrateInHorizontallySplitWindow(int window1ID, int window2ID, String newTitle, KahinaController control)
    {
        KahinaWindow wrapperWindow1 = windowByID.get(window1ID);
        if (wrapperWindow1 == null)
        {
            wrapperWindow1 = new KahinaDefaultWindow(new KahinaEmptyView(kahina), this, kahina);
            System.err.println("WARNING: split window could not access window \"" + window1ID + "\"");
        }
        KahinaWindow wrapperWindow2 = windowByID.get(window2ID);
        if (wrapperWindow2 == null)
        {
            wrapperWindow2 = new KahinaDefaultWindow(new KahinaEmptyView(kahina), this, kahina);
            System.err.println("WARNING: split window could not access window \"" + window2ID + "\"");
        }
        KahinaHorizontallySplitWindow splitWindow = new KahinaHorizontallySplitWindow(this, kahina);
        splitWindow.setTitle(newTitle);
        splitWindow.setLeftWindow(wrapperWindow1);
        splitWindow.setRightWindow(wrapperWindow2);
        splitWindow.setSize(600, 150);
        splitWindow.setLocation(200, 200);
        splitWindow.setVisible(true);
        registerWindow(splitWindow);
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window1ID));
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window2ID));
        return splitWindow;
    }

    public void displayWindows()
    {
        if (getPerspective() == null)
        {
            System.err.println("No perspective defined, createWindows() was probably not called.");
            System.err.println("A default perspective can be generated via KahinaPerspective.generateDefaultPerspective()");
            System.err.println("Unable to display windows, quitting.");
            System.exit(1);
        }
        else
        {
            if (mainWindow != null) displayWindow(mainWindow);
            for (int winID : getArrangement().getTopLevelWindows())
            {
                displayWindow(getWindowByID(winID));
            }
        }
    }

    private void displayWindow(final JFrame window)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                window.setVisible(true);
                window.repaint();
            }

        });
    }

    @Override
    public void processEvent(KahinaEvent e)
    {
        if (e instanceof KahinaPerspectiveEvent)
        {
            processPerspectiveEvent((KahinaPerspectiveEvent) e);
        }
        else if (e instanceof KahinaWindowEvent)
        {
            processWindowEvent((KahinaWindowEvent) e);
        }
    }

    private void processPerspectiveEvent(KahinaPerspectiveEvent e)
    {
        int type = e.getPerspectiveEventType();
        if (type == KahinaPerspectiveEvent.SAVE_PERSPECTIVE)
        {
            savePerspectiveAs(e.getFile());
        }
        else if (type == KahinaPerspectiveEvent.LOAD_PERSPECTIVE)
        {
            setAndApplyPerspective(loadPerspective(e.getFile()));
        }
        else if (type == KahinaPerspectiveEvent.LOAD_RECENT_PERSPECTIVE)
        {
            setAndApplyPerspective(recentPerspectives.get(e.getID()).copy());
        }
        else if (type == KahinaPerspectiveEvent.LOAD_DEFAULT_PERSPECTIVE)
        {
            setAndApplyPerspective(defaultPerspectives.get(e.getID()).copy());
        }
    }

    private void processWindowEvent(KahinaWindowEvent e)
    {
        int type = e.getWindowEventType();
        if (type == KahinaWindowEventType.NEW_DEFAULT)
        {
            KahinaWindow viewWindow = new KahinaDummyWindow(this, kahina);
            viewWindow.setTitle(e.getStringContent());
            viewWindow.setSize(300, 100);
            viewWindow.setLocation(200, 200);
            viewWindow.setVisible(true);
            registerWindow(viewWindow);
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, viewWindow.getID()));
        }
        else if (type == KahinaWindowEventType.NEW_HORI_SPLIT)
        {
            KahinaHorizontallySplitWindow splitWindow = new KahinaHorizontallySplitWindow(this, kahina);
            splitWindow.setTitle(e.getStringContent());
            splitWindow.setLeftWindow(new KahinaDummyWindow(this, kahina));
            splitWindow.setRightWindow(new KahinaDummyWindow(this, kahina));
            splitWindow.setSize(600, 150);
            splitWindow.setLocation(200, 200);
            splitWindow.setVisible(true);
            registerWindow(splitWindow);
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, splitWindow.getID()));
        }
        else if (type == KahinaWindowEventType.NEW_VERT_SPLIT)
        {
            KahinaVerticallySplitWindow splitWindow = new KahinaVerticallySplitWindow(this, kahina, .5);
            splitWindow.setTitle(e.getStringContent());
            splitWindow.setUpperWindow(new KahinaDummyWindow(this, kahina));
            splitWindow.setLowerWindow(new KahinaDummyWindow(this, kahina));
            splitWindow.setSize(300, 250);
            splitWindow.setLocation(200, 200);
            splitWindow.setVisible(true);
            registerWindow(splitWindow);
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, splitWindow.getID()));
        }
        else if (type == KahinaWindowEventType.NEW_TABBED)
        {
            KahinaTabbedWindow tabbedWindow = new KahinaTabbedWindow(this, kahina);
            tabbedWindow.setTitle(e.getStringContent());
            tabbedWindow.addSubwindow(new KahinaDummyWindow(this, kahina));
            tabbedWindow.setSize(300, 250);
            tabbedWindow.setLocation(200, 200);
            tabbedWindow.setVisible(true);
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, tabbedWindow.getID()));
        }
        else if (type == KahinaWindowEventType.NEW_LIST)
        {
            KahinaListWindow listWindow = new KahinaListWindow(this, kahina);
            listWindow.setTitle(e.getStringContent());
            listWindow.addSubwindow(new KahinaDummyWindow(this, kahina));
            listWindow.setSize(300, 250);
            listWindow.setLocation(200, 200);
            listWindow.setVisible(true);
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, listWindow.getID()));
        }
        else if (type == KahinaWindowEventType.TOGGLE_VISIBLE)
        {
            if (getArrangement().getEmbeddingWindowID(e.getWindowID()) == -1)
            {
                getPerspective().toggleVisibility(e.getWindowID());
                KahinaWindow window = windowByID.get(e.getWindowID());
                if (window == null)
                {
                    System.err.println("WARNING: could not find window \"" + e.getWindowID() + "\"");
                }
                else
                {
                    window.setVisible(getPerspective().isVisible(e.getWindowID()));
                    if (!window.isVisible()) window.dispose();
                }
                getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
            }
            else
            {
                System.err.println("WARNING: cannot hide/show non-top-level window \"" + e.getWindowID() + "\"");
            }
        }
        else if (type == KahinaWindowEventType.REMOVE)
        {
            getPerspective().setVisibility(e.getWindowID(), false);
            KahinaWindow window = windowByID.get(e.getWindowID());
            window.dispose();
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
        }
        else if (type == KahinaWindowEventType.FUSE)
        {
            KahinaWindow win = windowByID.get(e.getWindowID());
            win.setBorder(false);
            win.validate();
            win.repaint();
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
        }
        else if (type == KahinaWindowEventType.RESTORE_FRAME)
        {
            KahinaWindow win = windowByID.get(e.getWindowID());
            win.setBorder(true);
            win.validate();
            win.repaint();
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
        }
        else if (type == KahinaWindowEventType.DISPOSE)
        {
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UNDOCK, e.getWindowID()));
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, e.getWindowID()));
            getPerspective().disposeWindow(e.getWindowID());
            windowByID.remove(e.getWindowID());
            getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
        }
        else if (type == KahinaWindowEventType.RENAME)
        {
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                // TODO: switch titles of clones as well; let clones always have
                // identical title + " (clone)"
                window.setTitle(e.getStringContent());
                window.repaintMainPanel();
                getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
            }
        }
        else if (type == KahinaWindowEventType.FLIP)
        {
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                if (window.isFlippableWindow())
                {
                    window.flipSubwindows();
                    window.repaintMainPanel();
                }
                else
                {
                    System.err.println("WARNING: Window \"" + e.getWindowID() + "\" is not flippable. Ignored.");
                }
            }
        }
        else if (type == KahinaWindowEventType.DYNAMIC_CLONE)
        {
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                KahinaWindow cloneWindow = window.createDynamicClone();
                cloneWindow.setVisible(true);
                getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, cloneWindow.getID()));
            }
        }
        else if (type == KahinaWindowEventType.SNAPSHOT_CLONE)
        {
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                KahinaWindow cloneWindow = window.createSnapshotClone();
                cloneWindow.setVisible(true);
                getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, cloneWindow.getID()));
            }
        }
        else if (type == KahinaWindowEventType.UNDOCK)
        {
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                KahinaWindow embeddingWindow = window.getEmbeddingWindow();
                if (embeddingWindow == null)
                {
                    // warning deactivated because undocking is used generically
                    // for drag & drop functionality
                    // System.err.println("WARNING: Window \"" + e.getWindowID()
                    // + "\" cannot be undocked, as is not embedded.");
                }
                // now comes the interesting case
                else
                {
                    // let the embeddingWindow release the window and provide an
                    // appropriate replacement
                    KahinaWindow replacementWindow = embeddingWindow.getReplacementAfterRelease(window);
                    // complicated case: embeddingWindow was embedded
                    if (!embeddingWindow.isTopLevelWindow())
                    {
                        KahinaWindow embEmbeddingWindow = embeddingWindow.getEmbeddingWindow();
                        embEmbeddingWindow.replaceSubwindow(embeddingWindow, replacementWindow);
                        embEmbeddingWindow.validate();
                        embEmbeddingWindow.repaint();
                    }
                    // simple case: embeddingWindow was top level window
                    else
                    {
                        replacementWindow.setVisible(true);
                    }
                    // the two windows are equal in the case of a tabbed window
                    if (embeddingWindow != replacementWindow)
                    {
                        getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.DISPOSE, embeddingWindow.getID()));
                        windowByID.remove(embeddingWindow.getID());
                    }
                    // register and display the undocked window
                    getPerspective().setVisibility(e.getWindowID(), true);
                    window.setVisible(true);
                    getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, e.getWindowID()));
                }
            }
        }
        else if (type == KahinaWindowEventType.VERT_SPLIT)
        {
            System.err.println("Window operation: vertical split of window " + e.getWindowID());
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                if (window.isDummyWindow())
                {
                    window.setTitle("Empty view");
                }
                KahinaWindow oldEmbeddingWindow = window.getEmbeddingWindow();
                KahinaVerticallySplitWindow splitWindow = new KahinaVerticallySplitWindow(this, kahina, .5);
                splitWindow.setTitle(e.getStringContent());
                splitWindow.setUpperWindow(window);
                splitWindow.setLowerWindow(new KahinaDummyWindow(this, kahina));
                splitWindow.setSize(window.getWidth(), window.getHeight());
                splitWindow.setLocation(window.getLocation());
                window.setSize(window.getWidth(), window.getHeight() / 2);
                if (oldEmbeddingWindow != null)
                {
                    oldEmbeddingWindow.replaceSubwindow(window, splitWindow);
                    getArrangement().setEmbeddingWindowID(window.getID(), splitWindow.getID());
                    oldEmbeddingWindow.validate();
                    oldEmbeddingWindow.repaint();
                }
                else
                {
                    getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, e.getWindowID()));
                    splitWindow.setVisible(true);
                    getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, splitWindow.getID()));
                }
            }
        }
        else if (type == KahinaWindowEventType.HORI_SPLIT)
        {
            KahinaWindow window = windowByID.get(e.getWindowID());
            if (window == null)
            {
                System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
            }
            else
            {
                if (window.isDummyWindow())
                {
                    window.setTitle("Empty view");
                }
                KahinaWindow oldEmbeddingWindow = window.getEmbeddingWindow();
                KahinaHorizontallySplitWindow splitWindow = new KahinaHorizontallySplitWindow(this, kahina);
                splitWindow.setTitle(e.getStringContent());
                splitWindow.setLeftWindow(window);
                splitWindow.setRightWindow(new KahinaDummyWindow(this, kahina));
                splitWindow.setSize(window.getWidth(), window.getHeight());
                splitWindow.setLocation(window.getLocation());
                window.setSize(window.getWidth(), window.getHeight() / 2);
                if (oldEmbeddingWindow != null)
                {
                    oldEmbeddingWindow.replaceSubwindow(window, splitWindow);
                    getArrangement().setEmbeddingWindowID(window.getID(), splitWindow.getID());
                    oldEmbeddingWindow.validate();
                    oldEmbeddingWindow.repaint();
                }
                else
                {
                    getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, e.getWindowID()));
                    splitWindow.setVisible(true);
                    getGuiControl().processEvent(new KahinaWindowEvent(KahinaWindowEventType.UPDATE_VIEW_MENU, splitWindow.getID()));
                }
            }
        }
    }

    private KahinaPerspective loadPerspective(File file)
    {
        try
        {
            InputStream stream = new BufferedInputStream(new FileInputStream(file));
            KahinaPerspective result = KahinaPerspective.importXML(XMLUtil.parseXMLStream(stream, false).getDocumentElement());
            stream.close();
            return result;
        }
        catch (IOException e)
        {
            throw new KahinaException("Failed to load perspective.", e);
        }
    }

    // by default, the five most recent perspectives are kept in memory
    private void registerRecentPerspective(KahinaPerspective psp)
    {
        // move to the front, or add to the front
        recentPerspectives.remove(psp);
        recentPerspectives.add(0, psp);
        if (recentPerspectives.size() > 5)
        {
            recentPerspectives.remove(5);
        }
    }

    private void savePerspectiveAs(File file)
    {
        Node node = getPerspective().exportXML(XMLUtil.newEmptyDocument());
        XMLUtil.writeXML(node, file.getAbsolutePath());
    }

    public KahinaController getGuiControl()
    {
        return kahina.getGuiControl();
    }

    public KahinaArrangement getArrangement()
    {
        return psp.getArrangement();
    }

    public void setPerspective(KahinaPerspective psp)
    {
        this.psp = psp;
    }
}
