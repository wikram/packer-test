Boolean ifGallery
def autoCanceled = false

BUILD_DIR = 'build'

node(WhichNode)
{
    stage('Validate Inputs')
    {
        print "Validate Inputs"
    }

    if (autoCanceled){return}

    stage('Copy Credentials')
    {
        dir(BUILD_DIR) 
        {
            try {
                    sh "rm -f creds.json ; touch creds.json ; ls -l creds.json"

                    writeFile file: 'creds.json', text: """{
                            "dst_image_name": "${ImageName}",
                            "resource_group_name": "${Resource_group_name}",
                            "net_res_grp": "${Net_res_grp}",
                            "vnetname": "${Vnetname}",
                            "subnetname": "${Subnetname}"
                    }"""
                    sh "ls -l creds.json"
            }
            catch(Exception e) {
                autoCanceled = true
                println e
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
                autoCanceled = true
                println e
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
                sh 'az vm create -g ${Resource_group_name} -n Test-Vm --image ${ImageName} --nsg "" --public-ip-address "" --authentication-type password --size Standard_DS2_v2 --admin-username testadmin --admin-password "Password1234!" --os-disk-name Test-Vm-os'
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
                sh 'az vm create -g ${Resource_group_name} -n Test-Vm --image ${ImageName} --nsg "" --public-ip-address "" --authentication-type password --size Standard_DS2_v2 --admin-username testadmin --admin-password "Password1234!" --os-disk-name Test-Vm-os'
                }
            }     
        }
        catch(Exception e) {
            autoCanceled = true
            println e
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
                sh 'az vm start --name Test-Vm --no-wait --resource-group ${Resource_group_name}'
                sh 'az vm run-command invoke -g ${Resource_group_name} -n Test-Vm --command-id RunShellScript --scripts "echo $1 $2" --parameters hello world'
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
                sh 'az vm start --name Test-Vm --no-wait --resource-group ${Resource_group_name}'
                sh 'az vm run-command invoke -g ${Resource_group_name} -n Test-Vm --command-id RunShellScript --scripts "echo $1 $2" --parameters hello world'
                }
            }   
        }
        catch(Exception e) {
           autoCanceled = true
           println e 
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
                sh 'az vm delete -g ${Resource_group_name} -n Test-Vm --yes ; az network nic delete -g ${Resource_group_name} -n Test-VmVMNic; az disk delete --name Test-Vm* --resource-group ${Resource_group_name} --yes'
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
                sh 'az vm delete -g ${Resource_group_name} -n Test-Vm --yes ; az network nic delete -g ${Resource_group_name} -n Test-VmVMNic; az disk delete --name Test-Vm* --resource-group ${Resource_group_name} --yes'
                }
            }
        }
        catch(Exception e) {
            autoCanceled = true
            println e 
        }
    }
}
