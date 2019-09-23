Boolean ifGallery

BUILD_DIR = 'build'

node(WhichNode)
{
    stage('Validate Inputs')
    {
        print "Validate Inputs"
    }

    stage('Copy Credentials')
    {
        dir(BUILD_DIR) 
        {
            sh "rm -f creds.json"
            sh "pwd"
            sh "touch creds.json"
            sh "ls -l creds.json"
            writeFile file: 'creds.json', text: """{
                    "dst_image_name": "${ImageName}",
                    "resource_group_name": "${Resource_group_name}",
                    "net_res_grp": "${Net_res_grp}",
                    "vnetname": "${Vnetname}",
                    "subnetname": "${Subnetname}"
}"""
          sh "ls -l creds.json"

        }
    }

    stage('Build Image')
    {
        print "Build Image"
        dir(BUILD_DIR) 
        {
           // sh "rm -f packer.json*" 
            //sh "wget https://raw.githubusercontent.com/wikram/packer-test/master/packer.json"
           // withCredentials([azureServicePrincipal('sandbox-packer')]) {
            //    sh (script: "/sbin/packer build -force -var \"client_secret=${AZURE_CLIENT_SECRET}\" -var-file=creds.json packer.json  2>&1 | tee packer_output.log",returnStdout: true)
            //} 
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
            
            sh "pwd"
            sh "ls -l"
            sh "rm -rf *"
            sh "ls -l"
        }
    }

    stage('Send Report')
    {
        print "Send Report"
        //sh ("ssh jenkinspacker@vlmazpacker01 cat .../$Buildjobname_output.log | mail -s Packer log xxxx@fisglobal.com")
    }

    stage('Deploy Image')
    {
        print "Checking Image"
        if (whichEnv == "Prod")
            {
               withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID'
                sh 'az vm create -g ${Resource_group_name} -n Test-Vm --image ${ImageName}'
                }
            }
            else
            {
                withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID'
                sh 'az vm create -g ${Resource_group_name} -n Test-Vm --image ${ImageName}'
                }
            }
    }

    stage('Check VM Image')
    {
        print "Checking Image"
        //sh ("ssh jenkinspacker@vlmazpacker01 cat .../$Buildjobname_output.log | mail -s Packer log xxxx@fisglobal.com")
    }

    stage('Delete Testing VM')
    {
        print "Checking Image"
        if (whichEnv == "Prod")
            {
               withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID'
                sh 'az vm delete -g ${Resource_group_name} -n Test-Vm --yes'
                }
            }
            else
            {
                withCredentials([azureServicePrincipal(credentialsId: 'sandbox-packer',
                                        subscriptionIdVariable: 'SUBS_ID',
                                        clientIdVariable: 'CLIENT_ID',
                                        clientSecretVariable: 'CLIENT_SECRET',
                                        tenantIdVariable: 'TENANT_ID')]) {
                sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID'
                sh 'az vm delete -g ${Resource_group_name} -n Test-Vm --yes'
                }
            }
    }
}
