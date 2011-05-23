package aethers.notebook.core;

import aethers.notebook.core.LoggerServiceIdentifier;

import android.location.Location;

interface AppenderService
{
    void configure();
    
    boolean isRunning();
    
    void start();
    
    void stop();
    
    void log(in LoggerServiceIdentifier identifier, in long timestamp, in Location location, in byte[] data);
}