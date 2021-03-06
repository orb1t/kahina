package org.kahina.core.edit.breakpoint;

import java.io.File;

import org.kahina.core.control.KahinaEvent;

public class BreakpointEditorEvent extends KahinaEvent
{    
    File file;
    int editorEventType;
    SingleNodeConstraintPanel panel;
    
    int goalID;
    
    public static final int NEW_BREAKPOINT = 0;
    public static final int EXPORT_BREAKPOINT = 1;
    public static final int IMPORT_BREAKPOINT = 2;
    public static final int ACTIVATE_BREAKPOINT = 3;
    public static final int DEACTIVATE_BREAKPOINT = 4;
    public static final int REMOVE_BREAKPOINT = 5;
    public static final int CHANGE_NODE_SELECTION_MODE = 6;
    public static final int TREE_NODE_UPDATE = 7;  
    public static final int TREE_PATTERN_CHANGE = 7;  
    public static final int TEST_BREAKPOINTS = 8; 
    public static final int SYNCHRONIZE_EDITOR_VIEWS = 9;  
    public static final int BREAKPOINT_NAME_UPDATE = 10;
    public static final int APPLY_QUIT = 11; 
    public static final int EXPORT_BREAKPOINT_PROFILE = 12;
    public static final int IMPORT_BREAKPOINT_PROFILE = 13;
    
    public BreakpointEditorEvent(int editorEventType)
    {
        super("breakpoint_editor");
        this.editorEventType = editorEventType;
    }
    
    public BreakpointEditorEvent(int editorEventType, File file)
    {
        super("breakpoint_editor");
        this.editorEventType = editorEventType;
        this.file = file;
    }
    
    public BreakpointEditorEvent(int editorEventType, SingleNodeConstraintPanel panel)
    {
        super("breakpoint_editor");
        this.editorEventType = editorEventType;
        this.panel = panel;
    }
    
    public BreakpointEditorEvent(int editorEventType, int goalID)
    {
        super("breakpoint_editor");
        this.editorEventType = editorEventType;
        this.goalID = goalID;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public int getEditorEventType()
    {
        return editorEventType;
    }
    
    public int getGoalID()
    {
        return goalID;
    }
    
    @Override
	public String toString()
    {
        String s = "breakpoint: ";
        if (editorEventType == NEW_BREAKPOINT)
        {
            s += "new "; 
        }
        else if (editorEventType == EXPORT_BREAKPOINT)
        {
            s += "export -> " + file; 
        }
        else if (editorEventType == IMPORT_BREAKPOINT)
        {
            s += "import <- " + file; 
        }
        else if (editorEventType == ACTIVATE_BREAKPOINT)
        {
            s += "activate " + goalID; 
        }
        else if (editorEventType == DEACTIVATE_BREAKPOINT)
        {
            s += "deactivate " + goalID; 
        }
        else if (editorEventType == REMOVE_BREAKPOINT)
        {
            s += "remove " + goalID; 
        }
        else if (editorEventType == CHANGE_NODE_SELECTION_MODE)
        {
            s += "selection mode " + goalID; 
        }
        else if (editorEventType == TREE_NODE_UPDATE)
        {
            s += "tree node update " + panel; 
        }
        else if (editorEventType == TREE_PATTERN_CHANGE)
        {
            s += "tree pattern change"; 
        }
        else if (editorEventType == TEST_BREAKPOINTS)
        {
            s += "test breakpoints"; 
        }
        else if (editorEventType == SYNCHRONIZE_EDITOR_VIEWS)
        {
            s += "synchronize editor views"; 
        }
        else if (editorEventType == BREAKPOINT_NAME_UPDATE)
        {
            s += "breakpoint name update"; 
        }
        else if (editorEventType == APPLY_QUIT)
        {
            s += "apply and quit"; 
        }
        else
        {
            s += "unknown operation";
        }
        return s;
    }

    public SingleNodeConstraintPanel getPanel()
    {
        return panel;
    }
}
