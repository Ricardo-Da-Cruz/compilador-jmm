package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;

public class Analysis implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        return new JmmSemanticsResult(jmmParserResult, new ClassTable(jmmParserResult.getRootNode()),new ArrayList<Report>());
        /*
        FINNALY I CAN COMMIT??
         */
    }
}
