pipeline {

  environment {
    devRegistry = 'ghcr.io/datakaveri/rs-dev'
    deplRegistry = 'ghcr.io/datakaveri/rs-depl'
    testRegistry = 'ghcr.io/datakaveri/rs-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }

  agent { 
    node {
      label 'slave1' 
    }
  }

  stages {

    stage('Trivy Code Scan (Dependencies)') {
      steps {
        script {
          sh '''
            trivy fs --scanners vuln,secret,misconfig --output trivy-fs-report.txt .
          '''
        }
      }
    }
    
    stage('Build images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }
    stage('Trivy Docker Image Scan') {
      steps {
        script {
          sh "trivy image --output trivy-dev-image-report.txt ${devImage.imageName()}"
          sh "trivy image --output trivy-depl-image-report.txt ${deplImage.imageName()}"
        }
      }
    }
    stage('Archive Trivy Reports') {
      steps {
        archiveArtifacts artifacts: 'trivy-*.txt', allowEmptyArchive: true
        publishHTML(target: [
          allowMissing: true,
          keepAll: true,
          reportDir: '.',
          reportFiles: 'trivy-fs-report.txt, trivy-dev-image-report.txt, trivy-depl-image-report.txt',
          reportName: 'Trivy Reports'
        ])
      }
    }

    stage('Unit Tests and Code Coverage Test'){
      steps{
        script{
          sh 'docker compose -f docker-compose.test.yml up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '40'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java', exclusionPattern:'iudx/resource/server/apiserver/ApiServerVerticle.class,**/*VertxEBProxy.class,**/Constants.class,**/*VertxProxyHandler.class,**/*Verticle.class,iudx/resource/server/database/archives/DatabaseService.class,iudx/resource/server/database/async/AsyncService.class,iudx/resource/server/database/latest/LatestDataService.class,iudx/resource/server/deploy/*.class,iudx/resource/server/database/postgres/PostgresService.class,iudx/resource/server/apiserver/ManagementRestApi.class,iudx/resource/server/apiserver/AdminRestApi.class,iudx/resource/server/apiserver/AsyncRestApi.class,iudx/resource/server/callback/CallbackService.class,**/JwtDataConverter.class,**/EncryptionService.class,**/EsResponseFormatter.class,**/AbstractEsSearchResponseFormatter.class'
      }
      post{
      always {
        recordIssues(
          enabledForFailure: true,
          skipBlames: true,
          qualityGates: [[threshold:0, type: 'TOTAL', unstable: false]],
          tool: checkStyle(pattern: 'target/checkstyle-result.xml')
        )
        recordIssues(
          enabledForFailure: true,
          skipBlames: true,
          qualityGates: [[threshold:5, type: 'TOTAL', unstable: false]],
          tool: pmdParser(pattern: 'target/pmd.xml')
        )
      }
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo rm -rf target/'
          }
        }        
      }
    }

    stage('Start Resource-Server for Performance and Integration Testing'){
      steps{
        script{
          sh 'scp Jmeter/ResourceServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/rs/Jmeter/'
          sh 'docker compose -f docker-compose.test.yml up -d perfTest'
          sh 'sleep 60'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
        }
      }
    }
    
    stage('Jmeter Performance Test'){
      steps{
        script{
          env.puneToken = sh(returnStdout: true, script: 'python3 Jenkins/resources/get-token.py --pune').trim()
          env.suratToken = sh(returnStdout: true, script: 'python3 Jenkins/resources/get-token.py --surat').trim()
        }
        node('built-in') {
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/rs/Jmeter/report ; mkdir -p /var/lib/jenkins/iudx/rs/Jmeter/report'
            sh "set +x;/var/lib/jenkins/apache-jmeter/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/rs/Jmeter/ResourceServer.jmx -l /var/lib/jenkins/iudx/rs/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/rs/Jmeter/report/ -Jhost=jenkins-slave1 -Jport=8080 -Jprotocol=http -JpuneToken=$env.puneToken -JsuratToken=$env.suratToken"
          }
          perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/rs/Jmeter/report/*.jtl'     
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml  down --remove-orphans'
          }
        }
      }
    }

    stage('Integration Tests and OWASP ZAP pen test'){
      steps{
        node('built-in') {
          script{
            startZap ([host: '0.0.0.0', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'curl http://0.0.0.0:8090/JSON/pscan/action/disableScanners/?ids=10096'
          }
        }
        script{
            sh 'mkdir -p configs'
            sh 'scp /home/ubuntu/configs/5.6.0/rs-config-test.json ./configs/config-test.json'
            sh 'mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestProxyHost=jenkins-master-priv -DintTestProxyPort=8090 -DintTestHost=jenkins-slave1 -DintTestPort=8080'
        }
        node('built-in') {
          script{
            runZapAttack()
            }
        }

      }
      post{
        always{
           xunit (
             thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
             tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
             )
          node('built-in') {
            script{
              archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
            }
          }
        }
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          } 
        }
      }
    }

    stage('Push Images') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/5.6.0';
          }
        }
      }
      steps {
          script {
            docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("5.6.0-${env.GIT_HASH}")
            deplImage.push("5.6.0-${env.GIT_HASH}")
            }
          }
      }
    }

  }
  post{
    failure{
      script{
        if (env.GIT_BRANCH == 'origin/5.6.0')
        emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
Check console output at $BUILD_URL to view the results.'''
      }
    }
  }
}