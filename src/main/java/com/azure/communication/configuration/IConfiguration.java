package com.azure.communication.configuration;

public interface IConfiguration<Value> {
    String getName();
    Value getConfigurationValue();
}
