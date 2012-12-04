package com.roo.bootstrap;

/**
 * Interface of commands that are available via the Roo shell.
 *
 * @since 1.1.1
 */
public interface BootstrapOperations {

    /**
     * @param propertyName to obtain (required)
     * @return a message that will ultimately be displayed on the shell
     */
    String getProperty(String propertyName);
    

    /**
     * Install tags used for MVC scaffolded apps into the target project.
     */
	void installBootstrap();
	
	/**
     * Indicate of the install tags command should be available
     * 
     * @return true if it should be available, otherwise false
     */
	boolean isInstallBootstrapAvailable();
    
}