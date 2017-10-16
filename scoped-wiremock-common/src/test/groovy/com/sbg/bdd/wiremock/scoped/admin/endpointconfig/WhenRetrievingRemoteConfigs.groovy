package com.sbg.bdd.wiremock.scoped.admin.endpointconfig

import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig
import com.sbg.bdd.wiremock.scoped.integration.HttpCommandExecutor
import spock.lang.Specification

class WhenRetrievingRemoteConfigs extends Specification {
    def 'Retrieving all EndPointConfigs from a local integration scope'() {
        given: 'I have four configured EndPointConfigs'
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH + 'all') {
                    return '[' +
                            '{"propertyName":"endpoint1","url":"http://host1.com/some/path1","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint2","url":"http://host2.com/some/path2","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint3","url":"http://host3.com/some/path3","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                            '{"propertyName":"endpoint4","url":"http://host4.com/some/path4","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                            ']'
                } else {
                    throw new IllegalArgumentException(args[0].url);
                }
            }
        }
        def endPointConfigRegistry = new RemoteEndpointConfigRegistry('http://som.host/sut',EndpointConfig.LOCAL_INTEGRATION_SCOPE)

        when: 'I retrieve all the EndPointConfigs'
        def endpoints = endPointConfigRegistry.allKnownExternalEndpoints()

        then: "the there will be have four EndpointConfigs"
        endpoints.size() == 4
    }
    def 'Retrieving all EndPointConfigs from a specific category'() {
        given: 'I have four configured EndPointConfigs, two of which belong to category1'
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH + 'all') {
                    return '[' +
                            '{"propertyName":"endpoint1","url":"http://host1.com/some/path1","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint2","url":"http://host2.com/some/path2","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint3","url":"http://host3.com/some/path3","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                            '{"propertyName":"endpoint4","url":"http://host4.com/some/path4","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                            ']'
                } else {
                    throw new IllegalArgumentException(args[0].url);
                }
            }
        }
        def endPointConfigRegistry = new RemoteEndpointConfigRegistry('http://som.host/sut',EndpointConfig.LOCAL_INTEGRATION_SCOPE)

        when: 'I retrieve all the EndPointConfigs from category1'
        def endpoints = endPointConfigRegistry.endpointConfigsInCategory('category1')

        then: "the I will be have two EndpointConfigs"
        endpoints.size() == 2
        endpoints[0].propertyName == 'endpoint1'
        endpoints[1].propertyName == 'endpoint2'
    }
    def 'Retrieving all EndPointConfigs from a integration scope that implies transitive services'() {
        given: 'I have four configured EndPointConfigs in the local service and four in a transitive dependency'
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH + 'all') {
                    return '[' +
                            '{"propertyName":"endpoint1","url":"http://host1.com/some/path1","endpointType":"REST","categories":["category1"],"scopes":["componentx"]},' +
                            '{"propertyName":"endpoint2","url":"http://host2.com/some/path2","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint3","url":"http://host3.com/some/path3","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                            '{"propertyName":"endpoint4","url":"http://host4.com/some/path4","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                            ']'
                }else if (args[0].url.path == '/some/path1' + EndpointConfig.ENDPOINT_CONFIG_PATH + 'all') {
                        return '[' +
                                '{"propertyName":"endpoint5","url":"http://host5.com/some/path5","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                                '{"propertyName":"endpoint6","url":"http://host6.com/some/path6","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                                '{"propertyName":"endpoint7","url":"http://host7.com/some/path7","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                                '{"propertyName":"endpoint8","url":"http://host8.com/some/path8","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                                ']'
                } else {
                    throw new IllegalArgumentException(args[0].url.path);
                }
            }
        }
        def endPointConfigRegistry = new RemoteEndpointConfigRegistry('http://som.host/sut','componentx')

        when: 'I retrieve all the EndPointConfigs'
        def endpoints = endPointConfigRegistry.allKnownExternalEndpoints()

        then: "the tehre will be have four EndpointConfigs"
        endpoints.size() == 8
    }


}
