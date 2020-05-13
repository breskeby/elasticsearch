apply plugin: 'elasticsearch.build'
description = 'Consumer of QL QA utils'

dependencies {
  testCompile project(":test:framework")
  testCompile project(path: ':x-pack:plugin:ql:qa:utils')
  
  testCompile project(':client:rest-high-level')
  testCompile project(path: xpackModule('ql'))
  testCompile project(path: ':x-pack:plugin:ql:qa:debug')
  testCompile project(path: xpackModule('eql'))
  testCompile project(path: ':x-pack:plugin:eql:qa:common')
  testCompile project(path: xpackModule('eql'))
  testCompile project(path: xpackModule('core'))
  testCompile "org.slf4j:slf4j-api:${versions.slf4j}"
  testCompile "org.apache.logging.log4j:log4j-slf4j-impl:${versions.log4j}"
}

forbiddenApisTest.enabled = false
testingConventions.enabled = false
thirdPartyAudit.enabled = false