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
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.web.WebRepositoryAccessor;
import com.izforge.izpack.util.IoHelper;
import com.izforge.izpack.util.OsVersion;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Console-based implementation of the {@link PackResources} interface.
 *
 * @author Tim Anderson
 */
public class ConsolePackResources extends AbstractPackResources
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ConsolePackResources.class.getName());

    /**
     * Constructs a {@code DefaultPackResources}.
     *
     * @param resources the local resources
     */
    public ConsolePackResources(Resources resources, InstallData installData)
    {
        super(resources, installData);
    }

    @Override
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
                tempFile = new File(
                    WebRepositoryAccessor.getCachedUrl(packURL, tempFolder, packFileName, false)
                );

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
            String startPath;
            if (OsVersion.IS_WINDOWS) {
                startPath = "jar:file:/";
            }
            else {
                startPath = "jar:file:";
            }
            path = startPath + packLocalFile.getPath() + "!/packs/pack-" + name;
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
}
