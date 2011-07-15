package aethers.notebook.core;

import aethers.notebook.core.LoggerServiceIdentifier;

import android.location.Location;

interface UnmanagedAppenderService
{    
    void log(in LoggerServiceIdentifier identifier, in long timestamp, in Location location, in byte[] data);
}