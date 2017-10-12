#Overview
There is a requirement for this module to run on Android devices. This module 
therefore only contains Android compatible, 'dexable' dependencies. The apache http-commons
project cannot be used, and therefore a OkHttp client was used to implement the WireMock client.