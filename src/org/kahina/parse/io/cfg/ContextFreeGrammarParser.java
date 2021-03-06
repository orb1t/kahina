package org.kahina.parse.io.cfg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.kahina.parse.data.cfg.ContextFreeGrammar;

public class ContextFreeGrammarParser
{
    public static ContextFreeGrammar parseCFGFile(String fileName)
    {
        ContextFreeGrammar cfg = new ContextFreeGrammar();
        try
        {
            Scanner in = new Scanner(new File(fileName));
            
            String currentLine;
            String[] tokens;
            String currentHead = null;
            List<String> currentBody = new LinkedList<String>();
            //read in rules
            while (in.hasNext())
            {
                currentLine = in.nextLine();
                tokens = currentLine.split(" ");
                if (tokens.length < 2)
                {
                    //a 0 line separates rules from the lexicon
                    if (tokens[0].equals("0"))
                    {
                        break;
                    }
                    System.err.println("ERROR: invalid CFG rule \"" + currentLine + "\", ignoring it.");
                    continue;
                }
                currentHead = tokens[0];
                currentBody = new LinkedList<String>();
                for (int i = 1; i < tokens.length; i++)
                {
                    currentBody.add(tokens[i]);
                }
                cfg.addRule(currentHead, currentBody);
            }
            //read in lexicon entries
            while (in.hasNext())
            {
                currentLine = in.nextLine();
                tokens = currentLine.split(" ");
                if (tokens.length != 2)
                {
                    System.err.println("ERROR: invalid lexicon entry \"" + currentLine + "\", ignoring it.");
                    continue;
                }
                cfg.addLexEntry(tokens[0], tokens[1]);
            }
            in.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("ERROR: CFG file not found: " + fileName);
            System.err.println("       Returning empty CFG!");
        }
        return cfg;
    }
}
