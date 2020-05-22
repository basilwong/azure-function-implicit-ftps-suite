package com.azure.functions.ftp;

import com.azure.util.FTPFunctionsUtil;
import com.microsoft.azure.functions.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

public class FTPDeleteTest {

    /**
     * Unit test for FTPDelete runtime method. Checks if the function is able to copy a dummy
     * file from Blob Storage then send that file to FTP Server.
     */
    @Test
    public void testFTPDelete() throws Exception {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final String nameOfDownloadedFile = "testOutput.txt";

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ftpFile", nameOfDownloadedFile);

        doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.empty();
        doReturn(queryBody).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();


        // Setup FTP connection information.
        Boolean isImplicit = Boolean.TRUE;
        FTPSClient ftpsClient = new FTPSClient(isImplicit);
        Map<String, String> ftpClientProperties = FTPFunctionsUtil.getPropertiesMap("ftps-connection.properties");

        String csvContent = "Test Data to be copied into file?";
        InputStream is = new ByteArrayInputStream(csvContent .getBytes());
        ftpsClient.connect(ftpClientProperties.get("ftpsServer"));
        ftpsClient.login(ftpClientProperties.get("user"), ftpClientProperties.get("password"));
        ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpsClient.changeWorkingDirectory(ftpClientProperties.get("popDirectory"));
        ftpsClient.enterLocalPassiveMode();
        ftpsClient.storeFile(nameOfDownloadedFile, is);
        TimeUnit.SECONDS.sleep(3);
        System.out.println(ftpsClient.getReplyCode());

        // Invoke
        @SuppressWarnings("unchecked")
        final HttpResponseMessage ret = new FTPDelete().run(req, context);

        // Verify
        System.out.println(ret.getBody());
        assertEquals(HttpStatus.OK, ret.getStatus());
    }

}
