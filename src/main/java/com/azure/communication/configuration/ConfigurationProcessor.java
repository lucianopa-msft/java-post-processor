package com.azure.communication.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;

public class ConfigurationProcessor {

    private final List<IConfiguration> mConfigurations = new ArrayList<>();

    public ConfigurationProcessor(Path configurationFile) throws IOException, IllegalArgumentException {
        if (configurationFile == null) {
            throw new IllegalArgumentException("We must provide a non-null configuration file.");
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.json");
        if (!matcher.matches(configurationFile.getFileName())) {
            throw new IllegalArgumentException("We must provide valid configuration *.json file.");
        }

        String filePath = configurationFile.toAbsolutePath().toFile().getPath();
        FileReader fr = new FileReader(filePath);
        JsonElement root = new Gson().fromJson(fr, JsonElement.class);

        addExtensibleEnumsConfiguration(root);
    }

    private void addExtensibleEnumsConfiguration(JsonElement root) {
        JsonArray object = root
                .getAsJsonObject()
                .get(ExtensibleEnumsConfiguration.CONFIGURATION_NAME)
                .getAsJsonArray();

        if (object == null) {
            return;
        }

        Set<String> extensibleEnumsList = new HashSet<>();
        for (int i = 0; i < object.size(); i++) {
            String enumName = object.get(i).getAsString();
            if (enumName != null) {
                extensibleEnumsList.add(enumName);
            }
        }

        mConfigurations.add(new ExtensibleEnumsConfiguration(extensibleEnumsList));
    }

    public <T> Optional<T> findConfiguration(Class<T> classType) {
        if (IConfiguration.class.isAssignableFrom(classType)) {
            Optional<IConfiguration> result = mConfigurations
                .stream()
                .filter(config -> config.getClass() == classType)
                .findFirst();
            return (Optional<T>)result;
        }
        return Optional.empty();
    }
}
