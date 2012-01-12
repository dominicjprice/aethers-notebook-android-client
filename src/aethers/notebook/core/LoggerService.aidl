package aethers.notebook.core;

import aethers.notebook.core.Action;
import aethers.notebook.core.LoggerServiceIdentifier;

interface LoggerService
{
    LoggerServiceIdentifier getIdentifier();

    void configure();
    
    boolean isRunning();
    
    void start();
    
    void stop();
    
    List<Action> listActions();
    
    void doAction(in Action action);
}