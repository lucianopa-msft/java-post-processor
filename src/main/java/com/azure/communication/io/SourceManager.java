package com.azure.communication.io;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceManager {

    public static Set<Path> getDirectorySourceFiles(String dir) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.java");
        try (Stream<Path> stream = Files.walk(Paths.get(dir))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(file -> matcher.matches(file.getFileName()))
                    .collect(Collectors.toSet());
        }
    }
}
