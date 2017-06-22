package sma;

import beast.core.Description;
import beast.evolution.alignment.Alignment;

@Description("Summary statistic for substitution model adequacy")
public interface SMAStatistic<T> {
    String getName();

    /**
     * @return the name=value pairs of this summary statistic for the given alignment data
     */
    public T [] getStatistics(Alignment data);
}
