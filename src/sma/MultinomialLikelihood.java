package sma;


import beast.core.BEASTObject;
import beast.core.Description;
import beast.evolution.alignment.Alignment;

@Description("Maximum likelihood value of the multinomial likelihood for an alignment")
public class MultinomialLikelihood extends BEASTObject implements SMAStatistic<Double> {

	@Override
	public void initAndValidate() {
	}

	@Override
	public String getName() {
		return "multinomial-likelihood";
	}

	@Override
	public Double [] getStatistics(Alignment data) {
		double p = 0;
		double n = 0;
		for (int w : data.getWeights()) {
			n += w;
		}
		for (int i = 0; i < data.getPatternCount(); i++) {
			int w = data.getPatternWeight(i);
			p += w * Math.log(w/n);
		}
		return new Double[]{p};
	}

}
