package com.azure.functions.ftp;

import java.io.*;
import java.util.*;

import com.azure.util.FTPFunctionsUtil;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * Azure Functions with HTTP Trigger.
 */
public class FTPDirectoryInfo {
    /**
     * Gets the files inside a specified director from an FTP Server defined by the query and properties file.
     *
     * This function listens at endpoint "/api/ASESIncoming". It takes the following queries:
     * - container: Blob Storage container to input the file into
     * - blobFile: What to name the input file in Blob Storage
     * - ftpFile: File in the FTP Server to copy (directory of the file is determined by properties file
     * FTPUpload?container=testContainer&inputFile=path%2Fto%2Ffile&outputFile=outputFile.name
     */
    @FunctionName("FTPDirectoryInfo")
    @StorageAccount("StorageAccount")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

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
            FTPFile[] files = ftpsClient.listFiles(ftpClientProperties.get("popDirectory"));
            int returnCode = ftpsClient.getReplyCode();
            if (returnCode == 550) {
                return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE).body("( 'response' : 'File does not exist.' }").build();
            }
            context.getLogger().info(String.valueOf(ftpsClient.getReplyCode()));
            context.getLogger().info(ftpsClient.getReplyString());
            ftpsClient.logout();

            StringJoiner joiner = new StringJoiner("");
            for (int i = 0; i < files.length; i++) {
                joiner.add("'" + files[i].getName() + "'");
                if (i < files.length - 1) {
                    joiner.add(",");
                }
            }
            String response = "{ 'response' : [" + joiner.toString() + "] }";
            return request.createResponseBuilder(HttpStatus.OK).body(response).build();
        } catch (IOException e) {
            return request.createResponseBuilder(HttpStatus.FAILED_DEPENDENCY).body("{ 'response' : 'Unsuccessful in connecting to the FTP server and deleting the file.' }").build();
        }
    }
}
