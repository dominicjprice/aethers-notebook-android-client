package aethers.notebook.core;

import aethers.notebook.core.LoggerServiceIdentifier;

interface AethersNotebook
{
    void log(in LoggerServiceIdentifier identifier, in byte[] data);
}