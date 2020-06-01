/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2012 Tim Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.installer.unpacker;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.exception.ResourceException;
import com.izforge.izpack.api.exception.ResourceInterruptedException;
import com.izforge.izpack.api.exception.ResourceNotFoundException;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.util.IoHelper;
import com.izforge.izpack.util.OsVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;


/**
 * Abstract implementation of the {@link PackResources} interface.
 *
 * @author Tim Anderson
 */
public abstract class AbstractPackResources implements PackResources
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(AbstractPackResources.class.getName());

    /**
     * Temporary directory for web installers.
     */
    protected static final String WEB_TEMP_SUB_PATH = "/IzpackWebTemp";

    /**
     * The resources.
     */
    private final Resources resources;

    /**
     * The installation data.
     */
    private final InstallData installData;

    /**
     * Constructs an {@code AbstractPackResources}.
     *
     * @param resources   the resources
     * @param installData the installation data
     */
    public AbstractPackResources(Resources resources, InstallData installData)
    {
        this.installData = installData;
        this.resources = resources;
    }

    /**
     * Returns the stream to a resource.
     *
     * @param name the resource name
     * @return a stream to the resource
     * @throws ResourceNotFoundException    if the resource cannot be found
     * @throws ResourceInterruptedException if resource retrieval is interrupted
     * @throws ResourceException            for any other resource error
     */
    @Override
    public InputStream getPackStream(String name)
    {
        InputStream result;
        String webDirURL = installData.getInfo().getWebDirURL();

        if (webDirURL == null)
        {
            result = getLocalPackStream(name);
        }
        else
        {
            result = getWebPackStream(name, webDirURL);
        }

        return result;
    }

    /**
     * Returns the stream to a resource.
     *
     * @param name the resource name
     * @return a stream to the resource
     * @throws ResourceNotFoundException if the resource cannot be found
     * @throws ResourceException         for any other resource error
     */
    @Override
    public InputStream getInputStream(String name)
    {
        // TODO - this is invoked to get multi-volume info, so should check on web dir.
        return resources.getInputStream(name);
    }

    /**
     * Returns a stream to a local pack.
     *
     * @param name the pack name
     * @return the pack stream
     */
    private InputStream getLocalPackStream(String name)
    {
        return resources.getInputStream("packs/pack-" + name);
    }

    /**
     * Returns the stream to a web-based pack resource.
     *
     * @param name      the resource name
     * @param webDirURL the web URL to load the resource from
     * @return a stream to the resource
     * @throws ResourceNotFoundException    if the resource cannot be found
     * @throws ResourceInterruptedException if resource retrieval is interrupted
     */
    protected InputStream getWebPackStream(String name, String webDirURL)
    {
        InputStream result;

        InstallData installData = getInstallData();

        String packFileName = name + ".jar";
        String path = null;

        // Look first in same directory as primary jar, then download it if not found
        File packLocalFile = new File(installData.getVariable(InstallData.INSTALL_PATH), packFileName);
        if (packLocalFile.exists() && packLocalFile.canRead())
        {
            logger.info("Found local pack " + packLocalFile.getAbsolutePath());
            try {
                path = "jar:" + packLocalFile.toURI().toURL() + "!/packs/pack-" + name;
            } catch(MalformedURLException exception) {
                throw new ResourceException("Malformed URL", exception);
            }
        }
        else
        {
            String packURL = webDirURL + "/" + name.replace(" ", "%20");
            logger.info("Downloading remote pack " + packURL);
            String tempFolder = IoHelper.translatePath(installData.getInfo().getUninstallerPath()
                                                       + WEB_TEMP_SUB_PATH, installData.getVariables());
            File tempFile;
            try
            {
                tempFile = new File(downloadPack(packURL, tempFolder, packFileName));

                packLocalFile = Files.move(
                    tempFile.toPath(),
                    Paths.get(installData.getVariable(InstallData.INSTALL_PATH), tempFile.getName()),
                    StandardCopyOption.REPLACE_EXISTING
                ).toFile();
            }
            catch (InterruptedIOException exception)
            {
                throw new ResourceInterruptedException("Retrieval of " + webDirURL + " interrupted", exception);
            }
            catch (IOException exception)
            {
                throw new ResourceException("Failed to read " + webDirURL, exception);
            }
            catch (NoSuchAlgorithmException exception)
            {
                throw new ResourceException("Failed to get the checksum for " + webDirURL, exception);
            }

            String separator = "";
            if (OsVersion.IS_WINDOWS) {
                separator = "/";
            }
            path = "jar:file:" + separator + packLocalFile.getPath() + "!/packs/pack-" + name;
        }

        try
        {
            URL url = new URL(path);
            result = url.openStream();
        }
        catch (IOException exception)
        {
            throw new ResourceException("Failed to read pack", exception);
        }
        return result;
    }

    /**
     * downloads the pack and returns its location
     *
     * @return location of the downloaded pack
     */
    protected abstract String downloadPack(String url, String tempFolder, String packFileName)
        throws IOException, NoSuchAlgorithmException;

    /**
     * Returns the installation data.
     *
     * @return the installation data
     */
    protected InstallData getInstallData()
    {
        return installData;
    }

}
