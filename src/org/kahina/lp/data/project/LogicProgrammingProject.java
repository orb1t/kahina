package org.kahina.lp.data.project;

import org.kahina.core.control.KahinaControlActuator;
import org.kahina.core.data.agent.KahinaControlAgent;
import org.kahina.core.data.agent.KahinaControlAgentProfile;
import org.kahina.core.data.project.KahinaProject;
import org.kahina.core.data.tree.KahinaTree;
import org.kahina.lp.LogicProgrammingInstance;
import org.kahina.lp.control.LogicProgrammingBreakActuator;
import org.kahina.lp.control.LogicProgrammingCompleteActuator;
import org.kahina.lp.control.LogicProgrammingCreepActuator;
import org.kahina.lp.control.LogicProgrammingFailActuator;
import org.kahina.lp.control.LogicProgrammingSkipActuator;
import org.kahina.lp.data.agent.LogicProgrammingControlAgentProfile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LogicProgrammingProject extends KahinaProject
{
    //store profiles for the various types of  control points
    protected KahinaControlAgentProfile breakPoints;
    protected KahinaControlAgentProfile creepPoints;
    protected KahinaControlAgentProfile completePoints;
    protected KahinaControlAgentProfile skipPoints;
    protected KahinaControlAgentProfile failPoints;
    protected KahinaControlAgentProfile warnPoints;
    
    //link to the Kahina instance
    protected LogicProgrammingInstance<?,?,?,?> kahina;
    
    public LogicProgrammingProject(String appID, String name, LogicProgrammingInstance<?,?,?,?> kahina)
    {
        super(appID, name);
        this.kahina = kahina;
        breakPoints = new LogicProgrammingControlAgentProfile(new LogicProgrammingBreakActuator(kahina));
        creepPoints = new LogicProgrammingControlAgentProfile(new LogicProgrammingCreepActuator(kahina));
        completePoints = new LogicProgrammingControlAgentProfile(new LogicProgrammingCompleteActuator(kahina));
        skipPoints = new LogicProgrammingControlAgentProfile(new LogicProgrammingSkipActuator(kahina));
        failPoints = new LogicProgrammingControlAgentProfile(new LogicProgrammingFailActuator(kahina));
        //warnPoints = new KahinaControlPointProfile();
    }
    
    public void register()
    {
        breakPoints.registerAll();
        creepPoints.registerAll();
        completePoints.registerAll();
        skipPoints.registerAll();
        failPoints.registerAll();
    }
    
    public void deregister()
    {
        breakPoints.deregisterAll();
        creepPoints.deregisterAll();
        completePoints.deregisterAll();
        skipPoints.deregisterAll();
        failPoints.deregisterAll();
    }
    
    public LogicProgrammingProject copy()
    {
        LogicProgrammingProject copy = new LogicProgrammingProject(appID, new String(name), kahina);
        copyDataInto(copy);
        return copy;
    }
   
    public void copyDataInto(LogicProgrammingProject copy)
    {
        super.copyDataInto(copy);
        copy.breakPoints = breakPoints.copy();
        copy.creepPoints = creepPoints.copy();
        copy.completePoints = completePoints.copy();
        copy.skipPoints = skipPoints.copy();
        copy.failPoints = failPoints.copy();
    }
    
    public KahinaControlAgentProfile getBreakPoints()
    {
        return breakPoints;
    }
    
    public KahinaControlAgentProfile getCreepPoints()
    {
        return creepPoints;
    }
    
    public KahinaControlAgentProfile getCompletePoints()
    {
        return completePoints;
    }
    
    public KahinaControlAgentProfile getSkipPoints()
    {
        return skipPoints;
    }
    
    public KahinaControlAgentProfile getFailPoints()
    {
        return failPoints;
    }
    
    public KahinaControlAgentProfile getWarnPoints()
    {
        return warnPoints;
    }
    
    public void addBreakPoint(KahinaControlAgent agent)
    {
        breakPoints.addControlAgent(agent);
    }
    
    public void addCreepPoint(KahinaControlAgent agent)
    {
        creepPoints.addControlAgent(agent);
    }
    
    public void addCompletePoint(KahinaControlAgent agent)
    {
        completePoints.addControlAgent(agent);
    }
    
    public void addSkipPoint(KahinaControlAgent agent)
    {
        skipPoints.addControlAgent(agent);
    }
    
    public void addFailPoint(KahinaControlAgent agent)
    {
        failPoints.addControlAgent(agent);
    }
    
    public void addWarnPoint(KahinaControlAgent agent)
    {
        warnPoints.addControlAgent(agent);
    }
    
    public Element exportXML(Document dom)
    {
        Element el = super.exportXML(dom);
        Element profilesEl = dom.createElementNS("http://www.kahina.org/xml/kahina","kahina:controlAgentProfiles");
        Element profileEl = breakPoints.exportXML(dom);
        profileEl.setAttribute("kahina:type", "break");
        profilesEl.appendChild(profileEl);
        profileEl = creepPoints.exportXML(dom);
        profileEl.setAttribute("kahina:type", "creep");
        profilesEl.appendChild(profileEl);
        profileEl = completePoints.exportXML(dom);
        profileEl.setAttribute("kahina:type", "complete");
        profilesEl.appendChild(profileEl);
        profileEl = skipPoints.exportXML(dom);
        profileEl.setAttribute("kahina:type", "skip");
        profilesEl.appendChild(profileEl);
        profileEl = failPoints.exportXML(dom);
        profileEl.setAttribute("kahina:type", "fail");
        profilesEl.appendChild(profileEl);
        /*profileEl = warnPoints.exportXML(dom);
        profileEl.setAttribute("kahina:type", "warn");
        profilesEl.appendChild(profileEl);*/
        el.appendChild(profilesEl);
        return el;
    }
    
    public static void importXML(Element topEl, LogicProgrammingProject project, LogicProgrammingInstance<?,?,?,?> kahina, KahinaTree stepTree)
    {
        KahinaProject.importXML(topEl, project);
        importControlAgentProfile(topEl, project, kahina, stepTree);
    }
    
    protected static void importControlAgentProfile(Element topEl, LogicProgrammingProject project, LogicProgrammingInstance<?,?,?,?> kahina, KahinaTree stepTree)
    {
        //read in control agent profiles
        NodeList profileList = topEl.getElementsByTagName("kahina:controlAgentProfile");
        //System.err.println("  " + profileList.getLength() + " breakpoint profiles found");
        for (int i = 0; i < profileList.getLength(); i++)
        {
            Element profileElement = (Element) profileList.item(i);
            String type = profileElement.getAttribute("kahina:type");
            if (type.equals("break"))
            {
                KahinaControlActuator actuator = new LogicProgrammingBreakActuator(kahina);  
                project.breakPoints = LogicProgrammingControlAgentProfile.importXML(profileElement, actuator, kahina);
            }
            else if (type.equals("creep"))
            {
                KahinaControlActuator actuator = new LogicProgrammingCreepActuator(kahina);  
                project.creepPoints = LogicProgrammingControlAgentProfile.importXML(profileElement, actuator, kahina);
            }
            else if (type.equals("complete"))
            {
                KahinaControlActuator actuator = new LogicProgrammingCompleteActuator(kahina);  
                project.completePoints = LogicProgrammingControlAgentProfile.importXML(profileElement, actuator, kahina);
            }
            else if (type.equals("skip"))
            {
                KahinaControlActuator actuator = new LogicProgrammingSkipActuator(kahina);  
                project.skipPoints = LogicProgrammingControlAgentProfile.importXML(profileElement, actuator, kahina);
            }
            else if (type.equals("fail"))
            {
                KahinaControlActuator actuator = new LogicProgrammingFailActuator(kahina);  
                project.failPoints = LogicProgrammingControlAgentProfile.importXML(profileElement, actuator, kahina);
            }
        }
    }
}
