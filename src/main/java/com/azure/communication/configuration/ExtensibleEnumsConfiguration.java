package com.azure.communication.configuration;

import java.util.Set;

public class ExtensibleEnumsConfiguration implements IConfiguration<Set<String>> {
    public static String CONFIGURATION_NAME = "extensible_enums";
    private Set<String> mEnumClassNames;

    public ExtensibleEnumsConfiguration(Set<String> enumClassNames) {
        this.mEnumClassNames = enumClassNames;
    }

    @Override
    public Set<String> getConfigurationValue() {
        return mEnumClassNames;
    }

    @Override
    public String getName() {
        return CONFIGURATION_NAME;
    }
}
