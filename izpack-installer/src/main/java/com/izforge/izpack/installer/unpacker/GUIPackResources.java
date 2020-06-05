package com.izforge.izpack.installer.unpacker;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.web.WebRepositoryAccessor;

import java.io.*;
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
    protected String downloadPack(String packURL, String tempFolder, String packFileName)
        throws IOException, NoSuchAlgorithmException {
        return WebRepositoryAccessor.getCachedUrl(packURL, tempFolder, packFileName, true);
    }

}
