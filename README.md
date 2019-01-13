# drift

<img width="600" alt="drift_logo" src="./documentation/drift_logo.png" />

A Websocket-Http Tunnel written in kotlin that lets you access any HTTP Api on servers deployed behind firewall(s)/Nat(s)/Proxy

###What is drift?
Drift lets you deploy any application anywhere and expose it on internet via a websocket-http tunnels. 
So how does it look like?

![drift architecture](documentation/drift_architecture.png)


###How does it work ?
Preconditions:
Your client should have access to internet/drift server.
The drift client establishes a connection to the drift server over outbound http which is usually not blocked by firewalls.
The drift server then sends an upgrade, upgrading just established http connection to a websocket connection.
Now your drift server is ready to accept incoming traffic and forward it to you app server deployed via the websocket connection.
 
 
###Getting Started
The drift app can be run int following three modes:
 - SERVER
 - CLIENT
 - SERVER AND CLIENT
 
To run as server: 
    ```docker run -it --net=host -e "TUNNEL__SERVER__ENABLED=TRUE" ankur4u007/drift```

To run as client: 
    ```docker run -it --net=host -e "TUNNEL__CLIENT__ENABLED=TRUE" ankur4u007/drift```

To run as both the server and client: 
    ```docker run -it --net=host -e "TUNNEL__SERVER__ENABLED=TRUE" -e "TUNNEL__CLIENT__ENABLED=TRUE" ankur4u007/drift```

###Build and locally 
The app is developed using spring reactive and can be easily build and run with `gradlew`
 - To build: ```./gradlew clean build```
 - To run locally: ```./gradlew clean run```