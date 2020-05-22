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
 * Azure Functions with HTTP Trigger.
 */
public class FTPDelete {
    /**
     * Deletes a file from the FTP Server defined by the query and properties file.
     *
     * This function listens at endpoint "/api/ASESIncoming". It takes the following queries:
     * - container: Blob Storage container to input the file into
     * - blobFile: What to name the input file in Blob Storage
     * - ftpFile: File in the FTP Server to copy (directory of the file is determined by properties file
     * FTPUpload?container=testContainer&inputFile=path%2Fto%2Ffile&outputFile=outputFile.name
     */
    @FunctionName("FTPDelete")
    @StorageAccount("StorageAccount")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String ftpFile = request.getQueryParameters().get("ftpFile");

        // If any query parameters are missing, fail out
        if (StringUtils.isEmpty(ftpFile)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(String.format("{ 'response' : 'Please make sure <ftpFile> is specified when making a request.' }")).build();
        }

        // Setup FTP connection information.
        Boolean isImplicit = Boolean.TRUE;
        FTPSClient ftpsClient = new FTPSClient(isImplicit);
        Map<String, String> ftpClientProperties = FTPFunctionsUtil.getPropertiesMap("ftps-connection.properties");

        try {
            context.getLogger().info("Connecting to FTP Server...");
            ftpsClient.connect(ftpClientProperties.get("ftpsServer"));
            ftpsClient.login(ftpClientProperties.get("user"), ftpClientProperties.get("password"));
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpsClient.changeWorkingDirectory(ftpClientProperties.get("popDirectory"));
            ftpsClient.enterLocalPassiveMode();

            context.getLogger().info("Downloading file from server directory...");
            Boolean fileDeleted = ftpsClient.deleteFile(ftpFile);
            int returnCode = ftpsClient.getReplyCode();
            if (!fileDeleted || returnCode == 550) {
                return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE).body("( 'response' : " + ftpsClient.getReplyString() + " }").build();
            }

            context.getLogger().info(String.valueOf(ftpsClient.getReplyCode()));
            context.getLogger().info(ftpsClient.getReplyString());
            ftpsClient.logout();
        } catch (IOException e) {
            return request.createResponseBuilder(HttpStatus.FAILED_DEPENDENCY).body(" 'response' : 'Unsuccessful in connecting to the FTP server and deleting the file.' }").build();
        }

        return request.createResponseBuilder(HttpStatus.OK).body("{ 'response': '" + ftpFile + " was deleted.' }").build();
    }
}
