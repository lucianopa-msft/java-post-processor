package com.azure.communication.configuration;

public interface IConfiguration<Value> {
    public String getName();
    public Value getConfigurationValue();
}
