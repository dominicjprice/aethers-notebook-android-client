package aethers.notebook.core;

import aethers.notebook.core.AppenderServiceIdentifier;
import aethers.notebook.core.LoggerServiceIdentifier;
import aethers.notebook.core.UnmanagedAppenderService;

interface AethersNotebook
{
    void log(in LoggerServiceIdentifier identifier, in byte[] data);
    
    void registerManagedLogger(in LoggerServiceIdentifier identifier);
    
    boolean isManagedLoggerInstalled(in LoggerServiceIdentifier identifier);
    
    void deregisterManagedLogger(in LoggerServiceIdentifier identifier);
    
    void registerManagedAppender(in AppenderServiceIdentifier identifier);
    
    boolean isManagedAppenderInstalled(in AppenderServiceIdentifier identifier);
    
    void deregisterManagedAppender(in AppenderServiceIdentifier identifier);
    
    void registerUnmanagedAppender(
            in AppenderServiceIdentifier identifier,
            in UnmanagedAppenderService service);
    
    void deregisterUnmanagedAppender(in AppenderServiceIdentifier identifier);
}