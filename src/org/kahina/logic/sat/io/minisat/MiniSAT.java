package org.kahina.logic.sat.io.minisat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import org.kahina.logic.sat.muc.data.MUCStatistics;

public class MiniSAT
{
    private static long timeout = 600000;
    private static File lastResultFile;
    private static File lastProofFile;
    
    private static boolean VERBOSE = false;
    
    public static boolean isSatisfiable(File cnfFile, File tmpResultFile) throws TimeoutException, InterruptedException, IOException
    {
        Process p = Runtime.getRuntime().exec("minisat " + cnfFile.getAbsolutePath() + " -c -r " + tmpResultFile.getAbsolutePath());
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        // Set a timer to interrupt the process if it does not return within
        // the timeout period
        Timer timer = new Timer();
        timer.schedule((new MiniSAT()).new InterruptScheduler(Thread.currentThread()), timeout);
        try
        {
            p.waitFor();
        }
        catch (InterruptedException e)
        {
            // Stop the process from running
            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            p.destroy();
            throw new TimeoutException("did not return after " + timeout + " milliseconds");
        }
        finally
        {
            // Stop the timer
            timer.cancel();
        }
        String line;
        while ((line = input.readLine()) != null)
        {
            if (VERBOSE) System.err.println(line);
        }
        input.close();
        BufferedReader input2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line2;
        while ((line2 = input2.readLine()) != null)
        {
            if (VERBOSE) System.err.println(line2);
        }
        input2.close();
        p.getInputStream().close();
        p.getErrorStream().close();
        p.getOutputStream().close();
        p.destroy();
        return !wasUnsatisfiable(tmpResultFile);
    }
    
    public static List<Integer> findUnsatisfiableCore(MUCStatistics stat, MiniSATFiles files) throws TimeoutException, InterruptedException
    {
        try
        {
            Process p = Runtime.getRuntime().exec("minisat " + files.tmpFile.getAbsolutePath() + " -p " + files.tmpProofFile.getAbsolutePath() + " -c -r " + files.tmpResultFile.getAbsolutePath() + " -f " + files.tmpFreezeFile);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // Set a timer to interrupt the process if it does not return within
            // the timeout period
            Timer timer = new Timer();
            timer.schedule((new MiniSAT()).new InterruptScheduler(Thread.currentThread()), timeout);
            try
            {
                p.waitFor();
            }
            catch (InterruptedException e)
            {
                // Stop the process from running
                p.getInputStream().close();
                p.getErrorStream().close();
                p.getOutputStream().close();
                p.destroy();
                throw new TimeoutException("did not return after " + timeout + " milliseconds");
            }
            finally
            {
                // Stop the timer
                timer.cancel();
            }
            String line;
            while ((line = input.readLine()) != null)
            {
                if (VERBOSE) System.err.println(line);
            }
            input.close();
            BufferedReader input2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line2;
            while ((line2 = input2.readLine()) != null)
            {
                if (VERBOSE) System.err.println(line2);
            }
            input2.close();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            p.destroy();
            if (wasUnsatisfiable(files.tmpResultFile))
            {
                //offsetID := stat.highestID
                return getRelevantAssumptions(stat.highestID + 1, files.tmpProofFile);
            }
            else
            {
                return new LinkedList<Integer>();
            }
        }
        catch (IOException e)
        {
            System.err.println("IO Error during SAT solving");
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    public static void solve(File inputFile, File proofFile, File resultFile, File freezeFile) throws TimeoutException, InterruptedException
    {
        try
        {
            Process p = Runtime.getRuntime().exec("minisat " + inputFile.getAbsolutePath() + " -p " + proofFile.getAbsolutePath() + " -c -r " + resultFile.getAbsolutePath() + " -f " + freezeFile);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // Set a timer to interrupt the process if it does not return within
            // the timeout period
            Timer timer = new Timer();
            timer.schedule((new MiniSAT()).new InterruptScheduler(Thread.currentThread()), timeout);

            try
            {
                p.waitFor();
            }
            catch (InterruptedException e)
            {
                // Stop the process from running
                p.getInputStream().close();
                p.getErrorStream().close();
                p.getOutputStream().close();
                p.destroy();
                throw new TimeoutException("did not return after " + timeout + " milliseconds");
            }
            finally
            {
                // Stop the timer
                timer.cancel();
            }
            String line;
            while ((line = input.readLine()) != null)
            {
                if (VERBOSE) System.out.println(line);
            }
            input.close();
            BufferedReader input2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line2;
            while ((line2 = input2.readLine()) != null)
            {
                if (VERBOSE) System.err.println(line2);
            }
            input2.close();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            p.destroy();
            lastResultFile = resultFile;
            lastProofFile = proofFile;
        }
        catch (IOException e)
        {
            System.err.println("IO-Fehler bei sATSolving");
            e.printStackTrace();
            System.exit(0);
        }

    }

    private class InterruptScheduler extends TimerTask
    {
        Thread target = null;

        public InterruptScheduler(Thread target)
        {
            this.target = target;
        }

        @Override
        public void run()
        {
            target.interrupt();
        }
    }

    // checks result file to see whether the last SAT problem was unsatisfiable
    public static boolean wasUnsatisfiable()
    {
        BufferedReader input;
        try
        {
            input = new BufferedReader(new FileReader(lastResultFile));
            String line;
            while ((line = input.readLine()) != null)
            {
                if (line.equals("UNSAT"))
                {
                    // System.out.println("UNSAT");
                    return true;
                }
                else if (line.equals("SAT"))
                {
                    // System.out.println("SAT");
                    return false;
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println("IOfailed testunsatisfiable");
            System.exit(0);
        }
        System.err.println("ERROR: Result file says neither SAT nor UNSAT! Assuming SAT!");
        return false;
    }
    
    // checks result file to see whether a SAT problem was unsatisfiable
    public static boolean wasUnsatisfiable(File resultFile)
    {
        BufferedReader input;
        try
        {
            input = new BufferedReader(new FileReader(resultFile));
            String line;
            while ((line = input.readLine()) != null)
            {
                if (line.equals("UNSAT"))
                {
                    // System.out.println("UNSAT");
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println("IOfailed testunsatisfiable");
            System.exit(0);
        }
        // System.out.println("SAT");
        return false;
    }

    // extract the relevant assumptions from the last proof file
    public static List<Integer> getRelevantAssumptions(boolean[] freezeVariables, int offsetID)
    {
        List<Integer> relevantAssumptions = new ArrayList<Integer>();
        BufferedReader input;
        try
        {
            input = new BufferedReader(new FileReader(lastProofFile));
            String line, line2 = "";
            while ((line = input.readLine()) != null)
            {
                line2 = line;
            }
            Arrays.fill(freezeVariables, Boolean.FALSE);
            StringTokenizer st = new StringTokenizer(line2);
            int i = 0;
            while (st.hasMoreTokens())
            {
                line = st.nextToken();
                if (i == 0)
                {
                    i++;
                }
                else
                {
                    if (!line.equals("0"))
                    {
                        freezeVariables[((-1) * Integer.parseInt(line)) - offsetID] = true;
                        relevantAssumptions.add(((-1) * Integer.parseInt(line)) - offsetID);
                    }
                    else
                    {
                        break;
                    }
                }
            }
            input.close();
            return relevantAssumptions;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("IOfailed rel_asmberechnen");
            System.exit(0);
        }
        return null;
    }
    
    //extract the relevant assumptions from proof file
    public static List<Integer> getRelevantAssumptions(int offsetID, File proofFile)
    {
        List<Integer> relevantAssumptions = new ArrayList<Integer>();
        BufferedReader input;
        try
        {
            input = new BufferedReader(new FileReader(proofFile));
            String line, line2 = "";
            while ((line = input.readLine()) != null)
            {
                line2 = line;
            }
            StringTokenizer st = new StringTokenizer(line2);
            int i = 0;
            while (st.hasMoreTokens())
            {
                line = st.nextToken();
                if (i == 0)
                {
                    i++;
                }
                else
                {
                    if (!line.equals("0"))
                    {
                        relevantAssumptions.add(((-1) * Integer.parseInt(line)) - offsetID);
                    }
                    else
                    {
                        break;
                    }
                }
            }
            input.close();
            return relevantAssumptions;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("IOfailed rel_asmberechnen");
            System.exit(0);
        }
        return null;
    }
    
    public static void createFreezeFile(boolean[] freezeVariables, File freezeFile, int offsetID)
    {
        StringBuffer freezeBuffer = new StringBuffer("");
        for (int i = 0; i < freezeVariables.length; i++)
        {
            if (freezeVariables[i])
            {
                if (i < (freezeVariables.length - 1))
                {
                    freezeBuffer.append("" + (offsetID + i) + ",");
                }
                else
                {
                    freezeBuffer.append("" + (offsetID + i));
                }
            }
        }
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(freezeFile));
            out.write("" + freezeBuffer);
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("IO error: failed to create temporary freeze file");
            System.exit(0);
        }
    }
}