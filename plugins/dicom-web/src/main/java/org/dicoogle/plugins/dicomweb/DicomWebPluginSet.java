/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle-plugin-sample.
 *
 * Dicoogle/dicoogle-plugin-sample is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle-plugin-sample is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dicoogle.plugins.dicomweb;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.PluginSet;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;


@PluginImplementation
public class DicomWebPluginSet implements PluginSet {
    // use slf4j for logging purposes
    private static final Logger logger = LoggerFactory.getLogger(DicomWebPluginSet.class);
    
    // We will list each of our plugins as an attribute to the plugin set

    private final DicomWebJettyPlugin jettyWeb;

    // Additional resources may be added here.
    private ConfigurationHolder settings;
    
    public DicomWebPluginSet() throws IOException {
        logger.info("Initializing RSI Plugin Set");

        // construct all plugins here
        this.jettyWeb = new DicomWebJettyPlugin();

        
        logger.info("RSI Plugin Set is ready");
    }
    

    /** This method is used to retrieve a name for identifying the plugin set. Keep it as a constant value.
     * 
     * @return a unique name for the plugin set
     */
    @Override
    public String getName() {
        return "DICOMweb";
    }



    @Override
    public Collection<JettyPluginInterface> getJettyPlugins() {
        return Collections.singleton((JettyPluginInterface) this.jettyWeb);
    }

    @Override
    public void shutdown() {
        logger.info("Plugin sample will shutdown");
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

}