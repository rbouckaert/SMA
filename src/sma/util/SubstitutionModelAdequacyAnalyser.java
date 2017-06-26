package sma.util;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
import beast.app.util.TreeFile;
import beast.app.util.XMLFile;
import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import beast.util.NexusParser;
import beast.util.XMLParser;
import beast.util.XMLParserException;
import sma.SMAStatistic;



@Description("Analyse set of trees created through Substitution Model Adequacy")
public class SubstitutionModelAdequacyAnalyser extends Runnable {
	// assume the root dir has subdirs called run0, run1, run2,...
	public Input<OutFile> outputInput = new Input<>("output", "base name of file where alignments are stored (if any). "
			+ "Note that number + '.fas' is added, so if output='/tmp/xyz' then files are called /tmp/xyz0, /tmp/xyz1, etc.");
	public Input<XMLFile> xmlFileInput = new Input<>("xml", "XML file containing original alignment and statistics"); 
	public Input<Alignment> dataInput = new Input<>("data", "original alignment for which to calculate statistics. If specified, xml f");

	public Input<List<SMAStatistic<?>>> statsInput = new Input<>("statistic", "set of statistics that need to be produced", new ArrayList<>());
	public Input<LogFile> logFileInput = new Input<>("logFile", "file with trace log with SMA statistics"); 
	
	
	
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
			Map<String,?> map = stat.getStatistics(origTree);
			for (String name : map.keySet()) {
				origStats.put(name, map.get(name));
			}
		}
		
		
		String rootDir = rootDirInput.get();
		int treeCount = treeCountInput.get();
		Map<String, Object>[] treeStats = new LinkedHashMap[treeCount];

		for (int i = 0; i < treeCount; i++) {
			treeStats[i] = new LinkedHashMap<>();
			NexusParser parser = new NexusParser();
			File file = new File(rootDir + "/run" + i + "/output.tree");
			parser.parseFile(file);
			Tree tree = parser.trees.get(0);
			for (TreeSummaryStatistic<?> stat : stats) {
				Map<String,?> map = stat.getStatistics(tree);
				for (String name : map.keySet()) {
					treeStats[i].put(name, map.get(name));
				}
			}
		}
		
		// report stats
		reportStats(origStats, treeStats);
		
		// log stats
		PrintStream out = System.out;
		if (outFileInput.get() != null && !outFileInput.get().getName().equals("[[none]]")) {
			out = new PrintStream(outFileInput.get());			
		}
		logStats(out, origStats, treeStats);
		
	}

	

	private void reportStats(Map<String, Object> origStats, Map<String, Object>[] treeStats) {
		List<String> labels = getLabels(origStats);
		int n = treeStats.length;
		
		Log.info("statistic\t2.5%\t5%\t50%\t95%\t97.5%\tp-value");
		for (String label : labels) {
			Object stat = origStats.get(label);
			if (stat instanceof Double) {
				Double origStat = (Double) stat;
				Double [] stats = new Double[n];
				for (int i = 0; i < n; i++) {
					stats[i] = (Double) treeStats[i].get(label);
				}
				Arrays.sort(stats);
				double r2_5  = stats[(int)(2.5 * n / 100.0)];
				double r5    = stats[(int)(5 * n / 100.0)];
				double r50   = stats[(int)(50 * n / 100.0)];
				double r95   = stats[(int)(95 * n / 100.0)];
				double r97_5 = stats[(int)(97.5 * n / 100.0)];
				int p = 0;
				while (stats[p] > origStat) {
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



	private void logStats(PrintStream out, Map<String, Object> origStats, Map<String, Object>[] treeStats) {
		List<String> labels = getLabels(origStats);
		
		out.print("state\t");
		for (String label : labels) {
			out.print(label+"\t");
		}
		out.println();
		
		// first line contains stats for original tree
		out.print("0\t");
		for (String label : labels) {
			Object o = origStats.get(label);
			out.print(o.toString() + "\t");
		}
		out.println();
		
		// print out stats for sampled trees
		for (int i = 0; i < treeStats.length; i++) {
			out.print((i+1) + "\t");
			Map<String, Object> stats = treeStats[i];
			for (String label : labels) {
				Object o = stats.get(label);
				out.print(o.toString() + "\t");
			}
			out.println();			
		}		
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
		Alignment data = getData(mcmc);
		return data;
	}
	
	private Alignment getData(BEASTInterface bi) {
		for (BEASTInterface bi2 : bi.listActiveBEASTObjects()) {
			if (bi2 instanceof Alignment) {
				return (Alignment) bi2;
			}
			bi2 = getData(bi2);
			if (bi2 != null) {
				return (Alignment) bi2;
			}
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		new Application(new SubstitutionModelAdequacyAnalyser(), "Tree Model Adeqaucy Analyser", args);
	}
}
