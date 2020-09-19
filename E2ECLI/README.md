**suggest reading the end-to-end.rst file in the e2e_cli folder carefully firstly**

e2e_cli network config folder has been put into E2ECLI/src/main/resources/

after revise several files, it finally works!

Now let us manipulate the Fabric network with chaincode together wit java sdk.

####Open Terminal 1

$ `cd src/main/resources/e2e_cli`

$ `./network_setup.sh restart`

Now we have start a minimal Fabric network

####Open Terminal 2
enter the cli docker environment

$ `docker exec -it cli bash`

$ `script/startChaincode.sh`

####Open Terminal 3
$ `cd src/main/java/cn/edu/ncepu/e2ecli/application`
$ `./startApp.sh`
or run src/main/java/cn/edu/ncepu/e2ecli/application/e2ecliApp.java

