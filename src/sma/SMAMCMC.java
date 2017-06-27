package sma;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.app.util.LogFile;
import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.MCMC;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import sma.util.SubstitutionModelAdequacyAnalyser;

@Description("Run SubstitutionModelAdequacyAnalyser after MCMC")
public class SMAMCMC extends MCMC {
	public Input<Integer> smaBurninInput = new Input<>("SMABurnin","burn in percentage used by SMAAnalyser when reading stats from log, default 10", 10);

	@Override
	public void run() throws IOException, SAXException, ParserConfigurationException {
		//super.run();
		analyse();
	}

	private void analyse() {		
		List<SMAStatistic<?>> stats = null;
		LogFile logFile = null;
		for (Logger logger :  loggersInput.get()) {
			List<?> logs = logger.loggersInput.get();
			if (logs.size() > 0 && logs.get(0) instanceof SMALogger) {
				SMALogger smaLogger = (SMALogger) logs.get(0);
				logFile = new LogFile(logger.fileNameInput.get());
				stats = smaLogger.statsInput.get();
			}
		}
		
		if (stats == null) {
			Log.warning("Could not find SMALogger in this analysis");
			return;
		}
		
		Alignment data = getData(this);
		
		SubstitutionModelAdequacyAnalyser analyser = new SubstitutionModelAdequacyAnalyser(data, stats, logFile, smaBurninInput.get());
		try {
			analyser.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	static public Alignment getData(BEASTInterface bi) {
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
	
}
