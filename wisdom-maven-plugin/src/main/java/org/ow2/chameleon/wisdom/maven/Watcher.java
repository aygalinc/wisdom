package org.ow2.chameleon.wisdom.maven;

import org.ow2.chameleon.wisdom.maven.processors.ProcessorException;

import java.io.File;

/**
 * The interface implemented by objects participating to the <em>watch mode</em>.
 */
public interface Watcher {

    public void tearDown();

    public boolean accept(File file);

    public boolean fileCreated(File file) throws WatchingException;

    public boolean fileUpdated(File file) throws WatchingException;

    public boolean fileDeleted(File file) throws WatchingException;

}