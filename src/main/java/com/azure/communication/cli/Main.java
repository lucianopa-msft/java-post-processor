package com.azure.communication.cli;

import com.azure.communication.configuration.ConfigurationManager;
import com.azure.communication.io.SourceManager;
import com.azure.communication.processors.ProcessorRunner;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Java Post Processor").build()
                .defaultHelp(true)
                .description("Post Process post apigen Java Files");

        parser.addArgument( "--source-dirs")
                .nargs("+")
                .required(true)
                .help("Specify the directory of the source files to process.");

        parser.addArgument( "--configuration")
                .required(true)
                .help("Specify the processor configuration settings");

        try {
            Namespace ns = parser.parseArgs(args);
            List<String> paths = ns.getList("source_dirs");
            String configPath = ns.getString("configuration");
            ConfigurationManager configurationManager = new ConfigurationManager(Paths.get(configPath));

            Set<Path> javaSources = SourceManager.getDirectorySourceFiles(paths.get(0));
            ProcessorRunner runner = new ProcessorRunner(javaSources, configurationManager);
            runner.run();
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}