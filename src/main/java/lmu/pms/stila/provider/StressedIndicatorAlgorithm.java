package lmu.pms.stila.provider;

public interface StressedIndicatorAlgorithm {


    /**
     * If the Computed Stress value is smaller than this value the user is relaxed
     * @return the upper Relaxed bound
     */
    float getRelaxedBound();

    /**
     * If the Computed stress value is larger than this value the user is stressed
     * @return the lower stressed bound
     */
     float getStressedBound();

    /**
     * Can be called to update the data this algorithm is working on.
     * If a (new) algorithm does not need this, just implement it and leave the body
     * empty.
     */
    void updateData();

}
