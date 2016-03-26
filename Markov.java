package barn1474;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
//import java.util.zip.GZIPInputStream;
//import java.util.zip.GZIPOutputStream;

//import spacesettlers.clients.ImmutableTeamInfo;
//import spacesettlers.clients.TeamClient;
//import spacesettlers.simulator.Toroidal2DPhysics;

public class Markov {

    //A unit test
    public static void main(String[] args) {
        System.out.println("Running unit test for Markov.java");
        double[] mins = {0, 0, 0};
        double[] maxs = {1, 1, 1};
        new File("unitTestData.dat").delete();
        new File("monteCarloGraphData.txt").delete();
	new File("monteCarloIndividualData.txt").delete();
        //Since this is going to be called multiple times per second, default source of random which inits from system clock isn't okay.
        final Random actualRand = new Random();
        for (int i = 0; i < /*30000*/ 200; i++) {
            Markov m = new Markov(0.7, /*100*/ 25, mins, maxs, "unitTestData.dat", 1);
            m.rand = actualRand;
            double[] ps = m.getParameters();
            double score = 0.7 - Math.abs(ps[0]-0.7) + ps[1]*5 - Math.floor(ps[1]*5) + ps[2] + actualRand.nextDouble();
            m.doEndTimes(score, ps);
        }
    }

    double reqdFraction; // What fraction of the old score our new range is required to contain in each case
    int sampleSize;
    double[] parMins, parMaxs;
    double[] hardMins, hardMaxs;
    int numPars;
    String knowledgeFile;
    double caution;
    Random rand;
    //GZIPInputStream in;
    //GZIPOutputStream out;
    FileInputStream in;
    FileOutputStream out;

    /**
     * Creates a new Markov object.
     * @param f What fraction of the score the selected subset must contain. Reasonable values seem to be 0.5-0.9 ish
     * @param s Sample size. After this many iterations, recalculates maxes and mins for parameters.
     * @param kf knowledge file location, should pull from TeamClient.knowledgeFile
     * @param parameterSpaceMins Lowest acceptable values for each of the parameters. Also serves as floor of starting search space if no prior knowledge file exists
     * @param parameterSpaceMaxs Highest acceptable values for each of the parameters. Also serves as ceiling of starting search space if no prior knowledge file exists
     * @param caution Less than 1, windows will narrow over time even if the parameter has no effect on score. Over 1, windows will widen unless there is good evidence that the parameter effects score
     */
    public Markov(double f, int s, double[] parameterSpaceMins, double[] parameterSpaceMaxs, String kf, double caution) {
        init(f, s, parameterSpaceMins, parameterSpaceMaxs, kf, caution);
    }

    public Markov(double[] pSpaceMins, double[] pSpaceMaxs, String kf) {
        init(0.8, 30, pSpaceMins, pSpaceMaxs, kf, 0.99);
    }

    void init(double f, int s, double[] parameterSpaceMins, double[] parameterSpaceMaxs, String kf, double caution) {
        reqdFraction = f;
        sampleSize = s;
        numPars = parameterSpaceMins.length;
        parMins = new double[numPars];
        hardMins = new double[numPars];
        parMaxs = new double[numPars];
        hardMaxs = new double[numPars];
        for (int i = 0; i < numPars; i++) {
            parMins[i] = hardMins[i] = parameterSpaceMins[i];
            parMaxs[i] = hardMaxs[i] = parameterSpaceMaxs[i];
        }
        knowledgeFile = kf;
        this.caution = caution;
        rand = new Random();
    }


    void printArray(double[] a) {
	try {
            out.write(String.format("{%.2f", a[0]).getBytes());
            for (int i = 1; i < a.length; i++) out.write(String.format(" %.2f", a[i]).getBytes());
            out.write("}\n".getBytes());
	} catch (IOException e) {
		System.out.println("*Burns to death*");
		e.printStackTrace();
	}
    }

    public double[] getParameters() {
        //System.out.print("Markov Init: ");
        File f = new File(knowledgeFile);
        try {
            if (f.createNewFile()) { // If the file doesn't exist...
                //System.out.println("Creating new file, writing default ranges");
                //out = new GZIPOutputStream(new FileOutputStream(f));
                out = new FileOutputStream(f);
                for (double d : parMins) writeDouble(d);
                for (double d : parMaxs) writeDouble(d);
                out.close();
                out = null;
            } else {
                //in = new GZIPInputStream(new FileInputStream(f));
                in = new FileInputStream(f);
                for (int i = 0; i < numPars; i++) parMins[i] = readDouble();
                for (int i = 0; i < parMaxs.length; i++) parMaxs[i] = readDouble();
                //System.out.println("Read parameter mins, maxes:");
                //printArray(parMins);
                //printArray(parMaxs);
                in.close();
                in = null;
            }
        } catch (IOException e) {
            System.out.println("Shit's on fire, yo");
            e.printStackTrace();
        }
        //Generate random parameters for this run between parMin and parMax
        double[] ret = new double[numPars];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = parMins[i] + rand.nextDouble() * (parMaxs[i]-parMins[i]);
        }
        //System.out.println("Chose parameters:");
        //printArray(ret);
        return ret;
    }

    public void doEndTimes(double teamScore, double[] parameters) {
	try {
		out = new FileOutputStream(new File("monteCarloIndividualData.txt"), true);
		printArray(parameters);
		out.write(((Double)teamScore).toString().getBytes());
		out.write('\n');
		out.close();
		out = null;
	} catch (IOException e) {
		try {
			out.close();
			out = null;
		} catch (Exception x) {}
		System.out.println("Failed writing individual data!");
	}
        //System.out.print("Markov Shutdown: ");
        File f = new File(knowledgeFile);
        try {
            //in = new GZIPInputStream(new FileInputStream(f));
            in = new FileInputStream(f);
            //Step 1: determine if we've finished sampling yet
            //Step 1.1: Read in the parameter ranges, which we don't really care about rn.
            for (int i = 0; i < numPars*2; i++) readDouble();
        } catch (IOException e) {
            System.out.println("Shit's broken, yo");
            e.printStackTrace();
            return;
        }
        try {
            //Step 1.2: Try to read in a metric tonne of data
            double[][] data = new double[sampleSize][numPars+1];
            for (int i = 0; i < sampleSize-1; i++) {
                for (int j = 0; j < numPars+1; j++) {
                    data[i][j] = readDouble();
                }
            }
            //Step 2: We succeeded, so add the current run to 'data'
            for (int i = 0; i < numPars; i++) {
                data[sampleSize-1][i] = parameters[i];
            }
            data[sampleSize-1][numPars] = teamScore;
            //Step 3: Profit
            double totalScore = 0;
            for (int i = 0; i < sampleSize; i++) totalScore += data[i][numPars];
            double slack = (1-reqdFraction)*totalScore;
            in.close();
            in = null;

            for (int i = 0; i < numPars; i++) {
                //The sample facet consists of pairs, connecting the value of some parameter with the score for that run
                ArrayList<RunFacet> sampleFacet = new ArrayList<RunFacet>();
                for (int j = 0; j < sampleSize; j++) {
                    sampleFacet.add(new RunFacet(data[j][i], data[j][numPars]));
                }
                Collections.sort(sampleFacet);
                RunFacet[] sampleFacetArray = sampleFacet.toArray(new RunFacet[0]);
                //Starting at the ends, eat up as many trials as possible while consuming no more than 'slack' in score
                pos p = getOptimalPos(sampleFacetArray, slack);
                //if p.low == 0, we can leave this particular parMin unchanged
                if (p.low != 0) parMins[i] = sampleFacetArray[p.low-1].value;
                if (p.high != sampleSize) parMaxs[i] = sampleFacetArray[p.high].value;
                //Consider the uniform distribution (score doesn't depend on a var).
                //If we take 75% of the score, we get 75% of the area,
                //when we really shouldn't be reducing at all.
                //So we divide by 0.75, and get about back where we started.
                //If there really is a better choice, our area will shrink by
                //more than 75%, and dividing by 0.75 will still result in an
                //area which is smaller than what we started with.
                double grow = (parMaxs[i]-parMins[i])*(1/reqdFraction-1)/2*caution;
                parMins[i] -= grow;
                parMaxs[i] += grow;
                if (parMins[i] < hardMins[i]) parMins[i] = hardMins[i];
                if (parMaxs[i] > hardMaxs[i]) parMaxs[i] = hardMaxs[i];
            }
            //Write the new parameter bounds to file, clearing all previous run data in the process
            //out = new GZIPOutputStream(new FileOutputStream(f, false));
            out = new FileOutputStream(f, false);
            for (double d : parMins) writeDouble(d);
            for (double d : parMaxs) writeDouble(d);
            out.close();
            out = new FileOutputStream(new File("monteCarloGraphData.txt"), true);
            //System.out.println("Wrote new par mins, maxes:");
            printArray(parMins);
            printArray(parMaxs);
            out.write(((Double)(totalScore/sampleSize)).toString().getBytes());
            out.write('\n');
            out.close();
            out = null;
        } catch (IOException e) {
            //Step 2, plan B: We don't have a full set of samples, so just add my data to the list and move on.
            //System.out.println("Appending to file:");
            try {
                in.close();
                in = null;
                //out = new GZIPOutputStream(new FileOutputStream(f, true));
                out = new FileOutputStream(f, true);
                for (double d : parameters) {
                    //System.out.printf("%.2f ", d);
                    writeDouble(d);
                }
                //System.out.printf("%.2f\n", teamScore);
                writeDouble(teamScore);
                out.close();
                out = null;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //readDouble and writeDouble function inspired by
    //http://stackoverflow.com/questions/2905556/how-can-i-convert-a-byte-array-into-a-double-and-back
    void writeDouble(double value) throws IOException {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        out.write(bytes);
    }

    double readDouble() throws IOException {
        byte[] bytes = new byte[8];
        if(-1 == in.read(bytes)) throw new IOException();
        return ByteBuffer.wrap(bytes).getDouble();
    }

    class pos {
        int low, high;
        double slack;
        public pos(int l, int h, double s) {
            low = l;
            high = h;
            slack = s;
        }
    }

    class RunFacet implements Comparable<RunFacet> {
        double value;
        double score;
        public RunFacet(double v, double s) {
            value = v;
            score = s;
        }
        public int compareTo(RunFacet o) {
            return ((Double)value).compareTo(o.value);
        }
    }

    pos getOptimalPos(RunFacet[] runs, double slack) {
        //Eat in from the right edge first
        int high = runs.length-1;
        while (runs[high].score <= slack) {
            slack -= runs[high].score;
            high--;
        }
        //See if the remainder can be eaten by the left edge
        int low = 0;
        while (runs[low].score <= slack) {
            slack -= runs[low].score;
            low++;
        }
        pos best = new pos(low, high+1, slack);
        int bestScore = best.high - best.low; // We want the smallest range possible
        pos current = best;
        while (null != (current = pushRight(runs, current))) {
            int score = current.high - current.low;
            if (score <= bestScore) {
                if (score == bestScore) {
                    int bestTiebreaker = (int)Math.min(best.low, runs.length-best.high);
                    int currentTiebreaker = (int)Math.min(current.low, runs.length-current.high);
                    if (currentTiebreaker <= bestTiebreaker) continue;
                }
                bestScore = score;
                best = current;
            }
        }
        return best;
    }

    pos pushRight(RunFacet[] runs, pos cur) {
        if (cur.high >= runs.length) return null;
        double scoreLow = runs[cur.low].score;
        double scoreHigh = runs[cur.high].score;
        if (scoreLow > scoreHigh) {
            double toProduce = scoreLow - cur.slack;
            int high = cur.high;
            while (toProduce >= 0) {
                if (high >= runs.length) return null;
                toProduce -= runs[high].score;
                high++;
            }
            return new pos(cur.low+1, high, -toProduce);
        } else {
            double toConsume = scoreHigh + cur.slack;
            int low = cur.low;
            while (toConsume >= runs[low].score) {
                toConsume -= runs[low].score;
                low++;
            }
            return new pos(low, cur.high+1, toConsume);
        }
    }
}
