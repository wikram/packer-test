Boolean ifGallery
def autoCanceled = false
String TestVM = "test-switch"

BUILD_DIR = 'build'

node(WhichNode)
{
    stage('Validate Inputs')
    {
        if (ImageName == "" || ImageName == null || ImageName.isEmpty()){
            println "Please specify the name of the image that you want to create."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (Resource_group_name == "" || Resource_group_name == null || Resource_group_name.isEmpty()){
            println "Please specify the resource group where you want to create the image."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (Net_res_grp == "" || Net_res_grp == null || Net_res_grp.isEmpty()){
            println "Please specify the resource group where you want to create the image."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (Vnetname == "" || Vnetname == null || Vnetname.isEmpty()){
            println "Please specify the virtual network that you want to use to create the image."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (Subnetname == "" || Subnetname == null || Subnetname.isEmpty()){
            println "Please specify the Subnet that you want to use to create the image."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (whichEnv == "" || whichEnv == null || whichEnv.isEmpty()){
            println "Please specify the environment for which you need to create the image."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (WhichNode == "" || WhichNode == null || WhichNode.isEmpty()){
            println "Please specify the slave that should run the packer build."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
        if (MailID == "" || MailID == null || MailID.isEmpty() || !MailID.toLowerCase().endsWith("@fisglobal.com")){
            println "Please specify a FIS email id to Send report to."
            currentBuild.result = 'Error'
            autoCanceled = true
        }
    }

    if (autoCanceled){return}

    stage('Copy Credentials')
    {
        dir(BUILD_DIR) 
        {
            try {
                    sh "rm -f creds.json ; touch creds.json"

                    writeFile file: 'creds.json', text: """{
                            "dst_image_name": "${ImageName}",
                            "resource_group_name": "${Resource_group_name}",
                            "net_res_grp": "${Net_res_grp}",
                            "vnetname": "${Vnetname}",
                            "subnetname": "${Subnetname}"
                    }"""
            }
            catch(Exception e) {
                autoCanceled = true
                println e
                currentBuild.result = 'Error'
            }       
        }
    }

    if (autoCanceled){return}

    stage('Build Image')
    {
        print "Build Image"
        dir(BUILD_DIR) 
        {
            try {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/wikram/packer-test']]])
                if (whichEnv == "Prod")
                {
                   withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                            subscriptionIdVariable: 'SUBS_ID',
                                            clientIdVariable: 'CLIENT_ID',
                                            clientSecretVariable: 'CLIENT_SECRET',
                                            tenantIdVariable: 'TENANT_ID')]) {
                    sh '/sbin/packer build -force -var subscription_id=${SUBS_ID} -var client_id=${CLIENT_ID} -var client_secret=${CLIENT_SECRET} -var-file=creds.json packer.json'
                    }
                }
                else
                {
                    withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                            subscriptionIdVariable: 'SUBS_ID',
                                            clientIdVariable: 'CLIENT_ID',
                                            clientSecretVariable: 'CLIENT_SECRET',
                                            tenantIdVariable: 'TENANT_ID')]) {
                    sh '/sbin/packer build -force -var subscription_id=${SUBS_ID} -var client_id=${CLIENT_ID} -var client_secret=${CLIENT_SECRET} -var-file=creds.json packer.json'
                    }
                }
                sh "rm -rf *"
            }
            catch(Exception e) {
                sh 'az vm delete -g ${Resource_group_name} -n pkr* --yes ; az network nic delete -g ${Resource_group_name} -n pkr*; az disk delete --name pkr* --resource-group ${Resource_group_name} --yes ; az disk delete --name datadisk-1 --resource-group ${Resource_group_name} --yes'
                autoCanceled = true
                println e
                currentBuild.result = 'Error'
            }
        }
    }

    if (autoCanceled){return}

    stage('Send Report')
    {
        try {
            print "Sending Report"
            //sh ("ssh jenkinspacker@vlmazpacker01 cat .../$Buildjobname_output.log | mail -s Packer log xxxx@fisglobal.com")
        }
        catch(Exception e) {
           // autoCanceled = true
            println e
        }
    }

    if (autoCanceled){return}

    stage('Deploy Image')
    {
        print "Deploying Created Image"
        try {
            if (whichEnv == "Prod")
            {
               withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID ; az account set -s $SUBS_ID'
                sh 'az vm create -g ${Resource_group_name} -n ${TestVM} --image ${ImageName} --nsg "" --public-ip-address "" --authentication-type password --size Standard_DS2_v2 --admin-username testadmin --admin-password "Password1234!" --os-disk-name ${TestVM}-os --vnet-name ${Vnetname} --subnet ${Subnetname}'
                }
            }
            else
            {
                withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID ; az account set -s $SUBS_ID'
                sh 'az vm create -g ${Resource_group_name} -n ${TestVM} --image ${ImageName} --nsg "" --public-ip-address "" --authentication-type password --size Standard_DS2_v2 --admin-username testadmin --admin-password "Password1234!" --os-disk-name ${TestVM}-os --vnet-name ${Vnetname} --subnet ${Subnetname}'
                }
            }     
        }
        catch(Exception e) {
            currentBuild.result = 'Error'
            println e
            autoCanceled = true
            println "Job failed in Deploy Image Stage"
            sh 'az vm delete -g ${Resource_group_name} -n ${TestVM} --yes ; az network nic delete -g ${Resource_group_name} -n ${TestVM}VMNic; az disk delete --name ${TestVM}* --resource-group ${Resource_group_name} --yes'
        }
    }

    if (autoCanceled){return}

    stage('Check VM Image')
    {
        
        try {
            print "Checking Image"
            //sh ("ssh jenkinspacker@vlmazpacker01 cat .../$Buildjobname_output.log | mail -s Packer log xxxx@fisglobal.com")
            if (whichEnv == "Prod")
            {
               withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID ; az account set -s $SUBS_ID'
                sh 'az vm start --name ${TestVM} --no-wait --resource-group ${Resource_group_name}'
                sh "az vm run-command invoke -g ${Resource_group_name} -n ${TestVM} --command-id RunShellScript --scripts 'echo \$1 \$2' --parameters hello world"
                }
            }
            else
            {
                withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID ; az account set -s $SUBS_ID'
                sh 'az vm start --name ${TestVM} --no-wait --resource-group ${Resource_group_name}'
                sh "az vm run-command invoke -g ${Resource_group_name} -n ${TestVM} --command-id RunShellScript --scripts 'echo \$1 \$2' --parameters hello world"
                }
            }
        }
        catch(Exception e) {
           currentBuild.result = 'Error'
           autoCanceled = true
           println e
           println "Job failed in Check VM Image Stage"
           sh 'az vm delete -g ${Resource_group_name} -n ${TestVM} --yes ; az network nic delete -g ${Resource_group_name} -n ${TestVM}VMNic; az disk delete --name ${TestVM}* --resource-group ${Resource_group_name} --yes'
        }
    }

    if (autoCanceled){return}

    stage('Delete Testing VM')
    {
        print "Checking Image"
        try {
            if (whichEnv == "Prod")
            {
               withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID ; az account set -s $SUBS_ID'
                sh 'az vm delete -g ${Resource_group_name} -n ${TestVM} --yes ; az network nic delete -g ${Resource_group_name} -n ${TestVM}VMNic; az disk delete --name ${TestVM}* --resource-group ${Resource_group_name} --yes'
                }
            }
            else
            {
                withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID ; az account set -s $SUBS_ID'
                sh 'az vm delete -g ${Resource_group_name} -n ${TestVM} --yes ; az network nic delete -g ${Resource_group_name} -n ${TestVM}VMNic; az disk delete --name ${TestVM}* --resource-group ${Resource_group_name} --yes'
                }
            }
        }
        catch(Exception e) {
            autoCanceled = true
            println e 
        }
    }
}