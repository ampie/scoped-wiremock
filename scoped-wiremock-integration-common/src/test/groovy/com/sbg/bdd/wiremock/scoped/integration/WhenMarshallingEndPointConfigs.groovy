package com.sbg.bdd.wiremock.scoped.integration

import spock.lang.Specification

class WhenMarshallingEndPointConfigs extends Specification {
    def 'it should read config from a given JSON string'() {
        given:
        def json ='{"propertyName":"prop.name","url":"http://localhost:8080/asdf/","endpointType":"REST","categories":["cat1","cat2"],"scopes":["scope1","scope2"]}'
        when:
        def config = EndpointConfig.oneFromJson(json);
        then:
        config.propertyName == 'prop.name'
        config.url == new URL('http://localhost:8080/asdf/')
        config.endpointType == EndpointConfig.EndpointType.REST
        config.categories == ['cat1','cat2']
        config.scopes == ['scope1','scope2']
    }
    def 'it should read multiple configs from a given JSON string'() {
        given:
        def one ='{"propertyName":"prop.name","url":"http://localhost:8080/asdf/","endpointType":"REST","categories":["cat1","cat2"],"scopes":["scope1","scope2"]}'
        def many='[' + one +',' + one + ']'
        when:
        def configs = EndpointConfig.manyFromJson(many);
        then:
        configs.length == 2
        configs[0].propertyName == 'prop.name'
        configs[0].url == new URL('http://localhost:8080/asdf/')
        configs[0].endpointType == EndpointConfig.EndpointType.REST
        configs[0].categories == ['cat1','cat2']
        configs[0].scopes == ['scope1','scope2']
    }

    def 'it should write a config to a JSON string'() {
        given:
        EndpointConfig endpointConfig = new EndpointConfig('prop.name', EndpointConfig.EndpointType.REST, 'cat1 cat2'.split(),'scope1 scope2'.split());
        endpointConfig.setUrl(new URL('http://localhost:8080/asdf/'));
        System.out.println(endpointConfig.toJson());
        when:
        def json = endpointConfig.toJson()
        then:
        json == '{"propertyName":"prop.name","url":"http://localhost:8080/asdf/","endpointType":"REST","categories":["cat1","cat2"],"scopes":["scope1","scope2"]}'
    }
}
