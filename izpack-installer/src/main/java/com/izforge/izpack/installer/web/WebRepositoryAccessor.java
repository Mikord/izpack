/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2007 Vladimir Ralev
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

package com.izforge.izpack.installer.web;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.izforge.izpack.api.exception.ChecksumsNotMatchException;

/**
 * This class enumerates the availabe packs at the web repository. Parses the config files
 * - install.xml, packsinfo.xml, langpacks and is used to override the static configuration
 * in the installer jar.
 *
 * @author <a href="vralev@redhat.com">Vladimir Ralev</a>
 * @version $Revision: 1.1 $
 */
public class WebRepositoryAccessor
{
    /**
     * Files to be looked for at the repository base url
     */
    private static final String installFilename = "install.xml";

    /**
     * Files being downloaded in the buffer, 1MB max
     */
    private static final int BUFFER_SIZE = 1000000;

    private static final String CHECKSUM_TYPE = "md5";
    /**
     * First download the jar file. The create the input stream from the
     * downloaded file. This is because the Jar connection's openInputStream
     * will blocks until the whole jar in order to unzip it (there is no way
     * to see the download progress there).
     *
     * @param url the base URL
     * @return the url
     */
    public static String getCachedUrl(String url, String tempFolder, String packFileName)
        throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(CHECKSUM_TYPE);

        String expectedChecksum = nexusPackChecksum(url);

        byte[] raw = new byte[BUFFER_SIZE];
        WebAccessor webAccessor = new WebAccessor(null);
        String packUrl = url + ".jar";
        InputStream in = webAccessor.openInputStream(new URL(packUrl));
        int r = in.read(raw);
        File tempDir = new File(tempFolder);

        tempDir.mkdirs();

        File temp = new File(tempFolder, packFileName);
        FileOutputStream fos = new FileOutputStream(temp);
        String path = temp.getAbsolutePath();
        while (r > 0)
        {
            fos.write(raw, 0, r);
            r = in.read(raw);
        }
        in.close();
        fos.close();

        if ( ! WebRepositoryAccessor.calculateChecksum(temp.getPath(), md).equals(expectedChecksum)) {
            throw new ChecksumsNotMatchException("File checksums do not match");
        }

        return path;
    }

    private static String nexusPackChecksum(String url)
        throws IOException {

        URL checksumUrl = new URL(url + "/checksum/" + CHECKSUM_TYPE);

        HttpURLConnection connection = (HttpURLConnection) checksumUrl.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            response.append(responseLine.trim());
        }

        return response.toString().replace("\"", "");
    }

    private static String calculateChecksum(String filepath, MessageDigest md) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(filepath);
            byte[]buffer = new byte[1024];
            int nread;
            while ((nread = fis.read(buffer)) != -1) {
                md.update(buffer, 0, nread);
            }
        }
        finally {
            if (fis != null) {
                fis.close();
            }
        }

        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
