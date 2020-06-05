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
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.web.WebRepositoryAccessor;

import java.io.*;
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
    protected String downloadPack(String packURL, String tempFolder, String packFileName)
        throws IOException, NoSuchAlgorithmException {
        return WebRepositoryAccessor.getCachedUrl(packURL, tempFolder, packFileName, false);
    }
}
