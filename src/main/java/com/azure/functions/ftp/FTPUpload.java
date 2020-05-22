package com.azure.functions.ftp;

import java.io.*;
import java.util.*;

import com.azure.util.FTPFunctionsUtil;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;


/**
 * Azure Function class defining protocol for outgoing files.
 */
public class FTPUpload {

    /**
     * Copies a file from Azure Blob Storage defined by the query. Then sends a file to FTP Server
     * determined by properties file. The file is decoded from ascii format and encoded into
     * latin-1 format.
     *
     * This function listens at endpoint "/api/FTPUpload". It takes the following queries:
     * - container
     * - blobFile
     * - ftpFile
     * FTPUpload?container=testContainer&blobFile=path%2Fto%2Ffile&ftpFile=ftpFile.name
     */
    @FunctionName("FTPUpload")
    @StorageAccount("StorageAccount")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BlobInput(
                    name = "file",
                    dataType= "binary",
                    path = "{Query.container}/{Query.blobFile}",
                    connection= "StorageAccount")
                    byte[] inputData,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String container = request.getQueryParameters().get("container");
        final String blobFile = request.getQueryParameters().get("blobFile");
        final String ftpFile = request.getQueryParameters().get("ftpFile");

        // If any query parameters are missing, fail out
        if(StringUtils.isEmpty(container) ||
                StringUtils.isEmpty(blobFile) ||
                StringUtils.isEmpty(ftpFile)){
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(String.format("{ 'response' : 'Please make sure <container>, <blobFile>, and <ftpFile> are specified when making a request.' }")).build();
        }

        // Check if the file in blob storage exists.
        if (inputData == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(String.format("{ 'response' : 'No data was obtained at the specified location in blob storage.' }")).build();
        }

        // Setup FTP connection information.
        Boolean isImplicit = Boolean.TRUE;
        FTPSClient ftpsClient = new FTPSClient(isImplicit);
        Map<String, String> ftpClientProperties = FTPFunctionsUtil.getPropertiesMap("ftps-connection.properties");

        try {
            context.getLogger().info("Connecting to FTP Server...");
            InputStream is = new ByteArrayInputStream(inputData);
            ftpsClient.connect(ftpClientProperties.get("ftpsServer"));
            ftpsClient.login(ftpClientProperties.get("user"), ftpClientProperties.get("password"));
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpsClient.changeWorkingDirectory(ftpClientProperties.get("appendDirectory"));
            ftpsClient.enterLocalPassiveMode();
            context.getLogger().info("Appending file to server directory...");
            ftpsClient.storeFile(ftpFile, is);
            context.getLogger().info(String.valueOf(ftpsClient.getReplyCode()));
            context.getLogger().info(ftpsClient.getReplyString());
            ftpsClient.logout();
        } catch (UnsupportedEncodingException e) {
            return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE).body("( 'response' : 'Issue encoding file. The file must have ASCII encoding.' }").build();
        } catch (IOException e) {
            return request.createResponseBuilder(HttpStatus.FAILED_DEPENDENCY).body(" 'response' : 'Unsuccessful in sending file to the FTP server.' }").build();
        }

    return request.createResponseBuilder(HttpStatus.OK).body("{ 'response': '" + ftpFile + " sent to FTP Server.' }").build();
    }
}
