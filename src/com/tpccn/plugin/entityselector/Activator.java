package com.tpccn.plugin.entityselector;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.tpccn.plugin.entityselector"; //$NON-NLS-1$
	
	public static final String CLASS_ICON_PATH = "icons/class_obj.gif"; //$NON-NLS-1$
	public static final String PACKAGE_ICON_PATH = "icons/package_obj.gif"; //$NON-NLS-1$

	public static ImageDescriptor CLASS_ICON_DESCRIPTOR;
	public static ImageDescriptor PACKAGE_ICON_DESCRIPTOR ;
	
	// The shared instance
	private static Activator plugin;

	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		CLASS_ICON_DESCRIPTOR = Activator.imageDescriptorFromPlugin(PLUGIN_ID, CLASS_ICON_PATH);
		PACKAGE_ICON_DESCRIPTOR = Activator.imageDescriptorFromPlugin(PLUGIN_ID, PACKAGE_ICON_PATH);;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
