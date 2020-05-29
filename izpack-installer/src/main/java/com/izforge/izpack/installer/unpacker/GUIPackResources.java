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
 * {@link PackResources} implementation for the GUI-based installer.
 * <p/>
 * This supports both local and web-based pack resources.
 */
public class GUIPackResources extends AbstractPackResources
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(GUIPackResources.class.getName());

    /**
     * Constructs a {@code GUIPackResources}.
     *
     * @param resources   the resources
     * @param installData the installation data
     */
    public GUIPackResources(Resources resources, InstallData installData)
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
                    WebRepositoryAccessor.getCachedUrl(packURL, tempFolder, packFileName, true)
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
