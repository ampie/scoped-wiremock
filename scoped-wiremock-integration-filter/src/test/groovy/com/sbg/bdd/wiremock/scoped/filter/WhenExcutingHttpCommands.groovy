package com.sbg.bdd.wiremock.scoped.filter

import spock.lang.Specification

class WhenExcutingHttpCommands extends Specification {
    def 'it should read the error stream'(){
        given:
        def os = new ByteArrayOutputStream()
        def connection=Mock(HttpURLConnection){
            1 * setRequestMethod('POST')
            getOutputStream() >> os
            getErrorStream() >> new ByteArrayInputStream('oops'.bytes)
        }


        def command = new HttpCommand(connection, 'POST', 'Please write this')
        when:
        def output = command.execute()

        then:
        new String(os.toByteArray()) == 'Please write this'
        output == 'oops'
    }
    def 'it should read the response stream'(){

        given:
        def os = new ByteArrayOutputStream()
        def connection=Mock(HttpURLConnection){
            1 * setRequestMethod('POST')
            getOutputStream() >> os
            getInputStream() >> new ByteArrayInputStream('yay!'.bytes)
        }


        def command = new HttpCommand(connection, 'POST', 'Please write this')
        when:
        def output = command.execute()

        then:
        new String(os.toByteArray()) == 'Please write this'
        output == 'yay!'
    }
}
