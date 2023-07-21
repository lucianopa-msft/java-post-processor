package com.azure.communication.cli;

import com.azure.communication.io.SourceManager;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.util.List;
import java.util.Set;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Java Post Processor").build()
                .defaultHelp(true)
                .description("Post Process post apigen Java Files");
        parser.addArgument("-d", "--source-dirs")
                .nargs("+")
                .required(true)
                .help("Specify hash function to use");


        try {
            Namespace ns = parser.parseArgs(args);
            List<String> paths = ns.getList("source_dirs");
            System.out.println(paths);

            Set<String> javaSources = SourceManager.getDirectorySourceFiles(paths.get(0));
            System.out.println(javaSources);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        } catch (IOException e) {
            System.exit(1);
        }

    }
}