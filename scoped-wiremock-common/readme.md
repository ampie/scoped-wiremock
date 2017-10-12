#Overview
This module contains code common to scoped-wiremock-client and scoped-wiremock-server. 
There is a requirement for scoped-wiremock-client to run on Android devices where it would
be impossible to import all the scoped-wiremock-server dependencies. This module therefore
contains all the code that may need to run on both the client and the server side, but without
bringing in the server side dependencies. The fact that the server-side AdminTask 
implementations reside in this project is rather counter-intuitive, but it was the only
way to deal with the dependencies.

#Testing
This project is never tested in isolation, but always in conjunction with either the 
scoped-wiremock-client or scoped-wiremock-client projects. The test folder therefore
only contains abstract test classes that need to be implemented for testing on the server
or the client. 
  