package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import java.util.Arrays;
import java.util.Random;
import static de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution.erf;

public class McdeMwpDependenceMeasure extends MCDEDependenceMeasure {

    /**
     * Constructor
     */

    public McdeMwpDependenceMeasure(int m, double alpha, double beta, RandomFactory rnd){
        super();
        this.m = m;
        this.alpha = alpha;
        this.beta = beta;
        this.rnd = rnd;
    }

    /**
     * Overloaded wrapper for ranks()
     */

    protected static <A> double[] ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
        return ranks(adapter, data, sortedIndex(adapter, data, len));
    }

    /**
     * Computes Corrected Rank Index as described in Algorithm 1 of source paper, adjusted for bivariate ELKI interface.
     * Notation as ELKI convention if applicable, else as in paper.
     *
     * @param adapter ELKI NumberArrayAdapter Subclass
     * @param data One dimensional array containing one dimension of the data
     * @param idx Return value of sortedIndex()
     * @return Array of doubles, 3 subsequent values being assigned to one data instance.
     * Containing sorted (ascending) row numbers, adjusted ranks and tying value corrections
     * as required by MWP test. Example:
     * double[] corrected_ranks = ranks(...);
     * double l = corrected_rank[0]; double adjusted_rank = corrected_rank[1]; double correction = corrected_rank[2];
     * // correspond to one instance of the original data
     */

    protected static <A> double[] ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx){
        final int len = adapter.size(data);
        double[] I = new double[len * 3];

        int j = 0; int correction = 0;
        while(j < len){
            int k = j; int t = 1; double adjust = 0.0;

            while((k < len - 1) && (adapter.getDouble(data, idx[k]) == adapter.getDouble(data, idx[k+1]))){
                adjust += k;
                k++; t++;
            }

            if(k > j){
                double adjusted = (adjust + k) / t;
                correction += (t*t*t) - t;

                for(int m = j; m <= k; m++){
                    int p = m*3;
                    I[p] = (double) idx[m];
                    I[p+1] = adjusted;
                    I[p+2] = correction;
                }
            }
            else {
                int p = j*3;
                I[p] = (double)idx[j];
                I[p+1] = j;
                I[p+2] = correction;
            }
            j += t;
        }

        return I;
    }

    /**
     * Data Slicing
     *
     * @param len No of data instances
     * @param nonRefIndex Index (see correctedRank()) for the dimension that is not the reference dimension
     * @return Array of booleans that states which instances are part of the slice
     */


    protected boolean[] randomSlice(int len, double[] nonRefIndex){
        final Random random = rnd.getSingleThreadedRandom();
        boolean slice[] = new boolean[len];
        Arrays.fill(slice, Boolean.TRUE);

        final int slizeSize = (int) Math.ceil(Math.pow(this.alpha, 1.0) * len);
        final int start = random.nextInt(len - slizeSize);
        final int end = start + slizeSize;

        for(int j = 0; j < start; j++){
            slice[(int) nonRefIndex[j*3]] = false;
        }

        for(int j = end; j < len; j++){
            slice[(int) nonRefIndex[j*3]] = false;
        }

        return slice;
    }

    protected double statistical_test(int len, boolean[] slice, double[] corrected_ranks){
        final Random random = rnd.getSingleThreadedRandom(); // TODO: No "safeCut", make safecut?
        int start = random.nextInt((int) (len * (1 - this.beta))); // TODO: Marginal restriction, eigentlich 1 bis (...) im paper, hier 0 bis, ACHTUNG!!
        int end = start + (int) Math.ceil(len * this.beta); // TODO: Marginal restriction

        double R = 0.0; int n1 = 0;
        for(int j = start; j < end; j++){ // TODO: < oder <=

            if(slice[(int) corrected_ranks[j*3]]){
                R += corrected_ranks[j*3 + 1];
                n1++;
            }
        }
        int n_bar = end - start;

        if((n1 == 0) || (n1 == n_bar)){
            return 1;
        }

        double U = R - start * n1; // TODO: in Scala it is R - (n1 * (n1-1))/2 "because ranking starts at 0"
        int n2 = n_bar - n1;
        double b_end = corrected_ranks[(end-1) *3 +2];
        double b_start = start == 0 ? 0 : corrected_ranks[(start-1) *3 +2];
        double correction = (b_end - b_start) / (n_bar * (n_bar -1));
        double std = Math.sqrt(( ((double) (n1 * n2)) / 12) * (n_bar + 1 - correction));

        if(std == 0) return 0;
        else{
            double mean = ((double) (n1 * n2)) / 2;
            double Z = Math.abs((U - mean) / std);
            return erf(Z / Math.sqrt(2)); // TODO: Check if this erf() is same as in scala
        }
    }
}
