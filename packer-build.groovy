import hudson.FilePath;
import hudson.model.*
import java.io.*

String ImageRG
String ImageGalleryName
Boolean ifGallery
String SlaveNode
def file1 = new File('abc.txt')
String subID
String cliID


if (whichEnv == "Prod")
{
    subID = "bfc181d8-0a2b-483a-95eb-23944b2724f1"
    cliID = "c2379bcf-c4c5-4acf-836e-84c458dbe40a"
}
else
{
    subID = "123"
    cliID = "456"
}

node(WhichNode)
{
    stage('Validate Inputs')
    {
        print "Validate Inputs"
    }

    stage('Copy Credentials')
    {

        print "Copy Credentials"
        sh "pwd"
        sh "rm -f creds.json"
        sh "touch creds.json"
        sh "ls -l creds.json"
        writeFile file: 'creds.json', text: """{
                "subscription_id" : "${subID}",
                "client_id": "${cliID}",
                "dst_image_name": "${ImageName}",
                "resource_group_name": "${Resource_group_name}",
                "net_res_grp": "${Net_res_grp}",
                "vnetname": "${Vnetname}",
                "subnetname": "${Subnetname}"
}"""

        sh "ls -l creds.json"
        sh "cat creds.json"
    }

    stage('Build Image')
    {
        print "Build Image"
        sh "rm -f packer.json*" 
        sh "wget https://raw.githubusercontent.com/wikram/packer-test/master/packer.json"
        withCredentials([azureServicePrincipal('sandbox-packer')]) {
            sh (script: "/sbin/packer build -force -var \"client_secret=${AZURE_CLIENT_SECRET}\" -var-file=creds.json packer.json  2>&1 | tee packer_output.log",returnStdout: true)
        } 
        withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                    subscriptionIdVariable: 'SUBS_ID',
                                    clientIdVariable: 'CLIENT_ID',
                                    clientSecretVariable: 'CLIENT_SECRET',
                                    tenantIdVariable: 'TENANT_ID')]) {
        sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID'
        }
        sh "pwd"
        sh "ls -l"
    }

    stage('Send Report')
    {
        print "Send Report"
        //sh ("ssh jenkinspacker@vlmazpacker01 cat .../$Buildjobname_output.log | mail -s Packer log xxxx@fisglobal.com")
    }
}
