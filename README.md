# Implicit Ftps Connector for Azure
Azure doesn't support implicit ftps so I created this suite of functions that can be used to send messages over that protocol. These azure functions can be used to upload/download files from Azure Blob storage to/from an FTP Server over Implicit FTP. Use cases could be to deploy it in combination with Azure Logic Apps or Azure Data Factory. 

## Usage

The functions are triggered via HTTP. For example, once the functions have been deployed as described in the **Setup** section then Azure should make URLs such as https://azurefunctionname.azurewebsites.us/api/FTPUpload available. To use the Azure Functions append the query parameters onto the end of the provided URL in a GET query. Below are example queries for each of the functions in this repository's suite of functions.

 * *Note that the appendDirectory and popDirectory definitions are in the FTP Server Information section*

### FTPUpload

**Uploads a file to ftp server from blob storage.**

Takes the following query parameters:

 * ftpFile - path from the *appendDirectory* specifying where to upload the file and the naming of the file in the ftp server
 * blobFile - path in the container(specified by container query) to the file to download from azure blob storage to be uploaded to the ftp server
 * container - container in the storage account blob storage with the file to download 


### FTPDownload

**Downloads a file from ftp server to blob storage**

Takes the following query parameters:

 * ftpFile - path from the *popDirectory* specifying where to download the file and the naming of the file in the ftp server
 * blobFile - path in the container(specified by container query) to upload to azure blob storage 
 * container - container in the storage account blob storage to upload the file

### FTPDelete

**Deletes a file from ftp server**

Takes the following query parameters:
 * ftpFile - path from the *popDirectory* specifying where the file to delete is


### FTPDirectoryInfo

**Returns a list in the response (json) containing a list of the files in the *popDirectory***

## Setup 

### 1. Install Dependencies onto Local Machine

Before you can get started, you should install the Java Developer Kit, version 8. Make sure that the JAVA_HOME environment variable gets set to the install location of the JDK. You will also need to install Apache Maven, version 3.0 or above.
You should also install Node.JS which includes npm. This is how you will obtain the Azure Functions Core Tools. If you prefer not to install Node, see the other installation options in the Core Tools reference.
Run the following command to install the Core Tools package:
```
npm install -g azure-functions-core-tools
```

The Core Tools make use of .NET Core 2.1, so you should install that, too.

Lastly, install the Azure CLI 2.0. Once this is installed, make sure you are logged in by running the login command and following the onscreen instructions:
https://docs.microsoft.com/en-us/azure/azure-government/documentation-government-get-started-connect-with-cli

```
az login
```

Additionally, you will have to clone this repository to your local machine.

### 2. Azure Login, Resource Authorization

*Before the azure function can be deployed, a service principal with a contributor role must be created via [Powershell](https://docs.microsoft.com/en-us/powershell/azure/create-azure-service-principal-azureps?view=azps-4.1.0) or [Portal](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal).*

Once the service principal has been created, that information can be entered into the following file:
```
mvn-settings/local-settings.xml
```

### 3. Creating Azure Function App Resource

In order to deploy the fucntions in this repository you have to create a function app in the portal with an associated storage account. Here is the Azure documentation for how to [create function app from portal.](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-azure-function#create-a-function-app).

Within this repository the name used to create the function app should now replace the placeholder name in the ```pom.xml``` file on line 18:

```
<functionAppName>azure-implicit-ftps-connector-suite</functionAppName>
```

### 4. Specifying Azure Storage Account

In order to test the Azure functions locally, you need to get the [Storage Account Connection String](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-keys-manage?tabs=azure-portal) for the storage account the azure functions will be reading and writing to and copy it into the ```local.settings.json``` file as the String value for "StorageAccount".

```
"StorageAccount" : "<connection string goes here>",
```

To run the functions in the cloud you need to go into the function app you created in step 2 and specify the same storage account connection string as the ```local.settings.json``` file in the **Configuration Tab** of the function app resource as a Custom Connection String. 

### 5. FTP Server Information

Add the login and static directory information to the following properties file:
```
src/main/resources/ftps-connection.properties
```

The information should be entered in as follows:

```
ftpsServer=<ip address or server url>
user=<username>
password=<password>
appendDirectory=<directory that FTPUpload will navigate to before uploading query specified files>
popDirectory=<directory that FTPDownload, FTPDelete, FTPDirectoryInfo will navigate to before performing query>
```
### 6. Deployment

To deploy the azure functions locally navigate to the cloned repository directory then run the following commands. If the configuration from the previous steps has been setup properly, then the tests should pass and the functions should compile properly. 

```
mvn clean package
mvn azure-functions:run
```

To deploy the azure functions to the cloud. Run the following commands:

```
mvn clean package
mvn azure-functions:deploy --settings ./mvn-settings/local-settings.xml
```
