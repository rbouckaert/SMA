package sma.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.app.util.Application;
import beast.app.util.LogFile;
import beast.app.util.OutFile;
import beast.app.util.XMLFile;
import beast.core.Description;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.util.LogAnalyser;
import beast.util.XMLParser;
import beast.util.XMLParserException;
import sma.SMAMCMC;
import sma.SMAStatistic;

@Description("Analyse set of trees created through Substitution Model Adequacy")
public class SubstitutionModelAdequacyAnalyser extends Runnable {
	// assume the root dir has subdirs called run0, run1, run2,...
	public Input<OutFile> outputInput = new Input<>("output", "base name of file where alignments are stored (if any). "
			+ "Note that number + '.fas' is added, so if output='/tmp/xyz' then files are called /tmp/xyz0, /tmp/xyz1, etc.");
	public Input<XMLFile> xmlFileInput = new Input<>("xml", "XML file containing original alignment and statistics"); 
	public Input<Alignment> dataInput = new Input<>("data", "original alignment for which to calculate statistics. If specified, xml f");
	
	// SMAStatistics are matched to columns in logFile by name specified as SMAStatistic.getName()
	public Input<List<SMAStatistic<?>>> statsInput = new Input<>("statistic", "set of statistics that need to be produced", new ArrayList<>());

	public Input<LogFile> logFileInput = new Input<>("logFile", "file with trace log with SMA statistics"); 
	public Input<Integer> burninInput = new Input<>("burnin","burn in percentage, default 10", 10);
	
	
	public SubstitutionModelAdequacyAnalyser() {}
		
	public SubstitutionModelAdequacyAnalyser(Alignment data, List<SMAStatistic<?>> stats, LogFile logFile, int burninPercentage) {
		dataInput.setValue(data, this);
		statsInput.get().addAll(stats);
		logFileInput.setValue(logFile, this);
		burninInput.setValue(burninPercentage, this);
		initAndValidate();
	}
	
	List<SMAStatistic<?>> stats;
	
	@Override
	public void initAndValidate() {
		stats = statsInput.get();
	}
	
	@Override
	public void run() throws Exception {
		
		Alignment origTree = getOriginalAlignment();
		
		// init stats
		Map<String, Object> origStats = new LinkedHashMap<>();
		for (SMAStatistic<?> stat : stats) {
			Object result = stat.getStatistics(origTree);
			origStats.put(stat.getName(), result);
		}
		
		LogAnalyser trace = new LogAnalyser(logFileInput.get().getPath(), burninInput.get(), true, false);
		
		// report stats
		reportStats(origStats, trace);
	}

	

	private void reportStats(Map<String, Object> origStats, LogAnalyser trace) {
		List<String> labels = getLabels(origStats);
		int n = trace.getTrace(0).length;
		
		Log.info("statistic\t2.5%\t5%\t50%\t95%\t97.5%\tp-value");
		for (String label : labels) {
			Object stat = origStats.get(label);
			if (stat instanceof Double[]) {
				Double origStat = ((Double[]) stat)[0];
				int i = matchingLabel(label, trace.getLabels());
				Double [] stats = trace.getTrace(i); 
				Arrays.sort(stats);
				double r2_5  = stats[(int)(2.5 * n / 100.0)];
				double r5    = stats[(int)(5 * n / 100.0)];
				double r50   = stats[(int)(50 * n / 100.0)];
				double r95   = stats[(int)(95 * n / 100.0)];
				double r97_5 = stats[(int)(97.5 * n / 100.0)];
				int p = 0;
				while (p < stats.length && stats[p] > origStat) {
					p++;
				}
				double pValue = (double) p / (double) n;
				Log.info.print(label + "\t");
				Log.info.print(String.format("%6.3e",r2_5) + "\t");
				Log.info.print(String.format("%6.3e",r5) + "\t");
				Log.info.print(String.format("%6.3e",r50) + "\t");
				Log.info.print(String.format("%6.3e",r95) + "\t");
				Log.info.print(String.format("%6.3e",r97_5) + "\t");
				Log.info.print(String.format("%6.3e",pValue) + "\n");
				
			}			
		}
	}

	private int matchingLabel(String label, List<String> labels) {
		for (int i = 0; i < labels.size(); i++) {
			if (labels.get(i).equals(label)) {
				return i+1;
			}
		}
		throw new IllegalArgumentException("Could not find statistic '" + label +"' in log file labels:" +
				Arrays.toString(labels.toArray()));
	}

	private List<String> getLabels(Map<String, Object> stats) {
		List<String> labels = new ArrayList<>();
		for (String label : stats.keySet()) {
			labels.add(label);
		}
		return labels;
	}

	private Alignment getOriginalAlignment() throws IOException, SAXException, ParserConfigurationException, XMLParserException {
		if (dataInput.get() != null) {
			return dataInput.get();
		}
		XMLParser parser = new XMLParser();
		Runnable mcmc = parser.parseFile(xmlFileInput.get());
		Alignment data = SMAMCMC.getData(mcmc);
		return data;
	}
	

	public static void main(String[] args) throws Exception {
		new Application(new SubstitutionModelAdequacyAnalyser(), "Tree Model Adeqaucy Analyser", args);
	}
}
