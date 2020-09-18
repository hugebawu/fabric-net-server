**suggest reading the end-to-end.rst file in the e2e_cli folder carefully firstly**

e2e_cli network config folder has been put into E2ECLI/src/main/resources/

after revise several files, it finally works!

Now let us manipulate the Fabric network with chaincode together wit java sdk.

$ `cd src/main/resources/e2e_cli`

$ `./network_setup.sh restart`

$ `./startChaincode.sh`
or run src/main/java/cn/edu/ncepu/e2ecli/chaincode/myChaincode.java

$ `./startChannel.sh`

$ `./startApp.sh`
or run src/main/java/cn/edu/ncepu/e2ecli/application/e2ecliApp.java

