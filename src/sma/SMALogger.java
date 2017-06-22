package sma;

import java.io.File;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.app.seqgen.SimulatedAlignment;
import beast.app.util.OutFile;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.evolution.alignment.Sequence;
import beast.evolution.likelihood.GenericTreeLikelihood;

@Description("Logger for substitution model adequacy test statistics")
public class SMALogger extends BEASTObject implements Loggable {
	public Input<List<SMAStatistic<?>>> statsInput = new Input<>("statistic", "Summary statistic on a alignments", new ArrayList<>(), Validate.REQUIRED);
	public Input<GenericTreeLikelihood> likelihoodInput = new Input<>("likelihood", "likelihood to simulate alignment for used to generate statistics", Validate.REQUIRED);
	public Input<OutFile> outputInput = new Input<>("output", "base name of file where alignments are stored (if any). "
			+ "Note that number + '.fas' is added, so if output='/tmp/xyz' then files are called /tmp/xyz0, /tmp/xyz1, etc.");
	
	public Input<Boolean> storeDataInput = new Input<>("storeData","flag to indicate each simulated alignment should be stored to disk", false);
	
	List<SMAStatistic<?>> stats;
	GenericTreeLikelihood likelihood;
	
	@Override
	public void initAndValidate() {
		stats = statsInput.get();
		likelihood = likelihoodInput.get();
	}

	@Override
	public void init(PrintStream out) {
		for (SMAStatistic<?> stat : stats) {
			out.print(stat.getName() + "\t");
		}
	}

	int fileCount = 0;
	@Override
	public void log(int sample, PrintStream out) {
		SimulatedAlignment data = new SimulatedAlignment();
		data.initByName("data", likelihood.dataInput.get(),
				"tree", likelihood.treeInput.get(),
				"siteModel", likelihood.siteModelInput.get(),
				"branchRateModel", likelihood.branchRateModelInput.get(),
				"sequencelength", likelihood.dataInput.get().getSiteCount());
		data.simulate();
		if (storeDataInput.get()) {
			String filename = outputInput.get().getPath() + fileCount + ".fas";
			PrintStream fasta;
			try {
				fasta = new PrintStream(new File(filename));
				for (Sequence seq : data.sequenceInput.get()) {
					fasta.println(">" + seq.taxonInput.get());
					fasta.println(seq.dataInput.get());
				}
				fasta.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());
			}
			fileCount++;
		}
		for (SMAStatistic<?> stat : stats) {
			Object[] result = stat.getStatistics(data);
			if (result.length != 1) {
				throw new IllegalArgumentException("expected only 1 result from " + stat.getClass().getName() + " but got " + result.length);
			}
			out.print(result[0].toString() + "\t");
		}
	}

	@Override
	public void close(PrintStream out) {
	}

}
