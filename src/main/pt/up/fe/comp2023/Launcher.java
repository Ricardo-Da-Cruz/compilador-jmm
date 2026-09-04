package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.jasmin.Jasmin;
import pt.up.fe.comp2023.ollir.Ollir;
import pt.up.fe.comp2023.semantics.Analyser;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        if (parserResult.getReports().size() > 0) {
            System.err.println("Num errors:" + parserResult.getReports().size());
            return;
        }

        // Print AST
        System.out.println(parserResult.getRootNode().toTree());


        ClassTable a = new ClassTable(parserResult.getRootNode());
        //print the values of the class table
        System.out.println("Symbol Table:");
        System.out.println("Imports:");
        for (String s : a.getImports())
            System.out.println("\t" + s);
        System.out.println("Class Name:" + a.getClassName());

        System.out.println("Fields:");
        for (Symbol s : a.getFields())
            System.out.println("\t" + s.print());
        System.out.println("Methods:");
        for (String s : a.getMethods()){
            System.out.println("\tName:" + s);
            System.out.println("\tReturn " + a.getReturnType(s));
            System.out.println("\tModifiers: " +a.isStatic(s) );

            System.out.println("\tParameters:");
            for (Symbol p : a.getParameters(s))
                System.out.println("\t\t" +p.print());

            System.out.println("\tLocal Variables:\n");
            for (Symbol l : a.getLocalVariables(s))
                System.out.println("\t\t" +l.print());
        }

        // Semantic stage
        Analyser semantic = new Analyser();
        JmmSemanticsResult semanticsResult = semantic.semanticAnalysis(parserResult);


        //OlliR stage
        System.out.println("fjdkva");
        Ollir ollir = new Ollir();
        semanticsResult = ollir.optimize(semanticsResult);

        System.out.println(semanticsResult.getRootNode().toTree());

        var result = ollir.toOllir(semanticsResult);

        //Jasmin
        Jasmin jasmin = new Jasmin();
        jasmin.toJasmin(result);
        // ... add remaining stages




    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
