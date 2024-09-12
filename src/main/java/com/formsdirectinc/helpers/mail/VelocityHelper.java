/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.formsdirectinc.helpers.mail;

import org.apache.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.EasyFactoryConfiguration;
import org.apache.velocity.tools.generic.ResourceTool;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author saravana
 */
public class VelocityHelper {

    private static final Logger log = Logger.getLogger(VelocityHelper.class);

    /**
     * Merge a template and return the result as a StringBuilder
     *
     * @param templateFileName Template file to merge
     * @param contextParams    Parameters to pass to Velocity
     * @return StringBuilder with merged template
     */
    public StringBuilder mergeTemplateToStringBuilder(String templateFileName, Map<String, Object> contextParams) {
        return stringReaderToBuilder((StringReader) mergeTemplate(templateFileName, contextParams));
    }

    /**
     * Merge a template and return the result as a StringBuilder
     *
     * @param templateFileName Template file to merge
     * @param templateEncoding Template character set
     * @param contextParams    Parameters to pass to Velocity
     * @return StringBuilder with merged template
     */
    public StringBuilder mergeTemplateToStringBuilder(String templateFileName, String templateEncoding,
                                                      Map<String, Object> contextParams) {
        return stringReaderToBuilder(
                (StringReader) mergeTemplate(templateFileName, templateEncoding, contextParams, null));
    }
    
    /**
     * Merge a template and return the result as a StringBuilder
     *
     * @param templateFileName Template file to merge
     * @param templateEncoding Template character set
     * @param contextParams    Parameters to pass to Velocity
     * @return StringBuilder with merged template
     */
    public StringBuilder mergeTemplateToStringBuilder(String templateFileName, String templateEncoding,
                                                      Map<String, Object> contextParams, String resourceBundleBaseName) {
        return stringReaderToBuilder(
                (StringReader) mergeTemplate(templateFileName, templateEncoding, contextParams, resourceBundleBaseName));
    }

    /**
     * Merge a template and return the result as a Reader
     *
     * @param templateFileName Template file to merge
     * @param contextParams    Parameters to pass to Velocity
     * @return Reader with merged template
     */
    public Reader mergeTemplate(String templateFileName, Map<String, Object> contextParams) {
        return mergeTemplate(templateFileName, System.getProperty("file.encoding"), contextParams, null);
    }

    public Reader mergeTemplate(String templateFileName, String templateEncoding, Map<String, Object> contextParams) {
        return mergeTemplate(templateFileName, templateEncoding, contextParams, null);
    }

    /**
     * Merge a template and return the result as a Reader, with a context
     * provided
     *
     * @param templateFileName Velocity template file name
     * @param context          Pre-created Velocity context
     * @return Reader with merged template data
     */
    public Reader mergeTemplate(String templateFileName, Context context) {
        return mergeTemplate(templateFileName, System.getProperty("file.encoding"), context);
    }

    /**
     * Merge a template and return the result as a Reader. This version of the
     * method allows callers to specify the template encoding. We construct a
     * new VelocityEngine each time because the Velocity documentation makes it
     * clear that using the static Velocity class allows us to init() it only
     * once - that would cause too many cases of callers stepping on each other.
     *
     * @param templateFileName File name of the velocity template
     * @param templateEncoding Template encoding (defaults to ISO-8859-1 if not
     *                         specified)
     * @param contextParams    Parameters to pass to template
     * @return Reader with merged template
     */
    public Reader mergeTemplate(String templateFileName, String templateEncoding,
                                Map<String, Object> contextParams, String resourceBundleBaseName) {

        //
        // Create ToolManager and ask it to create a context for us. This context should
        // have all the tools available, lazy-loaded as they are needed.
        //
        Context context;
        ToolManager toolManager = new ToolManager();

        if (resourceBundleBaseName != null) {
            EasyFactoryConfiguration config = new EasyFactoryConfiguration();
            config.toolbox("application").tool(ResourceTool.class).property("bundles", resourceBundleBaseName);
            toolManager.configure(config);
            context = toolManager.createContext();

        } else {
            context = toolManager.createContext();
        }

        for (String key : contextParams.keySet()) {
            context.put(key, contextParams.get(key));
        }

        return mergeTemplate(templateFileName, templateEncoding, context);
    }

    /**
     * Merge a template to a Reader, given the template file name, the encoding,
     * and a Velocity Context
     *
     * @param templateFileName Velocity template file name
     * @param templateEncoding Template encoding
     * @param context          Velocity Context
     * @return Reader with merged template data
     */
    public Reader mergeTemplate(String templateFileName, String templateEncoding, Context context) {
        StringWriter writer = new StringWriter();

        //
        // If the template file name starts with classpath: (This is the spring convention),
        // set the resource loader to the ClasspathResourceLoader
        //

        if (templateFileName.startsWith("classpath:")) {
            try {

                VelocityEngine classpathVelocityEngine = new VelocityEngine();
                classpathVelocityEngine.setProperty("runtime.log", "");
                classpathVelocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.Log4JLogChute");
                classpathVelocityEngine.setProperty("runtime.log.logsystem.log4j.logger", "org.apache.velocity");
                classpathVelocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
                classpathVelocityEngine
                        .setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
                classpathVelocityEngine.init();
                String resolvedFileName = templateFileName.substring(templateFileName.indexOf(':') + 1);
                classpathVelocityEngine.mergeTemplate(resolvedFileName, templateEncoding == null
                                                              ? System.getProperty("file.encoding") : templateEncoding,
                                                      context, writer);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            VelocityEngine fileVelocityEngine = new VelocityEngine();
            try {
                fileVelocityEngine.mergeTemplate(templateFileName,
                                                 templateEncoding == null ? System.getProperty("file.encoding")
                                                         : templateEncoding, context, writer);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //
        // Merge and return result
        //

        return new StringReader(writer.toString());
    }

    /**
     * Convert a StringReader to a StringBuilder
     *
     * @param reader StringReader to convert
     * @return Reader as StringBuilder
     */
    private StringBuilder stringReaderToBuilder(StringReader reader) {

        StringBuilder builder = new StringBuilder();
        int i;

        try {
            while ((i = reader.read()) != -1) {
                builder.append((char) i);
            }
        } catch (IOException ie) {
            log.error("IOException reading from StringReader: " + reader, ie);
        }

        return builder;
    }
}
