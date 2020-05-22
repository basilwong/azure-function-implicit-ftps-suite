package com.azure.functions.ftp;

import com.microsoft.azure.functions.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Unit test for Function class.
 */
public class FTPUploadTest {

    /**
     * Unit test for FTPUpload runtime method. Checks if the function is able to copy a dummy
     * file from Blob Storage then send that file to FTP Server.
     */
    @Test
    public void testFTPUpload() throws Exception {

        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("container", "test");
        queryParams.put("blobFile", "DBSRVADMIN.ATTACHMENT.txt");
        queryParams.put("ftpFile", "testOutput.txt");

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

        // Invoke
        byte[] testBytes = "test data".getBytes(StandardCharsets.UTF_8);
        final HttpResponseMessage ret = new FTPUpload().run(req, testBytes, context);

        // Verify
        System.out.println(ret.getBody());
        assertEquals(HttpStatus.OK, ret.getStatus());
    }

}
