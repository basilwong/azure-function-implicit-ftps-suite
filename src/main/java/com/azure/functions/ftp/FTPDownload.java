package com.azure.functions.ftp;

import org.apache.commons.io.IOUtils;

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
public class FTPDownload {
    /**
     * Copies a file from the FTP Server defined by the query and properties file. Then sends a file
     * to Blob Storage determined by properties file. The file is decoded from latin-1 format and
     * encoded into ascii format.
     *
     * This function listens at endpoint "/api/ASESIncoming". It takes the following queries:
     * - container: Blob Storage container to input the file into
     * - blobFile: What to name the input file in Blob Storage
     * - ftpFile: File in the FTP Server to copy (directory of the file is determined by properties file
     * FTPUpload?container=testContainer&inputFile=path%2Fto%2Ffile&outputFile=outputFile.name
     */
    @FunctionName("FTPDownload")
    @StorageAccount("StorageAccount")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @BlobOutput(
                    name = "file",
                    dataType = "binary",
                    path = "{Query.container}/{Query.blobFile}",
                    connection= "StorageAccount")
                    OutputBinding<byte[]> output,
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
            InputStream is = ftpsClient.retrieveFileStream(ftpFile);
            int returnCode = ftpsClient.getReplyCode();
            if (is == null || returnCode == 550) {
                return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE).body("( 'response' : 'File does not exist.' }").build();
            }

            output.setValue(IOUtils.toByteArray(is));
            context.getLogger().info(String.valueOf(ftpsClient.getReplyCode()));
            context.getLogger().info(ftpsClient.getReplyString());
            ftpsClient.logout();
        } catch (UnsupportedEncodingException e) {
            return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE).body("( 'response' : 'Issue encoding file. The file must have LATIN-1 encoding.' }").build();
        } catch (IOException e) {
            return request.createResponseBuilder(HttpStatus.FAILED_DEPENDENCY).body(" 'response' : 'Unsuccessful in downloading file from the FTP server.' }").build();
        }

        return request.createResponseBuilder(HttpStatus.OK).body("{ 'response': '" + ftpFile + " sent to blob storage.' }").build();
    }
}
