import java.util.Arrays;

/**
 * Bejamini Hochberg FDR Correction Code.
 * <p/>
 * For details, refer to:  http://www.tau.ac.il/cc/pages/docs/sas8/stat/chap43/sect14.htm
 *
 * @author Steven Maere, Karel Heymans, and Ethan Cerami
 */
public final class FDR {

    private double[] pvalues;
    private double[] adjustedPvalues;
    private int m;

    public FDR(double[] p) {
        this.pvalues = p;
        this.m = pvalues.length;
        this.adjustedPvalues = new double[m];
    }

    public void calculate() {

        // order the pvalues.
        Arrays.sort(pvalues);

        // iterate through all p-values:  largest to smallest
        for (int i = m - 1; i >= 0; i--) {
            if (i == m - 1) {
                adjustedPvalues[i] = pvalues[i];
            } else {
                double unadjustedPvalue = pvalues[i];
                int divideByM = i + 1;
                double left = adjustedPvalues[i + 1];
                double right = (m / (double) divideByM) * unadjustedPvalue;
                adjustedPvalues[i] = Math.min(left, right);
            }
        }
    }

    public double[] getOrdenedPvalues() {
        return pvalues;
    }

    public double[] getAdjustedPvalues() {
        return adjustedPvalues;
    }
}