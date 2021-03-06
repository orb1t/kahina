package org.kahina.logic.sat.io.free;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.kahina.logic.sat.data.free.BooleanFormula;
import org.kahina.logic.sat.io.cnf.DimacsCnfOutput;

public class BooleanFormulaOutput
{
    public static void writeDaimlerFile(String fileName, BooleanFormula f)
    {
        try
        {
            FileWriter out = new FileWriter(new File(fileName));
            out.write(f.toStringWithMinimumBracing());
            out.close();
        }
        catch (IOException e)
        {
            System.err.println("ERROR: Could not write to DIMACS CNF file: " + fileName);
            e.printStackTrace();
        }
    }
    
    public static void writeDimacsCnfFile(String fileName, BooleanFormula f)
    {
        TseitinTransformationVisitor visitor = new TseitinTransformationVisitor();
        int topVar = f.accept(visitor);
        DimacsCnfOutput.writeDimacsCnfFile(fileName, visitor.getCNF(topVar));
    }
}
