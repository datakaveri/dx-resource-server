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
          qualityGates: [[threshold:100, type: 'TOTAL', unstable: false]],
          tool: checkStyle(pattern: 'target/checkstyle-result.xml')
        )
        recordIssues(
          enabledForFailure: true,
          skipBlames: true,
          qualityGates: [[threshold:100, type: 'TOTAL', unstable: false]],
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
            sh """
              echo '[*] Cleaning up old ZAP container if exists...'
              docker stop zap-daemon || true && docker rm zap-daemon || true
              echo '[*] Starting ZAP in Docker...'
              docker run --name zap-daemon --network host -u zap -d \
                -p 8090:8090 \
                ghcr.io/zaproxy/zaproxy:stable \
                zap.sh -daemon \
                  -host 0.0.0.0 \
                  -port 8090 \
                  -config api.disablekey=true \
                  -config api.addrs.addr.name=.* \
                  -config api.addrs.addr.regex=true

              echo '[*] Waiting for ZAP to be ready...'
              until curl -s http://localhost:8090/JSON/core/view/version/ > /dev/null; do
                sleep 2
              done
              echo '[✅] ZAP is ready at http://localhost:8090'
            """
            sh "curl http://localhost:8090/JSON/pscan/action/disableScanners/?ids=10096"
          }
        }
        script{
            sh 'mkdir -p configs'
            sh 'scp /home/ubuntu/configs/rs-config-test.json ./configs/config-test.json'
            sh 'bash Jenkins/resources/post-zap.sh --mvn'
            publishHTML(target: [
              allowMissing: false,
              alwaysLinkToLastBuild: true,
              keepAll: true,
              reportDir: '/var/lib/jenkins/iudx/rs/zap-artifacts',
              reportFiles: 'zap-report.html',
              reportName: 'OWASP ZAP Report'
            ])
         }
      }
      post{
        always{
           xunit (
             thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
             tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
             )
        }
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java'
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
            sh 'docker stop zap-daemon || true && docker rm zap-daemon || true'
          }
        }
      }
    }

    stage('Continuous Deployment') {
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
            return env.GIT_BRANCH == 'origin/master';
          }
        }
      }
      stages {
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                devImage.push("5.6.0-alpha-${env.GIT_HASH}")
                deplImage.push("5.6.0-alpha-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update rs_rs --image ghcr.io/datakaveri/rs-depl:5.6.0-alpha-${env.GIT_HASH}'"
              sh 'sleep 60'
            }
          }
          post{
            failure{
              error "Failed to deploy image in Docker Swarm"
            }
          }          
        }
        stage('Integration test on swarm deployment') {
          steps {
              script{
                sh 'sudo update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java'
                sh 'mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestDepl=true'
              }
          }
          post{
            always{
             script{
                sh 'sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java'
             }
             xunit (
               thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
               tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
               )
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
          }
        }
      }
    }
  }
  post{
    failure{
      script{
        if (env.GIT_BRANCH == 'origin/master')
        emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
Check console output at $BUILD_URL to view the results.'''
      }
    }
  }
}
